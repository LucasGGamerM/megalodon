package org.joinmastodon.android.api.session;

import static org.joinmastodon.android.GlobalUserPreferences.fromJson;
import static org.joinmastodon.android.GlobalUserPreferences.enumValue;
import static org.joinmastodon.android.api.MastodonAPIController.gson;

import android.content.SharedPreferences;

import androidx.annotation.StringRes;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.model.ContentType;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.TimelineDefinition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AccountLocalPreferences{
	private final SharedPreferences prefs;

	public boolean showInteractionCounts;
	public boolean customEmojiInNames;
	public boolean revealCWs;
	public boolean hideSensitiveMedia;
	public boolean serverSideFiltersSupported;

	// MEGALODON
	public boolean showReplies;
	public boolean showBoosts;
	public ArrayList<String> recentLanguages;
	public boolean bottomEncoding;
	public ContentType defaultContentType;
	public boolean contentTypesEnabled;
	public ArrayList<TimelineDefinition> timelines;
	public boolean localOnlySupported;
	public boolean glitchInstance;
	public String publishButtonText;
	public String timelineReplyVisibility; // akkoma-only
	public boolean keepOnlyLatestNotification;
	public boolean emojiReactionsEnabled;
	public ShowEmojiReactions showEmojiReactions;
	public ColorPreference color;
	public ArrayList<Emoji> recentCustomEmoji;
	public boolean preReplySheet;

	private final static Type recentLanguagesType=new TypeToken<ArrayList<String>>() {}.getType();
	private final static Type timelinesType=new TypeToken<ArrayList<TimelineDefinition>>() {}.getType();
	private final static Type recentCustomEmojiType=new TypeToken<ArrayList<Emoji>>() {}.getType();

	// MOSHIDON
//	private final static Type recentEmojisType = new TypeToken<Map<String, Integer>>() {}.getType();
//	public Map<String, Integer> recentEmojis;
	private final static Type notificationFiltersType = new TypeToken<PushSubscription.Alerts>() {}.getType();
	public PushSubscription.Alerts notificationFilters;

	public AccountLocalPreferences(SharedPreferences prefs, AccountSession session){
		this.prefs=prefs;
		showInteractionCounts=prefs.getBoolean("interactionCounts", false);
		customEmojiInNames=prefs.getBoolean("emojiInNames", true);
		revealCWs=prefs.getBoolean("revealCWs", false);
		hideSensitiveMedia=prefs.getBoolean("hideSensitive", true);
		serverSideFiltersSupported=prefs.getBoolean("serverSideFilters", false);
//		preReplySheet=prefs.getBoolean("preReplySheet", false);

		// MEGALODON
		Optional<Instance> instance=session.getInstance();
		showReplies=prefs.getBoolean("showReplies", true);
		showBoosts=prefs.getBoolean("showBoosts", true);
		recentLanguages=fromJson(prefs.getString("recentLanguages", null), recentLanguagesType, new ArrayList<>());
		bottomEncoding=prefs.getBoolean("bottomEncoding", false);
		defaultContentType=enumValue(ContentType.class, prefs.getString("defaultContentType", instance.map(Instance::isIceshrimp).orElse(false) ? ContentType.MISSKEY_MARKDOWN.name() : ContentType.PLAIN.name()));
		contentTypesEnabled=prefs.getBoolean("contentTypesEnabled", instance.map(i->!i.isIceshrimp()).orElse(false));
		timelines=fromJson(prefs.getString("timelines", null), timelinesType, TimelineDefinition.getDefaultTimelines(session.getID()));
		localOnlySupported=prefs.getBoolean("localOnlySupported", false);
		glitchInstance=prefs.getBoolean("glitchInstance", false);
		publishButtonText=prefs.getString("publishButtonText", null);
		timelineReplyVisibility=prefs.getString("timelineReplyVisibility", null);
		keepOnlyLatestNotification=prefs.getBoolean("keepOnlyLatestNotification", false);
		emojiReactionsEnabled=prefs.getBoolean("emojiReactionsEnabled", instance.map(i->i.isAkkoma() || i.isIceshrimp()).orElse(false));
		showEmojiReactions=ShowEmojiReactions.valueOf(prefs.getString("showEmojiReactions", ShowEmojiReactions.HIDE_EMPTY.name()));
		color=prefs.contains("color") ? ColorPreference.valueOf(prefs.getString("color", null)) : null;
		recentCustomEmoji=fromJson(prefs.getString("recentCustomEmoji", null), recentCustomEmojiType, new ArrayList<>());

		// MOSHIDON
//		recentEmojis=fromJson(prefs.getString("recentEmojis", "{}"), recentEmojisType, new HashMap<>());
		notificationFilters=fromJson(prefs.getString("notificationFilters", gson.toJson(PushSubscription.Alerts.ofAll())), notificationFiltersType, PushSubscription.Alerts.ofAll());
	}

	public long getNotificationsPauseEndTime(){
		return prefs.getLong("notificationsPauseTime", 0L);
	}

	public void setNotificationsPauseEndTime(long time){
		prefs.edit().putLong("notificationsPauseTime", time).apply();
	}

	public ColorPreference getCurrentColor(){
		return color!=null ? color : GlobalUserPreferences.color!=null ? GlobalUserPreferences.color : ColorPreference.MATERIAL3;
	}

	public void save(){
		prefs.edit()
				.putBoolean("interactionCounts", showInteractionCounts)
				.putBoolean("emojiInNames", customEmojiInNames)
				.putBoolean("revealCWs", revealCWs)
				.putBoolean("hideSensitive", hideSensitiveMedia)
				.putBoolean("serverSideFilters", serverSideFiltersSupported)

				//TODO figure this stuff out
//				.putBoolean("preReplySheet", preReplySheet)

				// MEGALODON
				.putBoolean("showReplies", showReplies)
				.putBoolean("showBoosts", showBoosts)
				.putString("recentLanguages", gson.toJson(recentLanguages))
				.putBoolean("bottomEncoding", bottomEncoding)
				.putString("defaultContentType", defaultContentType==null ? null : defaultContentType.name())
				.putBoolean("contentTypesEnabled", contentTypesEnabled)
				.putString("timelines", gson.toJson(timelines))
				.putBoolean("localOnlySupported", localOnlySupported)
				.putBoolean("glitchInstance", glitchInstance)
				.putString("publishButtonText", publishButtonText)
				.putString("timelineReplyVisibility", timelineReplyVisibility)
				.putBoolean("keepOnlyLatestNotification", keepOnlyLatestNotification)
				.putBoolean("emojiReactionsEnabled", emojiReactionsEnabled)
				.putString("showEmojiReactions", showEmojiReactions.name())
				.putString("color", color!=null ? color.name() : null)
				.putString("recentCustomEmoji", gson.toJson(recentCustomEmoji))

				// MOSHIDON
//				.putString("recentEmojis", gson.toJson(recentEmojis))
				.putString("notificationFilters", gson.toJson(notificationFilters))
				.apply();
	}

	public enum ColorPreference{
		MATERIAL3,
		PURPLE,
		PINK,
		GREEN,
		BLUE,
		BROWN,
		RED,
		YELLOW,
		NORD,
		WHITE;

		public @StringRes int getName() {
			return switch(this){
				case MATERIAL3 -> R.string.sk_color_palette_material3;
				case PINK -> R.string.sk_color_palette_pink;
				case PURPLE -> R.string.sk_color_palette_purple;
				case GREEN -> R.string.sk_color_palette_green;
				case BLUE -> R.string.sk_color_palette_blue;
				case BROWN -> R.string.sk_color_palette_brown;
				case RED -> R.string.sk_color_palette_red;
				case YELLOW -> R.string.sk_color_palette_yellow;
				case NORD -> R.string.mo_color_palette_nord;
				case WHITE -> R.string.mo_color_palette_black_and_white;
			};
		}
	}

	public enum ShowEmojiReactions{
		HIDE_EMPTY,
		ONLY_OPENED,
		ALWAYS
	}
}
