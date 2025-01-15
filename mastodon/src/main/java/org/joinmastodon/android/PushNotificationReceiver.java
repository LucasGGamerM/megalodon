package org.joinmastodon.android;

import static org.joinmastodon.android.GlobalUserPreferences.PrefixRepliesMode.ALWAYS;
import static org.joinmastodon.android.GlobalUserPreferences.PrefixRepliesMode.TO_OTHERS;
import static org.joinmastodon.android.GlobalUserPreferences.getPrefs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.notifications.GetNotificationByID;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.SetStatusBookmarked;
import org.joinmastodon.android.api.requests.statuses.SetStatusFavorited;
import org.joinmastodon.android.api.requests.statuses.SetStatusReblogged;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.NotificationAction;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.PushNotification;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class PushNotificationReceiver extends BroadcastReceiver{
	private static final String TAG="PushNotificationReceive";

	public static final int NOTIFICATION_ID=178;
	private static final String ACTION_KEY_TEXT_REPLY = "ACTION_KEY_TEXT_REPLY";

	private static final int SUMMARY_ID = 791;
	private static int notificationId = 0;
	private static final Map<String, Integer> notificationIdsForAccounts = new HashMap<>();

	@Override
	public void onReceive(Context context, Intent intent){
		UiUtils.setUserPreferredTheme(context);
		if(BuildConfig.DEBUG){
			Log.e(TAG, "received: "+intent);
			Bundle extras=intent.getExtras();
			for(String key : extras.keySet()){
				Log.i(TAG, key+" -> "+extras.get(key));
			}
		}
		if("com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())){
			String k=intent.getStringExtra("k");
			String p=intent.getStringExtra("p");
			String s=intent.getStringExtra("s");
			String pushAccountID=intent.getStringExtra("x");
			if(!TextUtils.isEmpty(pushAccountID) && !TextUtils.isEmpty(k) && !TextUtils.isEmpty(p) && !TextUtils.isEmpty(s)){
				MastodonAPIController.runInBackground(()->{
					try{
						List<AccountSession> accounts=AccountSessionManager.getInstance().getLoggedInAccounts();
						AccountSession account=null;
						for(AccountSession acc:accounts){
							if(pushAccountID.equals(acc.pushAccountID)){
								account=acc;
								break;
							}
						}
						if(account==null){
							Log.w(TAG, "onReceive: account for id '"+pushAccountID+"' not found");
							return;
						}
						if(account.getLocalPreferences().getNotificationsPauseEndTime()>System.currentTimeMillis()){
							Log.i(TAG, "onReceive: dropping notification because user has paused notifications for this account");
							return;
						}
						String accountID=account.getID();
						PushNotification pn=AccountSessionManager.getInstance().getAccount(accountID).getPushSubscriptionManager().decryptNotification(k, p, s);
						new GetNotificationByID(pn.notificationId)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(org.joinmastodon.android.model.Notification result){
										MastodonAPIController.runInBackground(()->PushNotificationReceiver.this.notify(context, pn, accountID, result));
									}

									@Override
									public void onError(ErrorResponse error){
										MastodonAPIController.runInBackground(()->PushNotificationReceiver.this.notify(context, pn, accountID, null));
									}
								})
								.exec(accountID);
					}catch(Exception x){
						Log.w(TAG, x);
					}
				});
			}else{
				Log.w(TAG, "onReceive: invalid push notification format");
			}
		}
		if(intent.getBooleanExtra("fromNotificationAction", false)){
			String accountID=intent.getStringExtra("accountID");
			int notificationId=intent.getIntExtra("notificationId", -1);

			if (notificationId >= 0){
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.cancel(accountID, notificationId);
			}

			if(intent.hasExtra("notification")){
				org.joinmastodon.android.model.Notification notification=Parcels.unwrap(intent.getParcelableExtra("notification"));

				String statusID = null;
				if(notification != null && notification.status != null)
					statusID=notification.status.id;

				if (statusID != null) {
					AccountSessionManager accountSessionManager = AccountSessionManager.getInstance();
					Preferences preferences = accountSessionManager.getAccount(accountID).preferences;

					switch (NotificationAction.values()[intent.getIntExtra("notificationAction", 0)]) {
						case FAVORITE -> new SetStatusFavorited(statusID, true).exec(accountID);
						case BOOKMARK -> new SetStatusBookmarked(statusID, true).exec(accountID);
						case BOOST -> new SetStatusReblogged(notification.status.id, true, preferences.postingDefaultVisibility).exec(accountID);
						case UNBOOST -> new SetStatusReblogged(notification.status.id, false, preferences.postingDefaultVisibility).exec(accountID);
						case REPLY -> handleReplyAction(context, accountID, intent, notification, notificationId, preferences);
						case FOLLOW_BACK -> new SetAccountFollowed(notification.account.id, true, true, false).exec(accountID);
						default -> Log.w(TAG, "onReceive: Failed to get NotificationAction");
					}
				}
			}else{
				Log.e(TAG, "onReceive: Failed to load notification");
			}
		}
	}

	public void notifyUnifiedPush(Context context, AccountSession account, org.joinmastodon.android.model.Notification notification) {
		// push notifications are only created from the official push notification, so we create a fake from by transforming the notification
		PushNotificationReceiver.this.notify(context, PushNotification.fromNotification(context, account, notification), account.getID(), notification);
	}

	void notify(Context context, PushNotification pn, String accountID, org.joinmastodon.android.model.Notification notification){
		NotificationManager nm=context.getSystemService(NotificationManager.class);
		AccountSession session=AccountSessionManager.get(accountID);
		Account self=session.self;
		String accountName="@"+self.username+"@"+AccountSessionManager.getInstance().getAccount(accountID).domain;
		Notification.Builder builder;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			boolean hasGroup=false;
			List<NotificationChannelGroup> channelGroups=nm.getNotificationChannelGroups();
			for(NotificationChannelGroup group:channelGroups){
				if(group.getId().equals(accountID)){
					hasGroup=true;
					break;
				}
			}
			if(!hasGroup){
				NotificationChannelGroup group=new NotificationChannelGroup(accountID, accountName);
				nm.createNotificationChannelGroup(group);
				List<NotificationChannel> channels=Arrays.stream(PushNotification.Type.values())
						.map(type->{
							NotificationChannel channel=new NotificationChannel(accountID+"_"+type, context.getString(type.localizedName), NotificationManager.IMPORTANCE_DEFAULT);
							channel.setLightColor(context.getColor(R.color.primary_700));
							channel.enableLights(true);
							channel.setGroup(accountID);
							return channel;
						})
						.collect(Collectors.toList());
				nm.createNotificationChannels(channels);
			}
			builder=new Notification.Builder(context, accountID+"_"+pn.notificationType);
		}else{
			builder=new Notification.Builder(context)
					.setPriority(Notification.PRIORITY_DEFAULT)
					.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
		}
		Drawable avatar=ImageCache.getInstance(context).get(new UrlImageLoaderRequest(pn.icon, V.dp(50), V.dp(50)));
		Intent contentIntent=new Intent(context, MainActivity.class);
		contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		contentIntent.putExtra("fromNotification", true);
		contentIntent.putExtra("accountID", accountID);
		if(notification!=null){
			contentIntent.putExtra("notification", Parcels.wrap(notification));
		}
		builder.setContentTitle(pn.title)
				.setContentText(pn.body)
				.setStyle(new Notification.BigTextStyle().bigText(pn.body))
				.setSmallIcon(R.drawable.ic_ntf_logo)
				.setContentIntent(PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
				.setWhen(notification==null ? System.currentTimeMillis() : notification.createdAt.toEpochMilli())
				.setShowWhen(true)
				.setCategory(Notification.CATEGORY_SOCIAL)
				.setAutoCancel(true)
				.setLights(context.getColor(R.color.primary_700), 500, 1000)
				.setColor(context.getColor(R.color.shortcut_icon_background));

		if (!GlobalUserPreferences.uniformNotificationIcon) {
			builder.setSmallIcon(switch (pn.notificationType) {
				case FAVORITE -> GlobalUserPreferences.likeIcon ? R.drawable.ic_fluent_heart_24_filled : R.drawable.ic_fluent_star_24_filled;
				case REBLOG -> R.drawable.ic_fluent_arrow_repeat_all_24_filled;
				case FOLLOW -> R.drawable.ic_fluent_person_add_24_filled;
				case MENTION -> R.drawable.ic_fluent_mention_24_filled;
				case POLL -> R.drawable.ic_fluent_poll_24_filled;
				case STATUS -> R.drawable.ic_fluent_chat_24_filled;
				case UPDATE -> R.drawable.ic_fluent_history_24_filled;
				case REPORT -> R.drawable.ic_fluent_warning_24_filled;
				case SIGN_UP -> R.drawable.ic_fluent_person_available_24_filled;
			});
		}

		if(avatar!=null){
			builder.setLargeIcon(UiUtils.getBitmapFromDrawable(avatar));
		}
		if(AccountSessionManager.getInstance().getLoggedInAccounts().size()>1){
			builder.setSubText(accountName);
		}

		int id;
		if(session.getLocalPreferences().keepOnlyLatestNotification){
			if(notificationIdsForAccounts.containsKey(accountID)){
				// we overwrite the existing notification
				id=notificationIdsForAccounts.get(accountID);
			}else{
				// there's no existing notification, so we increment
				id=notificationId++;
				// and store the notification id for this account
				notificationIdsForAccounts.put(accountID, id);
			}
		}else{
			// we don't want to overwrite anything, therefore incrementing
			id=notificationId++;
		}

		if (notification != null){
			switch (pn.notificationType){
				case MENTION, STATUS -> {
					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
						builder.addAction(buildReplyAction(context, id, accountID, notification));
					}
					builder.addAction(buildNotificationAction(context, id, accountID, notification,  context.getString(R.string.button_favorite), NotificationAction.FAVORITE));
					if(GlobalUserPreferences.swapBookmarkWithBoostAction){
						if(notification.status.visibility != StatusPrivacy.DIRECT) {
							builder.addAction(buildNotificationAction(context, id, accountID, notification,  context.getString(R.string.button_reblog), NotificationAction.BOOST));
						}else{
							// This is just so there is a bookmark action if you cannot reblog the toot
							builder.addAction(buildNotificationAction(context, id, accountID, notification, context.getString(R.string.add_bookmark), NotificationAction.BOOKMARK));
						}
					} else {
						builder.addAction(buildNotificationAction(context, id, accountID, notification, context.getString(R.string.add_bookmark), NotificationAction.BOOKMARK));
					}
				}
				case UPDATE -> {
					if(notification.status.reblogged)
						builder.addAction(buildNotificationAction(context, id, accountID, notification,  context.getString(R.string.sk_undo_reblog), NotificationAction.UNBOOST));
				}
				case FOLLOW -> {
					builder.addAction(buildNotificationAction(context, id, accountID, notification, context.getString(R.string.follow_back), NotificationAction.FOLLOW_BACK));
				}
			}
		}

		nm.notify(accountID, id, builder.build());
	}

	private Notification.Action buildNotificationAction(Context context, int notificationId, String accountID, org.joinmastodon.android.model.Notification notification, String title, NotificationAction action){
		Intent notificationIntent=new Intent(context, PushNotificationReceiver.class);
		notificationIntent.putExtra("notificationId", notificationId);
		notificationIntent.putExtra("fromNotificationAction", true);
		notificationIntent.putExtra("accountID", accountID);
		notificationIntent.putExtra("notificationAction", action.ordinal());
		notificationIntent.putExtra("notification", Parcels.wrap(notification));
		PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

		return new Notification.Action.Builder(null, title, actionPendingIntent).build();
	}

	private Notification.Action buildReplyAction(Context context, int notificationId, String accountID, org.joinmastodon.android.model.Notification notification){
		String replyLabel = context.getResources().getString(R.string.button_reply);
		RemoteInput remoteInput = new RemoteInput.Builder(ACTION_KEY_TEXT_REPLY)
				.setLabel(replyLabel)
				.build();

		Intent notificationIntent=new Intent(context, PushNotificationReceiver.class);
		notificationIntent.putExtra("notificationId", notificationId);
		notificationIntent.putExtra("fromNotificationAction", true);
		notificationIntent.putExtra("accountID", accountID);
		notificationIntent.putExtra("notificationAction", NotificationAction.REPLY.ordinal());
		notificationIntent.putExtra("notification", Parcels.wrap(notification));

		int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
		PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent,flags);
		return new Notification.Action.Builder(null, replyLabel, replyPendingIntent).addRemoteInput(remoteInput).build();
	}

	private void handleReplyAction(Context context, String accountID, Intent intent, org.joinmastodon.android.model.Notification notification, int notificationId, Preferences preferences) {
		Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
		if (remoteInput == null) {
			Log.e(TAG, "handleReplyAction: Could not get reply input");
			return;
		}
		CharSequence input = remoteInput.getCharSequence(ACTION_KEY_TEXT_REPLY);

		// copied from ComposeFragment - TODO: generalize?
		ArrayList<String> mentions=new ArrayList<>();
		Status status = notification.status;
		String ownID=AccountSessionManager.getInstance().getAccount(accountID).self.id;
		if(!status.account.id.equals(ownID))
			mentions.add('@'+status.account.acct);
		for(Mention mention:status.mentions){
			if(mention.id.equals(ownID))
				continue;
			String m='@'+mention.acct;
			if(!mentions.contains(m))
				mentions.add(m);
		}
		String initialText=mentions.isEmpty() ? "" : TextUtils.join(" ", mentions)+" ";

		CreateStatus.Request req=new CreateStatus.Request();
		req.status = initialText + input.toString();
		req.language = notification.status.language;
		req.visibility = (notification.status.visibility == StatusPrivacy.PUBLIC && GlobalUserPreferences.defaultToUnlistedReplies ? StatusPrivacy.UNLISTED : notification.status.visibility);
		req.inReplyToId = notification.status.id;

		if (notification.status.hasSpoiler() &&
				(GlobalUserPreferences.prefixReplies == ALWAYS
						|| (GlobalUserPreferences.prefixReplies == TO_OTHERS && !ownID.equals(notification.status.account.id)))
				&& !notification.status.spoilerText.startsWith("re: ")) {
			req.spoilerText = "re: " + notification.status.spoilerText;
		}

		new CreateStatus(req, UUID.randomUUID().toString()).setCallback(new Callback<>() {
			@Override
			public void onSuccess(Status status) {
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification.Builder builder = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
						new Notification.Builder(context, accountID+"_"+notification.type) :
						new Notification.Builder(context)
								.setPriority(Notification.PRIORITY_DEFAULT)
								.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

				notification.status = status;
				Intent contentIntent=new Intent(context, MainActivity.class);
				contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				contentIntent.putExtra("fromNotification", true);
				contentIntent.putExtra("accountID", accountID);
				contentIntent.putExtra("notification", Parcels.wrap(notification));

				Notification repliedNotification = builder.setSmallIcon(R.drawable.ic_ntf_logo)
						.setContentTitle(context.getString(R.string.sk_notification_action_replied, notification.status.account.displayName))
						.setContentText(status.getStrippedText())
						.setCategory(Notification.CATEGORY_SOCIAL)
						.setContentIntent(PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
						.build();
				notificationManager.notify(accountID, notificationId, repliedNotification);
			}

			@Override
			public void onError(ErrorResponse errorResponse) {

			}
		}).exec(accountID);
	}
}
