package org.joinmastodon.android;

import static org.joinmastodon.android.api.MastodonAPIController.gson;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonSyntaxException;

import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;

import java.lang.reflect.Type;

public class GlobalUserPreferences{
	public static boolean playGifs;
	public static boolean useCustomTabs;
	public static boolean altTextReminders, confirmUnfollow, confirmBoost, confirmDeletePost;
	public static ThemePreference theme=ThemePreference.AUTO;
	public static boolean useDynamicColors;
	public static boolean showInteractionCounts;
	public static boolean customEmojiInNames;
	public static boolean showCWs;
	public static boolean hideSensitiveMedia;

	// MOSHIDON:
	public static boolean trueBlackTheme;
	public static boolean loadNewPosts;
	public static boolean showNewPostsButton;
	public static boolean toolbarMarquee;
	public static boolean disableSwipe;
	public static boolean enableDeleteNotifications;
	public static boolean translateButtonOpenedOnly;
	public static boolean uniformNotificationIcon;
	public static boolean reduceMotion;
	public static boolean showAltIndicator;
	public static boolean showNoAltIndicator;
	public static boolean enablePreReleases;
	public static PrefixRepliesMode prefixReplies;
	public static boolean collapseLongPosts;
	public static boolean spectatorMode;
	public static boolean autoHideFab;
	public static boolean allowRemoteLoading;
	public static AutoRevealMode autoRevealEqualSpoilers;
	public static boolean disableM3PillActiveIndicator;
	public static boolean showNavigationLabels;
	public static boolean displayPronounsInTimelines, displayPronounsInThreads, displayPronounsInUserListings;
	public static boolean overlayMedia;
	public static boolean showSuicideHelp;
	public static boolean underlinedLinks;
	public static AccountLocalPreferences.ColorPreference color;
	public static boolean likeIcon;
	public static boolean showDividers;
	public static boolean relocatePublishButton;
	public static boolean defaultToUnlistedReplies;
	public static boolean doubleTapToSearch;
	public static boolean doubleTapToSwipe;
	public static boolean confirmBeforeReblog;
	public static boolean hapticFeedback;
	public static boolean replyLineAboveHeader;
	public static boolean swapBookmarkWithBoostAction;
	public static boolean mentionRebloggerAutomatically;
	public static boolean showPostsWithoutAlt;
	public static boolean showMediaPreview;
	public static boolean removeTrackingParams;

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("global", Context.MODE_PRIVATE);
	}

	private static SharedPreferences getPreReplyPrefs(){
		return MastodonApp.context.getSharedPreferences("pre_reply_sheets", Context.MODE_PRIVATE);
	}

	public static void load(){
		SharedPreferences prefs=getPrefs();
		playGifs=prefs.getBoolean("playGifs", true);
		useCustomTabs=prefs.getBoolean("useCustomTabs", true);
		altTextReminders=prefs.getBoolean("altTextReminders", false);
		confirmUnfollow=prefs.getBoolean("confirmUnfollow", false);
		confirmBoost=prefs.getBoolean("confirmBoost", false);
		confirmDeletePost=prefs.getBoolean("confirmDeletePost", true);
		theme=ThemePreference.values()[prefs.getInt("theme", 0)];
		useDynamicColors=prefs.getBoolean("useDynamicColors", true);
		showInteractionCounts=prefs.getBoolean("interactionCounts", true);
		customEmojiInNames=prefs.getBoolean("emojiInNames", true);
		showCWs=prefs.getBoolean("showCWs", true);
		hideSensitiveMedia=prefs.getBoolean("hideSensitive", true);
		if(!prefs.getBoolean("perAccountMigrationDone", false)){
			AccountSession account=AccountSessionManager.getInstance().getLastActiveAccount();
			if(account!=null){
				SharedPreferences accPrefs=account.getRawLocalPreferences();
				showInteractionCounts=accPrefs.getBoolean("interactionCounts", true);
				customEmojiInNames=accPrefs.getBoolean("emojiInNames", true);
				showCWs=accPrefs.getBoolean("showCWs", true);
				hideSensitiveMedia=accPrefs.getBoolean("hideSensitive", true);
				save();
			}
			// Also applies to new app installs
			prefs.edit().putBoolean("perAccountMigrationDone", true).apply();
		}
	}

	public static void save(){
		getPrefs().edit()
				.putBoolean("playGifs", playGifs)
				.putBoolean("useCustomTabs", useCustomTabs)
				.putInt("theme", theme.ordinal())
				.putBoolean("altTextReminders", altTextReminders)
				.putBoolean("confirmUnfollow", confirmUnfollow)
				.putBoolean("confirmBoost", confirmBoost)
				.putBoolean("confirmDeletePost", confirmDeletePost)
				.putBoolean("useDynamicColors", useDynamicColors)
				.putBoolean("interactionCounts", showInteractionCounts)
				.putBoolean("emojiInNames", customEmojiInNames)
				.putBoolean("showCWs", showCWs)
				.putBoolean("hideSensitive", hideSensitiveMedia)
				.apply();
	}

	public static boolean isOptedOutOfPreReplySheet(PreReplySheetType type, Account account, String accountID){
		if(getPreReplyPrefs().getBoolean("opt_out_"+type, false))
			return true;
		if(account==null)
			return false;
		String accountKey=account.acct;
		if(!accountKey.contains("@"))
			accountKey+="@"+AccountSessionManager.get(accountID).domain;
		return getPreReplyPrefs().getBoolean("opt_out_"+type+"_"+accountKey.toLowerCase(), false);
	}

	public static void optOutOfPreReplySheet(PreReplySheetType type, Account account, String accountID){
		String key;
		if(account==null){
			key="opt_out_"+type;
		}else{
			String accountKey=account.acct;
			if(!accountKey.contains("@"))
				accountKey+="@"+AccountSessionManager.get(accountID).domain;
			key="opt_out_"+type+"_"+accountKey.toLowerCase();
		}
		getPreReplyPrefs().edit().putBoolean(key, true).apply();
	}

	public static void resetPreReplySheets(){
		getPreReplyPrefs().edit().clear().apply();
	}

	public enum ThemePreference{
		AUTO,
		LIGHT,
		DARK
	}

	public enum PreReplySheetType{
		OLD_POST,
		NON_MUTUAL
	}

	// MOSHIDON:
	public enum AutoRevealMode {
		NEVER,
		THREADS,
		DISCUSSIONS
	}

	// MOSHIDON:
	public enum PrefixRepliesMode {
		NEVER,
		ALWAYS,
		TO_OTHERS
	}

	// MOSHIDON: we have jason
	public static <T> T fromJson(String json, Type type, T orElse){
		if(json==null) return orElse;
		try{
			T value=gson.fromJson(json, type);
			return value==null ? orElse : value;
		}catch(JsonSyntaxException ignored){
			return orElse;
		}
	}

	// MOSHIDON: enums too!
	public static <T extends Enum<T>> T enumValue(Class<T> enumType, String name) {
		try { return Enum.valueOf(enumType, name); }
		catch (NullPointerException npe) { return null; }
	}
}
