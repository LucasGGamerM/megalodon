package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.announcements.DismissAnnouncement;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.GetStatusSourceText;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.ListsFragment;
import org.joinmastodon.android.fragments.NotificationsListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Announcement;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import androidx.annotation.LayoutRes;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class HeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;
	public boolean hasVisibilityToggle;
	boolean needBottomPadding;
	private CharSequence extraText;
	private Notification notification;
	private ScheduledStatus scheduledStatus;
	private Announcement announcement;
	private Consumer<String> consumeReadAnnouncement;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment parentFragment, String accountID, Status status, CharSequence extraText, Notification notification, ScheduledStatus scheduledStatus){
		super(parentID, parentFragment);
		AccountSession session = AccountSessionManager.get(accountID);
		user=scheduledStatus != null ? session.self : user;
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(
				TextUtils.isEmpty(user.avatar) ? session.getDefaultAvatarUrl() :
						GlobalUserPreferences.playGifs ? user.avatar : user.avatarStatic,
				V.dp(50), V.dp(50));
		this.accountID=accountID;
		parsedName=new SpannableStringBuilder(user.getDisplayName());
		this.status=status;
		this.notification=notification;
		this.scheduledStatus=scheduledStatus;
		if(AccountSessionManager.get(accountID).getLocalPreferences().customEmojiInNames)
			HtmlParser.parseCustomEmoji(parsedName, user.emojis);
		emojiHelper.setText(parsedName);
		if(status!=null){
			// visibility toggle can't do much for non-"image" attachments
			hasVisibilityToggle=status.mediaAttachments.stream().anyMatch(m -> m.type.isImage());
		}
		this.extraText=extraText;
		emojiHelper.addText(extraText);
	}

	public static HeaderStatusDisplayItem fromAnnouncement(Announcement a, Status fakeStatus, Account instanceUser, BaseStatusListFragment parentFragment, String accountID, Consumer<String> consumeReadID) {
		HeaderStatusDisplayItem item = new HeaderStatusDisplayItem(a.id, instanceUser, a.startsAt!=null ? a.startsAt : fakeStatus.createdAt, parentFragment, accountID, fakeStatus, null, null, null);
		item.announcement = a;
		item.consumeReadAnnouncement = consumeReadID;
		return item;
	}

	@Override
	public Type getType(){
		return Type.HEADER;
	}

	@Override
	public int getImageCount(){
		return 1+emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index>0){
			return emojiHelper.getImageRequest(index-1);
		}
		return avaRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<HeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, time, username, extraText;
		private final View collapseBtn, timeUsernameSeparator;
		private final ImageView avatar, more, visibility, deleteNotification, unreadIndicator, markAsRead, collapseBtnIcon, botIcon;
		private final PopupMenu optionsMenu;
		private Relationship relationship;
		private APIRequest<?> currentRelationshipRequest;

		public Holder(Activity activity, ViewGroup parent){
			this(activity, R.layout.display_item_header, parent);
		}

		protected Holder(Activity activity, @LayoutRes int layout, ViewGroup parent){
			super(activity, layout, parent);
			name=findViewById(R.id.name);
			time=findViewById(R.id.time);
			username=findViewById(R.id.username);
			botIcon=findViewById(R.id.bot_icon);
			timeUsernameSeparator=findViewById(R.id.separator);
			avatar=findViewById(R.id.avatar);
			more=findViewById(R.id.more);
			visibility=findViewById(R.id.visibility);
			deleteNotification=findViewById(R.id.delete_notification);
			unreadIndicator=findViewById(R.id.unread_indicator);
			markAsRead=findViewById(R.id.mark_as_read);
			collapseBtn=findViewById(R.id.collapse_btn);
			collapseBtnIcon=findViewById(R.id.collapse_btn_icon);
			extraText=findViewById(R.id.extra_text);
			avatar.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(OutlineProviders.roundedRect(12));
			avatar.setClipToOutline(true);
			more.setOnClickListener(this::onMoreClick);
			visibility.setOnClickListener(v->item.parentFragment.onVisibilityIconClick(this));
			deleteNotification.setOnClickListener(v->UiUtils.confirmDeleteNotification(activity, item.parentFragment.getAccountID(), item.notification, ()->{
				if (item.parentFragment instanceof NotificationsListFragment fragment) {
					fragment.removeNotification(item.notification);
				}
			}));
			collapseBtn.setOnClickListener(l -> item.parentFragment.onToggleExpanded(item.status, item.isForQuote, getItemID()));

			optionsMenu=new PopupMenu(activity, more);
			optionsMenu.inflate(R.menu.post);
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic())
				optionsMenu.getMenu().setGroupDividerEnabled(true);
			optionsMenu.setOnMenuItemClickListener(menuItem->{
				Account account=item.user;
				int id=menuItem.getItemId();
				if(id==R.id.edit || id==R.id.delete_and_redraft){
					final Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("editStatus", Parcels.wrap(item.status));
					boolean redraft = id == R.id.delete_and_redraft;
					if (redraft) {
						args.putBoolean("redraftStatus", true);
						if (item.parentFragment instanceof ThreadFragment thread && !thread.isItemEnabled(item.status.id)) {
							// ("enabled" = clickable; opened status is not clickable)
							// request navigation to the re-drafted status if status is currently opened
							args.putBoolean("navigateToStatus", true);
						}
					}
					boolean isPixelfed = item.parentFragment.isInstancePixelfed();
					boolean textEmpty = TextUtils.isEmpty(item.status.content) && !item.status.hasSpoiler();
					if(!redraft && (isPixelfed || textEmpty)){
						// pixelfed doesn't support /statuses/:id/source :/
						if (isPixelfed) {
							args.putString("sourceText", HtmlParser.text(item.status.content));
							args.putString("sourceSpoiler", item.status.spoilerText);
						}
						Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
					}else if(item.scheduledStatus!=null){
						args.putString("sourceText", item.status.text);
						args.putString("sourceSpoiler", item.status.spoilerText);
						args.putParcelable("scheduledStatus", Parcels.wrap(item.scheduledStatus));
						Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
					}else{
						new GetStatusSourceText(item.status.id)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(GetStatusSourceText.Response result){
										args.putString("sourceText", result.text);
										args.putString("sourceSpoiler", result.spoilerText);
										if(result.contentType!=null){
											args.putString("sourceContentType", result.contentType.name());
										}
										if(redraft){
											UiUtils.confirmDeletePost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, s->{
												Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
											}, true);
										}else{
											Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
										}
									}

									@Override
									public void onError(ErrorResponse error){
										error.showToast(item.parentFragment.getActivity());
									}
								})
								.wrapProgress(item.parentFragment.getActivity(), R.string.loading, true)
								.exec(item.parentFragment.getAccountID());
					}
				}else if(id==R.id.delete){
					if (item.scheduledStatus != null) {
						UiUtils.confirmDeleteScheduledPost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.scheduledStatus, ()->{});
					} else {
						UiUtils.confirmDeletePost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, s->{}, false);
					}
				}else if(id==R.id.pin || id==R.id.unpin) {
					UiUtils.confirmPinPost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, !item.status.pinned, s->{});
				}else if(id==R.id.mute){
					UiUtils.confirmToggleMuteUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.muting, r->{});
				}else if (id==R.id.mute_conversation || id==R.id.unmute_conversation) {
					UiUtils.confirmToggleMuteConversation(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, ()->{});
				}else if(id==R.id.block){
					UiUtils.confirmToggleBlockUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.blocking, r->{});
				}else if(id==R.id.report){
					Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("status", Parcels.wrap(item.status));
					args.putParcelable("reportAccount", Parcels.wrap(item.status.account));
					args.putParcelable("relationship", Parcels.wrap(relationship));
					Nav.go(item.parentFragment.getActivity(), ReportReasonChoiceFragment.class, args);
				}else if(id==R.id.open_in_browser){
					UiUtils.launchWebBrowser(activity, item.status.url);
				}else if(id==R.id.copy_link){
					UiUtils.copyText(parent, item.status.url);
				}else if(id==R.id.follow){
					if(relationship==null)
						return true;
					ProgressDialog progress=new ProgressDialog(activity);
					progress.setCancelable(false);
					progress.setMessage(activity.getString(R.string.loading));
					UiUtils.performAccountAction(activity, account, item.parentFragment.getAccountID(), relationship, null, visible->{
						if(visible)
							progress.show();
						else
							progress.dismiss();
					}, rel->{
						relationship=rel;
						Toast.makeText(activity, activity.getString(rel.following ? R.string.followed_user : rel.requested ? R.string.following_user_requested : R.string.unfollowed_user, account.getDisplayUsername()), Toast.LENGTH_SHORT).show();
					});
				}else if(id==R.id.block_domain){
					UiUtils.confirmToggleBlockDomain(activity, item.parentFragment.getAccountID(), account.getDomain(), relationship!=null && relationship.domainBlocking, ()->{});
				}else if(id==R.id.bookmark){
					AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setBookmarked(item.status, !item.status.bookmarked);
				}else if(id==R.id.manage_user_lists){
					final Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putString("profileAccount", account.id);
					args.putString("profileDisplayUsername", account.getDisplayUsername());
					Nav.go(item.parentFragment.getActivity(), ListsFragment.class, args);
				}else if(id==R.id.share){
					UiUtils.openSystemShareSheet(activity, item.status);
				}else if(id==R.id.open_with_account){
					UiUtils.pickAccount(item.parentFragment.getActivity(), item.accountID, R.string.sk_open_with_account, R.drawable.ic_fluent_person_swap_24_regular, session ->UiUtils.openURL(
							item.parentFragment.getActivity(), session.getID(), item.status.url, false
					), null);
				}
				return true;
			});
			UiUtils.enablePopupMenuIcons(activity, optionsMenu);
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			String time = null;
			if (item.scheduledStatus!=null) {
				if (item.scheduledStatus.scheduledAt.isAfter(CreateStatus.DRAFTS_AFTER_INSTANT)) {
					time = item.parentFragment.getString(R.string.sk_draft);
				} else {
					DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
					time = item.scheduledStatus.scheduledAt.atZone(ZoneId.systemDefault()).format(formatter);
				}
			} else if(item.status==null || item.status.editedAt==null)
				time=UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt);
			else if (item.status != null && item.status.editedAt != null)
				time=item.parentFragment.getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(itemView.getContext(), item.status.editedAt));

			this.username.setText(item.user.getDisplayUsername());
			this.timeUsernameSeparator.setVisibility(time==null ? View.GONE : View.VISIBLE);
			this.time.setVisibility(time==null ? View.GONE : View.VISIBLE);
			if(time!=null) this.time.setText(time);

			botIcon.setVisibility(item.user.bot ? View.VISIBLE : View.GONE);
			botIcon.setColorFilter(username.getCurrentTextColor());

			deleteNotification.setVisibility(GlobalUserPreferences.enableDeleteNotifications && item.notification!=null && !item.inset ? View.VISIBLE : View.GONE);
			visibility.setVisibility(item.hasVisibilityToggle ? View.VISIBLE : View.GONE);
			if (item.hasVisibilityToggle){
				boolean visible = item.status.sensitiveRevealed && (!item.status.hasSpoiler() || item.status.spoilerRevealed);
				visibility.setAlpha(visible ? 1 : 0f);
				visibility.setScaleY(visible ? 1 : 0.8f);
				visibility.setScaleX(visible ? 1 : 0.8f);
				visibility.setEnabled(visible);
			}
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.needBottomPadding ? V.dp(16) : 0);
			if(TextUtils.isEmpty(item.extraText)){
				if (item.status != null) {
					boolean displayPronouns=item.parentFragment instanceof ThreadFragment ? GlobalUserPreferences.displayPronounsInThreads : GlobalUserPreferences.displayPronounsInTimelines;
					UiUtils.setExtraTextInfo(item.parentFragment.getContext(), extraText, displayPronouns, item.status.visibility==StatusPrivacy.DIRECT, item.status.localOnly || item.status.visibility==StatusPrivacy.LOCAL, item.status.account);
				}
			}else{
				extraText.setVisibility(View.VISIBLE);
				extraText.setText(item.extraText);
			}
			more.setVisibility(item.announcement != null || item.inset ||
					(item.notification != null && item.notification.report != null)
					? View.GONE : View.VISIBLE);
			more.setOnClickListener(this::onMoreClick);
			avatar.setClickable(!item.inset);
			avatar.setContentDescription(item.parentFragment.getString(R.string.avatar_description, item.user.acct));
			if(currentRelationshipRequest!=null){
				currentRelationshipRequest.cancel();
			}
			relationship=null;

			if (item.announcement != null) {
				int vis = item.announcement.read ? View.GONE : View.VISIBLE;
				V.setVisibilityAnimated(unreadIndicator, vis);
				V.setVisibilityAnimated(markAsRead, vis);

				markAsRead.setEnabled(!item.announcement.read);
				markAsRead.setOnClickListener(v -> {
					if (item.announcement.read) return;
					new DismissAnnouncement(item.announcement.id).setCallback(new Callback<>() {
						@Override
						public void onSuccess(Object o) {
							item.consumeReadAnnouncement.accept(item.announcement.id);
							item.announcement.read = true;
							if (item.parentFragment.getActivity() == null) return;
							rebind();
						}

						@Override
						public void onError(ErrorResponse error) {
							error.showToast(item.parentFragment.getActivity());
						}
					}).exec(item.accountID);
				});
			} else {
				markAsRead.setVisibility(View.GONE);
			}

			bindCollapseButton();

			itemView.setPaddingRelative(itemView.getPaddingStart(), itemView.getPaddingTop(),
					item.inset ? V.dp(10) : V.dp(4), itemView.getPaddingBottom());
		}

		public void bindCollapseButton(){
			boolean expandable=item.status!=null && item.status.textExpandable;
			collapseBtn.setVisibility(expandable ? View.VISIBLE : View.GONE);
			if(expandable) {
				bindCollapseButtonText();
				collapseBtnIcon.setScaleY(item.status.textExpanded ? -1 : 1);
			}
		}

		private void bindCollapseButtonText(){
			String collapseText = item.parentFragment.getString(item.status.textExpanded ? R.string.sk_collapse : R.string.sk_expand);
			collapseBtn.setContentDescription(collapseText);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) collapseBtn.setTooltipText(collapseText);
		}

		public void animateExpandToggle(){
			bindCollapseButtonText();
			collapseBtnIcon.animate().scaleY(item.status.textExpanded ? -1 : 1).start();
		}

		public void animateVisibilityToggle(boolean visible){
			visibility.animate()
					.alpha(visible ? 1 : 0)
					.scaleX(visible ? 1 : 0.8f)
					.scaleY(visible ? 1 : 0.8f)
					.setInterpolator(CubicBezierInterpolator.DEFAULT)
					.start();
			visibility.setEnabled(visible);
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index>0){
				item.emojiHelper.setImageDrawable(index-1, drawable);
				name.setText(name.getText());
			}else{
				avatar.setImageDrawable(drawable);
			}
			if(drawable instanceof Animatable)
				((Animatable) drawable).start();
		}

		@Override
		public void clearImage(int index){
			if(index==0){
				avatar.setImageResource(R.drawable.image_placeholder);
				return;
			}
			setImage(index, null);
		}

		private void onAvaClick(View v){
			if (TextUtils.isEmpty(item.user.url))
				return;
			if (item.announcement != null) {
				UiUtils.openURL(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.user.url);
				return;
			}
			Bundle args=new Bundle();
			if(item.status != null && item.status.isRemote){
				UiUtils.lookupAccount(v.getContext(), item.status.account, item.accountID, null, account -> {
					args.putString("account", item.accountID);
					args.putParcelable("profileAccount", Parcels.wrap(account));
					Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
				});
				return;
			}
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.user));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}

		private void onMoreClick(View v){
			if(item.status.preview) return;
			updateOptionsMenu();
			optionsMenu.show();
			if(relationship==null && currentRelationshipRequest==null){
				currentRelationshipRequest=new GetAccountRelationships(Collections.singletonList(item.user.id))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Relationship> result){
								if(!result.isEmpty()){
									relationship=result.get(0);
									updateOptionsMenu();
								}
								currentRelationshipRequest=null;
							}

							@Override
							public void onError(ErrorResponse error){
								currentRelationshipRequest=null;
							}
						})
						.exec(item.parentFragment.getAccountID());
			}
		}

		private void updateOptionsMenu(){
			if(item.parentFragment.getActivity()==null)
				return;
			if (item.announcement != null) return;
			boolean hasMultipleAccounts = AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1;
			Account account=item.user;
			Menu menu=optionsMenu.getMenu();


			String username = account.getShortUsername();
			boolean isOwnPost=AccountSessionManager.getInstance().isSelf(item.parentFragment.getAccountID(), account);
			boolean isPostScheduled=item.scheduledStatus!=null;
			menu.findItem(R.id.open_with_account).setVisible(!isPostScheduled && hasMultipleAccounts);
			menu.findItem(R.id.edit).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete_and_redraft).setVisible(!isPostScheduled && item.status!=null && isOwnPost);
			menu.findItem(R.id.pin).setVisible(!isPostScheduled && item.status!=null && isOwnPost && !item.status.pinned);
			menu.findItem(R.id.unpin).setVisible(!isPostScheduled && item.status!=null && isOwnPost && item.status.pinned);
			menu.findItem(R.id.mute_conversation).setVisible((item.status!=null && !item.status.muted && !isPostScheduled) && (isOwnPost || item.status.mentions.stream().anyMatch(m->{
				if(m==null)
					return false;
				return AccountSessionManager.get(item.parentFragment.getAccountID()).self.id.equals(m.id) ||
						AccountSessionManager.get(item.parentFragment.getAccountID()).self.getFullyQualifiedName().equals(m.username) ||
						AccountSessionManager.get(item.parentFragment.getAccountID()).self.acct.equals(m.acct);
			})));
			menu.findItem(R.id.unmute_conversation).setVisible(item.status!=null && item.status.muted);
			menu.findItem(R.id.open_in_browser).setVisible(!isPostScheduled && item.status!=null);
			menu.findItem(R.id.copy_link).setVisible(!isPostScheduled && item.status!=null);
			MenuItem blockDomain=menu.findItem(R.id.block_domain);
			MenuItem mute=menu.findItem(R.id.mute);
			MenuItem block=menu.findItem(R.id.block);
			MenuItem report=menu.findItem(R.id.report);
			MenuItem follow=menu.findItem(R.id.follow);
			MenuItem manageUserLists = menu.findItem(R.id.manage_user_lists);
			/* disabled in megalodon: add/remove bookmark is already available through status footer
			MenuItem bookmark=menu.findItem(R.id.bookmark);
			bookmark.setVisible(false);
			if(item.status!=null){
				bookmark.setVisible(true);
				bookmark.setTitle(item.status.bookmarked ? R.string.remove_bookmark : R.string.add_bookmark);
			}else{
				bookmark.setVisible(false);
			}
			*/
			if(isPostScheduled || isOwnPost){
				mute.setVisible(false);
				block.setVisible(false);
				report.setVisible(false);
				follow.setVisible(false);
				blockDomain.setVisible(false);
				manageUserLists.setVisible(false);
			}else{
				mute.setVisible(true);
				// hiding when following to keep menu item count equal (trading it for user lists)
				block.setVisible(relationship == null || !relationship.following);
				report.setVisible(true);
				follow.setVisible(relationship==null || relationship.following || (!relationship.blocking && !relationship.blockedBy && !relationship.domainBlocking && !relationship.muting));
				mute.setTitle(item.parentFragment.getString(relationship!=null && relationship.muting ? R.string.unmute_user : R.string.mute_user, username));
				mute.setIcon(relationship!=null && relationship.muting ? R.drawable.ic_fluent_speaker_0_24_regular : R.drawable.ic_fluent_speaker_off_24_regular);
				UiUtils.insetPopupMenuIcon(item.parentFragment.getContext(), mute);
				block.setTitle(item.parentFragment.getString(relationship!=null && relationship.blocking ? R.string.unblock_user : R.string.block_user, username));
				report.setTitle(item.parentFragment.getString(R.string.report_user, username));
				// disabled in megalodon. domain blocks from a post clutters the context menu and looks out of place
//				if(!account.isLocal()){
//					blockDomain.setVisible(true);
//					blockDomain.setTitle(item.parentFragment.getString(relationship!=null && relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
//				}else{
					blockDomain.setVisible(false);
//				}
				boolean following = relationship!=null && relationship.following;
				follow.setTitle(item.parentFragment.getString(following ? R.string.unfollow_user : R.string.follow_user, username));
				follow.setIcon(following ? R.drawable.ic_fluent_person_delete_24_regular : R.drawable.ic_fluent_person_add_24_regular);
				manageUserLists.setVisible(relationship != null && relationship.following);
				manageUserLists.setTitle(item.parentFragment.getString(R.string.sk_lists_with_user, username));
				// ic_fluent_person_add_24_regular actually has a width of 25dp -.-
				UiUtils.insetPopupMenuIcon(item.parentFragment.getContext(), follow, following ? 0 : V.dp(-1));
			}

			workaroundChangingMenuItemWidths(menu, username);
		}

		// ugliest piece of code you'll see in a while: i measure the menu items' text widths to
		// determine the biggest one, because it's probably not being displayed at first
		// (before the relationship loaded). i take the largest one's size and add a space to the
		// last item ("open in browser") until it takes up as much space as the largest item.
		// goal: no more ugly ellipsis after the relationship loads in when opening the context menu
		// of a post
		private void workaroundChangingMenuItemWidths(Menu menu, String username) {
			String openInBrowserText = item.parentFragment.getString(R.string.open_in_browser);
			if (relationship == null) {
				float largestWidth = 0;
				Paint paint = new Paint();
				paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
				String[] otherStrings = new String[] {
						item.parentFragment.getString(R.string.unfollow_user, username),
						item.parentFragment.getString(R.string.unblock_user, username),
						item.parentFragment.getString(R.string.unmute_user, username),
						item.parentFragment.getString(R.string.sk_lists_with_user, username),
				};
				for (int i = 0; i < menu.size(); i++) {
					MenuItem item = menu.getItem(i);
					if (item.getItemId() == R.id.open_in_browser || !item.isVisible()) continue;
					float width = paint.measureText(menu.getItem(i).getTitle().toString());
					if (width > largestWidth) largestWidth = width;
				}
				for (String str : otherStrings) {
					float width = paint.measureText(str);
					if (width > largestWidth) largestWidth = width;
				}
				float textWidth = paint.measureText(openInBrowserText);
				float missingWidth = Math.max(0, largestWidth - textWidth);
				float singleSpaceWidth = paint.measureText(" ");
				int howManySpaces = (int) Math.ceil(missingWidth / singleSpaceWidth);
				String enlargedText = openInBrowserText + " ".repeat(howManySpaces);
				menu.findItem(R.id.open_in_browser).setTitle(enlargedText);
			} else {
				menu.findItem(R.id.open_in_browser).setTitle(openInBrowserText);
			}
		}
	}
}
