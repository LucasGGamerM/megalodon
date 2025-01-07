package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.announcements.AddAnnouncementReaction;
import org.joinmastodon.android.api.requests.announcements.DeleteAnnouncementReaction;
import org.joinmastodon.android.api.requests.statuses.AddStatusReaction;
import org.joinmastodon.android.api.requests.statuses.DeleteStatusReaction;
import org.joinmastodon.android.api.requests.statuses.PleromaAddStatusReaction;
import org.joinmastodon.android.api.requests.statuses.PleromaDeleteStatusReaction;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.EmojiReactionsUpdatedEvent;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.account_list.StatusEmojiReactionsListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiReaction;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.CustomEmojiPopupKeyboard;
import org.joinmastodon.android.ui.utils.TextDrawable;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ProgressBarButton;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class EmojiReactionsStatusDisplayItem extends StatusDisplayItem {
	private final Drawable placeholder;
	private final boolean hideEmpty, forAnnouncement, playGifs;
	private final String accountID;
	private static final float ALPHA_DISABLED=0.55f;

    public EmojiReactionsStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, Status status, String accountID, boolean hideEmpty, boolean forAnnouncement) {
		super(parentID, parentFragment);
		this.status=status;
		this.hideEmpty=hideEmpty;
		this.forAnnouncement=forAnnouncement;
		this.accountID=accountID;
		placeholder=parentFragment.getContext().getDrawable(R.drawable.image_placeholder).mutate();
		placeholder.setBounds(0, 0, V.sp(24), V.sp(24));
		playGifs=GlobalUserPreferences.playGifs;
    }

	@Override
	public int getImageCount(){
		return (int) status.reactions.stream().filter(r->r.getUrl(playGifs)!=null).count();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return status.reactions.get(index).request;
	}

    @Override
    public Type getType(){
        return Type.EMOJI_REACTIONS;
    }

	public boolean isHidden(){
		return status.reactions.isEmpty() && hideEmpty;
	}

	// borrowed from ProfileFragment
	private void setActionProgressVisible(Holder.EmojiReactionViewHolder vh, boolean visible){
		if(vh==null) return;
		vh.progress.setVisibility(visible ? View.VISIBLE : View.GONE);
		vh.btn.setClickable(!visible);
		vh.btn.setAlpha(visible ? ALPHA_DISABLED : 1);
	}

	private MastodonAPIRequest<?> createRequest(String name, int count, boolean delete, Holder.EmojiReactionViewHolder vh, Runnable cb, Runnable err){
		setActionProgressVisible(vh, true);
		boolean ak=parentFragment.isInstanceAkkoma();
		boolean keepSpinning=delete && count == 1;
		if(forAnnouncement){
			MastodonAPIRequest<Object> req=delete
					? new DeleteAnnouncementReaction(status.id, name)
					: new AddAnnouncementReaction(status.id, name);
			return req.setCallback(new Callback<>(){
				@Override
				public void onSuccess(Object result){
					if(!keepSpinning) setActionProgressVisible(vh, false);
					cb.run();
				}
				@Override
				public void onError(ErrorResponse error){
					setActionProgressVisible(vh, false);
					error.showToast(parentFragment.getContext());
					if(err!=null) err.run();
				}
			});
		}else{
			MastodonAPIRequest<Status> req=delete
					? (ak ? new PleromaDeleteStatusReaction(status.id, name) : new DeleteStatusReaction(status.id, name))
					: (ak ? new PleromaAddStatusReaction(status.id, name) : new AddStatusReaction(status.id, name));
			return req.setCallback(new Callback<>(){
				@Override
				public void onSuccess(Status result){
					if(!keepSpinning) setActionProgressVisible(vh, false);
					cb.run();
				}
				@Override
				public void onError(ErrorResponse error){
					setActionProgressVisible(vh, false);
					error.showToast(parentFragment.getContext());
					if(err!=null) err.run();
				}
			});
		}
	}

	public static class Holder extends StatusDisplayItem.Holder<EmojiReactionsStatusDisplayItem> implements ImageLoaderViewHolder, CustomEmojiPopupKeyboard.Listener {
		private final UsableRecyclerView list;
		private final LinearLayout root, line;
		private CustomEmojiPopupKeyboard emojiKeyboard;
		private final View space;
		private final ImageButton addButton;
		private final ProgressBar progress;
		private final EmojiReactionsAdapter adapter;
		private final ListImageLoaderWrapper imgLoader;

		public Holder(Activity activity, ViewGroup parent) {
			super(activity, R.layout.display_item_emoji_reactions, parent);
			root=(LinearLayout) itemView;
			line=findViewById(R.id.line);
			list=findViewById(R.id.list);
			imgLoader=new ListImageLoaderWrapper(activity, list, new RecyclerViewDelegate(list), null);
			list.setAdapter(adapter=new EmojiReactionsAdapter(this, imgLoader));
			addButton=findViewById(R.id.add_btn);
			progress=findViewById(R.id.progress);
			addButton.setOnClickListener(this::onReactClick);
			space=findViewById(R.id.space);
			list.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        }

        @Override
        public void onBind(EmojiReactionsStatusDisplayItem item) {
			if(emojiKeyboard != null) root.removeView(emojiKeyboard.getView());
			addButton.setSelected(false);
			AccountSession session=item.parentFragment.getSession();
			item.status.reactions.forEach(r->r.request=r.getUrl(item.playGifs)!=null
					? new UrlImageLoaderRequest(r.getUrl(item.playGifs), 0, V.sp(24))
					: null);
			emojiKeyboard=new CustomEmojiPopupKeyboard(
					(Activity) item.parentFragment.getContext(),
					item.accountID,
					AccountSessionManager.getInstance().getCustomEmojis(session.domain),
					session.domain, true);
			emojiKeyboard.setListener(this);
			space.setVisibility(View.GONE);
			root.addView(emojiKeyboard.getView());
			boolean hidden=item.isHidden();
			root.setVisibility(hidden ? View.GONE : View.VISIBLE);
			line.setVisibility(hidden ? View.GONE : View.VISIBLE);
			line.setPadding(
					list.getPaddingLeft(),
					hidden ? 0 : V.dp(8),
					list.getPaddingRight(),
					item.forAnnouncement ? V.dp(8) : 0
			);
			imgLoader.updateImages();
			adapter.notifyDataSetChanged();
        }

		private void hideEmojiKeyboard(){
			space.setVisibility(View.GONE);
			addButton.setSelected(false);
			if(emojiKeyboard.isVisible()) emojiKeyboard.toggleKeyboardPopup(null);
		}

		@Override
		public void onEmojiSelected(Emoji emoji) {
			addEmojiReaction(emoji.shortcode, emoji);
			hideEmojiKeyboard();
		}

		@Override
		public void onEmojiSelected(String emoji){
			addEmojiReaction(emoji, null);
			hideEmojiKeyboard();
		}

		private void addEmojiReaction(String emoji, Emoji info) {
			int countBefore=item.status.reactions.size();
			for(int i=0; i<item.status.reactions.size(); i++){
				EmojiReaction r=item.status.reactions.get(i);
				if(r.name.equals(emoji) && r.me){
					RecyclerView.SmoothScroller scroller=new LinearSmoothScroller(list.getContext());
					scroller.setTargetPosition(i);
					list.getLayoutManager().startSmoothScroll(scroller);
					return; // nothing to do, already added
				}
			}

			progress.setVisibility(View.VISIBLE);
			addButton.setClickable(false);
			addButton.setAlpha(ALPHA_DISABLED);

			Runnable resetBtn=()->{
				progress.setVisibility(View.GONE);
				addButton.setClickable(true);
				addButton.setAlpha(1f);
			};
			Account me=AccountSessionManager.get(item.accountID).self;
			EmojiReaction existing=null;
			for(int i=0; i<item.status.reactions.size(); i++){
				EmojiReaction r=item.status.reactions.get(i);
				if(r.name.equals(emoji)){
					existing=r;
					break;
				}
			}
			EmojiReaction finalExisting=existing;
			item.createRequest(emoji, existing==null ? 1 : existing.count, false, null, ()->{
				resetBtn.run();
				if(finalExisting==null){
					int pos=item.status.reactions.size();
					item.status.reactions.add(pos, info!=null ? EmojiReaction.of(info, me) : EmojiReaction.of(emoji, me));
					adapter.notifyItemRangeInserted(pos, 1);
					RecyclerView.SmoothScroller scroller=new LinearSmoothScroller(list.getContext());
					scroller.setTargetPosition(pos);
					list.getLayoutManager().startSmoothScroll(scroller);
				}else{
					finalExisting.add(me);
					adapter.notifyItemChanged(item.status.reactions.indexOf(finalExisting));
				}
				E.post(new EmojiReactionsUpdatedEvent(item.status.id, item.status.reactions, countBefore==0, adapter.parentHolder));
			}, resetBtn).exec(item.accountID);
		}

		@Override
		public void onBackspace() {}

		private void onReactClick(View v){
			emojiKeyboard.customToggleKeyboardPopup();
			v.setSelected(emojiKeyboard.isVisible());
			space.setVisibility(emojiKeyboard.isVisible() ? View.VISIBLE : View.GONE);
			DisplayMetrics displayMetrics = new DisplayMetrics();
			int[] locationOnScreen = new int[2];
			((Activity) v.getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
			v.getLocationOnScreen(locationOnScreen);
			double fromScreenTop = (double) locationOnScreen[1] / displayMetrics.heightPixels;
			if (fromScreenTop > 0.75) {
				item.parentFragment.scrollBy(0, (int) (displayMetrics.heightPixels * 0.3));
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			View child=list.getChildAt(index);
			if(child==null) return;
			((EmojiReactionViewHolder) list.getChildViewHolder(child)).setImage(index, image);
		}

		@Override
		public void clearImage(int index){
			if(item.status.reactions.get(index).getUrl(item.playGifs)==null) return;
			setImage(index, item.placeholder);
		}

		private class EmojiReactionsAdapter extends UsableRecyclerView.Adapter<EmojiReactionViewHolder> implements ImageLoaderRecyclerAdapter{
			ListImageLoaderWrapper imgLoader;
			Holder parentHolder;

			public EmojiReactionsAdapter(Holder parentHolder, ListImageLoaderWrapper imgLoader){
				super(imgLoader);
				this.parentHolder=parentHolder;
				this.imgLoader=imgLoader;
			}

			@NonNull
			@Override
			public EmojiReactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
				return new EmojiReactionViewHolder(parent.getContext(), list);
			}

			@Override
			public void onBindViewHolder(EmojiReactionViewHolder holder, int position){
				holder.bind(Pair.create(item, item.status.reactions.get(position)));
				super.onBindViewHolder(holder, position);
			}

			@Override
			public int getItemCount(){
				return item.status.reactions.size();
			}

			@Override
			public int getImageCountForItem(int position){
				return item.status.reactions.get(position).getUrl(item.playGifs)==null ? 0 : 1;
			}

			@Override
			public ImageLoaderRequest getImageRequest(int position, int image){
				return item.status.reactions.get(position).request;
			}
		}

		private static class EmojiReactionViewHolder extends BindableViewHolder<Pair<EmojiReactionsStatusDisplayItem, EmojiReaction>> implements ImageLoaderViewHolder{
			private final ProgressBarButton btn;
			private final ProgressBar progress;

			public EmojiReactionViewHolder(Context context, RecyclerView list){
				super(context, R.layout.item_emoji_reaction, list);
				btn=findViewById(R.id.btn);
				progress=findViewById(R.id.progress);
				itemView.setClickable(true);
			}

			@Override
			public void setImage(int index, Drawable drawable){
				int height=V.sp(24);
				int width=drawable.getIntrinsicWidth()*height/drawable.getIntrinsicHeight();
				drawable.setBounds(0, 0, width, height);
				btn.setCompoundDrawablesRelative(drawable, null, null, null);
				if(drawable instanceof Animatable) ((Animatable) drawable).start();
			}

			@Override
			public void clearImage(int index){
				setImage(index, item.first.placeholder);
			}

			@Override
			public void onBind(Pair<EmojiReactionsStatusDisplayItem, EmojiReaction> item){
				item.first.setActionProgressVisible(this, false);
				EmojiReactionsStatusDisplayItem parent=item.first;
				EmojiReaction reaction=item.second;
				btn.setText(UiUtils.abbreviateNumber(reaction.count));
				btn.setContentDescription(reaction.name);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) btn.setTooltipText(reaction.name);
				if(reaction.getUrl(parent.playGifs)==null){
					Paint p=new Paint();
					p.setTextSize(V.sp(18));
					TextDrawable drawable=new TextDrawable(p, reaction.name);
					btn.setCompoundDrawablesRelative(drawable, null, null, null);
				}else{
					btn.setCompoundDrawablesRelative(item.first.placeholder, null, null, null);
				}
				btn.setSelected(reaction.me);
				btn.setOnClickListener(e->{
					boolean deleting=reaction.me;
					parent.createRequest(reaction.name, reaction.count, deleting, this, ()->{
						EmojiReactionsAdapter adapter = (EmojiReactionsAdapter) getBindingAdapter();
						for(int i=0; i<parent.status.reactions.size(); i++){
							EmojiReaction r=parent.status.reactions.get(i);
							if(!r.name.equals(reaction.name)) continue;
							if(deleting && r.count==1) {
								parent.status.reactions.remove(i);
								adapter.notifyItemRemoved(i);
								break;
							}
							r.me=!deleting;
							if(deleting) r.count--;
							else r.count++;
							adapter.notifyItemChanged(i);
							break;
						}

						if(parent.isHidden()){
							adapter.parentHolder.root.setVisibility(View.GONE);
							adapter.parentHolder.line.setVisibility(View.GONE);
						}
						E.post(new EmojiReactionsUpdatedEvent(parent.status.id, parent.status.reactions, parent.status.reactions.isEmpty(), adapter.parentHolder));
						adapter.parentHolder.imgLoader.updateImages();
					}, null).exec(parent.parentFragment.getAccountID());
				});

				if (parent.parentFragment.isInstanceAkkoma()) {
					// glitch-soc doesn't have this, afaik
					btn.setOnLongClickListener(e->{
						EmojiReaction emojiReaction=parent.status.reactions.get(getAbsoluteAdapterPosition());
						Bundle args=new Bundle();
						args.putString("account", parent.parentFragment.getAccountID());
						args.putString("statusID", parent.status.id);
						int atSymbolIndex = emojiReaction.name.indexOf("@");
						args.putString("emoji", atSymbolIndex != -1 ? emojiReaction.name.substring(0, atSymbolIndex) : emojiReaction.name);
						args.putString("url", emojiReaction.getUrl(parent.playGifs));
						args.putInt("count", emojiReaction.count);
						Nav.go(parent.parentFragment.getActivity(), StatusEmojiReactionsListFragment.class, args);
						return true;
					});
				}
			}
		}
	}
}
