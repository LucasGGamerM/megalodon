package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.function.Consumer;

import me.grishka.appkit.Nav;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class FooterStatusDisplayItem extends StatusDisplayItem{
	private final String accountID;
	public boolean hideCounts;

	public FooterStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status, String accountID){
		super(parentID, parentFragment);
		this.status=status;
		this.accountID=accountID;
	}

	@Override
	public Type getType(){
		return Type.FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<FooterStatusDisplayItem>{
		private final TextView replies, boosts, favorites;
		private final View reply, boost, favorite, share, bookmark;
		private final ImageView favIcon;
		private static Animation spin;


		private View touchingView = null;
		private boolean longClickPerformed = false;
		private final Runnable longClickRunnable = () -> {
			longClickPerformed = touchingView != null && touchingView.performLongClick();
			if (longClickPerformed && touchingView != null) {
				UiUtils.opacityIn(touchingView);
				touchingView.animate().scaleX(1).scaleY(1).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(150).start();
			}
		};

		private final View.AccessibilityDelegate buttonAccessibilityDelegate=new View.AccessibilityDelegate(){
			@Override
			public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info){
				super.onInitializeAccessibilityNodeInfo(host, info);
				info.setClassName(Button.class.getName());
				info.setText(item.parentFragment.getString(descriptionForId(host.getId())));
			}
		};

		static {
			spin = new RotateAnimation(0, 360,
					Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
					0.5f);
			spin.setDuration(400);
		}

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_footer, parent);

			replies=findViewById(R.id.reply);
			boosts=findViewById(R.id.boost);
			favorites=findViewById(R.id.favorite);

			reply=findViewById(R.id.reply_btn);
			boost=findViewById(R.id.boost_btn);
			favorite=findViewById(R.id.favorite_btn);
			share=findViewById(R.id.share_btn);
			bookmark=findViewById(R.id.bookmark_btn);
			favIcon=findViewById(R.id.favorite_icon);

			reply.setOnTouchListener(this::onButtonTouch);
			reply.setOnClickListener(this::onReplyClick);
			reply.setOnLongClickListener(this::onReplyLongClick);
			reply.setAccessibilityDelegate(buttonAccessibilityDelegate);
			boost.setOnTouchListener(this::onButtonTouch);
			boost.setOnClickListener(this::onBoostClick);
			boost.setOnLongClickListener(this::onBoostLongClick);
			boost.setAccessibilityDelegate(buttonAccessibilityDelegate);
			favorite.setOnTouchListener(this::onButtonTouch);
			favorite.setOnClickListener(this::onFavoriteClick);
			favorite.setOnLongClickListener(this::onFavoriteLongClick);
			favorite.setAccessibilityDelegate(buttonAccessibilityDelegate);
			bookmark.setOnTouchListener(this::onButtonTouch);
			bookmark.setOnClickListener(this::onBookmarkClick);
			bookmark.setOnLongClickListener(this::onBookmarkLongClick);
			bookmark.setAccessibilityDelegate(buttonAccessibilityDelegate);
			share.setOnTouchListener(this::onButtonTouch);
			share.setOnClickListener(this::onShareClick);
			share.setOnLongClickListener(this::onShareLongClick);
			share.setAccessibilityDelegate(buttonAccessibilityDelegate);
		}

		@Override
		public void onBind(FooterStatusDisplayItem item){
			bindText(replies, item.status.repliesCount);
			bindText(boosts, item.status.reblogsCount);
			bindText(favorites, item.status.favouritesCount);
			// in thread view, direct descendant posts display one direct reply to themselves,
			// hence in that case displaying whether there is another reply
			int compareTo = item.isMainStatus || !item.hasDescendantNeighbor ? 0 : 1;
			reply.setSelected(item.status.repliesCount > compareTo);
			boost.setSelected(item.status.reblogged);
			favorite.setSelected(item.status.favourited);
			bookmark.setSelected(item.status.bookmarked);
			boost.setEnabled(item.status.isReblogPermitted(item.accountID));

			int nextPos = getAbsoluteAdapterPosition() + 1;
			boolean nextIsWarning = item.parentFragment.getDisplayItems().size() > nextPos &&
					item.parentFragment.getDisplayItems().get(nextPos) instanceof WarningFilteredStatusDisplayItem;
			boolean condenseBottom = !item.isMainStatus && item.hasDescendantNeighbor &&
					!nextIsWarning;

			ColorStateList color=item.parentFragment.getResources().getColorStateList(
					GlobalUserPreferences.likeIcon ? R.color.like_icon : R.color.favorite_icon, item.parentFragment.getContext().getTheme()
			);
			favIcon.setImageResource(GlobalUserPreferences.likeIcon ? R.drawable.ic_fluent_heart_24_selector : R.drawable.ic_fluent_star_24_selector);
			favIcon.setImageTintList(color);
			favorites.setTextColor(color);

			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
			params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,
					condenseBottom ? V.dp(-5) : 0);

			itemView.requestLayout();
		}

		private void bindText(TextView btn, long count){
			if(AccountSessionManager.get(item.accountID).getLocalPreferences().showInteractionCounts
					&& count>0 && !item.hideCounts){
				btn.setText(UiUtils.abbreviateNumber(count));
				btn.setCompoundDrawablePadding(V.dp(8));
			}else{
				btn.setText("");
				btn.setCompoundDrawablePadding(0);
			}
		}

		private boolean onButtonTouch(View v, MotionEvent event){
			if(item.status.preview) return false;
			boolean disabled = !v.isEnabled() || (v instanceof FrameLayout parentFrame &&
					parentFrame.getChildCount() > 0 && !parentFrame.getChildAt(0).isEnabled());
			int action = event.getAction();
			if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				touchingView = null;
				v.removeCallbacks(longClickRunnable);
				if (!longClickPerformed) v.animate().scaleX(1).scaleY(1).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(150).start();
				if (disabled) return true;
				if (action == MotionEvent.ACTION_UP && !longClickPerformed) v.performClick();
				else if (!longClickPerformed) UiUtils.opacityIn(v);
			} else if (action == MotionEvent.ACTION_DOWN) {
				longClickPerformed = false;
				touchingView = v;
				v.animate().scaleX(0.85f).scaleY(0.85f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(75).start();
				if (disabled) return true;
				v.postDelayed(longClickRunnable, ViewConfiguration.getLongPressTimeout());
				UiUtils.opacityOut(v);
			}
			return true;
		}

		private void onReplyClick(View v){
			if(item.status.preview) return;
			applyInteraction(v, status -> {
				UiUtils.opacityIn(v);
				openComposeView(status, item.accountID);
			});
		}

		private boolean onReplyLongClick(View v) {
			if(item.status.preview) return false;
			if (AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
			UiUtils.pickAccount(v.getContext(), item.accountID, R.string.sk_reply_as, R.drawable.ic_fluent_arrow_reply_28_regular, session -> {
				String accountID = session.getID();
				UiUtils.lookupStatus(v.getContext(), item.status, accountID, item.accountID, status -> {
					if (status == null) return;
					openComposeView(status, accountID);
				});
			}, null);
			return true;
		}

		private void openComposeView(Status status, String accountID) {
			item.parentFragment.maybeShowPreReplySheet(status, () ->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("replyTo", Parcels.wrap(status));
				Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
			});
		}

		private void onBoostClick(View v){
			if(item.status.preview) return;
			if (GlobalUserPreferences.confirmBoost) {
				UiUtils.opacityIn(v);
				onBoostLongClick(v);
				return;
			}
			applyInteraction(v, status -> {
				if(status == null)
					return;
				boost.setSelected(!status.reblogged);
				vibrateForAction(boost, !status.reblogged);
				AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setReblogged(status, !status.reblogged, null, r->boostConsumer(v, r));
			});
		}

		private void boostConsumer(View v, Status r) {
			UiUtils.opacityIn(v);
			bindText(boosts, r.reblogsCount);
		}

		private boolean onBoostLongClick(View v){
			if(item.status.preview) return false;
			Context ctx = itemView.getContext();
			View menu = LayoutInflater.from(ctx).inflate(R.layout.item_boost_menu, null);
			Dialog dialog = new M3AlertDialogBuilder(ctx).setView(menu).create();
			AccountSession session = AccountSessionManager.getInstance().getAccount(item.accountID);

			Consumer<StatusPrivacy> doReblog = (visibility) -> {
				UiUtils.opacityOut(v);
				applyInteraction(v,status -> {
					session.getStatusInteractionController()
							.setReblogged(status, !status.reblogged, visibility, r->boostConsumer(v, r));
					boost.setSelected(status.reblogged);
					dialog.dismiss();
				});
			};

			View separator = menu.findViewById(R.id.separator);
			TextView reblogHeader = menu.findViewById(R.id.reblog_header);
			TextView undoReblog = menu.findViewById(R.id.delete_reblog);
			TextView reblogAs = menu.findViewById(R.id.reblog_as);
			TextView itemPublic = menu.findViewById(R.id.vis_public);
			TextView itemUnlisted = menu.findViewById(R.id.vis_unlisted);
			TextView itemFollowers = menu.findViewById(R.id.vis_followers);

			undoReblog.setVisibility(item.status.reblogged ? View.VISIBLE : View.GONE);
			separator.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			reblogHeader.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			reblogAs.setVisibility(AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1 ? View.VISIBLE : View.GONE);

			itemPublic.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			itemUnlisted.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			itemFollowers.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);

			Drawable checkMark = ctx.getDrawable(R.drawable.ic_fluent_checkmark_circle_20_regular);
			Drawable publicDrawable = ctx.getDrawable(R.drawable.ic_fluent_earth_24_regular);
			Drawable unlistedDrawable = ctx.getDrawable(R.drawable.ic_fluent_lock_open_24_regular);
			Drawable followersDrawable = ctx.getDrawable(R.drawable.ic_fluent_lock_closed_24_regular);

			StatusPrivacy defaultVisibility = session.preferences != null ? session.preferences.postingDefaultVisibility : null;
			itemPublic.setCompoundDrawablesWithIntrinsicBounds(publicDrawable, null, StatusPrivacy.PUBLIC.equals(defaultVisibility) ? checkMark : null, null);
			itemUnlisted.setCompoundDrawablesWithIntrinsicBounds(unlistedDrawable, null, StatusPrivacy.UNLISTED.equals(defaultVisibility) ? checkMark : null, null);
			itemFollowers.setCompoundDrawablesWithIntrinsicBounds(followersDrawable, null, StatusPrivacy.PRIVATE.equals(defaultVisibility) ? checkMark : null, null);

			undoReblog.setOnClickListener(c->doReblog.accept(null));
			itemPublic.setOnClickListener(c->doReblog.accept(StatusPrivacy.PUBLIC));
			itemUnlisted.setOnClickListener(c->doReblog.accept(StatusPrivacy.UNLISTED));
			itemFollowers.setOnClickListener(c->doReblog.accept(StatusPrivacy.PRIVATE));
			reblogAs.setOnClickListener(c->{
				dialog.dismiss();
				UiUtils.pickInteractAs(v.getContext(),
						item.accountID, item.status,
						s -> s.reblogged,
						(ic, status, consumer) -> ic.setReblogged(status, true, null, consumer),
						R.string.sk_reblog_as,
						R.string.sk_reblogged_as,
						R.string.sk_already_reblogged,
						// TODO: replace once available: https://raw.githubusercontent.com/microsoft/fluentui-system-icons/main/android/library/src/main/res/drawable/ic_fluent_arrow_repeat_all_28_regular.xml
						R.drawable.ic_fluent_arrow_repeat_all_24_regular
				);
			});

			menu.findViewById(R.id.quote).setOnClickListener(c->{
				dialog.dismiss();
				UiUtils.opacityIn(v);
				Bundle args=new Bundle();
				args.putString("account", item.accountID);
				Instance instance=AccountSessionManager.get(item.accountID).getInstance().get();
				if(instance.isAkkoma() || instance.isIceshrimp()){
					args.putParcelable("quote", Parcels.wrap(item.status));
				}else{
					StringBuilder prefilledText = new StringBuilder().append("\n\n");
					String ownID = AccountSessionManager.getInstance().getAccount(item.accountID).self.id;
					if (!item.status.account.id.equals(ownID)) prefilledText.append('@').append(item.status.account.acct).append(' ');
					prefilledText.append(item.status.url);
					args.putString("prefilledText", prefilledText.toString());
					args.putInt("selectionStart", 0);
				}
				Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
			});

			dialog.show();
			return true;
		}

		public void onFavoriteClick() {
			favorite.setSelected(item.status.favourited);
			favorite.animate().scaleX(0.95f).scaleY(0.95f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(75).start();
			UiUtils.opacityOut(favorite);
			favorite.postDelayed(() -> {
				favorite.animate().scaleX(1).scaleY(1).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(150).start();
				UiUtils.opacityIn(favorite);
				if(item.status.favourited && !GlobalUserPreferences.reduceMotion && !GlobalUserPreferences.likeIcon) {
					favorite.startAnimation(spin);
				}
			}, 300);
			bindText(favorites, item.status.favouritesCount);
		}

		private void onFavoriteClick(View v){
			if(item.status.preview) return;
			applyInteraction(v, status -> {
				if(status == null)
					return;
				favorite.setSelected(!status.favourited);
				vibrateForAction(favorite, !status.favourited);
				AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setFavorited(status, !status.favourited, r->{
					if (status.favourited && !GlobalUserPreferences.reduceMotion && !GlobalUserPreferences.likeIcon) {
						v.startAnimation(spin);
					}
					UiUtils.opacityIn(v);
					bindText(favorites, r.favouritesCount);
				});
			});
		}

		private boolean onFavoriteLongClick(View v) {
			if(item.status.preview) return false;
			if (AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
			UiUtils.pickInteractAs(v.getContext(),
					item.accountID, item.status,
					s -> s.favourited,
					(ic, status, consumer) -> ic.setFavorited(status, true, consumer),
					R.string.sk_favorite_as,
					R.string.sk_favorited_as,
					R.string.sk_already_favorited,
					GlobalUserPreferences.likeIcon ? R.drawable.ic_fluent_heart_28_regular : R.drawable.ic_fluent_star_28_regular
			);
			return true;
		}

		private void onBookmarkClick(View v){
			if(item.status.preview) return;
			applyInteraction(v,
					status -> {
						if(status == null)
							return;
						bookmark.setSelected(!status.bookmarked);
						vibrateForAction(bookmark, !status.bookmarked);
						AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setBookmarked(status, !status.bookmarked, r->{
							UiUtils.opacityIn(v);
						});
					});
		}

		private boolean onBookmarkLongClick(View v) {
			if(item.status.preview) return false;
			if (AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
			UiUtils.pickInteractAs(v.getContext(),
					item.accountID, item.status,
					s -> s.bookmarked,
					(ic, status, consumer) -> ic.setBookmarked(status, true, consumer),
					R.string.sk_bookmark_as,
					R.string.sk_bookmarked_as,
					R.string.sk_already_bookmarked,
					R.drawable.ic_fluent_bookmark_28_regular
			);
			return true;
		}

		private void onShareClick(View v){
			if(item.status.preview) return;
			UiUtils.opacityIn(v);
			UiUtils.openSystemShareSheet(v.getContext(), item.status);
		}

		private boolean onShareLongClick(View v){
			if(item.status.preview) return false;
			UiUtils.copyText(v, item.status.url);
			return true;
		}

		private int descriptionForId(int id){
			if(id==R.id.reply_btn)
				return R.string.button_reply;
			if(id==R.id.boost_btn)
				return R.string.button_reblog;
			if(id==R.id.favorite_btn)
				return R.string.button_favorite;
			if(id==R.id.bookmark_btn)
				return R.string.add_bookmark;
			if(id==R.id.share_btn)
				return R.string.button_share;
			return 0;
		}

		private void applyInteraction(View v, Consumer<Status> interactionConsumer) {
			if(!item.status.isRemote){
				interactionConsumer.accept(item.status);
				return;
			}
			UiUtils.lookupStatus(v.getContext(),
					item.status, item.accountID, null,
					interactionConsumer
			);
		}

		private static void vibrateForAction(View view, boolean isPositive) {
			if (!GlobalUserPreferences.hapticFeedback) return;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				view.performHapticFeedback(isPositive ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.REJECT);
				return;
			}

			Vibrator vibrator = view.getContext().getSystemService(Vibrator.class);

			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
				vibrator.vibrate(VibrationEffect.createPredefined(isPositive ? VibrationEffect.EFFECT_CLICK : VibrationEffect.EFFECT_DOUBLE_CLICK));
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				VibrationEffect effect = isPositive
						? VibrationEffect.createOneShot(75L, 128)
						: VibrationEffect.createWaveform(new long[]{0L, 75L, 75L, 75L}, new int[]{0, 128, 0, 128}, -1);
				vibrator.vibrate(effect);
			} else {
				if (isPositive) vibrator.vibrate(75L);
				else vibrator.vibrate(new long[]{0L, 75L, 75L, 75L}, -1);
			}
		}
	}
}
