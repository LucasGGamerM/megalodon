package org.joinmastodon.android.fragments.settings;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.UnifiedPushHelper;
import org.unifiedpush.android.connector.UnifiedPush;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class SettingsNotificationsFragment extends BaseSettingsFragment<Void>{
	private PushSubscription pushSubscription;
	private CheckableListItem<Void> pauseItem;
	private ListItem<Void> policyItem;
	private MergeRecyclerAdapter mergeAdapter;

	private HideableSingleViewRecyclerAdapter bannerAdapter;
	private ImageView bannerIcon;
	private TextView bannerText;
	private TextView bannerTitle;
	private Button bannerButton;

	private CheckableListItem<Void> mentionsItem, boostsItem, favoritesItem, followersItem, pollsItem;
	private List<CheckableListItem<Void>> typeItems;
	private boolean needUpdateNotificationSettings;
	private boolean notificationsAllowed=true;

	// MEGALODON
	private boolean useUnifiedPush = false;
	private boolean hasAnyUnifiedPushDistrib = false;
	private CheckableListItem<Void> uniformIconItem, deleteItem, onlyLatestItem, unifiedPushItem;
	private CheckableListItem<Void> postsItem, updateItem;

	// MOSHIDON
	private CheckableListItem<Void> swapBookmarkWithReblogItem;

	private AccountLocalPreferences lp;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_notifications);
		lp=AccountSessionManager.get(accountID).getLocalPreferences();

		getPushSubscription();
		useUnifiedPush=UnifiedPushHelper.isUnifiedPushEnabled(getContext());
		hasAnyUnifiedPushDistrib=UnifiedPushHelper.hasAnyDistributorInstalled(getContext());

		onDataLoaded(List.of(
				pauseItem=new CheckableListItem<>(getString(R.string.pause_all_notifications), getPauseItemSubtitle(), CheckableListItem.Style.SWITCH, false, R.drawable.ic_fluent_alert_snooze_24_regular, i->onPauseNotificationsClick(false)),
				policyItem=new ListItem<>(R.string.settings_notifications_policy, 0, R.drawable.ic_fluent_people_24_regular, this::onNotificationsPolicyClick, 0, true),

				mentionsItem=new CheckableListItem<>(R.string.notification_type_mentions_and_replies, 0, CheckableListItem.Style.CHECKBOX, pushSubscription.alerts.mention, R.drawable.ic_fluent_mention_24_regular, i->toggleCheckableItem(mentionsItem)),
				boostsItem=new CheckableListItem<>(R.string.notification_type_reblog, 0, CheckableListItem.Style.CHECKBOX, pushSubscription.alerts.reblog, R.drawable.ic_fluent_arrow_repeat_all_24_regular, i->toggleCheckableItem(boostsItem)),
				favoritesItem=new CheckableListItem<>(R.string.notification_type_favorite, 0, CheckableListItem.Style.CHECKBOX, pushSubscription.alerts.favourite, R.drawable.ic_fluent_star_24_regular, i->toggleCheckableItem(favoritesItem)),
				followersItem=new CheckableListItem<>(R.string.notification_type_follow, 0, CheckableListItem.Style.CHECKBOX, pushSubscription.alerts.follow, R.drawable.ic_fluent_person_add_24_regular, i->toggleCheckableItem(followersItem)),
				pollsItem=new CheckableListItem<>(R.string.notification_type_poll, 0, CheckableListItem.Style.CHECKBOX, pushSubscription.alerts.poll, R.drawable.ic_fluent_poll_24_regular, i->toggleCheckableItem(pollsItem)),
				updateItem=new CheckableListItem<>(R.string.sk_notification_type_update, 0, CheckableListItem.Style.CHECKBOX, pushSubscription.alerts.update, R.drawable.ic_fluent_history_24_regular, i->toggleCheckableItem(updateItem)),
				postsItem=new CheckableListItem<>(R.string.sk_notification_type_posts, 0, CheckableListItem.Style.CHECKBOX, pushSubscription.alerts.status, R.drawable.ic_fluent_chat_24_regular, i->toggleCheckableItem(postsItem), true),

				uniformIconItem=new CheckableListItem<>(R.string.sk_settings_uniform_icon_for_notifications, R.string.mo_setting_uniform_summary, CheckableListItem.Style.SWITCH, GlobalUserPreferences.uniformNotificationIcon, R.drawable.ic_ntf_logo, i->toggleCheckableItem(uniformIconItem)),
				swapBookmarkWithReblogItem=new CheckableListItem<>(R.string.mo_swap_bookmark_with_reblog, R.string.mo_swap_bookmark_with_reblog_summary, CheckableListItem.Style.SWITCH, GlobalUserPreferences.swapBookmarkWithBoostAction, R.drawable.ic_boost, i->toggleCheckableItem(swapBookmarkWithReblogItem)),
				deleteItem=new CheckableListItem<>(R.string.sk_settings_enable_delete_notifications, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.enableDeleteNotifications, R.drawable.ic_fluent_mail_inbox_dismiss_24_regular, i->toggleCheckableItem(deleteItem)),
				onlyLatestItem=new CheckableListItem<>(R.string.sk_settings_single_notification, 0, CheckableListItem.Style.SWITCH, lp.keepOnlyLatestNotification, R.drawable.ic_fluent_convert_range_24_regular, i->toggleCheckableItem(onlyLatestItem), true),
				unifiedPushItem=new CheckableListItem<>(R.string.sk_settings_unifiedpush, 0, CheckableListItem.Style.SWITCH, useUnifiedPush, R.drawable.ic_fluent_alert_arrow_up_24_regular, i->onUnifiedPushClick(), true)
		));

		//only enable when distributors, who can receive notifications, are available
		unifiedPushItem.isEnabled=hasAnyUnifiedPushDistrib;
		if (!unifiedPushItem.isEnabled) {
			unifiedPushItem.subtitleRes=R.string.sk_settings_unifiedpush_no_distributor_body;
		}

		typeItems=List.of(mentionsItem, boostsItem, favoritesItem, followersItem, pollsItem, updateItem, postsItem);
		pauseItem.checkedChangeListener=checked->onPauseNotificationsClick(true);
		unifiedPushItem.checkedChangeListener=checked->onUnifiedPushClick();
		updatePolicyItem(null);
		updatePauseItem();
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void onHidden(){
		super.onHidden();
		PushSubscription ps=getPushSubscription();
		needUpdateNotificationSettings|=mentionsItem.checked!=ps.alerts.mention
				|| boostsItem.checked!=ps.alerts.reblog
				|| favoritesItem.checked!=ps.alerts.favourite
				|| followersItem.checked!=ps.alerts.follow
				|| pollsItem.checked!=ps.alerts.poll;
		GlobalUserPreferences.uniformNotificationIcon=uniformIconItem.checked;
		GlobalUserPreferences.enableDeleteNotifications=deleteItem.checked;
		GlobalUserPreferences.swapBookmarkWithBoostAction=swapBookmarkWithReblogItem.checked;
		GlobalUserPreferences.save();
		lp.keepOnlyLatestNotification=onlyLatestItem.checked;
		lp.save();
		if(needUpdateNotificationSettings && (PushSubscriptionManager.arePushNotificationsAvailable() || useUnifiedPush)){
			ps.alerts.mention=mentionsItem.checked;
			ps.alerts.reblog=boostsItem.checked;
			ps.alerts.favourite=favoritesItem.checked;
			ps.alerts.follow=followersItem.checked;
			ps.alerts.poll=pollsItem.checked;
			ps.alerts.status=postsItem.checked;
			ps.alerts.update=updateItem.checked;
			AccountSessionManager.getInstance().getAccount(accountID).getPushSubscriptionManager().updatePushSettings(pushSubscription);
		}
	}

	@Override
	protected void onShown(){
		super.onShown();
		boolean allowed=areNotificationsAllowed();
		PushSubscription ps=getPushSubscription();
		if(allowed!=notificationsAllowed){
			notificationsAllowed=allowed;
			updateBanner();
			pauseItem.isEnabled=allowed;
			policyItem.isEnabled=allowed;
			rebindItem(pauseItem);
			rebindItem(policyItem);
			for(CheckableListItem<Void> item:typeItems){
				item.isEnabled=allowed && ps.policy!=PushSubscription.Policy.NONE;
				rebindItem(item);
			}
		}
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		View banner=getActivity().getLayoutInflater().inflate(R.layout.item_settings_banner, list, false);
		bannerTitle=banner.findViewById(R.id.title);
		bannerText=banner.findViewById(R.id.text);
		bannerIcon=banner.findViewById(R.id.icon);
		bannerButton=banner.findViewById(R.id.button);
		bannerAdapter=new HideableSingleViewRecyclerAdapter(banner);
		bannerAdapter.setVisible(false);
		banner.findViewById(R.id.button2).setVisibility(View.GONE);
		banner.findViewById(R.id.title).setVisibility(View.GONE);

		mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(bannerAdapter);
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	@Override
	protected int indexOfItemsAdapter(){
		return mergeAdapter.getPositionForAdapter(itemsAdapter);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		updateBanner();
	}

	private boolean areNotificationsAllowed(){
		return Build.VERSION.SDK_INT<Build.VERSION_CODES.N || getActivity().getSystemService(NotificationManager.class).areNotificationsEnabled();
	}

	private PushSubscription getPushSubscription(){
		if(pushSubscription!=null)
			return pushSubscription;
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		if(session.pushSubscription==null){
			pushSubscription=new PushSubscription();
			pushSubscription.alerts=PushSubscription.Alerts.ofAll();
		}else{
			pushSubscription=session.pushSubscription.clone();
		}
		return pushSubscription;
	}

	private String getPauseItemSubtitle(){
		return getString(R.string.pause_notifications_off);
	}

	private void resumePausedNotifications(){
		AccountSessionManager.get(accountID).getLocalPreferences().setNotificationsPauseEndTime(0);
		updatePauseItem();
	}

	private void openSystemNotificationSettings(){
		Intent intent;
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O){
			intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getActivity().getPackageName(), null));
		}else{
			intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
			intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private void onPauseNotificationsClick(boolean fromSwitch){
		long time=AccountSessionManager.get(accountID).getLocalPreferences().getNotificationsPauseEndTime();
		if(time>System.currentTimeMillis() && fromSwitch){
			resumePausedNotifications();
			return;
		}
		int[] durationOptions={
				1800,
				3600,
				12*3600,
				24*3600,
				3*24*3600,
				7*24*3600
		};
		int[] selectedOption={0};
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.pause_all_notifications_title)
				.setSupportingText(time>System.currentTimeMillis() ? getString(R.string.pause_notifications_ends, UiUtils.formatRelativeTimestampAsMinutesAgo(getActivity(), Instant.ofEpochMilli(time), false)) : null)
				.setSingleChoiceItems((String[])Arrays.stream(durationOptions).mapToObj(d->UiUtils.formatDuration(getActivity(), d)).toArray(String[]::new), -1, (dlg, item)->{
					if(selectedOption[0]==0){
						((AlertDialog)dlg).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}
					selectedOption[0]=durationOptions[item];
				})
				.setPositiveButton(R.string.ok, (dlg, item)->AccountSessionManager.get(accountID).getLocalPreferences().setNotificationsPauseEndTime(System.currentTimeMillis()+selectedOption[0]*1000L))
				.setNegativeButton(R.string.cancel, null)
				.show();
		alert.setOnDismissListener(dialog->updatePauseItem());
		alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
	}

	private void onNotificationsPolicyClick(ListItem<?> item_){
		String[] items=Stream.of(
				R.string.notifications_policy_anyone,
				R.string.notifications_policy_followed,
				R.string.notifications_policy_follower,
				R.string.notifications_policy_no_one
		).map(this::getString).toArray(String[]::new);
		int[] selectedItem={getPushSubscription().policy.ordinal()};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.settings_notifications_policy)
				.setSingleChoiceItems(items, selectedItem[0], (dlg, which)->selectedItem[0]=which)
				.setPositiveButton(R.string.ok, (dlg, which)->{
					PushSubscription.Policy prevValue=getPushSubscription().policy;
					PushSubscription.Policy newValue=PushSubscription.Policy.values()[selectedItem[0]];
					if(prevValue==newValue)
						return;
					getPushSubscription().policy=newValue;
					updatePolicyItem(prevValue);
					needUpdateNotificationSettings=true;
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void updatePolicyItem(PushSubscription.Policy prevValue){
		policyItem.subtitleRes=switch(getPushSubscription().policy){
			case ALL -> R.string.notifications_policy_anyone;
			case FOLLOWED -> R.string.notifications_policy_followed;
			case FOLLOWER -> R.string.notifications_policy_follower;
			case NONE -> R.string.notifications_policy_no_one;
		};
		rebindItem(policyItem);
		if(pushSubscription.policy==PushSubscription.Policy.NONE || prevValue==PushSubscription.Policy.NONE){
			for(CheckableListItem<Void> item:typeItems){
				item.checked=item.isEnabled=prevValue==PushSubscription.Policy.NONE;
				rebindItem(item);
			}
		}
	}

	private void updatePauseItem(){
		long time=AccountSessionManager.get(accountID).getLocalPreferences().getNotificationsPauseEndTime();
		if(time<System.currentTimeMillis()){
			pauseItem.subtitle=getString(R.string.pause_notifications_off);
			pauseItem.checked=false;
		}else{
			pauseItem.subtitle=getString(R.string.pause_notifications_ends, UiUtils.formatRelativeTimestampAsMinutesAgo(getActivity(), Instant.ofEpochMilli(time), false));
			pauseItem.checked=true;
		}
		rebindItem(pauseItem);
		updateBanner();
	}

	private void updateBanner(){
		if(bannerAdapter==null)
			return;
		long pauseTime=AccountSessionManager.get(accountID).getLocalPreferences().getNotificationsPauseEndTime();
		if(!areNotificationsAllowed()){
			bannerAdapter.setVisible(true);
			bannerIcon.setImageResource(R.drawable.ic_fluent_alert_badge_24_regular);
			bannerText.setText(R.string.notifications_disabled_in_system);
			bannerButton.setText(R.string.open_system_notification_settings);
			bannerButton.setOnClickListener(v->openSystemNotificationSettings());
		}else if(BuildConfig.BUILD_TYPE.equals("fdroidRelease") && useUnifiedPush){
			bannerAdapter.setVisible(true);
			bannerIcon.setImageResource(R.drawable.ic_fluent_warning_24_filled);
			bannerTitle.setVisibility(View.VISIBLE);
			bannerTitle.setText(R.string.mo_settings_unifiedpush_warning);
			if(!hasAnyUnifiedPushDistrib) {
				bannerText.setText(R.string.mo_settings_unifiedpush_warning_no_distributors);
				bannerButton.setText(R.string.info);
				bannerButton.setOnClickListener(v->UiUtils.launchWebBrowser(getContext(), "https://unifiedpush.org/"));
			} else {
				bannerText.setText(R.string.mo_settings_unifiedpush_warning_disabled);
				bannerButton.setText(R.string.mo_settings_unifiedpush_enable);
				bannerButton.setOnClickListener(v->onUnifiedPushClick());
			}
		}else if(pauseTime>System.currentTimeMillis()){
			bannerAdapter.setVisible(true);
			bannerIcon.setImageResource(R.drawable.ic_fluent_alert_snooze_24_regular);
			bannerText.setText(getString(R.string.pause_notifications_banner, UiUtils.formatRelativeTimestampAsMinutesAgo(getActivity(), Instant.ofEpochMilli(pauseTime), false)));
			bannerButton.setText(R.string.resume_notifications_now);
			bannerButton.setOnClickListener(v->resumePausedNotifications());
		}else{
			bannerAdapter.setVisible(false);
		}
	}

	private void onUnifiedPushClick(){
		if(!useUnifiedPush){
			List<String> distributors = UnifiedPush.getDistributors(getContext());
			showUnifiedPushRegisterDialog(distributors);
			return;
		}
		UnifiedPushHelper.unregisterAllAccounts(getContext());
		unifiedPushItem.toggle();
		rebindItem(unifiedPushItem);
		useUnifiedPush = false;
	}

	private void showUnifiedPushRegisterDialog(List<String> distributors){
		new M3AlertDialogBuilder(getContext()).setTitle(R.string.sk_settings_unifiedpush_choose).setItems(distributors.toArray(String[]::new),
				(dialog, which)->{
					String userDistrib = distributors.get(which);
					UnifiedPush.saveDistributor(getContext(), userDistrib);
					UnifiedPushHelper.registerAllAccounts(getContext());
					unifiedPushItem.toggle();
					rebindItem(unifiedPushItem);
					useUnifiedPush = true;
				}).setOnCancelListener(d->rebindItem(unifiedPushItem)).show();
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return base.path("/settings/preferences/notifications").build();
	}
}