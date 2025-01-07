package org.joinmastodon.android.fragments;

import static java.util.stream.Collectors.toList;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.announcements.GetAnnouncements;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.GetScheduledStatuses;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.ScheduledStatusCreatedEvent;
import org.joinmastodon.android.events.ScheduledStatusDeletedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Announcement;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.DummyStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.EmojiReactionsStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.PaginatedList;
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
		Status fakeStatus = a.toStatus(isInstanceIceshrimp());
		TextStatusDisplayItem textItem = new TextStatusDisplayItem(a.id, HtmlParser.parse(a.content, a.emojis, a.mentions, a.tags, accountID), this, fakeStatus, true);
		textItem.textSelectable = true;

		List<StatusDisplayItem> items=new ArrayList<>();
		items.add(HeaderStatusDisplayItem.fromAnnouncement(a, fakeStatus, instanceUser, this, accountID, this::onMarkAsRead));
		items.add(textItem);
		if(!isInstanceAkkoma() && !isInstanceIceshrimp()) items.add(new EmojiReactionsStatusDisplayItem(a.id, this, fakeStatus, accountID, false, true));
		return items;
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
						if(getActivity()==null) return;

						// get unread items first
						List<Announcement> data = result.stream().filter(a -> !a.read).collect(toList());
						if (data.isEmpty()) setResult(true, null);
						else unreadIDs = data.stream().map(a -> a.id).collect(toList());

						// append read items at the end
						data.addAll(result.stream().filter(a -> a.read).collect(toList()));
						onDataLoaded(data, false);
					}
				})
				.exec(accountID);
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return isInstanceAkkoma() ? base.path("/announcements").build() : null;
	}
}
