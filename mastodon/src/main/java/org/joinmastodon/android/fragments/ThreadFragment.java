package org.joinmastodon.android.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.GlobalUserPreferences;
import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.GlobalUserPreferences.AutoRevealMode;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusByID;
import org.joinmastodon.android.api.requests.statuses.GetStatusContext;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusMuteChangedEvent;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusContext;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.SpoilerStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.WarningFilteredStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ProvidesAssistContent;
import org.parceler.Parcels;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class ThreadFragment extends StatusListFragment implements ProvidesAssistContent {
	protected Status mainStatus, updatedStatus, replyTo;
	private final HashMap<String, NeighborAncestryInfo> ancestryMap = new HashMap<>();
	private StatusContext result;
	protected boolean contextInitiallyRendered, transitionFinished, preview;
	private FrameLayout replyContainer;
	private LinearLayout replyButton;
	private ImageView replyButtonAva;
	private TextView replyButtonText;
	private int lastBottomInset;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setLayout(R.layout.fragment_thread);
		mainStatus=Parcels.unwrap(getArguments().getParcelable("status"));
		replyTo=Parcels.unwrap(getArguments().getParcelable("inReplyTo"));
		Account inReplyToAccount=Parcels.unwrap(getArguments().getParcelable("inReplyToAccount"));
		refreshing=contextInitiallyRendered=getArguments().getBoolean("refresh", false);
		if(inReplyToAccount!=null)
			knownAccounts.put(inReplyToAccount.id, inReplyToAccount);
		data.add(mainStatus);
		onAppendItems(Collections.singletonList(mainStatus));
		preview=mainStatus.preview;
		if(preview) setRefreshEnabled(false);
		setTitle(preview ? getString(R.string.sk_post_preview) : HtmlParser.parseCustomEmoji(getString(R.string.post_from_user, mainStatus.account.getDisplayName()), mainStatus.account.emojis));
		transitionFinished = getArguments().getBoolean("noTransition", false);

		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Subscribe
	public void onStatusMuteChanged(StatusMuteChangedEvent ev){
		for(Status s:data){
			s.getContentStatus().update(ev);
			AccountSessionManager.get(accountID).getCacheController().updateStatus(s);
			for(int i=0;i<list.getChildCount();i++){
				RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
				if(holder instanceof HeaderStatusDisplayItem.Holder header && header.getItem().status==s.getContentStatus()){
					header.rebind();
				}
			}
		}
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		List<StatusDisplayItem> items=super.buildDisplayItems(s);
		// "what the fuck is a deque"? yes
		// (it's just so the last-added item automatically comes first when looping over it)
		Deque<Integer> deleteTheseItems = new ArrayDeque<>();

		// modifying hidden filtered items if status is displayed as a warning
		List<StatusDisplayItem> itemsToModify =
				(items.get(0) instanceof WarningFilteredStatusDisplayItem warning)
						? warning.filteredItems
						: items;

		for(int i = 0; i < itemsToModify.size(); i++){
			StatusDisplayItem item = itemsToModify.get(i);
			NeighborAncestryInfo ancestryInfo = ancestryMap.get(s.id);
			if (ancestryInfo != null) {
				item.setAncestryInfo(
						ancestryInfo.descendantNeighbor != null,
						ancestryInfo.ancestoringNeighbor != null,
						s.id.equals(mainStatus.id),
						Optional.ofNullable(ancestryInfo.ancestoringNeighbor)
								.map(ancestor -> ancestor.id.equals(mainStatus.id))
								.orElse(false)
				);
			}

			if (item instanceof ReblogOrReplyLineStatusDisplayItem &&
					(!item.isDirectDescendant && item.hasAncestoringNeighbor)) {
				deleteTheseItems.add(i);
			}

			if(s.id.equals(mainStatus.id)){
				if(item instanceof TextStatusDisplayItem text)
					text.textSelectable=true;
				else if(item instanceof FooterStatusDisplayItem footer)
					footer.hideCounts=true;
				else if(item instanceof SpoilerStatusDisplayItem spoiler){
					for(StatusDisplayItem subItem:spoiler.contentItems){
						if(subItem instanceof TextStatusDisplayItem text)
							text.textSelectable=true;
					}
				}
			}
		}

		for (int deleteThisItem : deleteTheseItems) itemsToModify.remove(deleteThisItem);
		if(s.id.equals(mainStatus.id)) {
			itemsToModify.add(itemsToModify.size()-1, new ExtendedFooterStatusDisplayItem(s.id, this, accountID, s.getContentStatus()));
		}
		return items;
	}

	@Override
	public void onTransitionFinished() {
		transitionFinished = true;
		maybeApplyContext();
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(preview && replyTo==null){
			result=new StatusContext();
			result.descendants=Collections.emptyList();
			result.ancestors=Collections.emptyList();
			return;
		}
		if(refreshing && !preview) loadMainStatus();
		currentRequest=new GetStatusContext(preview ? replyTo.id : mainStatus.id)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(StatusContext result){
						if(preview){
							result.descendants=Collections.emptyList();
							result.ancestors.add(replyTo);
						}
						ThreadFragment.this.result = result;
						maybeApplyContext();
					}
				})
				.exec(accountID);
	}

	private void loadMainStatus() {
		new GetStatusByID(mainStatus.id)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(Status status) {
						if (getContext() == null || status == null) return;
						updatedStatus = status;
						// for the case that the context has already loaded (and the animation has
						// already finished), falling back to applying it ourselves:
						maybeApplyMainStatus();
					}

					@Override
					public void onError(ErrorResponse error) {}
				}).exec(accountID);
	}

	private void restoreStatusStates(List<Status> newData, Map<String, Status> oldData) {
		for (Status s : newData) {
			if (s == mainStatus) continue;
			Status oldStatus = oldData == null ? null : oldData.get(s.id);
			// restore previous spoiler/filter revealed states when refreshing
			if (oldStatus != null) {
				s.spoilerRevealed = oldStatus.spoilerRevealed;
				s.sensitiveRevealed = oldStatus.sensitiveRevealed;
				s.filterRevealed = oldStatus.filterRevealed;
				s.textExpanded = oldStatus.textExpanded;
			}
			if (GlobalUserPreferences.autoRevealEqualSpoilers != AutoRevealMode.NEVER &&
					s.spoilerText != null){
				if (s.spoilerText.equals(mainStatus.spoilerText) ||
						(s.spoilerText.toLowerCase().startsWith("re: ") &&
								s.spoilerText.substring(4).equals(mainStatus.spoilerText))){
					if (GlobalUserPreferences.autoRevealEqualSpoilers == AutoRevealMode.DISCUSSIONS || Objects.equals(mainStatus.account.id, s.account.id)) {
						s.spoilerRevealed = mainStatus.spoilerRevealed;
					}
				}
			}
		}

	}
	protected void maybeApplyContext() {
		if (!transitionFinished || result == null || getContext() == null) return;
		Map<String, Status> oldData = null;
		if(refreshing){
			oldData = new HashMap<>(data.size());
			for (Status s : data) oldData.put(s.id, s);
			data.clear();
			ancestryMap.clear();
			displayItems.clear();
			data.add(mainStatus);
			onAppendItems(Collections.singletonList(mainStatus));
		}

		// TODO: figure out how this code works
		if (isInstanceAkkoma()) sortStatusContext(mainStatus, result);

		filterStatuses(result.descendants);
		filterStatuses(result.ancestors);
		restoreStatusStates(result.descendants, oldData);
		restoreStatusStates(result.ancestors, oldData);

		for (NeighborAncestryInfo i : mapNeighborhoodAncestry(mainStatus, result)) {
			ancestryMap.put(i.status.id, i);
		}

		if(footerProgress!=null)
			footerProgress.setVisibility(View.GONE);
		data.addAll(result.descendants);

		int prevCount=displayItems.size();
		onAppendItems(result.descendants);

		int count=displayItems.size();
		if(!refreshing)
			adapter.notifyItemRangeInserted(prevCount, count-prevCount);
		int prependedCount = prependItems(result.ancestors, !refreshing);
		if (prependedCount > 0 && displayItems.get(prependedCount) instanceof ReblogOrReplyLineStatusDisplayItem) {
			displayItems.remove(prependedCount);
			adapter.notifyItemRemoved(prependedCount);
			count--;
		}

		dataLoaded();
		if(refreshing){
			refreshDone();
			adapter.notifyDataSetChanged();
		}
		list.scrollToPosition(displayItems.size()-count);

		// no animation is going to happen, so proceeding to apply right now
		if (data.size() == 1) {
			contextInitiallyRendered = true;
			// for the case that the main status has already finished loading
			maybeApplyMainStatus();
		}

		result = null;
	}
	protected Object maybeApplyMainStatus() {
		if (updatedStatus == null || !contextInitiallyRendered) return null;

		// restore revealed states for main status because it gets updated after doLoadData
		updatedStatus.filterRevealed = mainStatus.filterRevealed;
		updatedStatus.spoilerRevealed = mainStatus.spoilerRevealed;
		updatedStatus.sensitiveRevealed = mainStatus.sensitiveRevealed;
		updatedStatus.textExpanded = mainStatus.textExpanded;
		if(updatedStatus.quote!=null && mainStatus.quote!=null){
			updatedStatus.quote.filterRevealed = mainStatus.quote.filterRevealed;
			updatedStatus.quote.spoilerRevealed = mainStatus.quote.spoilerRevealed;
			updatedStatus.quote.sensitiveRevealed = mainStatus.quote.sensitiveRevealed;
			updatedStatus.quote.textExpanded = mainStatus.quote.textExpanded;
		}

		// returning fired event object to facilitate testing
		Object event;
		if (updatedStatus.editedAt != null &&
				(mainStatus.editedAt == null ||
						updatedStatus.editedAt.isAfter(mainStatus.editedAt))) {
			event = new StatusUpdatedEvent(updatedStatus);
		} else {
			event = new StatusCountersUpdatedEvent(updatedStatus);
		}

		mainStatus = updatedStatus;
		updatedStatus = null;
		E.post(event);
		return event;
	}

	public static List<NeighborAncestryInfo> mapNeighborhoodAncestry(Status mainStatus, StatusContext context) {
		List<NeighborAncestryInfo> ancestry = new ArrayList<>();

		List<Status> statuses = new ArrayList<>(context.ancestors);
		statuses.add(mainStatus);
		statuses.addAll(context.descendants);

		int count = statuses.size();
		for (int index = 0; index < count; index++) {
			Status current = statuses.get(index);
			ancestry.add(new NeighborAncestryInfo(
					current,
					// descendant neighbor
					Optional
							.ofNullable(count > index + 1 ? statuses.get(index + 1) : null)
							.filter(s -> current.id.equals(s.inReplyToId))
							.orElse(null),
					// ancestoring neighbor
					Optional.ofNullable(index > 0 ? ancestry.get(index - 1) : null)
							.filter(ancestor -> Optional.ofNullable(ancestor.descendantNeighbor)
									.map(ancestorsDescendant -> current.id.equals(ancestorsDescendant.id))
									.orElse(false))
							.map(a -> a.status)
							.orElse(null)
			));
		}

		return ancestry;
	}

	public static void sortStatusContext(Status mainStatus, StatusContext context) {
		List<String> threadIds=new ArrayList<>();
		threadIds.add(mainStatus.id);
		for(Status s:context.descendants){
			if(threadIds.contains(s.inReplyToId)){
				threadIds.add(s.id);
			}
		}
		threadIds.add(mainStatus.inReplyToId);
		for(int i=context.ancestors.size()-1; i >= 0; i--){
			Status s=context.ancestors.get(i);
			if(s.inReplyToId != null && threadIds.contains(s.id)){
				threadIds.add(s.inReplyToId);
			}
		}

		context.ancestors=context.ancestors.stream().filter(s -> threadIds.contains(s.id)).collect(Collectors.toList());
		context.descendants=getDescendantsOrdered(mainStatus.id,
				context.descendants.stream()
						.filter(s -> threadIds.contains(s.id))
						.collect(Collectors.toList()));
	}

	private static List<Status> getDescendantsOrdered(String id, List<Status> statuses){
		List<Status> out=new ArrayList<>();
		for(Status s:getDirectDescendants(id, statuses)){
			out.add(s);
			getDirectDescendants(s.id, statuses).forEach(d ->{
				out.add(d);
				out.addAll(getDescendantsOrdered(d.id, statuses));
			});
		}
		return out;
	}

	private static List<Status> getDirectDescendants(String id, List<Status> statuses){
		return statuses.stream()
				.filter(s -> id.equals(s.inReplyToId))
				.collect(Collectors.toList());
	}

	private void filterStatuses(List<Status> statuses){
		AccountSessionManager.get(accountID).filterStatuses(statuses, getFilterContext());
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading){
			dataLoading=true;
			doLoadData();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		replyContainer=view.findViewById(R.id.reply_button_wrapper);
		replyButton=replyContainer.findViewById(R.id.reply_button);
		replyButtonText=replyButton.findViewById(R.id.reply_btn_text);
		replyButtonAva=replyButton.findViewById(R.id.avatar);
		replyButton.setOutlineProvider(OutlineProviders.roundedRect(20));
		replyButton.setClipToOutline(true);
		replyButtonText.setText(HtmlParser.parseCustomEmoji(getString(R.string.reply_to_user, mainStatus.account.displayName), mainStatus.account.emojis));
		UiUtils.loadCustomEmojiInTextView(replyButtonText);
		replyButtonAva.setOutlineProvider(OutlineProviders.OVAL);
		replyButtonAva.setClipToOutline(true);
		replyButton.setOnClickListener(v->openReply(mainStatus, accountID));
		replyButton.setOnLongClickListener(this::onReplyLongClick);
		Account self=AccountSessionManager.get(accountID).self;
		if(!TextUtils.isEmpty(self.avatar)){
			ViewImageLoader.loadWithoutAnimation(replyButtonAva, getResources().getDrawable(R.drawable.image_placeholder, getActivity().getTheme()), new UrlImageLoaderRequest(self.avatar, V.dp(24), V.dp(24)));
		}
		UiUtils.loadCustomEmojiInTextView(toolbarTitleView);
		showContent();
		if(!loaded)
			footerProgress.setVisibility(View.VISIBLE);

		list.setItemAnimator(new BetterItemAnimator() {
			@Override
			public void onAnimationFinished(@NonNull RecyclerView.ViewHolder viewHolder) {
				super.onAnimationFinished(viewHolder);
				contextInitiallyRendered = true;
				// for the case that both requests are already done (and thus won't apply it)
				maybeApplyMainStatus();
			}
		});
	}

	protected void onStatusCreated(Status status){
		if (status.inReplyToId == null) return;
		Status repliedToStatus = getStatusByID(status.inReplyToId);
		if (repliedToStatus == null) return;
		NeighborAncestryInfo ancestry = ancestryMap.get(repliedToStatus.id);

		int nextDisplayItemsIndex = -1, indexOfPreviousDisplayItem = -1;

		if (ancestry != null) for (int i = 0; i < displayItems.size(); i++) {
			StatusDisplayItem item = displayItems.get(i);
			if (repliedToStatus.id.equals(item.parentID)) {
				// saving the replied-to status' display items index to eventually reach the last one
				indexOfPreviousDisplayItem = i;
				item.hasDescendantNeighbor = true;
			} else if (indexOfPreviousDisplayItem >= 0 && nextDisplayItemsIndex == -1) {
				// previous display item was the replied-to status' display items
				nextDisplayItemsIndex = i;
				// nothing left to do if there's no other reply to that status
				if (ancestry.descendantNeighbor == null) break;
			}
			if (ancestry.descendantNeighbor != null && item.parentID.equals(ancestry.descendantNeighbor.id)) {
				// existing reply shall no longer have the replied-to status as its neighbor
				item.hasAncestoringNeighbor = false;
			}
		}

		// fall back to inserting the item at the end
		nextDisplayItemsIndex = nextDisplayItemsIndex >= 0 ? nextDisplayItemsIndex : displayItems.size();
		int nextDataIndex = data.indexOf(repliedToStatus) + 1;

		// if replied-to status already has another reply...
		if (ancestry != null && ancestry.descendantNeighbor != null) {
			// update the reply's ancestry to remove its ancestoring neighbor (as we did above)
			ancestryMap.get(ancestry.descendantNeighbor.id).ancestoringNeighbor = null;
			// make sure the existing reply has a reply line
			if (nextDataIndex < data.size() &&
					!(displayItems.get(nextDisplayItemsIndex) instanceof ReblogOrReplyLineStatusDisplayItem)) {
				Status nextStatus = data.get(nextDataIndex);
				if (!nextStatus.account.id.equals(repliedToStatus.account.id)) {
					// create reply line manually since we're not building that status' items
					displayItems.add(nextDisplayItemsIndex, StatusDisplayItem.buildReplyLine(
							this, nextStatus, accountID, nextStatus, repliedToStatus.account, false
					));
				}
			}
		}

		// update replied-to status' ancestry
		if (ancestry != null) ancestry.descendantNeighbor = status;

		// add ancestry for newly created status before building its display items
		ancestryMap.put(status.id, new NeighborAncestryInfo(status, null, repliedToStatus));
		displayItems.addAll(nextDisplayItemsIndex, buildDisplayItems(status));
		data.add(nextDataIndex, status);
		adapter.notifyDataSetChanged();
	}

	public Status getMainStatus(){
		return mainStatus;
	}

	@Override
	public boolean isItemEnabled(String id){
		return !id.equals(mainStatus.id) || !mainStatus.filterRevealed;
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}


	@Override
	protected FilterContext getFilterContext() {
		return FilterContext.THREAD;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return Uri.parse(mainStatus.url);
	}

	protected static class NeighborAncestryInfo {
		protected Status status, descendantNeighbor, ancestoringNeighbor;

		protected NeighborAncestryInfo(@NonNull Status status, Status descendantNeighbor, Status ancestoringNeighbor) {
			this.status = status;
			this.descendantNeighbor = descendantNeighbor;
			this.ancestoringNeighbor = ancestoringNeighbor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NeighborAncestryInfo that = (NeighborAncestryInfo) o;
			return status.equals(that.status)
					&& Objects.equals(descendantNeighbor, that.descendantNeighbor)
					&& Objects.equals(ancestoringNeighbor, that.ancestoringNeighbor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(status, descendantNeighbor, ancestoringNeighbor);
		}
	}

	@Override
	protected void onErrorRetryClick(){
		if(preloadingFailed){
			preloadingFailed=false;
			V.setVisibilityAnimated(footerProgress, View.VISIBLE);
			V.setVisibilityAnimated(footerError, View.GONE);
			doLoadData();
			return;
		}
		super.onErrorRetryClick();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		lastBottomInset=insets.getSystemWindowInsetBottom();
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(replyContainer, insets));
	}

	private void openReply(Status status, String accountID){
		maybeShowPreReplySheet(status, ()->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("replyTo", Parcels.wrap(status));
			args.putBoolean("fromThreadFragment", true);
			Nav.go(getActivity(), ComposeFragment.class, args);
		});
	}
	private boolean onReplyLongClick(View v) {
		if(mainStatus.preview) return false;
		if (AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
		UiUtils.pickAccount(v.getContext(), accountID, R.string.sk_reply_as, R.drawable.ic_fluent_arrow_reply_28_regular, session -> {
			String pickedAccountID = session.getID();
				UiUtils.lookupStatus(v.getContext(), mainStatus, pickedAccountID, accountID, status -> {
				if (status == null) return;
				openReply(status, pickedAccountID);
			});
		}, null);
		return true;
	}

	public int getSnackbarOffset(){
		return replyContainer.getHeight()-lastBottomInset;
	}
}
