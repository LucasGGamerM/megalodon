package org.joinmastodon.android.fragments;

import static org.joinmastodon.android.api.session.AccountLocalPreferences.ShowEmojiReactions.ONLY_OPENED;

import android.content.res.Configuration;
import android.os.Bundle;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusMuteChangedEvent;
import org.joinmastodon.android.events.EmojiReactionsUpdatedEvent;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.events.ReblogDeletedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.EmojiReactionsStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.GapStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;

public abstract class StatusListFragment extends BaseStatusListFragment<Status> {
	protected EventListener eventListener=new EventListener();

	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		boolean isMainThreadStatus = this instanceof ThreadFragment t && s.id.equals(t.mainStatus.id);
		int flags = 0;
		AccountLocalPreferences lp=getLocalPrefs();
		if(GlobalUserPreferences.spectatorMode)
			flags |= StatusDisplayItem.FLAG_NO_FOOTER;
		if(!lp.emojiReactionsEnabled || lp.showEmojiReactions==ONLY_OPENED)
			flags |= StatusDisplayItem.FLAG_NO_EMOJI_REACTIONS;
		if(GlobalUserPreferences.translateButtonOpenedOnly)
			flags |= StatusDisplayItem.FLAG_NO_TRANSLATE;
		if(!GlobalUserPreferences.showMediaPreview)
			flags |= StatusDisplayItem.FLAG_NO_MEDIA_PREVIEW;
		/* MOSHIDON: we make the filterContext null in the main status in the thread fragment, so that the main status is never filtered (because you just clicked on it).
		This also restores old behavior that got lost to time and changes in the filter system	*/
		return StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, isMainThreadStatus ? null : getFilterContext(), isMainThreadStatus ? 0 : flags);
	}

	protected abstract FilterContext getFilterContext();

	@Override
	protected void addAccountToKnown(Status s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
		if(s.reblog!=null && !knownAccounts.containsKey(s.reblog.account.id))
			knownAccounts.put(s.reblog.account.id, s.reblog.account);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(eventListener);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(eventListener);
	}

	@Override
	public void onItemClick(String id){
		Status status=getContentStatusByID(id);
		if(status==null || status.preview) return;
		if(status.isRemote){
			UiUtils.lookupStatus(getContext(), status, accountID, null, status1 -> {
				status1.filterRevealed = true;
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("status", Parcels.wrap(status1));
				if(status1.inReplyToAccountId!=null && knownAccounts.containsKey(status1.inReplyToAccountId))
					args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status1.inReplyToAccountId)));
				Nav.go(getActivity(), ThreadFragment.class, args);
			});
			return;
		}
		status.filterRevealed=true;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("status", Parcels.wrap(status.clone()));
		if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId))
			args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status.inReplyToAccountId)));
		Nav.go(getActivity(), ThreadFragment.class, args);
	}

	protected void onStatusCreated(Status status){}

	protected void onStatusUpdated(Status status){
		ArrayList<Status> statusesForDisplayItems=new ArrayList<>();
		for(int i=0;i<data.size();i++){
			Status s=data.get(i);
			if(s.reblog!=null && s.reblog.id.equals(status.id)){
				s.reblog=status.clone();
				statusesForDisplayItems.add(s);
			}else if(s.id.equals(status.id)){
				data.set(i, status);
				statusesForDisplayItems.add(status);
			}
		}
		for(int i=0;i<preloadedData.size();i++){
			Status s=preloadedData.get(i);
			if(s.reblog!=null && s.reblog.id.equals(status.id)){
				s.reblog=status.clone();
			}else if(s.id.equals(status.id)){
				preloadedData.set(i, status);
			}
		}

		if(statusesForDisplayItems.isEmpty())
			return;

		for(Status s:statusesForDisplayItems){
			int i=0;
			for(StatusDisplayItem item:displayItems){
				if(item.parentID.equals(s.id)){
					int start=i;
					for(;i<displayItems.size();i++){
						if(!displayItems.get(i).parentID.equals(s.id))
							break;
					}
					List<StatusDisplayItem> postItems=displayItems.subList(start, i);
					postItems.clear();
					postItems.addAll(buildDisplayItems(s));
					int oldSize=i-start, newSize=postItems.size();
					if(oldSize==newSize){
						adapter.notifyItemRangeChanged(start, newSize);
					}else if(oldSize<newSize){
						adapter.notifyItemRangeChanged(start, oldSize);
						adapter.notifyItemRangeInserted(start+oldSize, newSize-oldSize);
					}else{
						adapter.notifyItemRangeChanged(start, newSize);
						adapter.notifyItemRangeRemoved(start+newSize, oldSize-newSize);
					}
					break;
				}
				i++;
			}
		}
	}

	public Status getContentStatusByID(String id){
		Status s=getStatusByID(id);
		return s==null ? null : s.getContentStatus();
	}

	public Status getStatusByID(String id){
		for(Status s:data){
			if(s.id.equals(id)){
				return s;
			}
		}
		for(Status s:preloadedData){
			if(s.id.equals(id)){
				return s;
			}
		}
		return null;
	}

	protected boolean shouldRemoveAccountPostsWhenUnfollowing(){
		return false;
	}

	protected void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		List<Status> toRemove=Stream.concat(data.stream(), preloadedData.stream())
				.filter(s->s.account.id.equals(ev.postsByAccountID) || (!ev.isUnfollow && s.reblog!=null && s.reblog.account.id.equals(ev.postsByAccountID)))
				.collect(Collectors.toList());
		for(Status s:toRemove){
			removeStatus(s);
		}
	}

	private boolean removeStatusDisplayItems(String parentID, int firstIndex, int ancestorFirstIndex, int ancestorLastIndex){
		// did we find an ancestor that is also the status' neighbor?
		if(ancestorFirstIndex>=0 && ancestorLastIndex==firstIndex-1){
			// update ancestor to have no descendant anymore
			displayItems.subList(ancestorFirstIndex, ancestorLastIndex+1).forEach(i->i.hasDescendantNeighbor=false);
			adapter.notifyItemRangeChanged(ancestorFirstIndex, ancestorLastIndex-ancestorFirstIndex+1);
		}

		if(firstIndex==-1) return false;
		int lastIndex=firstIndex;
		while(lastIndex<displayItems.size()){
			StatusDisplayItem item=displayItems.get(lastIndex);
			if(!item.parentID.equals(parentID) || item instanceof GapStatusDisplayItem) break;
			lastIndex++;
		}
		int count=lastIndex-firstIndex;
		if(count<1) return false;
		displayItems.subList(firstIndex, lastIndex).clear();
		adapter.notifyItemRangeRemoved(firstIndex, count);
		return true;
	}

	protected void removeStatus(Status status){
		final AccountSessionManager asm=AccountSessionManager.getInstance();
		final CacheController cache=AccountSessionManager.get(accountID).getCacheController();
		final boolean unReblogging=status.reblog!=null && asm.isSelf(accountID, status.account);
		final Predicate<Status> isToBeRemovedReblog=item->item!=null && item.reblog!=null
				&& item.reblog.id.equals(status.reblog.id)
				&& asm.isSelf(accountID, item.account);
		final BiPredicate<String, Supplier<String>> isToBeRemovedContent=(parentId, contentIdSupplier)->
				parentId.equals(status.id) || contentIdSupplier.get().equals(status.id);

		int ancestorFirstIndex=-1, ancestorLastIndex=-1;
		for(int i=0;i<displayItems.size();i++){
			StatusDisplayItem item=displayItems.get(i);
			// we found a status that the to-be-removed status replies to!
			// storing indices to maybe update its display items
			if(item.parentID.equals(status.inReplyToId)){
				if(ancestorFirstIndex==-1) ancestorFirstIndex=i;
				ancestorLastIndex=i;
			}
			// if we're un-reblogging, we compare the reblogged status's id with the current status's
			if(unReblogging
					? isToBeRemovedReblog.test(getStatusByID(item.parentID))
					: isToBeRemovedContent.test(item.parentID, item::getContentStatusID)){
				// if statuses are removed from index i, the next iteration should be on the same index again
				if(removeStatusDisplayItems(item.parentID, i, ancestorFirstIndex, ancestorLastIndex)) i--;
				// resetting in case we find another occurrence of the same status that also has ancestors
				// (we won't - unless the timeline is being especially weird)
				ancestorFirstIndex=-1; ancestorLastIndex=-1;
			}
		}

		Consumer<List<Status>> removeStatusFromData=(list)->{
			Iterator<Status> it=list.iterator();
			while(it.hasNext()){
				Status s=it.next();
				if(unReblogging
						? isToBeRemovedReblog.test(s)
						: isToBeRemovedContent.test(s.id, s::getContentStatusID)){
					it.remove();
					cache.deleteStatus(s.id);
				}
			}
		};
		removeStatusFromData.accept(data);
		removeStatusFromData.accept(preloadedData);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (getParentFragment() instanceof HomeTabFragment home) home.updateToolbarLogo();
	}

	public class EventListener{

		@Subscribe
		public void onStatusCountersUpdated(StatusCountersUpdatedEvent ev){
			for(Status s:data){
				if(s.getContentStatus().id.equals(ev.id)){
					s.getContentStatus().update(ev);
					AccountSessionManager.get(accountID).getCacheController().updateStatus(s);
					for(int i=0;i<list.getChildCount();i++){
						RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
						if(holder instanceof FooterStatusDisplayItem.Holder footer && footer.getItem().status==s.getContentStatus()){
							footer.rebind();
						}else if(holder instanceof ExtendedFooterStatusDisplayItem.Holder footer && footer.getItem().status==s.getContentStatus()){
							footer.rebind();
						}
					}
				}
			}
			for(Status s:preloadedData){
				if(s.getContentStatus().id.equals(ev.id)){
					s.getContentStatus().update(ev);
					AccountSessionManager.get(accountID).getCacheController().updateStatus(s);
				}
			}
		}

		@Subscribe
		public void onStatusMuteChaged(StatusMuteChangedEvent ev){
			for(Status s:data){
				if(s.getContentStatus().id.equals(ev.id)){
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
			for(Status s:preloadedData){
				if(s.getContentStatus().id.equals(ev.id)){
					s.getContentStatus().update(ev);
					AccountSessionManager.get(accountID).getCacheController().updateStatus(s);
				}
			}
		}

		@Subscribe
		public void onEmojiReactionsChanged(EmojiReactionsUpdatedEvent ev){
			for(Status s:data){
				if(s.getContentStatus().id.equals(ev.id)){
					for(int i=0;i<list.getChildCount();i++){
						RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
						if(holder instanceof EmojiReactionsStatusDisplayItem.Holder reactions && reactions.getItem().status==s.getContentStatus() && ev.viewHolder!=holder){
							reactions.updateReactions(ev.reactions);
						}
					}
					AccountSessionManager.get(accountID).getCacheController().updateStatus(s);
					for(int i=0;i<list.getChildCount();i++){
						RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
						if(holder instanceof TextStatusDisplayItem.Holder text && text.getItem().parentID.equals(s.getID())){
							text.rebind();
						}
					}
				}
			}
			for(Status s:preloadedData){
				if(s.getContentStatus().id.equals(ev.id)){
					s.getContentStatus().update(ev);
					AccountSessionManager.get(accountID).getCacheController().updateStatus(s);
				}
			}
		}

		@Subscribe
		public void onStatusDeleted(StatusDeletedEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			Status status=getStatusByID(ev.id);
			if(status==null)
				return;
			removeStatus(status);
		}

		@Subscribe
		public void onReblogDeleted(ReblogDeletedEvent ev){
			AccountSessionManager asm=AccountSessionManager.getInstance();
			if(!ev.accountID.equals(accountID))
				return;
			for(Status item : data){
				boolean itemIsOwnReblog=item.reblog!=null
						&& item.getContentStatusID().equals(ev.statusID)
						&& asm.isSelf(accountID, item.account);
				if(itemIsOwnReblog){
					removeStatus(item);
					break;
				}
			}
		}

		@Subscribe
		public void onStatusCreated(StatusCreatedEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			StatusListFragment.this.onStatusCreated(ev.status.clone());
		}

		@Subscribe
		public void onStatusUpdated(StatusUpdatedEvent ev){
			StatusListFragment.this.onStatusUpdated(ev.status);
		}

		@Subscribe
		public void onPollUpdated(PollUpdatedEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			for(Status status:data){
				Status contentStatus=status.getContentStatus();
				if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
					updatePoll(status.id, contentStatus, ev.poll);
					AccountSessionManager.get(accountID).getCacheController().updateStatus(contentStatus);
				}
			}
		}

		@Subscribe
		public void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			if(ev.isUnfollow && !shouldRemoveAccountPostsWhenUnfollowing())
				return;
			StatusListFragment.this.onRemoveAccountPostsEvent(ev);
		}
	}
}
