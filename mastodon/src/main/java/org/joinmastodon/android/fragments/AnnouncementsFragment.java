package org.joinmastodon.android.fragments;

import static java.util.stream.Collectors.toList;

import android.app.Activity;
import android.text.TextUtils;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.announcements.GetAnnouncements;
import org.joinmastodon.android.api.mastodon.session.AccountSession;
import org.joinmastodon.android.api.mastodon.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Announcement;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class AnnouncementsFragment extends BaseStatusListFragment<Announcement> {
	private Instance instance;
	private AccountSession session;
	private List<String> unreadIDs = null;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.sk_announcements);
		session = AccountSessionManager.getInstance().getAccount(accountID);
		instance = AccountSessionManager.getInstance().getInstanceInfo(session.domain);
		loadData();
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Announcement a) {
		if(TextUtils.isEmpty(a.content)) return List.of();
		Account instanceUser = new Account();
		instanceUser.id = instanceUser.acct = instanceUser.username = session.domain;
		instanceUser.displayName = instance.title;
		instanceUser.url = "https://"+session.domain+"/about";
		instanceUser.avatar = instanceUser.avatarStatic = instance.thumbnail;
		instanceUser.emojis = List.of();
		Status fakeStatus = a.toStatus();
		TextStatusDisplayItem textItem = new TextStatusDisplayItem(a.id, HtmlParser.parse(a.content, a.emojis, a.mentions, a.tags, accountID), this, fakeStatus, true);
		textItem.textSelectable = true;
		return List.of(
				HeaderStatusDisplayItem.fromAnnouncement(a, fakeStatus, instanceUser, this, accountID, this::onMarkAsRead),
				textItem
		);
	}

	public void onMarkAsRead(String id) {
		if (unreadIDs == null) return;
		unreadIDs.remove(id);
		if (unreadIDs.isEmpty()) setResult(true, null);
	}

	@Override
	protected void addAccountToKnown(Announcement s) {}

	@Override
	public void onItemClick(String id) {}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAnnouncements(true)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Announcement> result){
						if (getActivity() == null) return;
						List<Announcement> unread = result.stream().filter(a -> !a.read).collect(toList());
						List<Announcement> read = result.stream().filter(a -> a.read).collect(toList());
						onDataLoaded(unread, true);
						onDataLoaded(read, false);
						if (unread.isEmpty()) setResult(true, null);
						else unreadIDs = unread.stream().map(a -> a.id).collect(toList());
					}
				})
				.exec(accountID);
	}
}
