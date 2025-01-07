package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import androidx.annotation.StringRes;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusDisplaySettingsChangedEvent;
import org.joinmastodon.android.fragments.HasAccountID;
import org.joinmastodon.android.model.ContentType;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.grishka.appkit.Nav;

public class SettingsInstanceFragment extends BaseSettingsFragment<Void> implements HasAccountID{
	private CheckableListItem<Void> contentTypesItem, emojiReactionsItem, localOnlyItem, glitchModeItem;
	private ListItem<Void> defaultContentTypeItem, showEmojiReactionsItem;
	private AccountLocalPreferences lp;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.sk_settings_instance);
		AccountSession s=AccountSessionManager.get(accountID);
		lp=s.getLocalPreferences();
		ArrayList<ListItem<Void>> items=new ArrayList<>(List.of(
				new ListItem<>(AccountSessionManager.get(accountID).domain, getString(R.string.settings_server_explanation), R.drawable.ic_fluent_server_24_regular, this::onServerClick),
				new ListItem<>(R.string.sk_settings_profile, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/settings/profile")),
				new ListItem<>(R.string.sk_settings_posting, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/settings/preferences/other")),
				new ListItem<>(R.string.sk_settings_auth, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/auth/edit"), 0, true),
				emojiReactionsItem=new CheckableListItem<>(R.string.sk_settings_emoji_reactions, R.string.sk_settings_emoji_reactions_explanation, CheckableListItem.Style.SWITCH, lp.emojiReactionsEnabled, R.drawable.ic_fluent_emoji_laugh_24_regular, i->onEmojiReactionsClick()),
				showEmojiReactionsItem=new ListItem<>(R.string.sk_settings_show_emoji_reactions, getShowEmojiReactionsString(), R.drawable.ic_fluent_emoji_24_regular, this::onShowEmojiReactionsClick, 0, true),
				localOnlyItem=new CheckableListItem<>(R.string.sk_settings_support_local_only, R.string.sk_settings_local_only_explanation, CheckableListItem.Style.SWITCH, lp.localOnlySupported, R.drawable.ic_fluent_eye_24_regular, i->onLocalOnlyClick()),
				glitchModeItem=new CheckableListItem<>(R.string.sk_settings_glitch_instance, R.string.sk_settings_glitch_mode_explanation, CheckableListItem.Style.SWITCH, lp.glitchInstance, R.drawable.ic_fluent_eye_24_filled, i->toggleCheckableItem(glitchModeItem))
		));
		if(!isInstanceIceshrimp()){
			items.add(4, contentTypesItem=new CheckableListItem<>(R.string.sk_settings_content_types, R.string.sk_settings_content_types_explanation, CheckableListItem.Style.SWITCH, lp.contentTypesEnabled, R.drawable.ic_fluent_text_edit_style_24_regular, i->onContentTypeClick()));
			items.add(5, defaultContentTypeItem=new ListItem<>(R.string.sk_settings_default_content_type, lp.defaultContentType.getName(), R.drawable.ic_fluent_text_bold_24_regular, this::onDefaultContentTypeClick, 0, true));
			contentTypesItem.checkedChangeListener=checked->onContentTypeClick();
			defaultContentTypeItem.isEnabled=contentTypesItem.checked;
		}
		emojiReactionsItem.checkedChangeListener=checked->onEmojiReactionsClick();
		showEmojiReactionsItem.isEnabled=emojiReactionsItem.checked;
		localOnlyItem.checkedChangeListener=checked->onLocalOnlyClick();
		glitchModeItem.isEnabled=localOnlyItem.checked;
		onDataLoaded(items);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void onHidden(){
		super.onHidden();
		if(contentTypesItem!=null)
			lp.contentTypesEnabled=contentTypesItem.checked;
		lp.emojiReactionsEnabled=emojiReactionsItem.checked;
		lp.localOnlySupported=localOnlyItem.checked;
		lp.glitchInstance=glitchModeItem.checked;
		lp.save();
		E.post(new StatusDisplaySettingsChangedEvent(accountID));
	}

	private void onServerClick(ListItem<?> item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), SettingsServerFragment.class, args);
	}

	private void onContentTypeClick(){
		toggleCheckableItem(contentTypesItem);
		defaultContentTypeItem.isEnabled=contentTypesItem.checked;
		resetDefaultContentType();
		rebindItem(defaultContentTypeItem);
	}

	private void resetDefaultContentType(){
		lp.defaultContentType=defaultContentTypeItem.isEnabled
				? isInstanceIceshrimp() ? ContentType.MISSKEY_MARKDOWN
				: ContentType.PLAIN : ContentType.UNSPECIFIED;
		defaultContentTypeItem.subtitleRes=lp.defaultContentType.getName();
	}

	private void onDefaultContentTypeClick(ListItem<?> item_){
		List<ContentType> supportedContentTypes=Arrays.stream(ContentType.values())
				.filter(t->t.supportedByInstance(getInstance().orElse(null)))
				.collect(Collectors.toList());
		int selected=supportedContentTypes.indexOf(lp.defaultContentType);
		int[] newSelected={selected};
		String[] names=supportedContentTypes.stream()
				.map(ContentType::getName)
				.map(this::getString)
				.toArray(String[]::new);

		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.sk_settings_default_content_type)
				.setSingleChoiceItems(names,
						selected, (dlg, item)->newSelected[0]=item)
				.setPositiveButton(R.string.ok, (dlg, item)->{
					ContentType type=supportedContentTypes.get(newSelected[0]);
					lp.defaultContentType=type;
					defaultContentTypeItem.subtitleRes=type.getName();
					rebindItem(defaultContentTypeItem);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onShowEmojiReactionsClick(ListItem<?> item_){
		int selected=lp.showEmojiReactions.ordinal();
		int[] newSelected={selected};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.sk_settings_show_emoji_reactions)
				.setSingleChoiceItems((String[]) IntStream.of(R.string.sk_settings_show_emoji_reactions_hide_empty, R.string.sk_settings_show_emoji_reactions_only_opened, R.string.sk_settings_show_emoji_reactions_always).mapToObj(this::getString).toArray(String[]::new),
						selected, (dlg, item)->newSelected[0]=item)
				.setPositiveButton(R.string.ok, (dlg, item)->{
					lp.showEmojiReactions=AccountLocalPreferences.ShowEmojiReactions.values()[newSelected[0]];
					showEmojiReactionsItem.subtitleRes=getShowEmojiReactionsString();
					rebindItem(showEmojiReactionsItem);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private @StringRes int getShowEmojiReactionsString(){
		return switch(lp.showEmojiReactions){
			case HIDE_EMPTY -> R.string.sk_settings_show_emoji_reactions_hide_empty;
			case ONLY_OPENED -> R.string.sk_settings_show_emoji_reactions_only_opened;
			case ALWAYS -> R.string.sk_settings_show_emoji_reactions_always;
		};
	}

	private void onEmojiReactionsClick(){
		toggleCheckableItem(emojiReactionsItem);
		showEmojiReactionsItem.isEnabled=emojiReactionsItem.checked;
		rebindItem(showEmojiReactionsItem);
	}

	private void onLocalOnlyClick(){
		toggleCheckableItem(localOnlyItem);
		glitchModeItem.checked=localOnlyItem.checked && !isInstanceAkkoma();
		glitchModeItem.isEnabled=localOnlyItem.checked;
		rebindItem(glitchModeItem);
	}

	@Override
	public String getAccountID(){
		return accountID;
	}
}
