package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.app.assist.AssistContent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.polls.SubmitPollVote;
import org.joinmastodon.android.api.requests.statuses.AkkomaTranslateStatus;
import org.joinmastodon.android.api.requests.statuses.TranslateStatus;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AkkomaTranslation;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.Translation;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.NonMutualPreReplySheet;
import org.joinmastodon.android.ui.OldPostPreReplySheet;
import org.joinmastodon.android.ui.displayitems.AccountStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.GapStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HashtagStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.MediaGridStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PollFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PollOptionStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PreviewlessMediaGridStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.SpoilerStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.WarningFilteredStatusDisplayItem;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.photoviewer.PhotoViewerHost;
import org.joinmastodon.android.ui.utils.InsetStatusItemDecoration;
import org.joinmastodon.android.ui.utils.MediaAttachmentViewController;
import org.joinmastodon.android.ui.utils.PreviewlessMediaAttachmentViewController;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ProvidesAssistContent;
import org.joinmastodon.android.utils.TypedObjectPool;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class BaseStatusListFragment<T extends DisplayItemsParent> extends MastodonRecyclerFragment<T> implements PhotoViewerHost, ScrollableToTop, IsOnTop, HasFab, ProvidesAssistContent.ProvidesWebUri {
	protected ArrayList<StatusDisplayItem> displayItems=new ArrayList<>();
	protected DisplayItemsAdapter adapter;
	protected String accountID;
	protected PhotoViewer currentPhotoViewer;
	protected ImageButton fab;
	protected int scrollDiff = 0;
	protected HashMap<String, Account> knownAccounts=new HashMap<>();
	protected HashMap<String, Relationship> relationships=new HashMap<>();
	protected Rect tmpRect=new Rect();
	protected TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, MediaAttachmentViewController> attachmentViewsPool=new TypedObjectPool<>(this::makeNewMediaAttachmentView);
	protected TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, PreviewlessMediaAttachmentViewController> previewlessAttachmentViewsPool=new TypedObjectPool<>(this::makeNewPreviewlessMediaAttachmentView);

	protected boolean currentlyScrolling;
	protected String maxID;

	public BaseStatusListFragment(){
		super(20);
		if (wantsComposeButton()) setListLayoutId(R.layout.recycler_fragment_with_fab);
	}

	protected boolean wantsComposeButton() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(GlobalUserPreferences.toolbarMarquee){
			setTitleMarqueeEnabled(false);
			setSubtitleMarqueeEnabled(false);
		}
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return adapter=new DisplayItemsAdapter();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		accountID=getArguments().getString("account");
	}

	@Override
	public void onAppendItems(List<T> items){
		super.onAppendItems(items);
		for(T s:items){
			addAccountToKnown(s);
		}
		for(T s:items){
			displayItems.addAll(buildDisplayItems(s));
		}
		loadRelationships(items.stream().map(DisplayItemsParent::getAccountID).filter(Objects::nonNull).collect(Collectors.toSet()));
	}

	@Override
	public void onClearItems(){
		super.onClearItems();
		displayItems.clear();
	}

	protected int prependItems(List<T> items, boolean notify){
		data.addAll(0, items);
		int offset=0;
		for(T s:items){
			addAccountToKnown(s);
		}
		for(T s:items){
			List<StatusDisplayItem> toAdd=buildDisplayItems(s);
			displayItems.addAll(offset, toAdd);
			offset+=toAdd.size();
		}
		if(notify)
			adapter.notifyItemRangeInserted(0, offset);
		loadRelationships(items.stream().map(DisplayItemsParent::getAccountID).filter(Objects::nonNull).collect(Collectors.toSet()));
		return offset;
	}

	protected String getMaxID(){
		if(refreshing) return null;
		if(maxID!=null) return maxID;
		if(!preloadedData.isEmpty())
			return preloadedData.get(preloadedData.size()-1).getID();
		else if(!data.isEmpty())
			return data.get(data.size()-1).getID();
		else
			return null;
	}

	protected boolean applyMaxID(List<Status> result){
		boolean empty=result.isEmpty();
		if(!empty) maxID=result.get(result.size()-1).id;
		return !empty;
	}

	protected abstract List<StatusDisplayItem> buildDisplayItems(T s);
	protected abstract void addAccountToKnown(T s);

	@Override
	protected void onHidden(){
		super.onHidden();
		// Clear any loaded images from the list to make it possible for the GC to deallocate them.
		// The delay avoids blank image views showing up in the app switcher.
		content.postDelayed(()->{
			if(!isHidden())
				return;
			imgLoader.deactivate();
			UsableRecyclerView list=(UsableRecyclerView) this.list;
			for(int i=0; i<list.getChildCount(); i++){
				RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
				if(holder instanceof ImageLoaderViewHolder ivh){
					int pos=holder.getAbsoluteAdapterPosition();
					if(pos<0)
						continue;
					for(int j=0;j<list.getImageCountForItem(pos);j++){
						ivh.clearImage(j);
					}
				}
			}
		}, 100);
	}

	@Override
	protected void onShown(){
		super.onShown();
		imgLoader.activate();
	}

	@Override
	public void openPhotoViewer(String parentID, Status _status, int attachmentIndex, MediaGridStatusDisplayItem.Holder gridHolder){
		final Status status=_status.getContentStatus();
		currentPhotoViewer=new PhotoViewer(getActivity(), status.mediaAttachments, attachmentIndex, status, accountID, new PhotoViewer.Listener(){
			private MediaAttachmentViewController transitioningHolder;

			@Override
			public void setPhotoViewVisibility(int index, boolean visible){
				MediaAttachmentViewController holder=findPhotoViewHolder(index);
				if(holder!=null)
					holder.photo.setAlpha(visible ? 1f : 0f);
			}

			@Override
			public boolean startPhotoViewTransition(int index, @NonNull Rect outRect, @NonNull int[] outCornerRadius){
				MediaAttachmentViewController holder=findPhotoViewHolder(index);
				if(holder!=null && list!=null){
					transitioningHolder=holder;
					View view=transitioningHolder.photo;
					int[] pos={0, 0};
					view.getLocationOnScreen(pos);
					outRect.set(pos[0], pos[1], pos[0]+view.getWidth(), pos[1]+view.getHeight());
					list.setClipChildren(false);
					gridHolder.setClipChildren(false);
					transitioningHolder.view.setElevation(1f);
					return true;
				}
				return false;
			}

			@Override
			public void setTransitioningViewTransform(float translateX, float translateY, float scale){
				View view=transitioningHolder.photo;
				view.setTranslationX(translateX);
				view.setTranslationY(translateY);
				view.setScaleX(scale);
				view.setScaleY(scale);
			}

			@Override
			public void endPhotoViewTransition(){
				// fix drawable callback
				Drawable d=transitioningHolder.photo.getDrawable();
				transitioningHolder.photo.setImageDrawable(null);
				transitioningHolder.photo.setImageDrawable(d);

				View view=transitioningHolder.photo;
				view.setTranslationX(0f);
				view.setTranslationY(0f);
				view.setScaleX(1f);
				view.setScaleY(1f);
				transitioningHolder.view.setElevation(0f);
				if(list!=null)
					list.setClipChildren(true);
				gridHolder.setClipChildren(true);
				transitioningHolder=null;
			}

			@Override
			public Drawable getPhotoViewCurrentDrawable(int index){
				MediaAttachmentViewController holder=findPhotoViewHolder(index);
				if(holder!=null)
					return holder.photo.getDrawable();
				return null;
			}

			@Override
			public void photoViewerDismissed(){
				currentPhotoViewer=null;
				gridHolder.itemView.setHasTransientState(false);
			}

			@Override
			public void onRequestPermissions(String[] permissions){
				requestPermissions(permissions, PhotoViewer.PERMISSION_REQUEST);
			}

			private MediaAttachmentViewController findPhotoViewHolder(int index){
				return gridHolder.getViewController(index);
			}
		});
		gridHolder.itemView.setHasTransientState(true);
	}


	public void openPreviewlessMediaPhotoViewer(String parentID, Status _status, int attachmentIndex, PreviewlessMediaGridStatusDisplayItem.Holder gridHolder){
		final Status status=_status.getContentStatus();
		currentPhotoViewer=new PhotoViewer(getActivity(), status.mediaAttachments, attachmentIndex, status, accountID, new PhotoViewer.Listener(){
			private PreviewlessMediaAttachmentViewController transitioningHolder;

			@Override
			public void setPhotoViewVisibility(int index, boolean visible){

			}

			@Override
			public boolean startPhotoViewTransition(int index, @NonNull Rect outRect, @NonNull int[] outCornerRadius){
				PreviewlessMediaAttachmentViewController holder=findPhotoViewHolder(index);
				if(holder!=null && list!=null){
					transitioningHolder=holder;
					View view=transitioningHolder.inner;
					int[] pos={0, 0};
					view.getLocationOnScreen(pos);
					outRect.set(pos[0], pos[1], pos[0]+view.getWidth(), pos[1]+view.getHeight());
					list.setClipChildren(false);
					gridHolder.setClipChildren(false);
					transitioningHolder.view.setElevation(1f);
					return true;
				}
				return false;
			}

			@Override
			public void setTransitioningViewTransform(float translateX, float translateY, float scale){
				View view=transitioningHolder.inner;
				view.setTranslationX(translateX);
				view.setTranslationY(translateY);
				view.setScaleX(scale);
				view.setScaleY(scale);
			}

			@Override
			public void endPhotoViewTransition(){
				View view=transitioningHolder.inner;
				view.setTranslationX(0f);
				view.setTranslationY(0f);
				view.setScaleX(1f);
				view.setScaleY(1f);
				transitioningHolder.view.setElevation(0f);
				if(list!=null)
					list.setClipChildren(true);
				gridHolder.setClipChildren(true);
				transitioningHolder=null;
			}

			@Nullable
			@Override
			public Drawable getPhotoViewCurrentDrawable(int index){
				return null;
			}

			@Override
			public void photoViewerDismissed(){
				currentPhotoViewer=null;
			}

			@Override
			public void onRequestPermissions(String[] permissions){
				requestPermissions(permissions, PhotoViewer.PERMISSION_REQUEST);
			}

			private PreviewlessMediaAttachmentViewController findPhotoViewHolder(int index){
				return gridHolder.getViewController(index);
			}
		});
	}

	@Override
	public @Nullable View getFab() {
		if (getParentFragment() instanceof HasFab l) return l.getFab();
		else return fab;
	}

	@Override
	public void showFab() {
		View fab = getFab();
		if (fab == null || fab.getVisibility() == View.VISIBLE) return;
		fab.setVisibility(View.VISIBLE);
		TranslateAnimation animate = new TranslateAnimation(
				0,
				0,
				fab.getHeight() * 2,
				0);
		animate.setDuration(300);
		fab.startAnimation(animate);
	}

	public boolean isScrolling() {
		return currentlyScrolling;
	}

	@Override
	public void hideFab() {
		View fab = getFab();
		if (fab == null || fab.getVisibility() != View.VISIBLE) return;
		TranslateAnimation animate = new TranslateAnimation(
				0,
				0,
				0,
				fab.getHeight() * 2);
		animate.setDuration(300);
		fab.startAnimation(animate);
		fab.setVisibility(View.INVISIBLE);
		scrollDiff = 0;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);

		list.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				if(currentPhotoViewer!=null)
					currentPhotoViewer.offsetView(-dx, -dy);

				View fab = getFab();
				if (fab!=null && GlobalUserPreferences.autoHideFab && dy != UiUtils.SCROLL_TO_TOP_DELTA) {
					if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
						hideFab();
					} else if (dy < 0 && fab.getVisibility() != View.VISIBLE) {
						if (list.getChildAt(0).getTop() == 0 || scrollDiff > 400) {
							showFab();
							scrollDiff = 0;
						} else {
							scrollDiff += Math.abs(dy);
						}
					}
				}
			}

			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				currentlyScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
			}
		});
		list.addItemDecoration(new StatusListItemDecoration());
		list.addItemDecoration(new InsetStatusItemDecoration(this));
		((UsableRecyclerView)list).setSelectorBoundsProvider(new UsableRecyclerView.SelectorBoundsProvider(){
			private Rect tmpRect=new Rect();
			@Override
			public void getSelectorBounds(View view, Rect outRect){
				if(list!=view.getParent()) return;
				boolean hasDescendant=false, hasAncestor=false, isWarning=false;
				int lastIndex=-1, firstIndex=-1;
				if(((UsableRecyclerView) list).isIncludeMarginsInItemHitbox()){
					list.getDecoratedBoundsWithMargins(view, outRect);
				}else{
					outRect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
				}
				RecyclerView.ViewHolder holder=list.getChildViewHolder(view);
				if(holder instanceof StatusDisplayItem.Holder){
					if(((StatusDisplayItem.Holder<?>) holder).getItem().getType()==StatusDisplayItem.Type.GAP){
						outRect.setEmpty();
						return;
					}
					String id=((StatusDisplayItem.Holder<?>) holder).getItemID();
					for(int i=0;i<list.getChildCount();i++){
						View child=list.getChildAt(i);
						holder=list.getChildViewHolder(child);
						if(holder instanceof StatusDisplayItem.Holder<?> h){
							String otherID=((StatusDisplayItem.Holder<?>) holder).getItemID();
							if(otherID.equals(id)){
								if (firstIndex < 0) firstIndex = i;
								lastIndex = i;
								StatusDisplayItem item = h.getItem();
								hasDescendant = item.hasDescendantNeighbor;
								// no for direct descendants because main status (right above) is
								// being displayed with an extended footer - no connected layout
								hasAncestor = item.hasAncestoringNeighbor && !item.isDirectDescendant;
								list.getDecoratedBoundsWithMargins(child, tmpRect);
								outRect.left=Math.min(outRect.left, tmpRect.left);
								outRect.top=Math.min(outRect.top, tmpRect.top);
								outRect.right=Math.max(outRect.right, tmpRect.right);
								outRect.bottom=Math.max(outRect.bottom, tmpRect.bottom);
								if (holder instanceof WarningFilteredStatusDisplayItem.Holder) {
									isWarning = true;
								}
							}
						}
					}
				}
				// shifting the selection box down
				// see also: FooterStatusDisplayItem#onBind (setMargins)
				if (isWarning || firstIndex < 0 || lastIndex < 0 ||
						!(list.getChildViewHolder(list.getChildAt(lastIndex))
						instanceof FooterStatusDisplayItem.Holder)) return;
				int prevIndex = firstIndex - 1, nextIndex = lastIndex + 1;
				boolean prevIsWarning = prevIndex > 0 && prevIndex < list.getChildCount() &&
						list.getChildViewHolder(list.getChildAt(prevIndex))
						instanceof WarningFilteredStatusDisplayItem.Holder;
				boolean nextIsWarning = nextIndex > 0 && nextIndex < list.getChildCount() &&
						list.getChildViewHolder(list.getChildAt(nextIndex))
						instanceof WarningFilteredStatusDisplayItem.Holder;
				if (!prevIsWarning && hasAncestor) outRect.top += V.dp(4);
				if (!nextIsWarning && hasDescendant) outRect.bottom += V.dp(4);
			}
		});
		list.setItemAnimator(new BetterItemAnimator());
		((UsableRecyclerView) list).setIncludeMarginsInItemHitbox(true);
		updateToolbar();

		if (wantsComposeButton() && !getArguments().getBoolean("__disable_fab", false)) {
			fab.setVisibility(View.VISIBLE);
			fab.setOnClickListener(this::onFabClick);
			fab.setOnLongClickListener(this::onFabLongClick);
		} else if (fab != null) {
			fab.setVisibility(View.GONE);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	private void updateToolbar(){
		Toolbar toolbar=getToolbar();
		if(toolbar==null)
			return;
		toolbar.setOnClickListener(v->scrollToTop());
		toolbar.setNavigationContentDescription(R.string.back);
	}

	protected int getMainAdapterOffset(){
		if(list.getAdapter() instanceof MergeRecyclerAdapter mergeAdapter){
			return mergeAdapter.getPositionForAdapter(adapter);
		}
		return 0;
	}

	protected void drawDivider(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder, RecyclerView parent, Canvas c, Paint paint){
		parent.getDecoratedBoundsWithMargins(child, tmpRect);
		tmpRect.offset(0, Math.round(child.getTranslationY()));
		float y=tmpRect.bottom-V.dp(.5f);
		paint.setAlpha(Math.round(255*child.getAlpha()));
		c.drawLine(0, y, parent.getWidth(), y, paint);
	}

	protected boolean needDividerForExtraItem(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
		return false;
	}

	public abstract void onItemClick(String id);

	protected void updatePoll(String itemID, Status status, Poll poll){
		status.poll=poll;
		int firstOptionIndex=-1, footerIndex=-1;
		int spoilerFirstOptionIndex=-1, spoilerFooterIndex=-1;
		SpoilerStatusDisplayItem spoilerItem=null;
		int i=0;
		for(StatusDisplayItem item:displayItems){
			if(item.parentID.equals(itemID)){
				if(item instanceof SpoilerStatusDisplayItem){
					spoilerItem=(SpoilerStatusDisplayItem) item;
				}else if(item instanceof PollOptionStatusDisplayItem && firstOptionIndex==-1){
					firstOptionIndex=i;
				}else if(item instanceof PollFooterStatusDisplayItem){
					footerIndex=i;
					break;
				}
			}
			i++;
		}

		// This is a temporary measure to deal with the app crashing when the poll isn't updated.
		// This is needed because of a possible id mismatch that screws with things
		if(firstOptionIndex==-1 || footerIndex==-1){
			for(StatusDisplayItem item:displayItems){
				if(status.id.equals(itemID)){
					if(item instanceof SpoilerStatusDisplayItem){
						spoilerItem=(SpoilerStatusDisplayItem) item;
					}else if(item instanceof PollOptionStatusDisplayItem && firstOptionIndex==-1){
						firstOptionIndex=i;
					}else if(item instanceof PollFooterStatusDisplayItem){
						footerIndex=i;
						break;
					}
				}
				i++;
			}
		}

		if(firstOptionIndex==-1 || footerIndex==-1)
			throw new IllegalStateException("Can't find all poll items in displayItems");
		List<StatusDisplayItem> pollItems=displayItems.subList(firstOptionIndex, footerIndex+1);
		int prevSize=pollItems.size();
		if(spoilerItem!=null){
			spoilerFirstOptionIndex=spoilerItem.contentItems.indexOf(pollItems.get(0));
			spoilerFooterIndex=spoilerItem.contentItems.indexOf(pollItems.get(pollItems.size()-1));
		}
		pollItems.clear();
		StatusDisplayItem.buildPollItems(itemID, this, poll, status, pollItems);
		if(spoilerItem!=null){
			spoilerItem.contentItems.subList(spoilerFirstOptionIndex, spoilerFooterIndex+1).clear();
			spoilerItem.contentItems.addAll(spoilerFirstOptionIndex, pollItems);
		}
		if(prevSize!=pollItems.size()){
			adapter.notifyItemRangeRemoved(firstOptionIndex, prevSize);
			adapter.notifyItemRangeInserted(firstOptionIndex, pollItems.size());
		}else{
			adapter.notifyItemRangeChanged(firstOptionIndex, pollItems.size());
		}
	}

	public void onPollOptionClick(PollOptionStatusDisplayItem.Holder holder){
		Poll poll=holder.getItem().poll;
		Poll.Option option=holder.getItem().option;
		// MEGALODON: always show vote button
//		if(poll.multiple){
			if(poll.selectedOptions==null)
				poll.selectedOptions=new ArrayList<>();
			boolean optionContained=poll.selectedOptions.contains(option);
			if(!poll.multiple) poll.selectedOptions.clear();
			if(optionContained){
				poll.selectedOptions.remove(option);
				holder.itemView.setSelected(false);
			}else{
				poll.selectedOptions.add(option);
				holder.itemView.setSelected(true);
			}
			for(int i=0;i<list.getChildCount();i++){
				RecyclerView.ViewHolder vh=list.getChildViewHolder(list.getChildAt(i));
				if(!poll.multiple && vh instanceof PollOptionStatusDisplayItem.Holder item){
					if(item!=holder) item.itemView.setSelected(false);
				}
				if(vh instanceof PollFooterStatusDisplayItem.Holder footer){
					if(footer.getItemID().equals(holder.getItemID())){
						footer.rebind();
						break;
					}
				}
			}
//		}else{
//			submitPollVote(holder.getItemID(), poll.id, Collections.singletonList(poll.options.indexOf(option)));
//		}
	}

	public void onPollVoteButtonClick(PollFooterStatusDisplayItem.Holder holder){
		Poll poll=holder.getItem().poll;
		submitPollVote(holder.getItemID(), poll.id, poll.selectedOptions.stream().map(opt->poll.options.indexOf(opt)).collect(Collectors.toList()));
	}

	public void onPollViewResultsButtonClick(PollFooterStatusDisplayItem.Holder holder, boolean shown){
		int firstOptionIndex=-1, footerIndex=-1;
		int i=0;
		for(StatusDisplayItem item:displayItems){
			if(item.parentID.equals(holder.getItemID())){
				if(item instanceof PollOptionStatusDisplayItem && firstOptionIndex==-1){
					firstOptionIndex=i;
				}else if(item instanceof PollFooterStatusDisplayItem){
					footerIndex=i;
					break;
				}
			}
			i++;
		}
		if(firstOptionIndex==-1 || footerIndex==-1)
			throw new IllegalStateException("Can't find all poll items in displayItems");
		List<StatusDisplayItem> pollItems=displayItems.subList(firstOptionIndex, footerIndex+1);

		for(StatusDisplayItem item:pollItems){
			if (item instanceof PollOptionStatusDisplayItem) {
				((PollOptionStatusDisplayItem) item).isAnimating=true;
				((PollOptionStatusDisplayItem) item).showResults=shown;
				adapter.notifyItemRangeChanged(firstOptionIndex, pollItems.size());
			}
		}
	}

	protected void submitPollVote(String parentID, String pollID, List<Integer> choices){
		if(refreshing)
			return;
		new SubmitPollVote(pollID, choices)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Poll result){
						E.post(new PollUpdatedEvent(accountID, result));
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, true)
				.exec(accountID);
	}

	public void onRevealSpoilerClick(SpoilerStatusDisplayItem.Holder holder){
		Status status=holder.getItem().status;
		boolean isForQuote=holder.getItem().isForQuote;
		toggleSpoiler(status, isForQuote, holder.getItemID());
	}

	public void updateStatusWithQuote(DisplayItemsParent parent) {
		Pair<Integer, Integer> items=findAllItemsOfParent(parent);
		if (items==null)
			return;

		// Only StatusListFragments/NotificationsListFragments can display status with quotes
		assert (this instanceof StatusListFragment) || (this instanceof NotificationsListFragment);
		List<StatusDisplayItem> oldItems = displayItems.subList(items.first, items.second+1);
		List<StatusDisplayItem> newItems=this.buildDisplayItems((T) parent);
		int prevSize=oldItems.size();
		oldItems.clear();
		displayItems.addAll(items.first, newItems);

		// Update the cache
		final CacheController cache=AccountSessionManager.get(accountID).getCacheController();
		if (parent instanceof Status) {
			cache.updateStatus((Status) parent);
		} else if (parent instanceof Notification) {
			cache.updateNotification((Notification) parent);
		}

		adapter.notifyItemRangeRemoved(items.first, prevSize);
		adapter.notifyItemRangeInserted(items.first, newItems.size());
	}

	public void removeStatus(DisplayItemsParent parent) {
		Pair<Integer, Integer> items=findAllItemsOfParent(parent);
		if (items==null)
			return;

		List<StatusDisplayItem> statusDisplayItems = displayItems.subList(items.first, items.second+1);
		int prevSize=statusDisplayItems.size();
		statusDisplayItems.clear();
		adapter.notifyItemRangeRemoved(items.first, prevSize);
	}

	public void onVisibilityIconClick(HeaderStatusDisplayItem.Holder holder) {
		Status status = holder.getItem().status;
		if(holder.getItem().hasVisibilityToggle) holder.animateVisibilityToggle(false);
		MediaGridStatusDisplayItem.Holder mediaGrid=findHolderOfType(holder.getItemID(), MediaGridStatusDisplayItem.Holder.class);
		if(mediaGrid!=null){
			if(!status.sensitiveRevealed) mediaGrid.revealSensitive();
			else mediaGrid.hideSensitive();
		}else{
			status.sensitiveRevealed=false;
			notifyItemChangedAfter(holder.getItem(), MediaGridStatusDisplayItem.class);
		}
	}

	public void onSensitiveRevealed(MediaGridStatusDisplayItem.Holder holder) {
		HeaderStatusDisplayItem.Holder header=findHolderOfType(holder.getItemID(), HeaderStatusDisplayItem.Holder.class);
		if(header!=null && header.getItem().hasVisibilityToggle) header.animateVisibilityToggle(true);
		else notifyItemChangedBefore(holder.getItem(), HeaderStatusDisplayItem.class);
	}

	protected void toggleSpoiler(Status status, boolean isForQuote, String itemID){
		status.spoilerRevealed=!status.spoilerRevealed;
		if (!status.spoilerRevealed && !AccountSessionManager.get(accountID).getLocalPreferences().revealCWs)
			status.sensitiveRevealed = false;

		List<SpoilerStatusDisplayItem.Holder> spoilers=findAllHoldersOfType(itemID, SpoilerStatusDisplayItem.Holder.class);
		SpoilerStatusDisplayItem.Holder spoiler=spoilers.size() > 1 && isForQuote ? spoilers.get(1) : spoilers.get(0);
		if(spoiler!=null) spoiler.rebind();
		else notifyItemChanged(itemID, SpoilerStatusDisplayItem.class);
		SpoilerStatusDisplayItem spoilerItem=Objects.requireNonNull(spoiler.getItem());

		int index=displayItems.indexOf(spoilerItem);
		if(status.spoilerRevealed){
			displayItems.addAll(index+1, spoilerItem.contentItems);
			adapter.notifyItemRangeInserted(index+1, spoilerItem.contentItems.size());
		}else{
			if(spoilers.size()>1 && !isForQuote && status.quote.spoilerRevealed)
				toggleSpoiler(status.quote, true, itemID);
			displayItems.subList(index+1, index+1+spoilerItem.contentItems.size()).clear();
			adapter.notifyItemRangeRemoved(index+1, spoilerItem.contentItems.size());
		}

		notifyItemChanged(itemID, TextStatusDisplayItem.class);
		HeaderStatusDisplayItem.Holder header=findHolderOfType(itemID, HeaderStatusDisplayItem.Holder.class);
		if(header!=null) header.rebind();
		else notifyItemChanged(itemID, HeaderStatusDisplayItem.class);

		list.invalidateItemDecorations();
	}

	public void onEnableExpandable(TextStatusDisplayItem.Holder holder, boolean expandable, boolean isForQuote) {
		Status s=holder.getItem().status;
		if(s.textExpandable!=expandable && list!=null) {
			s.textExpandable=expandable;
			List<HeaderStatusDisplayItem.Holder> headers=findAllHoldersOfType(holder.getItemID(), HeaderStatusDisplayItem.Holder.class);
			if(headers!=null && !headers.isEmpty()){
				HeaderStatusDisplayItem.Holder header=headers.size() > 1 && isForQuote ? headers.get(1) : headers.get(0);
				if(header!=null) header.bindCollapseButton();
			}
		}
	}

	public void onToggleExpanded(Status status, boolean isForQuote, String itemID) {
		status.textExpanded = !status.textExpanded;
		// TODO: simplify this to a single case
		if(!isForQuote)
			// using the adapter directly to update the item does not work for non-quoted texts
			notifyItemChanged(itemID, TextStatusDisplayItem.class);
		else{
			List<TextStatusDisplayItem.Holder> textItems=findAllHoldersOfType(itemID, TextStatusDisplayItem.Holder.class);
			TextStatusDisplayItem.Holder text=textItems.size()>1 ? textItems.get(1) : textItems.get(0);
			adapter.notifyItemChanged(text.getAbsoluteAdapterPosition());
		}
		List<HeaderStatusDisplayItem.Holder> headers=findAllHoldersOfType(itemID, HeaderStatusDisplayItem.Holder.class);
		if (headers.isEmpty())
			return;
		HeaderStatusDisplayItem.Holder header=headers.size() > 1 && isForQuote ? headers.get(1) : headers.get(0);
		if(header!=null) header.animateExpandToggle();
		else notifyItemChanged(itemID, HeaderStatusDisplayItem.class);
	}

	public void onGapClick(GapStatusDisplayItem.Holder item, boolean downwards){}

	public void onWarningClick(WarningFilteredStatusDisplayItem.Holder warning){
		WarningFilteredStatusDisplayItem filterItem=findItemOfType(warning.getItemID(), WarningFilteredStatusDisplayItem.class);
		int startPos=displayItems.indexOf(filterItem);
		displayItems.remove(startPos);
		displayItems.addAll(startPos, warning.filteredItems);
		adapter.notifyItemRangeInserted(startPos, warning.filteredItems.size() - 1);
		if (startPos == 0) scrollToTop();
		warning.getItem().status.filterRevealed = true;
		list.invalidateItemDecorations();
	}

	public void onFavoriteChanged(Status status, String itemID) {
		FooterStatusDisplayItem.Holder footer=findHolderOfType(itemID, FooterStatusDisplayItem.Holder.class);
		if(footer!=null){
			footer.getItem().status=status;
			footer.onFavoriteClick();
		}
	}

	@Override
	public String getAccountID(){
		return accountID;
	}

	public Relationship getRelationship(String id){
		return relationships.get(id);
	}

	public void putRelationship(String id, Relationship rel){
		relationships.put(id, rel);
	}

	protected void loadRelationships(Set<String> ids){
		if(ids.isEmpty())
			return;
		ids=ids.stream().filter(id->!relationships.containsKey(id)).collect(Collectors.toSet());
		if(ids.isEmpty())
			return;
		// TODO somehow manage these and cancel outstanding requests on refresh
		new GetAccountRelationships(ids)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						for(Relationship r:result)
							relationships.put(r.id, r);
						onRelationshipsLoaded();
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(accountID);
	}

	protected void onRelationshipsLoaded(){}

	@Nullable
	protected <I extends StatusDisplayItem> I findItemOfType(String id, Class<I> type){
		for(StatusDisplayItem item:displayItems){
			if(item.parentID.equals(id) && type.isInstance(item))
				return type.cast(item);
		}
		return null;
	}

	/**
	 * Use this as a fallback if findHolderOfType fails to find the ViewHolder.
	 * It might still be bound but off-screen and therefore not a child of the RecyclerView -
	 * resulting in the ViewHolder displaying an outdated state once scrolled back into view.
	 */
	protected <I extends StatusDisplayItem> int notifyItemChanged(String id, Class<I> type){
		boolean encounteredParent=false;
		for(int i=0; i<displayItems.size(); i++){
			StatusDisplayItem item=displayItems.get(i);
			boolean idEquals=id.equals(item.parentID);
			if(!encounteredParent && idEquals) encounteredParent=true; // reached top of the parent
			else if(encounteredParent && !idEquals) break; // passed by bottom of the parent. man muss ja wissen wann schluss is
			if(idEquals && type.isInstance(item)){
				adapter.notifyItemChanged(i);
				return i;
			}
		}
		return -1;
	}

	protected <I extends StatusDisplayItem> int notifyItemChangedAfter(StatusDisplayItem afterThis, Class<I> type){
		int startIndex=displayItems.indexOf(afterThis);
		if(startIndex == -1) throw new IllegalStateException("notifyItemChangedAfter didn't find the passed StatusDisplayItem");
		String parentID=afterThis.parentID;
		for(int i=startIndex; i<displayItems.size(); i++){
			StatusDisplayItem item=displayItems.get(i);
			if(!parentID.equals(item.parentID)) break; // didn't find anything
			if(type.isInstance(item)){
				// found it
				adapter.notifyItemChanged(i);
				return i;
			}
		}
		return -1;
	}

	protected <I extends StatusDisplayItem> int notifyItemChangedBefore(StatusDisplayItem beforeThis, Class<I> type){
		int startIndex=displayItems.indexOf(beforeThis);
		if(startIndex == -1) throw new IllegalStateException("notifyItemChangedBefore didn't find the passed StatusDisplayItem");
		String parentID=beforeThis.parentID;
		for(int i=startIndex; i>=0; i--){
			StatusDisplayItem item=displayItems.get(i);
			if(!parentID.equals(item.parentID)) break; // didn't find anything
			if(type.isInstance(item)){
				// found it
				adapter.notifyItemChanged(i);
				return i;
			}
		}
		return -1;
	}

	@Nullable
	protected <I extends StatusDisplayItem, H extends StatusDisplayItem.Holder<I>> H findHolderOfType(String id, Class<H> type){
		for(int i=0; i<list.getChildCount(); i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof StatusDisplayItem.Holder<?> itemHolder && itemHolder.getItemID().equals(id) && type.isInstance(holder))
				return type.cast(holder);
		}
		return null;
	}

	@Nullable
	protected Pair<Integer, Integer> findAllItemsOfParent(DisplayItemsParent parent){
		int startIndex=-1;
		int endIndex=-1;
		for(int i=0; i<displayItems.size(); i++){
			StatusDisplayItem item = displayItems.get(i);
			if(item.parentID.equals(parent.getID())) {
				startIndex= startIndex==-1 ? i : startIndex;
				endIndex=i;
			}
		}

		if(startIndex==-1 || endIndex==-1)
			return null;
		return Pair.create(startIndex, endIndex);
	}

	protected <I extends StatusDisplayItem, H extends StatusDisplayItem.Holder<I>> List<H> findAllHoldersOfType(String id, Class<H> type){
		ArrayList<H> holders=new ArrayList<>();
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof StatusDisplayItem.Holder<?> itemHolder && itemHolder.getItemID().equals(id) && type.isInstance(holder))
				holders.add(type.cast(holder));
		}
		return holders;
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	@Override
	public boolean isOnTop() {
		return isRecyclerViewOnTop(list);
	}

	protected int getListWidthForMediaLayout(){
		return list.getWidth();
	}

	protected boolean wantsOverlaySystemNavigation(){
		return true;
	}

	protected void onSetFabBottomInset(int inset){

	}

	public boolean isItemEnabled(String id){
		return true;
	}

	public ArrayList<StatusDisplayItem> getDisplayItems(){
		return displayItems;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0 && wantsOverlaySystemNavigation()){
			list.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
			onSetFabBottomInset(insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}else{
			onSetFabBottomInset(0);
		}
		super.onApplyWindowInsets(insets);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
		if(requestCode==PhotoViewer.PERMISSION_REQUEST && currentPhotoViewer!=null){
			currentPhotoViewer.onRequestPermissionsResult(permissions, grantResults);
		}
	}

	@Override
	public void onPause(){
		super.onPause();
		if(currentPhotoViewer!=null)
			currentPhotoViewer.onPause();
	}

	public void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	public boolean onFabLongClick(View v) {
		return UiUtils.pickAccountForCompose(getActivity(), accountID);
	}

	private MediaAttachmentViewController makeNewMediaAttachmentView(MediaGridStatusDisplayItem.GridItemType type){
		return new MediaAttachmentViewController(getActivity(), type);
	}

	private PreviewlessMediaAttachmentViewController makeNewPreviewlessMediaAttachmentView(MediaGridStatusDisplayItem.GridItemType type){
		return new PreviewlessMediaAttachmentViewController(getActivity(), type);
	}

	public TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, MediaAttachmentViewController> getAttachmentViewsPool(){
		return attachmentViewsPool;
	}

	public TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, PreviewlessMediaAttachmentViewController> getPreviewlessAttachmentViewsPool(){
		return previewlessAttachmentViewsPool;
	}

	@Override
	public void onProvideAssistContent(AssistContent assistContent) {
		assistContent.setWebUri(getWebUri(getSession().getInstanceUri().buildUpon()));
	}

	public void togglePostTranslation(Status status, String itemID){
		switch(status.translationState){
			case LOADING -> {
				return;
			}
			case SHOWN -> {
				status.translationState=Status.TranslationState.HIDDEN;
			}
			case HIDDEN -> {
				if(status.translation!=null){
					status.translationState=Status.TranslationState.SHOWN;
				}else{
					status.translationState=Status.TranslationState.LOADING;
					Consumer<Translation> successCallback=(result)->{
						status.translation=result;
						status.translationState=Status.TranslationState.SHOWN;
						updateTranslation(itemID);
					};
					MastodonAPIRequest<?> req=isInstanceAkkoma()
							? new AkkomaTranslateStatus(status.getContentStatus().id, Locale.getDefault().getLanguage()).setCallback(new Callback<>(){
								@Override
								public void onSuccess(AkkomaTranslation result){
									if(getActivity()!=null) successCallback.accept(result.toTranslation());
								}
								@Override
								public void onError(ErrorResponse error){
									if(getActivity()!=null) translationCallbackError(status, itemID);
								}
							})
							: new TranslateStatus(status.getContentStatus().id, Locale.getDefault().getLanguage()).setCallback(new Callback<>(){
								@Override
								public void onSuccess(Translation result){
									if(getActivity()!=null) successCallback.accept(result);
								}

								@Override
								public void onError(ErrorResponse error){
									if(getActivity()!=null) translationCallbackError(status, itemID);
								}
							});

					// 1 minute
					req.setTimeout(60000).exec(accountID);
				}
			}
		}
		updateTranslation(itemID);
	}

	private void translationCallbackError(Status status, String itemID) {
		status.translationState=Status.TranslationState.HIDDEN;
		updateTranslation(itemID);
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.error)
				.setMessage(R.string.translation_failed)
				.setPositiveButton(R.string.ok, null)
				.show();
	}

	private void updateTranslation(String itemID) {
		TextStatusDisplayItem.Holder text=findHolderOfType(itemID, TextStatusDisplayItem.Holder.class);
		if(text!=null){
			text.updateTranslation(true);
			imgLoader.bindViewHolder((ImageLoaderRecyclerAdapter) list.getAdapter(), text, text.getAbsoluteAdapterPosition());
		}else{
			notifyItemChanged(itemID, TextStatusDisplayItem.class);
		}

		if(isInstanceAkkoma())
			return;

		SpoilerStatusDisplayItem.Holder spoiler=findHolderOfType(itemID, SpoilerStatusDisplayItem.Holder.class);
		if(spoiler!=null){
			spoiler.rebind();
		}

		MediaGridStatusDisplayItem.Holder media=findHolderOfType(itemID, MediaGridStatusDisplayItem.Holder.class);
		if (media!=null) {
			media.rebind();
		}

		PreviewlessMediaGridStatusDisplayItem.Holder previewLessMedia=findHolderOfType(itemID, PreviewlessMediaGridStatusDisplayItem.Holder.class);
		if (previewLessMedia!=null) {
			previewLessMedia.rebind();
		}

		for(int i=0;i<list.getChildCount();i++){
			if(list.getChildViewHolder(list.getChildAt(i)) instanceof PollOptionStatusDisplayItem.Holder item){
				item.rebind();
			}
		}
	}

	public void rebuildAllDisplayItems(){
		displayItems.clear();
		for(T item:data){
			displayItems.addAll(buildDisplayItems(item));
		}
		adapter.notifyDataSetChanged();
	}

	public void maybeShowPreReplySheet(Status status, Runnable proceed){
		Relationship rel=getRelationship(status.account.id);
		if(!GlobalUserPreferences.isOptedOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.NON_MUTUAL, status.account, accountID) &&
				!status.account.id.equals(AccountSessionManager.get(accountID).self.id) && rel!=null && !rel.followedBy && status.account.followingCount>=1){
			new NonMutualPreReplySheet(getActivity(), notAgain->{
				GlobalUserPreferences.optOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.NON_MUTUAL, notAgain ? null : status.account, accountID);
				proceed.run();
			}, status.account, accountID).show();
		}else if(!GlobalUserPreferences.isOptedOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.OLD_POST, null, null) &&
				status.createdAt.isBefore(Instant.now().minus(90, ChronoUnit.DAYS))){
			new OldPostPreReplySheet(getActivity(), notAgain->{
				if(notAgain)
					GlobalUserPreferences.optOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.OLD_POST, null, null);
				proceed.run();
			}, status).show();
		}else{
			proceed.run();
		}
	}

	protected void onModifyItemViewHolder(BindableViewHolder<StatusDisplayItem> holder){}

	@Override
	protected void onDataLoaded(List<T> d, boolean more) {
		if(getContext()==null) return;
		super.onDataLoaded(d, more);
		// more available, but the page isn't even full yet? seems wrong, let's load some more
		if(more && data.size() < itemsPerPage){
			preloader.onScrolledToLastItem();
		}
	}

	public void scrollBy(int x, int y) {
		list.scrollBy(x, y);
	}

	protected class DisplayItemsAdapter extends UsableRecyclerView.Adapter<BindableViewHolder<StatusDisplayItem>> implements ImageLoaderRecyclerAdapter{

		public DisplayItemsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BindableViewHolder<StatusDisplayItem> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			BindableViewHolder<StatusDisplayItem> holder=(BindableViewHolder<StatusDisplayItem>) StatusDisplayItem.createViewHolder(StatusDisplayItem.Type.values()[viewType & (~0x80000000)], getActivity(), parent, BaseStatusListFragment.this);
			onModifyItemViewHolder(holder);
			return holder;
		}

		@Override
		public void onBindViewHolder(BindableViewHolder<StatusDisplayItem> holder, int position){
			holder.bind(displayItems.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return displayItems.size();
		}

		@Override
		public int getItemViewType(int position){
			return displayItems.get(position).getType().ordinal() | 0x80000000;
		}

		@Override
		public int getImageCountForItem(int position){
			return displayItems.get(position).getImageCount();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return displayItems.get(position).getImageRequest(image);
		}
	}

	private class StatusListItemDecoration extends RecyclerView.ItemDecoration{
		private Paint dividerPaint=new Paint();

		{
			dividerPaint.setColor(UiUtils.getThemeColor(getActivity(), GlobalUserPreferences.showDividers ? R.attr.colorM3OutlineVariant : R.attr.colorM3Surface));
			dividerPaint.setStyle(Paint.Style.STROKE);
			dividerPaint.setStrokeWidth(V.dp(1f));
		}

		@Override
		public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
			for(int i=0;i<parent.getChildCount()-1;i++){
				View child=parent.getChildAt(i);
				View bottomSibling=parent.getChildAt(i+1);
				RecyclerView.ViewHolder holder=parent.getChildViewHolder(child);
				RecyclerView.ViewHolder siblingHolder=parent.getChildViewHolder(bottomSibling);
				if(needDrawDivider(holder, siblingHolder)){
					drawDivider(child, bottomSibling, holder, siblingHolder, parent, c, dividerPaint);
				}
			}
		}

		private boolean needDrawDivider(RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
			if(needDividerForExtraItem(holder.itemView, siblingHolder.itemView, holder, siblingHolder))
				return true;
			if(holder instanceof StatusDisplayItem.Holder<?> ih && siblingHolder instanceof StatusDisplayItem.Holder<?> sh){
				// Do not draw dividers between hashtag and/or account rows
				if((ih instanceof HashtagStatusDisplayItem.Holder || ih instanceof AccountStatusDisplayItem.Holder) && (sh instanceof HashtagStatusDisplayItem.Holder || sh instanceof AccountStatusDisplayItem.Holder))
					return false;
				if (!ih.getItem().isMainStatus && ih.getItem().hasDescendantNeighbor) return false;
				return (!ih.getItemID().equals(sh.getItemID()) || sh instanceof ExtendedFooterStatusDisplayItem.Holder) && ih.getItem().getType()!=StatusDisplayItem.Type.GAP;
			}
			return false;
		}
	}
}
