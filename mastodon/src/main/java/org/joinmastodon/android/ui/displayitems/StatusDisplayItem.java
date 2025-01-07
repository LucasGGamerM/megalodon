package org.joinmastodon.android.ui.displayitems;

import static org.joinmastodon.android.api.session.AccountLocalPreferences.ShowEmojiReactions.ALWAYS;
import static org.joinmastodon.android.api.session.AccountLocalPreferences.ShowEmojiReactions.ONLY_OPENED;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.HashtagTimelineFragment;
import org.joinmastodon.android.fragments.HomeTabFragment;
import org.joinmastodon.android.fragments.ListTimelineFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.StatusListFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.FilterResult;
import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class StatusDisplayItem{
	public final String parentID;
	public final BaseStatusListFragment<?> parentFragment;
	public Status status;
	public boolean inset;
	public int index;
	public boolean
			hasDescendantNeighbor=false,
			hasAncestoringNeighbor=false,
			isMainStatus=true,
			isDirectDescendant=false,
			isForQuote=false;

	public static final int FLAG_INSET=1;
	public static final int FLAG_NO_FOOTER=1 << 1;
	public static final int FLAG_CHECKABLE=1 << 2;
	public static final int FLAG_MEDIA_FORCE_HIDDEN=1 << 3;
	public static final int FLAG_NO_HEADER=1 << 4;
	public static final int FLAG_NO_TRANSLATE=1 << 5;
	public static final int FLAG_NO_EMOJI_REACTIONS=1 << 6;
	public static final int FLAG_IS_FOR_QUOTE=1 << 7;
	public static final int FLAG_NO_MEDIA_PREVIEW=1 << 8;


	private final static  Pattern QUOTE_MENTION_PATTERN=Pattern.compile("(?:<p>)?\\s?(?:RE:\\s?(<br\\s?\\/?>)?)?<a href=\"https:\\/\\/[^\"]+\"[^>]*><span class=\"invisible\">https:\\/\\/<\\/span><span class=\"ellipsis\">[^<]+<\\/span><span class=\"invisible\">[^<]+<\\/span><\\/a>(?:<\\/p>)?$");
	private final static  Pattern QUOTE_PATTERN=Pattern.compile("https://[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,8}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$");

	public void setAncestryInfo(
			boolean hasDescendantNeighbor,
			boolean hasAncestoringNeighbor,
			boolean isMainStatus,
			boolean isDirectDescendant
	) {
		this.hasDescendantNeighbor = hasDescendantNeighbor;
		this.hasAncestoringNeighbor = hasAncestoringNeighbor;
		this.isMainStatus = isMainStatus;
		this.isDirectDescendant = isDirectDescendant;
	}

	public StatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment){
		this.parentID=parentID;
		this.parentFragment=parentFragment;
	}

	@NonNull
	public String getContentStatusID(){
		if(parentFragment instanceof StatusListFragment slf){
			Status s=slf.getContentStatusByID(parentID);
			return s!=null ? s.id : parentID;
		}else{
			return parentID;
		}
	}

	public abstract Type getType();

	public int getImageCount(){
		return 0;
	}

	public ImageLoaderRequest getImageRequest(int index){
		return null;
	}

	public static BindableViewHolder<? extends StatusDisplayItem> createViewHolder(Type type, Activity activity, ViewGroup parent, Fragment parentFragment){
		return switch(type){
			case HEADER -> new HeaderStatusDisplayItem.Holder(activity, parent);
			case HEADER_CHECKABLE -> new CheckableHeaderStatusDisplayItem.Holder(activity, parent);
			case REBLOG_OR_REPLY_LINE -> new ReblogOrReplyLineStatusDisplayItem.Holder(activity, parent);
			case TEXT -> new TextStatusDisplayItem.Holder(activity, parent);
			case AUDIO -> new AudioStatusDisplayItem.Holder(activity, parent);
			case POLL_OPTION -> new PollOptionStatusDisplayItem.Holder(activity, parent);
			case POLL_FOOTER -> new PollFooterStatusDisplayItem.Holder(activity, parent);
			case CARD_LARGE -> new LinkCardStatusDisplayItem.Holder(activity, parent, true);
			case CARD_COMPACT -> new LinkCardStatusDisplayItem.Holder(activity, parent, false);
			case EMOJI_REACTIONS -> new EmojiReactionsStatusDisplayItem.Holder(activity, parent);
			case FOOTER -> new FooterStatusDisplayItem.Holder(activity, parent);
			case ACCOUNT_CARD -> new AccountCardStatusDisplayItem.Holder(activity, parent);
			case ACCOUNT -> new AccountStatusDisplayItem.Holder(new AccountViewHolder(parentFragment, parent, null));
			case HASHTAG -> new HashtagStatusDisplayItem.Holder(activity, parent);
			case GAP -> new GapStatusDisplayItem.Holder(activity, parent);
			case EXTENDED_FOOTER -> new ExtendedFooterStatusDisplayItem.Holder(activity, parent);
			case MEDIA_GRID -> new MediaGridStatusDisplayItem.Holder(activity, parent);
			case PREVIEWLESS_MEDIA_GRID -> new PreviewlessMediaGridStatusDisplayItem.Holder(activity, parent);
			case WARNING -> new WarningFilteredStatusDisplayItem.Holder(activity, parent);
			case FILE -> new FileStatusDisplayItem.Holder(activity, parent);
			case SPOILER, FILTER_SPOILER -> new SpoilerStatusDisplayItem.Holder(activity, parent, type);
			case SECTION_HEADER -> null; // new SectionHeaderStatusDisplayItem.Holder(activity, parent);
			case NOTIFICATION_HEADER -> new NotificationHeaderStatusDisplayItem.Holder(activity, parent);
			case ERROR_ITEM -> new ErrorStatusDisplayItem.Holder(activity, parent);
			case DUMMY -> new DummyStatusDisplayItem.Holder(activity);
		};
	}

	public static ReblogOrReplyLineStatusDisplayItem buildReplyLine(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parent, Account account, boolean threadReply) {
		String parentID = parent.getID();
		String text = threadReply ? fragment.getString(R.string.sk_show_thread)
				: account == null ? fragment.getString(R.string.sk_in_reply)
				: status.reblog != null ? account.getDisplayName()
				: fragment.getString(R.string.in_reply_to, account.getDisplayName());
		String fullText = threadReply ? fragment.getString(R.string.sk_show_thread)
				: account == null ? fragment.getString(R.string.sk_in_reply)
				: fragment.getString(R.string.in_reply_to, account.getDisplayName());
		return new ReblogOrReplyLineStatusDisplayItem(
				parentID, fragment, text, account == null ? List.of() : account.emojis,
				R.drawable.ic_fluent_arrow_reply_20sp_filled, null, null, fullText, status, account
		);
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, FilterContext filterContext, int flags){
		String parentID=parentObject.getID();
		ArrayList<StatusDisplayItem> items=new ArrayList<>();
		Status statusForContent=status.getContentStatus();
		Bundle args=new Bundle();
		args.putString("account", accountID);
		try{
			ScheduledStatus scheduledStatus=parentObject instanceof ScheduledStatus s ? s : null;

			HeaderStatusDisplayItem header=null;
			boolean hideCounts=!AccountSessionManager.get(accountID).getLocalPreferences().showInteractionCounts;

			if((flags&FLAG_NO_HEADER)==0){
				ReblogOrReplyLineStatusDisplayItem replyLine=null;
				boolean threadReply=statusForContent.inReplyToAccountId!=null &&
						statusForContent.inReplyToAccountId.equals(statusForContent.account.id);

				if(statusForContent.inReplyToAccountId!=null && !(threadReply && fragment instanceof ThreadFragment)){
					Account account=knownAccounts.get(statusForContent.inReplyToAccountId);
					replyLine=buildReplyLine(fragment, status, accountID, parentObject, account, threadReply);
				}

				if(status.reblog!=null){
					boolean isOwnPost=AccountSessionManager.getInstance().isSelf(fragment.getAccountID(), status.account);

					statusForContent.rebloggedBy=status.account;

					String text=fragment.getString(R.string.user_boosted, status.account.getDisplayName());
					items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, fragment, text, status.account.emojis, R.drawable.ic_fluent_arrow_repeat_all_20sp_filled, isOwnPost ? status.visibility : null, i->{
						args.putParcelable("profileAccount", Parcels.wrap(status.account));
						Nav.go(fragment.getActivity(), ProfileFragment.class, args);
					}, null, status, status.account));
				}else if(!(status.tags.isEmpty() ||
						fragment instanceof HashtagTimelineFragment ||
						fragment instanceof ListTimelineFragment
				) && fragment.getParentFragment() instanceof HomeTabFragment home){
					home.getHashtags().stream()
							.filter(followed->status.tags.stream()
									.anyMatch(hashtag->followed.name.equalsIgnoreCase(hashtag.name)))
							.findAny()
							// post contains a hashtag the user is following
							.ifPresent(hashtag->items.add(new ReblogOrReplyLineStatusDisplayItem(
									parentID, fragment, hashtag.name, List.of(),
									R.drawable.ic_fluent_number_symbol_20sp_filled, null,
									i->UiUtils.openHashtagTimeline(fragment.getActivity(), accountID, hashtag),
									status
							)));
				}

				if(replyLine!=null){
					Optional<ReblogOrReplyLineStatusDisplayItem> primaryLine=items.stream()
							.filter(i->i instanceof ReblogOrReplyLineStatusDisplayItem)
							.map(ReblogOrReplyLineStatusDisplayItem.class::cast)
							.findFirst();

					if(primaryLine.isPresent()){
						primaryLine.get().extra=replyLine;
					}else{
						items.add(replyLine);
					}
				}

				if((flags&FLAG_CHECKABLE)!=0)
					items.add(header=new CheckableHeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, fragment, accountID, statusForContent, null));
				else
					items.add(header=new HeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, fragment, accountID, statusForContent, null, parentObject instanceof Notification n ? n : null, scheduledStatus));
			}

			LegacyFilter applyingFilter=null;
			if(status.filtered!=null){
				ArrayList<FilterResult> filters= new ArrayList<>(status.filtered);

				// Only add client filters if there are no pre-existing status filter
				if(filters.isEmpty())
					filters.addAll(AccountSessionManager.get(accountID).getClientSideFilters(status));

				for(FilterResult filter : filters){
					LegacyFilter f=filter.filter;
					if(f.isActive() && filterContext!=null && f.context.contains(filterContext)){
						applyingFilter=f;
						break;
					}
				}
			}

			ArrayList<StatusDisplayItem> contentItems;
			if(statusForContent.hasSpoiler()){
				if(AccountSessionManager.get(accountID).getLocalPreferences().revealCWs) statusForContent.spoilerRevealed=true;
				SpoilerStatusDisplayItem spoilerItem=new SpoilerStatusDisplayItem(parentID, fragment, null, statusForContent, Type.SPOILER);
				if((flags&FLAG_IS_FOR_QUOTE)!=0){
					for(StatusDisplayItem item : spoilerItem.contentItems){
						item.isForQuote=true;
					}
				}
				items.add(spoilerItem);
				contentItems=spoilerItem.contentItems;
			}else{
				contentItems=items;
			}

			if(statusForContent.quote!=null) {
				int quoteInlineIndex=statusForContent.content.lastIndexOf("<span class=\"quote-inline\"><br/><br/>RE:");
				if(quoteInlineIndex==-1)
					quoteInlineIndex=statusForContent.content.lastIndexOf("<span class=\"quote-inline\"><br><br>RE:");
				if(quoteInlineIndex!=-1)
					statusForContent.content=statusForContent.content.substring(0, quoteInlineIndex);
				else {
					// hide non-official quote patters
					Matcher matcher=QUOTE_MENTION_PATTERN.matcher(status.content);
					if(matcher.find()){
						String quoteMention=matcher.group();
						statusForContent.content=statusForContent.content.replace(quoteMention, "");
					}
				}
			}

			boolean hasSpoiler=!TextUtils.isEmpty(statusForContent.spoilerText);
			if(!TextUtils.isEmpty(statusForContent.content)){
				SpannableStringBuilder parsedText=HtmlParser.parse(statusForContent.content, statusForContent.emojis, statusForContent.mentions, statusForContent.tags, accountID, fragment.getContext());
				if(applyingFilter!=null)
					HtmlParser.applyFilterHighlights(fragment.getActivity(), parsedText, status.filtered);
				TextStatusDisplayItem text=new TextStatusDisplayItem(parentID, parsedText, fragment, statusForContent, (flags&FLAG_NO_TRANSLATE)!=0);
				contentItems.add(text);
			}else if(!hasSpoiler && header!=null){
				header.needBottomPadding=true;
			}else if(hasSpoiler){
				contentItems.add(new DummyStatusDisplayItem(parentID, fragment));
			}

			List<Attachment> imageAttachments=statusForContent.mediaAttachments.stream().filter(att->att.type.isImage()).collect(Collectors.toList());
			if(!imageAttachments.isEmpty() && (flags&FLAG_NO_MEDIA_PREVIEW)==0){
				int color=UiUtils.getThemeColor(fragment.getContext(), R.attr.colorM3SurfaceVariant);
				for(Attachment att : imageAttachments){
					if(att.blurhashPlaceholder==null){
						att.blurhashPlaceholder=new ColorDrawable(color);
					}
				}
				PhotoLayoutHelper.TiledLayoutResult layout=PhotoLayoutHelper.processThumbs(imageAttachments);
				MediaGridStatusDisplayItem mediaGrid=new MediaGridStatusDisplayItem(parentID, fragment, layout, imageAttachments, statusForContent);
				if((flags&FLAG_MEDIA_FORCE_HIDDEN)!=0){
					mediaGrid.sensitiveTitle=fragment.getString(R.string.media_hidden);
					statusForContent.sensitiveRevealed=false;
					statusForContent.sensitive=true;
				}else if(statusForContent.sensitive && AccountSessionManager.get(accountID).getLocalPreferences().revealCWs && !AccountSessionManager.get(accountID).getLocalPreferences().hideSensitiveMedia)
					statusForContent.sensitiveRevealed=true;
				contentItems.add(mediaGrid);
			}
			if((flags&FLAG_NO_MEDIA_PREVIEW)!=0){
				contentItems.add(new PreviewlessMediaGridStatusDisplayItem(parentID, fragment, null, imageAttachments, statusForContent));

			}
			for(Attachment att : statusForContent.mediaAttachments){
				if(att.type==Attachment.Type.AUDIO){
					contentItems.add(new AudioStatusDisplayItem(parentID, fragment, statusForContent, att));
				}
				if(att.type==Attachment.Type.UNKNOWN){
					contentItems.add(new FileStatusDisplayItem(parentID, fragment, att));
				}
			}
			if(statusForContent.poll!=null){
				buildPollItems(parentID, fragment, statusForContent.poll, status, contentItems);
			}
			if(statusForContent.card!=null && statusForContent.mediaAttachments.isEmpty() && statusForContent.quote==null && !statusForContent.card.isHashtagUrl(statusForContent.url)){
				contentItems.add(new LinkCardStatusDisplayItem(parentID, fragment, statusForContent, (flags&FLAG_NO_MEDIA_PREVIEW)==0));
			}
			if(statusForContent.quote!=null && (flags & FLAG_INSET)==0){
				if(!statusForContent.mediaAttachments.isEmpty() && statusForContent.poll==null) // add spacing if immediately preceded by attachment
					contentItems.add(new DummyStatusDisplayItem(parentID, fragment));
				contentItems.addAll(buildItems(fragment, statusForContent.quote, accountID, parentObject, knownAccounts, filterContext, FLAG_NO_FOOTER|FLAG_INSET|FLAG_NO_EMOJI_REACTIONS|FLAG_IS_FOR_QUOTE));
			} else if((flags & FLAG_INSET)==0 && statusForContent.mediaAttachments.isEmpty() && statusForContent.account!=null){
				tryAddNonOfficialQuote(statusForContent, fragment, accountID, filterContext);
			}
			if(contentItems!=items && statusForContent.spoilerRevealed){
				items.addAll(contentItems);
			}
			AccountLocalPreferences lp=fragment.getLocalPrefs();
			if((flags&FLAG_NO_EMOJI_REACTIONS)==0 && !status.preview && lp.emojiReactionsEnabled &&
					(lp.showEmojiReactions!=ONLY_OPENED || fragment instanceof ThreadFragment) &&
					statusForContent.reactions!=null){
				boolean isMainStatus=fragment instanceof ThreadFragment t && t.getMainStatus().id.equals(statusForContent.id);
				boolean showAddButton=lp.showEmojiReactions==ALWAYS || isMainStatus;
				items.add(new EmojiReactionsStatusDisplayItem(parentID, fragment, statusForContent, accountID, !showAddButton, false));
			}
			FooterStatusDisplayItem footer=null;
			if((flags&FLAG_NO_FOOTER)==0){
				footer=new FooterStatusDisplayItem(parentID, fragment, statusForContent, accountID);
				footer.hideCounts=hideCounts;
				items.add(footer);
			}
			boolean inset=(flags&FLAG_INSET)!=0;
			boolean isForQuote=(flags&FLAG_IS_FOR_QUOTE)!=0;
			// add inset dummy so last content item doesn't clip out of inset bounds
			if((inset || footer==null) && (flags&FLAG_CHECKABLE)==0 && !isForQuote){
				items.add(new DummyStatusDisplayItem(parentID, fragment));
				// in case we ever need the dummy to display a margin for the media grid again:
				// (i forgot why we apparently don't need this anymore)
				// !contentItems.isEmpty() && contentItems
				// 	.get(contentItems.size() - 1) instanceof MediaGridStatusDisplayItem));
			}
			GapStatusDisplayItem gap=null;
			if((flags&FLAG_NO_FOOTER)==0 && status.hasGapAfter!=null && !(fragment instanceof ThreadFragment))
				items.add(gap=new GapStatusDisplayItem(parentID, fragment, status));
			int i=1;
			for(StatusDisplayItem item : items){
				if(inset)
					item.inset=true;
				if(isForQuote){
					item.status=statusForContent;
					item.isForQuote=true;
				}
				item.index=i++;
			}
			if(items!=contentItems && !statusForContent.spoilerRevealed){
				for(StatusDisplayItem item : contentItems){
					if(inset)
						item.inset=true;
					if(isForQuote){
						item.status=statusForContent;
						item.isForQuote=true;
					}
					item.index=i++;
				}
			}

			List<StatusDisplayItem> nonGapItems=gap!=null ? items.subList(0, items.size()-1) : items;
			WarningFilteredStatusDisplayItem warning=applyingFilter==null ? null :
					new WarningFilteredStatusDisplayItem(parentID, fragment, statusForContent, nonGapItems, applyingFilter);
			if(warning!=null)
				warning.inset=inset;
			return applyingFilter==null ? items : new ArrayList<>(gap!=null
					? List.of(warning, gap)
					: Collections.singletonList(warning)
			);
		} catch(Exception e) {
			Log.e("StatusDisplayItem", "buildItems: failed to build StatusDisplayItem " + e);
			return new ArrayList<>(Collections.singletonList(new ErrorStatusDisplayItem(parentID, statusForContent, fragment, e)));
		}
	}

	public static void buildPollItems(String parentID, BaseStatusListFragment fragment, Poll poll, Status status, List<StatusDisplayItem> items){
		int i=0;
		for(Poll.Option opt:poll.options){
			items.add(new PollOptionStatusDisplayItem(parentID, poll, i, fragment, status));
			i++;
		}
		items.add(new PollFooterStatusDisplayItem(parentID, fragment, poll, status));
	}

	/**
	 * Tries to adds a non-official quote to a status.
	 * A non-official quote is a quote on an instance that does not support quotes officially.
	 */
	private static void tryAddNonOfficialQuote(Status status, BaseStatusListFragment fragment, String accountID, FilterContext filterContext) {
		Matcher matcher=QUOTE_PATTERN.matcher(status.getStrippedText());

		if(!matcher.find())
			return;
		String quoteURL=matcher.group();

		// account may be null for scheduled posts
		if (!UiUtils.looksLikeFediverseUrl(quoteURL))
			return;

		new GetSearchResults(quoteURL, GetSearchResults.Type.STATUSES, true, null, 0, 0).setCallback(new Callback<>(){
			@Override
			public void onSuccess(SearchResults results){
				AccountSessionManager.get(accountID).filterStatuses(results.statuses, filterContext);
				if (results.statuses == null || results.statuses.isEmpty())
					return;

				Status quote=results.statuses.get(0);
				new GetAccountRelationships(Collections.singletonList(quote.account.id))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Relationship> relationships){
								if(relationships.isEmpty())
									return;

								Relationship relationship=relationships.get(0);
								String selfId=AccountSessionManager.get(accountID).self.id;
								if(!status.account.id.equals(selfId) && (relationship.domainBlocking || relationship.muting || relationship.blocking)) {
									// do not show posts that are quoting a muted/blocked user
									fragment.removeStatus(status);
									return;
								}

								status.quote=results.statuses.get(0);
								fragment.updateStatusWithQuote(status);
							}

							@Override
							public void onError(ErrorResponse error){}
						})
						.exec(accountID);
			}

			@Override
			public void onError(ErrorResponse error){
				Log.w("StatusDisplayItem", "onError: failed to find quote status with URL: " + quoteURL + " " + error);
			}
		}).exec(accountID);
	}

	public enum Type{
		HEADER,
		REBLOG_OR_REPLY_LINE,
		TEXT,
		AUDIO,
		POLL_OPTION,
		POLL_FOOTER,
		CARD_LARGE,
		CARD_COMPACT,
		EMOJI_REACTIONS,
		FOOTER,
		ACCOUNT_CARD,
		ACCOUNT,
		HASHTAG,
		GAP,
		EXTENDED_FOOTER,
		MEDIA_GRID,
		PREVIEWLESS_MEDIA_GRID,
		WARNING,
		FILE,
		SPOILER,
		SECTION_HEADER,
		HEADER_CHECKABLE,
		NOTIFICATION_HEADER,
		ERROR_ITEM,
		FILTER_SPOILER,
		DUMMY
	}

	public static abstract class Holder<T extends StatusDisplayItem> extends BindableViewHolder<T> implements UsableRecyclerView.DisableableClickable{
		private Context context;

		public Holder(View itemView){
			super(itemView);
		}

		public Holder(Context context, int layout, ViewGroup parent){
			super(context, layout, parent);
			this.context=context;
		}

		public String getItemID(){
			return item.parentID;
		}

		@Override
		public void onClick(){
			if(item.isForQuote){
				item.status.filterRevealed=true;
				Bundle args=new Bundle();
				args.putString("account", item.parentFragment.getAccountID());
				args.putParcelable("status", Parcels.wrap(item.status.clone()));
				args.putBoolean("refresh", true);
				Nav.go((Activity) context, ThreadFragment.class, args);
				return;
			}

			item.parentFragment.onItemClick(item.parentID);
		}

		public Optional<StatusDisplayItem> getNextVisibleDisplayItem(){
			return getNextVisibleDisplayItem(null);
		}
		public Optional<StatusDisplayItem> getNextVisibleDisplayItem(Predicate<StatusDisplayItem> predicate){
			Optional<StatusDisplayItem> next=getNextDisplayItem();
			for(int offset=1; next.isPresent(); next=getDisplayItemOffset(++offset)){
				boolean isHidden=next.map(n->(n instanceof EmojiReactionsStatusDisplayItem e && e.isHidden())
						|| (n instanceof DummyStatusDisplayItem)).orElse(false);
				if(!isHidden && (predicate==null || predicate.test(next.get()))) return next;
			}
			return Optional.empty();
		}

		public Optional<StatusDisplayItem> getNextDisplayItem(){
			return getDisplayItemOffset(1);
		}

		public Optional<StatusDisplayItem> getDisplayItemOffset(int offset){
			List<StatusDisplayItem> displayItems=item.parentFragment.getDisplayItems();
			int thisPos=displayItems.indexOf(item);
			int offsetPos=thisPos + offset;
			return displayItems.size() > offsetPos && thisPos >= 0 && offsetPos >= 0
					? Optional.of(displayItems.get(offsetPos))
					: Optional.empty();
		}

		public boolean isLastDisplayItemForStatus(){
			return getNextVisibleDisplayItem()
					.map(next->!next.parentID.equals(item.parentID) || item.inset && !next.inset)
					.orElse(true);
		}

		@Override
		public boolean isEnabled(){
			return item.parentFragment.isItemEnabled(item.parentID) || item.isForQuote;
		}
	}
}
