package org.joinmastodon.android.ui.utils;

import static android.view.Menu.NONE;
import static org.joinmastodon.android.GlobalUserPreferences.ThemePreference.*;
import static org.joinmastodon.android.GlobalUserPreferences.theme;
import static org.joinmastodon.android.GlobalUserPreferences.trueBlackTheme;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.TypefaceSpan;
import android.transition.ChangeBounds;
import android.transition.ChangeScroll;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowInsets;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.E;
import org.joinmastodon.android.FileProvider;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.StatusInteractionController;
import org.joinmastodon.android.api.requests.accounts.SetAccountBlocked;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.SetAccountMuted;
import org.joinmastodon.android.api.requests.accounts.SetDomainBlocked;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.api.requests.accounts.AuthorizeFollowRequest;
import org.joinmastodon.android.api.requests.accounts.RejectFollowRequest;
import org.joinmastodon.android.api.requests.instance.GetInstance;
import org.joinmastodon.android.api.requests.lists.DeleteList;
import org.joinmastodon.android.api.requests.notifications.DismissNotification;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.DeleteStatus;
import org.joinmastodon.android.api.requests.statuses.GetStatusByID;
import org.joinmastodon.android.api.requests.statuses.SetStatusMuted;
import org.joinmastodon.android.api.requests.statuses.SetStatusPinned;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusMuteChangedEvent;
import org.joinmastodon.android.events.ScheduledStatusDeletedEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.FollowRequestHandledEvent;
import org.joinmastodon.android.events.NotificationDeletedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.events.StatusUnpinnedEvent;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.HashtagTimelineFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.settings.SettingsServerFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Searchable;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.sheets.AccountSwitcherSheet;
import org.joinmastodon.android.ui.sheets.BlockAccountConfirmationSheet;
import org.joinmastodon.android.ui.sheets.MuteAccountConfirmationSheet;
import org.joinmastodon.android.ui.sheets.BlockDomainConfirmationSheet;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.utils.Tracking;
import org.parceler.Parcels;

import java.io.File;
import java.lang.reflect.Field;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import okhttp3.MediaType;

public class UiUtils {
	private static Handler mainHandler = new Handler(Looper.getMainLooper());
	private static final DateTimeFormatter DATE_FORMATTER_SHORT_WITH_YEAR = DateTimeFormatter.ofPattern("d MMM uuuu"), DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("d MMM");
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);
	private static final DateTimeFormatter TIME_FORMATTER=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
	public static int MAX_WIDTH, SCROLL_TO_TOP_DELTA;

	public static final float ALPHA_PRESSED=0.55f;

	private UiUtils() {
	}

	public static void launchWebBrowser(Context context, String url) {
		if(GlobalUserPreferences.removeTrackingParams)
			url=Tracking.removeTrackingParameters(url);
		try {
			if (GlobalUserPreferences.useCustomTabs) {
				new CustomTabsIntent.Builder()
						.setShowTitle(true)
						.build()
						.launchUrl(context, Uri.parse(url));
			} else {
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}
		} catch (ActivityNotFoundException x) {
			Toast.makeText(context, R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
		}
	}

	public static String formatRelativeTimestamp(Context context, Instant instant) {
		return formatPeriodBetween(context, instant, null);
	}

	public static String formatPeriodBetween(Context context, Instant since, Instant until) {
		boolean ago = until == null;
		long t = since.toEpochMilli();
		long now = ago ? System.currentTimeMillis() : until.toEpochMilli();
		long diff = now - t;
		if(diff<1000L){
			return context.getString(R.string.time_now);
		}else if(diff<60_000L){
			long time = diff/1000L;
			return ago ?
					context.getString(R.string.time_seconds_ago_short, time) :
					context.getResources().getQuantityString(R.plurals.sk_time_seconds, (int) time, time);
		}else if(diff<3600_000L){
			long time = diff/60_000L;
			return ago ?
					context.getString(R.string.time_minutes_ago_short, time) :
					context.getResources().getQuantityString(R.plurals.sk_time_minutes, (int) time, time);
		}else if(diff<3600_000L*24L){
			long time = diff/3600_000L;
			return ago ?
					context.getString(R.string.time_hours_ago_short, time) :
					context.getResources().getQuantityString(R.plurals.sk_time_hours, (int) time, time);
		} else {
			int days = (int) (diff / (3600_000L * 24L));
			if (ago && days > 30) {
				ZonedDateTime dt = since.atZone(ZoneId.systemDefault());
				if (dt.getYear() == ZonedDateTime.now().getYear()) {
					return DATE_FORMATTER_SHORT.format(dt);
				} else {
					return DATE_FORMATTER_SHORT_WITH_YEAR.format(dt);
				}
			}
			return ago ? context.getString(R.string.time_days_ago_short, days) : context.getResources().getQuantityString(R.plurals.sk_time_days, days, days);
		}
	}

	public static String formatRelativeTimestampAsMinutesAgo(Context context, Instant instant, boolean relativeHours){
		long t=instant.toEpochMilli();
		long diff=System.currentTimeMillis()-t;
		if(diff<1000L && diff>-1000L){
			return context.getString(R.string.time_just_now);
		}else if(diff>0){
			if(diff<60_000L){
				int secs=(int)(diff/1000L);
				return context.getResources().getQuantityString(R.plurals.x_seconds_ago, secs, secs);
			}else if(diff<3600_000L){
				int mins=(int)(diff/60_000L);
				return context.getResources().getQuantityString(R.plurals.x_minutes_ago, mins, mins);
			}else if(relativeHours && diff<24*3600_000L){
				int hours=(int)(diff/3600_000L);
				return context.getResources().getQuantityString(R.plurals.x_hours_ago, hours, hours);
			}
		}else{
			if(diff>-60_000L){
				int secs=-(int)(diff/1000L);
				return context.getResources().getQuantityString(R.plurals.in_x_seconds, secs, secs);
			}else if(diff>-3600_000L){
				int mins=-(int)(diff/60_000L);
				return context.getResources().getQuantityString(R.plurals.in_x_minutes, mins, mins);
			}else if(relativeHours && diff>-24*3600_000L){
				int hours=-(int)(diff/3600_000L);
				return context.getResources().getQuantityString(R.plurals.in_x_hours, hours, hours);
			}
		}
		ZonedDateTime dt=instant.atZone(ZoneId.systemDefault());
		ZonedDateTime now=ZonedDateTime.now();
		String formattedTime=TIME_FORMATTER.format(dt);
		String formattedDate;
		LocalDate today=now.toLocalDate();
		LocalDate date=dt.toLocalDate();
		if(date.equals(today)){
			formattedDate=context.getString(R.string.today);
		}else if(date.equals(today.minusDays(1))){
			formattedDate=context.getString(R.string.yesterday);
		}else if(date.equals(today.plusDays(1))){
			formattedDate=context.getString(R.string.tomorrow);
		}else if(date.getYear()==today.getYear()){
			formattedDate=DATE_FORMATTER_SHORT.format(dt);
		}else{
			formattedDate=DATE_FORMATTER_SHORT_WITH_YEAR.format(dt);
		}
		return context.getString(R.string.date_at_time, formattedDate, formattedTime);
	}

	public static String formatTimeLeft(Context context, Instant instant) {
		long t = instant.toEpochMilli();
		long now = System.currentTimeMillis();
		long diff = t - now;
		if (diff < 60_000L) {
			int secs = (int) (diff / 1000L);
			return context.getResources().getQuantityString(R.plurals.x_seconds_left, secs, secs);
		} else if (diff < 3600_000L) {
			int mins = (int) (diff / 60_000L);
			return context.getResources().getQuantityString(R.plurals.x_minutes_left, mins, mins);
		} else if (diff < 3600_000L * 24L) {
			int hours = (int) (diff / 3600_000L);
			return context.getResources().getQuantityString(R.plurals.x_hours_left, hours, hours);
		} else {
			int days = (int) (diff / (3600_000L * 24L));
			return context.getResources().getQuantityString(R.plurals.x_days_left, days, days);
		}
	}

	@SuppressLint("DefaultLocale")
	public static String abbreviateNumber(int n) {
		if (n < 1000) {
			return String.format("%,d", n);
		} else if (n < 1_000_000) {
			float a = n / 1000f;
			return a > 99f ? String.format("%,dK", (int) Math.floor(a)) : String.format("%,.1fK", a);
		} else {
			float a = n / 1_000_000f;
			return a > 99f ? String.format("%,dM", (int) Math.floor(a)) : String.format("%,.1fM", n / 1_000_000f);
		}
	}

	@SuppressLint("DefaultLocale")
	public static String abbreviateNumber(long n) {
		if (n < 1_000_000_000L)
			return abbreviateNumber((int) n);

		double a = n / 1_000_000_000.0;
		return a > 99f ? String.format("%,dB", (int) Math.floor(a)) : String.format("%,.1fB", n / 1_000_000_000.0);
	}

	/**
	 * Android 6.0 has a bug where start and end compound drawables don't get tinted.
	 * This works around it by setting the tint colors directly to the drawables.
	 *
	 * @param textView
	 */
	public static void fixCompoundDrawableTintOnAndroid6(TextView textView) {
		Drawable[] drawables = textView.getCompoundDrawablesRelative();
		for (int i = 0; i < drawables.length; i++) {
			if (drawables[i] != null) {
				Drawable tinted = drawables[i].mutate();
				tinted.setTintList(textView.getTextColors());
				drawables[i] = tinted;
			}
		}
		textView.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
	}

	public static void runOnUiThread(Runnable runnable) {
		mainHandler.post(runnable);
	}

	public static void runOnUiThread(Runnable runnable, long delay) {
		mainHandler.postDelayed(runnable, delay);
	}

	public static void removeCallbacks(Runnable runnable) {
		mainHandler.removeCallbacks(runnable);
	}

	/**
	 * Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}.
	 */
	public static int lerp(int startValue, int endValue, float fraction) {
		return startValue + Math.round(fraction * (endValue - startValue));
	}

	public static String getFileName(Uri uri) {
		if (uri.getScheme().equals("content")) {
			try (Cursor cursor = MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
				cursor.moveToFirst();
				String name = cursor.getString(0);
				if (name != null)
					return name;
			} catch (Throwable ignore) {
			}
		}
		return uri.getLastPathSegment();
	}

	public static String formatFileSize(Context context, long size, boolean atLeastKB) {
		if (size < 1024 && !atLeastKB) {
			return context.getString(R.string.file_size_bytes, size);
		} else if (size < 1024 * 1024) {
			return context.getString(R.string.file_size_kb, size / 1024.0);
		} else if (size < 1024 * 1024 * 1024) {
			return context.getString(R.string.file_size_mb, size / (1024.0 * 1024.0));
		} else {
			return context.getString(R.string.file_size_gb, size / (1024.0 * 1024.0 * 1024.0));
		}
	}

	public static MediaType getFileMediaType(File file) {
		String name = file.getName();
		return MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(name.lastIndexOf('.') + 1)));
	}

	public static void loadCustomEmojiInTextView(TextView view) {
		CharSequence _text = view.getText();
		if (!(_text instanceof Spanned))
			return;
		Spanned text = (Spanned) _text;
		CustomEmojiSpan[] spans = text.getSpans(0, text.length(), CustomEmojiSpan.class);
		if (spans.length == 0)
			return;
		Map<Emoji, List<CustomEmojiSpan>> spansByEmoji = Arrays.stream(spans).collect(Collectors.groupingBy(s -> s.emoji));
		for (Map.Entry<Emoji, List<CustomEmojiSpan>> emoji : spansByEmoji.entrySet()) {
			ViewImageLoader.load(new ViewImageLoader.Target() {
				@Override
				public void setImageDrawable(Drawable d) {
					if (d == null)
						return;
					for (CustomEmojiSpan span : emoji.getValue()) {
						span.setDrawable(d);
					}
					view.setText(view.getText());
				}

				@Override
				public View getView() {
					return view;
				}
			}, null, new UrlImageLoaderRequest(emoji.getKey().url, 0, V.dp(20)), null, false, true);
		}
	}

	public static int getThemeColor(Context context, @AttrRes int attr) {
		if (context == null) return 0xff00ff00;
		TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
		int color = ta.getColor(0, 0xff00ff00);
		ta.recycle();
		return color;
	}

	public static int getThemeColorRes(Context context, @AttrRes int attr) {
		if (context == null) return 0xff00ff00;
		TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
		int color = ta.getResourceId(0, R.color.black);
		ta.recycle();
		return color;
	}

	public static void openProfileByID(Context context, String selfID, String id) {
		Bundle args = new Bundle();
		args.putString("account", selfID);
		args.putString("profileAccountID", id);
		Nav.go((Activity) context, ProfileFragment.class, args);
	}

	public static void openHashtagTimeline(Context context, String accountID, Hashtag hashtag){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("hashtag", Parcels.wrap(hashtag));
		Nav.go((Activity)context, HashtagTimelineFragment.class, args);
	}

	public static void openHashtagTimeline(Context context, String accountID, String hashtag){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("hashtagName", hashtag);
		Nav.go((Activity)context, HashtagTimelineFragment.class, args);
	}

	public static void showConfirmationAlert(Context context, @StringRes int title, @StringRes int message, @StringRes int confirmButton, Runnable onConfirmed) {
		showConfirmationAlert(context, title, message, confirmButton, 0, onConfirmed);
	}

	public static void showConfirmationAlert(Context context, @StringRes int title, @StringRes int message, @StringRes int confirmButton, @DrawableRes int icon, Runnable onConfirmed) {
		showConfirmationAlert(context, context.getString(title), message==0 ? null : context.getString(message), context.getString(confirmButton), icon, onConfirmed);
	}

	public static void showConfirmationAlert(Context context, CharSequence title, CharSequence message, CharSequence confirmButton, int icon, Runnable onConfirmed) {
		showConfirmationAlert(context, title, message, confirmButton, icon, onConfirmed, null);
	}

	public static void showConfirmationAlert(Context context, CharSequence title, CharSequence message, CharSequence confirmButton, int icon, Runnable onConfirmed, Runnable onDenied){
		new M3AlertDialogBuilder(context)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(confirmButton, (dlg, i)->onConfirmed.run())
				.setNegativeButton(R.string.cancel, (dialog, which) -> {
					if (onDenied != null)
						onDenied.run();
				})
				.setIcon(icon)
				.show();
	}

	public static void confirmToggleBlockUser(Activity activity, String accountID, Account account, boolean currentlyBlocked, Consumer<Relationship> resultCallback) {
		if(!currentlyBlocked){
			new BlockAccountConfirmationSheet(activity, account, (onSuccess, onError)->{
				new SetAccountBlocked(account.id, true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.accept(result);
								onSuccess.run();
								E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								onError.run();
							}
						})
						.exec(accountID);
			}).show();
		}else{
			new SetAccountBlocked(account.id, false)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							resultCallback.accept(result);
							new Snackbar.Builder(activity)
									.setText(activity.getString(R.string.unblocked_user_x, account.getDisplayUsername()))
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
						}
					})
					.wrapProgress(activity, R.string.loading, false)
					.exec(accountID);
		}
	}

	public static void confirmSoftBlockUser(Activity activity, String accountID, Account account, Consumer<Relationship> resultCallback) {
		showConfirmationAlert(activity,
				activity.getString(R.string.sk_remove_follower),
				activity.getString(R.string.sk_remove_follower_confirm, account.getDisplayName()),
				activity.getString(R.string.sk_do_remove_follower),
				R.drawable.ic_fluent_person_delete_24_regular,
				() -> new SetAccountBlocked(account.id, true).setCallback(new Callback<>() {
					@Override
					public void onSuccess(Relationship relationship) {
						new SetAccountBlocked(account.id, false).setCallback(new Callback<>() {
							@Override
							public void onSuccess(Relationship relationship) {
								if (activity == null) return;
								Toast.makeText(activity, R.string.sk_remove_follower_success, Toast.LENGTH_SHORT).show();
								resultCallback.accept(relationship);
							}

							@Override
							public void onError(ErrorResponse error) {
								error.showToast(activity);
								resultCallback.accept(relationship);
							}
						}).exec(accountID);
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(activity);
					}
				}).exec(accountID)
		);
	}

	public static void confirmToggleBlockDomain(Activity activity, String accountID, Account account, boolean currentlyBlocked, Runnable resultCallback){
		if(!currentlyBlocked){
			new BlockDomainConfirmationSheet(activity, account, (onSuccess, onError)->{
				new SetDomainBlocked(account.getDomain(), true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Object result){
								resultCallback.run();
								onSuccess.run();
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								onError.run();
							}
						})
						.exec(accountID);
			}, (onSuccess, onError)->{
				new SetAccountBlocked(account.id, true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.run();
								onSuccess.run();
								E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								onError.run();
							}
						})
						.exec(accountID);
			}).show();
		}else{
			new SetDomainBlocked(account.getDomain(), false)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Object result){
							resultCallback.run();
							new Snackbar.Builder(activity)
									.setText(activity.getString(R.string.unblocked_domain_x, account.getDomain()))
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
						}
					})
					.wrapProgress(activity, R.string.loading, false)
					.exec(accountID);
		}
	}
	public static void confirmToggleMuteUser(Context context, String accountID, Account account, boolean currentlyMuted, Consumer<Relationship> resultCallback){
		if(!currentlyMuted){
			//pass a references, so they can be changed inside the confirmation sheet
			AtomicReference<Duration> muteDuration=new AtomicReference<>(Duration.ZERO);
			AtomicBoolean muteNotifications=new AtomicBoolean(true);
			new MuteAccountConfirmationSheet(context, account, muteDuration, muteNotifications, (onSuccess, onError)->{
				new SetAccountMuted(account.id, true, muteDuration.get().getSeconds(), muteNotifications.get())
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.accept(result);
								onSuccess.run();
								E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(context);
								onError.run();
							}
						})
						.exec(accountID);
			}).show();
		}else{
			new SetAccountMuted(account.id, false, 0, false)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							resultCallback.accept(result);
							new Snackbar.Builder(context)
									.setText(context.getString(R.string.unmuted_user_x, account.getDisplayUsername()))
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(context);
						}
					})
					.wrapProgress(context, R.string.loading, false)
					.exec(accountID);
		}

		// I need to readd the mute thing, so this is gonna stay as a comment for now
//		View durationView=LayoutInflater.from(context).inflate(R.layout.mute_user_dialog, null);
//		LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//		params.setMargins(0, V.dp(-12), 0, 0);
//		durationView.setLayoutParams(params);
//		Button button=durationView.findViewById(R.id.button);
//		((TextView) durationView.findViewById(R.id.message)).setText(context.getString(R.string.confirm_mute, account.getDisplayName()));
//
//		AtomicReference<Duration> muteDuration=new AtomicReference<>(Duration.ZERO);
//
//		PopupMenu popupMenu=new PopupMenu(context, button, Gravity.CENTER_HORIZONTAL);
//		popupMenu.inflate(R.menu.mute_duration);
//		popupMenu.setOnMenuItemClickListener(item->{
//			int id=item.getItemId();
//			if(id==R.id.duration_indefinite)
//				muteDuration.set(Duration.ZERO);
//			else if(id==R.id.duration_minutes_5){
//				muteDuration.set(Duration.ofMinutes(5));
//			}else if(id==R.id.duration_minutes_30){
//				muteDuration.set(Duration.ofMinutes(30));
//			}else if(id==R.id.duration_hours_1){
//				muteDuration.set(Duration.ofHours(1));
//			}else if(id==R.id.duration_hours_6){
//				muteDuration.set(Duration.ofHours(6));
//			}else if(id==R.id.duration_days_1){
//				muteDuration.set(Duration.ofDays(1));
//			}else if(id==R.id.duration_days_3){
//				muteDuration.set(Duration.ofDays(3));
//			}else if(id==R.id.duration_days_7){
//				muteDuration.set(Duration.ofDays(7));
//			}
//			button.setText(item.getTitle());
//			return true;
//		});
//		button.setOnTouchListener(popupMenu.getDragToOpenListener());
//		button.setOnClickListener(v->popupMenu.show());
//		button.setText(popupMenu.getMenu().getItem(0).getTitle());
//
//		new M3AlertDialogBuilder(context)
//				.setTitle(context.getString(currentlyMuted ? R.string.confirm_unmute_title : R.string.confirm_mute_title))
//				.setMessage(currentlyMuted ? context.getString(R.string.confirm_unmute, account.getDisplayName()) : null)
//				.setView(currentlyMuted ? null : durationView)
//				.setPositiveButton(context.getString(currentlyMuted ? R.string.do_unmute : R.string.do_mute), (dlg, i)->{
//					new SetAccountMuted(account.id, !currentlyMuted, muteDuration.get().getSeconds())
//							.setCallback(new Callback<>(){
//								@Override
//								public void onSuccess(Relationship result){
//									resultCallback.accept(result);
//									if(!currentlyMuted){
//										E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
//									}
//								}
//
//								@Override
//								public void onError(ErrorResponse error){
//									error.showToast(context);
//								}
//							})
//							.wrapProgress(context, R.string.loading, false)
//							.exec(accountID);
//				})
//				.setNegativeButton(R.string.cancel, null)
//				.setIcon(currentlyMuted ? R.drawable.ic_fluent_speaker_2_28_regular : R.drawable.ic_fluent_speaker_off_28_regular)
//				.show();
	}

	public static void confirmDeletePost(Activity activity, String accountID, Status status, Consumer<Status> resultCallback, boolean forRedraft) {
		Status s=status.getContentStatus();
		showConfirmationAlert(activity,
				forRedraft ? R.string.sk_confirm_delete_and_redraft_title : R.string.confirm_delete_title,
				forRedraft ? R.string.sk_confirm_delete_and_redraft : R.string.confirm_delete,
				forRedraft ? R.string.sk_delete_and_redraft : R.string.delete,
				forRedraft ? R.drawable.ic_fluent_arrow_clockwise_28_regular : R.drawable.ic_fluent_delete_28_regular,
				() -> new DeleteStatus(s.id)
						.setCallback(new Callback<>() {
							@Override
							public void onSuccess(Status result) {
								resultCallback.accept(result);
								E.post(new StatusDeletedEvent(s.id, accountID));
								if(status!=s){
									E.post(new StatusDeletedEvent(status.id, accountID));
								}
							}

							@Override
							public void onError(ErrorResponse error) {
								error.showToast(activity);
							}
						})
						.wrapProgress(activity, R.string.deleting, false)
						.exec(accountID)
		);
	}

	public static void confirmToggleMuteConversation(Activity activity, String accountID, Status status, Runnable resultCallback) {
		showConfirmationAlert(activity,
				status.muted ? R.string.mo_unmute_conversation : R.string.mo_mute_conversation,
				status.muted ? R.string.mo_confirm_to_unmute_conversation : R.string.mo_confirm_to_mute_conversation,
				status.muted ? R.string.do_unmute : R.string.do_mute,
				status.muted ? R.drawable.ic_fluent_alert_28_regular : R.drawable.ic_fluent_alert_off_28_regular,
				() -> new SetStatusMuted(status.id, !status.muted)
						.setCallback(new Callback<Status>(){
							@Override
							public void onSuccess(Status result){
								resultCallback.run();
								Toast.makeText(activity, result.muted ? R.string.mo_muted_conversation_successfully : R.string.mo_unmuted_conversation_successfully, Toast.LENGTH_SHORT).show();
								E.post(new StatusMuteChangedEvent(result));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
							}
						})
						.wrapProgress(activity, status.muted ? R.string.mo_unmuting : R.string.mo_muting, false)
						.exec(accountID)

		);
	}

	public static void confirmDeleteScheduledPost(Activity activity, String accountID, ScheduledStatus status, Runnable resultCallback) {
		boolean isDraft = status.scheduledAt.isAfter(CreateStatus.DRAFTS_AFTER_INSTANT);
		showConfirmationAlert(activity,
				isDraft ? R.string.sk_confirm_delete_draft_title : R.string.sk_confirm_delete_scheduled_post_title,
				isDraft ? R.string.sk_confirm_delete_draft : R.string.sk_confirm_delete_scheduled_post,
				R.string.delete,
				R.drawable.ic_fluent_delete_28_regular,
				() -> new DeleteStatus.Scheduled(status.id)
						.setCallback(new Callback<>() {
							@Override
							public void onSuccess(Object o) {
								resultCallback.run();
								E.post(new ScheduledStatusDeletedEvent(status.id, accountID));
							}

							@Override
							public void onError(ErrorResponse error) {
								error.showToast(activity);
							}
						})
						.wrapProgress(activity, R.string.deleting, false)
						.exec(accountID)
		);
	}

	public static void confirmPinPost(Activity activity, String accountID, Status status, boolean pinned, Consumer<Status> resultCallback) {
		showConfirmationAlert(activity,
				pinned ? R.string.sk_confirm_pin_post_title : R.string.sk_confirm_unpin_post_title,
				pinned ? R.string.sk_confirm_pin_post : R.string.sk_confirm_unpin_post,
				pinned ? R.string.sk_pin_post : R.string.sk_unpin_post,
				pinned ? R.drawable.ic_fluent_pin_28_regular : R.drawable.ic_fluent_pin_off_28_regular,
				() -> {
					new SetStatusPinned(status.id, pinned)
							.setCallback(new Callback<>() {
								@Override
								public void onSuccess(Status result) {
									resultCallback.accept(result);
									E.post(new StatusCountersUpdatedEvent(result));
									if (!result.pinned)
										E.post(new StatusUnpinnedEvent(status.id, accountID));
								}

								@Override
								public void onError(ErrorResponse error) {
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, pinned ? R.string.sk_pinning : R.string.sk_unpinning, false)
							.exec(accountID);
				}
		);
	}

	public static void confirmDeleteNotification(Activity activity, String accountID, Notification notification, Runnable callback) {
		showConfirmationAlert(activity,
				notification == null ? R.string.sk_clear_all_notifications : R.string.sk_delete_notification,
				notification == null ? R.string.sk_clear_all_notifications_confirm : R.string.sk_delete_notification_confirm,
				notification == null ? R.string.sk_clear_all_notifications_confirm_action : R.string.sk_delete_notification_confirm_action,
				notification == null ? R.drawable.ic_fluent_mail_inbox_dismiss_28_regular : R.drawable.ic_fluent_delete_28_regular,
				() -> new DismissNotification(notification != null ? notification.id : null).setCallback(new Callback<>() {
					@Override
					public void onSuccess(Object o) {
						callback.run();
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(activity);
					}
				}).exec(accountID)
		);
	}

	public static void confirmDeleteList(Activity activity, String accountID, String listID, String listTitle, Runnable callback) {
		showConfirmationAlert(activity,
				activity.getString(R.string.sk_delete_list),
				activity.getString(R.string.sk_delete_list_confirm, listTitle),
				activity.getString(R.string.delete),
				R.drawable.ic_fluent_delete_28_regular,
				() -> new DeleteList(listID).setCallback(new Callback<>() {

							@Override
							public void onSuccess(Void result){
								callback.run();
							}

							@Override
							public void onError(ErrorResponse error) {
								error.showToast(activity);
							}
						})
						.wrapProgress(activity, R.string.deleting, false)
						.exec(accountID));
	}

	public static void performToggleAccountNotifications(Activity activity, Account account, String accountID, Relationship relationship, Button button, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback) {
		progressCallback.accept(true);
		new SetAccountFollowed(account.id, true, relationship.showingReblogs, !relationship.notifying)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(Relationship result) {
						resultCallback.accept(result);
						progressCallback.accept(false);
						Toast.makeText(activity, activity.getString(result.notifying ? R.string.sk_user_post_notifications_on : R.string.sk_user_post_notifications_off, '@' + account.username), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onError(ErrorResponse error) {
						progressCallback.accept(false);
						error.showToast(activity);
					}
				}).exec(accountID);
	}

	public static void setRelationshipToActionButtonM3(Relationship relationship, Button button){
		int styleRes;
		if(relationship.blocking){
			button.setText(R.string.button_blocked);
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal_Error;
		}else if(relationship.requested){
			button.setText(R.string.button_follow_pending);
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal;
		}else if(!relationship.following){
			button.setText(relationship.followedBy ? R.string.follow_back : R.string.button_follow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}else{
			button.setText(relationship.followedBy ? R.string.sk_button_mutuals : R.string.button_following);
			styleRes=relationship.followedBy ? R.style.Widget_Mastodon_M3_Button_Tonal_Outlined : R.style.Widget_Mastodon_M3_Button_Tonal;
		}

		TypedArray ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	public static void performAccountAction(Activity activity, Account account, String accountID, Relationship relationship, Button button, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback) {
		if (relationship.blocking) {
			confirmToggleBlockUser(activity, accountID, account, true, resultCallback);
		} else if (relationship.muting) {
			confirmToggleMuteUser(activity, accountID, account, true, resultCallback);
		} else {
			Runnable action=()->{
				progressCallback.accept(true);
				new SetAccountFollowed(account.id, !relationship.following && !relationship.requested, true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.accept(result);
								progressCallback.accept(false);
								if(!result.following && !result.requested){
									E.post(new RemoveAccountPostsEvent(accountID, account.id, true));
								}
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								progressCallback.accept(false);
							}
						})
						.exec(accountID);
			};
			if(relationship.following && GlobalUserPreferences.confirmUnfollow){
				showConfirmationAlert(activity, activity.getString(R.string.unfollow), activity.getString(R.string.unfollow_confirmation, account.getDisplayUsername()), activity.getString(R.string.unfollow), R.drawable.ic_fluent_person_delete_24_regular, action);
			}else{
				action.run();
			}
		}
	}


	public static void handleFollowRequest(Activity activity, Account account, String accountID, @Nullable String notificationID, boolean accepted, Relationship relationship, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback) {
		progressCallback.accept(true);
		if (accepted) {
			new AuthorizeFollowRequest(account.id).setCallback(new Callback<>() {
				@Override
				public void onSuccess(Relationship rel) {
					E.post(new FollowRequestHandledEvent(accountID, true, account, rel));
					progressCallback.accept(false);
					resultCallback.accept(rel);
				}

				@Override
				public void onError(ErrorResponse error) {
					progressCallback.accept(false);
					resultCallback.accept(relationship);
					error.showToast(activity);
				}
			}).exec(accountID);
		} else {
			new RejectFollowRequest(account.id).setCallback(new Callback<>() {
				@Override
				public void onSuccess(Relationship rel) {
					E.post(new FollowRequestHandledEvent(accountID, false, account, rel));
					if (notificationID != null)
						E.post(new NotificationDeletedEvent(notificationID));
					progressCallback.accept(false);
					resultCallback.accept(rel);
				}

				@Override
				public void onError(ErrorResponse error) {
					progressCallback.accept(false);
					resultCallback.accept(relationship);
					error.showToast(activity);
				}
			}).exec(accountID);
		}
	}

	public static <T> void updateList(List<T> oldList, List<T> newList, RecyclerView list, RecyclerView.Adapter<?> adapter, BiPredicate<T, T> areItemsSame) {
		// Save topmost item position and offset because for some reason RecyclerView would scroll the list to weird places when you insert items at the top
		int topItem, topItemOffset;
		if (list.getChildCount() == 0) {
			topItem = topItemOffset = 0;
		} else {
			View child = list.getChildAt(0);
			topItem = list.getChildAdapterPosition(child);
			topItemOffset = child.getTop();
		}
		DiffUtil.calculateDiff(new DiffUtil.Callback() {
			@Override
			public int getOldListSize() {
				return oldList.size();
			}

			@Override
			public int getNewListSize() {
				return newList.size();
			}

			@Override
			public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
				return areItemsSame.test(oldList.get(oldItemPosition), newList.get(newItemPosition));
			}

			@Override
			public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
				return true;
			}
		}).dispatchUpdatesTo(adapter);
		list.scrollToPosition(topItem);
		list.scrollBy(0, topItemOffset);
	}

	public static Bitmap getBitmapFromDrawable(Drawable d) {
		if (d instanceof BitmapDrawable)
			return ((BitmapDrawable) d).getBitmap();
		Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		d.draw(new Canvas(bitmap));
		return bitmap;
	}

	public static void insetPopupMenuIcon(Context context, MenuItem item) {
		insetPopupMenuIcon(context, item, 0);
	}

	public static void insetPopupMenuIcon(Context context, MenuItem item, int addWidth) {
		ColorStateList iconTint = ColorStateList.valueOf(UiUtils.getThemeColor(context, android.R.attr.textColorSecondary));
		insetPopupMenuIcon(item, iconTint, addWidth);
	}

	/**
	 * @param addWidth set if icon is too wide/narrow. if icon is 25dp in width, set to -1dp
	 */
	public static void insetPopupMenuIcon(MenuItem item, ColorStateList iconTint, int addWidth) {
		Drawable icon=item.getIcon().mutate();
		if(Build.VERSION.SDK_INT>=26) item.setIconTintList(iconTint);
		else icon.setTintList(iconTint);
		int pad=V.dp(8);
		boolean rtl=icon.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;
		icon=new InsetDrawable(icon, rtl ? pad+addWidth : pad, 0, rtl ? pad : addWidth+pad, 0);
		item.setIcon(icon);
 		SpannableStringBuilder ssb = new SpannableStringBuilder(item.getTitle());
		item.setTitle(ssb);
	}

	public static void resetPopupItemTint(MenuItem item) {
		if (Build.VERSION.SDK_INT >= 26) {
			item.setIconTintList(null);
		} else {
			Drawable icon = item.getIcon().mutate();
			icon.setTintList(null);
			item.setIcon(icon);
		}
	}

	public static void enableOptionsMenuIcons(Context context, Menu menu, @IdRes int... asAction) {
		if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
			try {
				Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
				m.setAccessible(true);
				m.invoke(menu, true);
				enableMenuIcons(context, menu, asAction);
			} catch (Exception ignored) {
			}
		}
	}

	public static void enableMenuIcons(Context context, Menu m, @IdRes int... exclude) {
		ColorStateList iconTint = ColorStateList.valueOf(UiUtils.getThemeColor(context, android.R.attr.textColorSecondary));
		for (int i = 0; i < m.size(); i++) {
			MenuItem item = m.getItem(i);
			SubMenu subMenu = item.getSubMenu();
			if (subMenu != null) enableMenuIcons(context, subMenu, exclude);
			if (item.getIcon() == null || Arrays.stream(exclude).anyMatch(id -> id == item.getItemId()))
				continue;
			insetPopupMenuIcon(item, iconTint, 0);
		}
	}

	public static void enablePopupMenuIcons(Context context, PopupMenu menu) {
		Menu m = menu.getMenu();

		// MOSHIDON disable menu icons on android 14 and higher because of InsetDrawables breaking
		if (Build.VERSION.SDK_INT >= 34) {
			return;
		}

		if (Build.VERSION.SDK_INT >= 29) {
			menu.setForceShowIcon(true);
		} else {
			try {
				Method setOptionalIconsVisible = m.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
				setOptionalIconsVisible.setAccessible(true);
				setOptionalIconsVisible.invoke(m, true);
			} catch (Exception ignore) {
			}
		}
		enableMenuIcons(context, m);
	}

	public static void setUserPreferredTheme(Context context) {
		setUserPreferredTheme(context, null);
	}

	public static void setUserPreferredTheme(Context context, @Nullable AccountSession session) {
		context.setTheme(switch(theme) {
			case LIGHT -> R.style.Theme_Mastodon_Light;
			case DARK -> R.style.Theme_Mastodon_Dark;
			default -> R.style.Theme_Mastodon_AutoLightDark;
		});

		AccountLocalPreferences prefs=session!=null ? session.getLocalPreferences() : null;
		AccountLocalPreferences.ColorPreference color=prefs!=null ? prefs.getCurrentColor() : AccountLocalPreferences.ColorPreference.MATERIAL3;
		ColorPalette palette = ColorPalette.palettes.get(color);
		if (palette != null) palette.apply(context, theme);

		Resources res = context.getResources();
		MAX_WIDTH = (int) res.getDimension(R.dimen.layout_max_width);
		SCROLL_TO_TOP_DELTA = (int) res.getDimension(R.dimen.scroll_to_top_delta);
	}

	public static int alphaBlendThemeColors(Context context, @AttrRes int color1, @AttrRes int color2, float alpha){
		if(UiUtils.isTrueBlackTheme()) return getThemeColor(context, color1);
		return alphaBlendColors(getThemeColor(context, color1), getThemeColor(context, color2), alpha);
	}

	public static boolean isTrueBlackTheme(){
		return isDarkTheme() && trueBlackTheme;
	}

	public static boolean isDarkTheme() {
		if (theme == AUTO)
			return (MastodonApp.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
		return theme == DARK;
	}

	public static Optional<Pair<String, Optional<String>>> parseFediverseHandle(String maybeFediHandle) {
		// https://stackoverflow.com/a/26987741, except i put a + here ... v
		String domainRegex = "^(((?!-))(xn--|_)?[a-z0-9-]{0,61}[a-z0-9]\\.)+(xn--)?([a-z0-9][a-z0-9\\-]{0,60}|[a-z0-9-]{1,30}\\.[a-z]{2,})$";
		if (maybeFediHandle.toLowerCase().startsWith("mailto:")) {
			maybeFediHandle = maybeFediHandle.substring("mailto:".length());
		}
		List<String> parts = Arrays.stream(maybeFediHandle.split("@"))
				.filter(part -> !part.isEmpty())
				.collect(Collectors.toList());
		if (parts.size() == 0 || !parts.get(0).matches("^[^/\\s]+$")) {
			return Optional.empty();
		} else if (parts.size() == 2) {
			try {
				String domain = IDN.toASCII(parts.get(1));
				if (!domain.matches(domainRegex)) return Optional.empty();
				return Optional.of(Pair.create(parts.get(0), Optional.of(parts.get(1))));
			} catch (IllegalArgumentException ignored) {
				return Optional.empty();
			}
		} else if (maybeFediHandle.startsWith("@")) {
			return Optional.of(Pair.create(parts.get(0), Optional.empty()));
		} else {
			return Optional.empty();
		}
	}

	// https://mastodon.foo.bar/@User
	// https://mastodon.foo.bar/@User/43456787654678
	// https://pleroma.foo.bar/users/User
	// https://pleroma.foo.bar/users/9qTHT2ANWUdXzENqC0
	// https://pleroma.foo.bar/notice/9sBHWIlwwGZi5QGlHc
	// https://pleroma.foo.bar/objects/d4643c42-3ae0-4b73-b8b0-c725f5819207
	// https://friendica.foo.bar/profile/user
	// https://friendica.foo.bar/display/d4643c42-3ae0-4b73-b8b0-c725f5819207
	// https://misskey.foo.bar/notes/83w6r388br (always lowercase)
	// https://pixelfed.social/p/connyduck/391263492998670833
	// https://pixelfed.social/connyduck
	// https://gts.foo.bar/@goblin/statuses/01GH9XANCJ0TA8Y95VE9H3Y0Q2
	// https://gts.foo.bar/@goblin
	// https://foo.microblog.pub/o/5b64045effd24f48a27d7059f6cb38f5
	//
	// COPIED FROM https://github.com/tuskyapp/Tusky/blob/develop/app/src/main/java/com/keylesspalace/tusky/util/LinkHelper.kt
	public static boolean looksLikeFediverseUrl(String urlString) {
		if(urlString == null)
			return false;
		URI uri;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			return false;
		}

		if (uri.getQuery() != null || uri.getFragment() != null || uri.getPath() == null)
			return false;

		String it = uri.getPath();
		return it.matches("^/@[^/]+$") ||
				it.matches("^/@[^/]+/\\d+$") ||
				it.matches("^/users/\\w+$") ||
				it.matches("^/notice/[a-zA-Z0-9]+$") ||
				it.matches("^/objects/[-a-f0-9]+$") ||
				it.matches("^/notes/[a-z0-9]+$") ||
				it.matches("^/display/[-a-f0-9]+$") ||
				it.matches("^/profile/\\w+$") ||
				it.matches("^/p/\\w+/\\d+$") ||
				it.matches("^/\\w+$") ||
				it.matches("^/@[^/]+/statuses/[a-zA-Z0-9]+$") ||
				it.matches("^/users/[^/]+/statuses/[a-zA-Z0-9]+$") ||
				it.matches("^/o/[a-f0-9]+$");
	}

	public static String getInstanceName(String accountID) {
		AccountSession session = AccountSessionManager.getInstance().getAccount(accountID);
		Optional<Instance> instance = session.getInstance();
		return instance.isPresent() && !instance.get().title.isBlank() ? instance.get().title : session.domain;
	}

	public static void pickAccount(Context context, String exceptFor, @StringRes int titleRes, @DrawableRes int iconRes, Consumer<AccountSession> sessionConsumer, Consumer<AlertDialog.Builder> transformDialog) {
		AccountSwitcherSheet sheet = new AccountSwitcherSheet((Activity) context, null, iconRes, titleRes == 0 ? R.string.choose_account : titleRes, exceptFor, false);
		sheet.setOnClick((accountId, open) ->sessionConsumer.accept(AccountSessionManager.get(accountId)));
		sheet.show();
	}

	public static void restartApp() {
		Intent intent = Intent.makeRestartActivityTask(MastodonApp.context.getPackageManager().getLaunchIntentForPackage(MastodonApp.context.getPackageName()).getComponent());
		MastodonApp.context.startActivity(intent);
		Runtime.getRuntime().exit(0);
	}

	public static MenuItem makeBackItem(Menu m) {
		MenuItem back = m.add(0, R.id.menu_back, NONE, R.string.back);
		back.setIcon(R.drawable.ic_fluent_arrow_left_24_regular);
		return back;
	}

	public static boolean setExtraTextInfo(Context ctx, @Nullable TextView extraText, boolean displayPronouns, boolean mentionedOnly, boolean localOnly, @Nullable Account account) {
		List<String> extraParts=new ArrayList<>();
		Optional<String> p=!displayPronouns ? Optional.empty() : extractPronouns(ctx, account);

		if(localOnly)
			extraParts.add(ctx.getString(R.string.sk_inline_local_only));
		if(mentionedOnly)
			extraParts.add(ctx.getString(R.string.sk_inline_direct));
		if(p.isPresent() && extraParts.isEmpty())
			extraParts.add(p.get());

		if(extraText!=null && !extraParts.isEmpty()) {
			String sepp=ctx.getString(R.string.sk_separator);
			String text=String.join(" " + sepp + " ", extraParts);
			if(account==null) extraText.setText(text);
			else HtmlParser.setTextWithCustomEmoji(extraText, text, account.emojis);
			extraText.setVisibility(View.VISIBLE);
			return true;
		}else{
			if(extraText!=null) extraText.setVisibility(View.GONE);
			return false;
		}
	}

	@FunctionalInterface
	public interface InteractionPerformer {
		void interact(StatusInteractionController ic, Status status, Consumer<Status> resultConsumer);
	}

	public static void pickInteractAs(Context context, String accountID, Status sourceStatus, Predicate<Status> checkInteracted, InteractionPerformer interactionPerformer, @StringRes int interactAsRes, @StringRes int interactedAsAccountRes, @StringRes int alreadyInteractedRes, @DrawableRes int iconRes) {
		pickAccount(context, accountID, interactAsRes, iconRes, session -> {
			lookupStatus(context, sourceStatus, session.getID(), accountID, status -> {
				if (status == null) return;

				if (checkInteracted.test(status)) {
					Toast.makeText(context, alreadyInteractedRes, Toast.LENGTH_SHORT).show();
					return;
				}

				StatusInteractionController ic = AccountSessionManager.getInstance().getAccount(session.getID()).getRemoteStatusInteractionController();
				interactionPerformer.interact(ic, status, s -> {
					if (checkInteracted.test(s)) {
						Toast.makeText(context, context.getString(interactedAsAccountRes, session.getFullUsername()), Toast.LENGTH_SHORT).show();
					}
				});
			});
		}, null);
	}

	public static Optional<MastodonAPIRequest<SearchResults>> lookupStatus(Context context, Status queryStatus, String targetAccountID, @Nullable String sourceAccountID, Consumer<Status> resultConsumer) {
		return lookup(context, queryStatus, targetAccountID, sourceAccountID, GetSearchResults.Type.STATUSES, resultConsumer, results ->
			!results.statuses.isEmpty() ? Optional.of(results.statuses.get(0)) : Optional.empty()
		);
	}

	public static Optional<MastodonAPIRequest<SearchResults>> lookupAccount(Context context, Account queryAccount, String targetAccountID, @Nullable String sourceAccountID, Consumer<Account> resultConsumer) {
		return lookup(context, queryAccount, targetAccountID, sourceAccountID, GetSearchResults.Type.ACCOUNTS, resultConsumer, results ->
				!results.accounts.isEmpty() ? Optional.of(results.accounts.get(0)) : Optional.empty()
		);
	}

	public static <T extends Searchable> Optional<MastodonAPIRequest<SearchResults>> lookup(Context context, T query, String targetAccountID, @Nullable String sourceAccountID, @Nullable GetSearchResults.Type type, Consumer<T> resultConsumer, Function<SearchResults, Optional<T>> extractResult) {
		if (sourceAccountID != null && targetAccountID.startsWith(sourceAccountID.substring(0, sourceAccountID.indexOf('_')))) {
			resultConsumer.accept(query);
			return Optional.empty();
		}

		return Optional.of(new GetSearchResults(query.getQuery(), type, true, null, 0, 0).setCallback(new Callback<>() {
			@Override
			public void onSuccess(SearchResults results) {
				Optional<T> result = extractResult.apply(results);
				if (result.isPresent()) resultConsumer.accept(result.get());
				else {
					Toast.makeText(context, R.string.sk_resource_not_found, Toast.LENGTH_SHORT).show();
					resultConsumer.accept(null);
				}
			}

			@Override
			public void onError(ErrorResponse error) {
				error.showToast(context);
			}
		})
				.wrapProgress((Activity) context, R.string.loading, true,
						d -> transformDialogForLookup(context, targetAccountID, null, d))
				.exec(targetAccountID));
	}

	public static void transformDialogForLookup(Context context, String accountID, @Nullable String url, ProgressDialog dialog) {
		if (accountID != null) {
			dialog.setTitle(context.getString(R.string.sk_loading_resource_on_instance_title, getInstanceName(accountID)));
		} else {
			dialog.setTitle(R.string.sk_loading_fediverse_resource_title);
		}
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel), (d, which) -> d.cancel());
		if (url != null) {
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.open_in_browser), (d, which) -> {
				d.cancel();
				launchWebBrowser(context, url);
			});
		}
	}

	private static Bundle bundleError(String error) {
		Bundle args = new Bundle();
		args.putString("error", error);
		return args;
	}

	private static Bundle bundleError(ErrorResponse error) {
		Bundle args = new Bundle();
		if (error instanceof MastodonErrorResponse e) {
			args.putString("error", e.error);
			args.putInt("httpStatus", e.httpStatus);
		}
		return args;
	}

	public static void openURL(Context context, String accountID, String url) {
		openURL(context, accountID, url, true);
	}

	public static void openURL(Context context, String accountID, String url, boolean launchBrowser) {
		lookupURL(context, accountID, url, (clazz, args) -> {
			if (clazz == null) {
				if (args != null && args.containsKey("error")) Toast.makeText(context, args.getString("error"), Toast.LENGTH_SHORT).show();
				if (launchBrowser) launchWebBrowser(context, url);
				return;
			}
			Nav.go((Activity) context, clazz, args);
		}).map(req -> req.wrapProgress((Activity) context, R.string.loading, true, d ->
				transformDialogForLookup(context, accountID, url, d)));
	}

	public static boolean acctMatches(String accountID, String acct, String queriedUsername, @Nullable String queriedDomain) {
		// check if the username matches
		if (!acct.split("@")[0].equalsIgnoreCase(queriedUsername)) return false;

		boolean resultOnHomeInstance = !acct.contains("@");
		if (resultOnHomeInstance) {
			// acct is formatted like 'someone'
			// only allow home instance result if query didn't specify a domain,
			// or the specified domain does, in fact, match the account session's domain
			AccountSession session = AccountSessionManager.getInstance().getAccount(accountID);
			return queriedDomain == null || session.domain.equalsIgnoreCase(queriedDomain);
		} else if (queriedDomain == null) {
			// accept whatever result we have as there's no queried domain to compare to
			return true;
		} else {
			// acct is formatted like 'someone@somewhere'
			return acct.split("@")[1].equalsIgnoreCase(queriedDomain);
		}
	}

	public static Optional<MastodonAPIRequest<SearchResults>> lookupAccountHandle(Context context, String accountID, String query, BiConsumer<Class<? extends Fragment>, Bundle> go) {
		return parseFediverseHandle(query).map(
				handle -> lookupAccountHandle(context, accountID, handle, go))
				.or(() -> {
					go.accept(null, null);
					return Optional.empty();
				});
	}
	public static MastodonAPIRequest<SearchResults> lookupAccountHandle(Context context, String accountID, Pair<String, Optional<String>> queryHandle, BiConsumer<Class<? extends Fragment>, Bundle> go) {
		String fullHandle = ("@" + queryHandle.first) + (queryHandle.second.map(domain -> "@" + domain).orElse(""));
		return new GetSearchResults(fullHandle, GetSearchResults.Type.ACCOUNTS, true, null, 0, 0)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(SearchResults results) {
						Bundle args = new Bundle();
						args.putString("account", accountID);
						Optional<Account> account = results.accounts.stream()
								.filter(a -> acctMatches(accountID, a.acct, queryHandle.first, queryHandle.second.orElse(null)))
								.findAny();
						if (account.isPresent()) {
							args.putParcelable("profileAccount", Parcels.wrap(account.get()));
							go.accept(ProfileFragment.class, args);
							return;
						}
						go.accept(null, bundleError(context.getString(R.string.sk_resource_not_found)));
					}

					@Override
					public void onError(ErrorResponse error) {
						go.accept(null, bundleError(error));
					}
				}).exec(accountID);
	}

	public static Optional<MastodonAPIRequest<?>> lookupURL(Context context, String accountID, String url, BiConsumer<Class<? extends Fragment>, Bundle> go) {
		Uri uri = Uri.parse(url);
		List<String> path = uri.getPathSegments();
		if (accountID != null && "https".equals(uri.getScheme())) {
			if (path.size() == 2 && path.get(0).matches("^@[a-zA-Z0-9_]+$") && path.get(1).matches("^[0-9]+$") && AccountSessionManager.getInstance().getAccount(accountID).domain.equalsIgnoreCase(uri.getAuthority())) {
				return Optional.of(new GetStatusByID(path.get(1))
						.setCallback(new Callback<>() {
							@Override
							public void onSuccess(Status result) {
								Bundle args = new Bundle();
								args.putString("account", accountID);
								args.putParcelable("status", Parcels.wrap(result));
								go.accept(ThreadFragment.class, args);
							}

							@Override
							public void onError(ErrorResponse error) {
								go.accept(null, bundleError(error));
							}
						})
						.exec(accountID));
			} else if (uri.getPath() != null && uri.getPath().matches("^/about$")) {
				return Optional.of(new GetInstance()
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Instance result){
								Bundle args = new Bundle();
								args.putParcelable("instance", Parcels.wrap(result));
								args.putString("account", accountID);
								go.accept(SettingsServerFragment.class, args);
							}

							@Override
							public void onError(ErrorResponse error){
								go.accept(null, bundleError(error));
							}
						})
						.execNoAuth(uri.getHost()));
			} else if (looksLikeFediverseUrl(url)) {
				return Optional.of(new GetSearchResults(url, null, true, null, 0, 0)
						.setCallback(new Callback<>() {
							@Override
							public void onSuccess(SearchResults results) {
								Bundle args = new Bundle();
								args.putString("account", accountID);
								if (!results.statuses.isEmpty()) {
									args.putParcelable("status", Parcels.wrap(results.statuses.get(0)));
									go.accept(ThreadFragment.class, args);
									return;
								}
								Optional<Account> account = results.accounts.stream()
										.filter(a -> uri.getPath().contains(a.username)).findAny();
								if (account.isPresent()) {
									args.putParcelable("profileAccount", Parcels.wrap(account.get()));
									go.accept(ProfileFragment.class, args);
									return;
								}
								go.accept(null, null);
							}

							@Override
							public void onError(ErrorResponse error) {
								go.accept(null, bundleError(error));
							}
						})
						.exec(accountID));
			}
		}
		go.accept(null, null);
		return Optional.empty();
	}

	public static void copyText(View v, String text) {
		if(GlobalUserPreferences.removeTrackingParams)
			text=Tracking.cleanUrlsInText(text);
		Context context = v.getContext();
		context.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, text));
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || UiUtils.isMIUI()) { // Android 13+ SystemUI shows its own thing when you put things into the clipboard
			Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show();
		}
		v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
	}

	private static String getSystemProperty(String key) {
		try {
			Class<?> props = Class.forName("android.os.SystemProperties");
			Method get = props.getMethod("get", String.class);
			return (String) get.invoke(null, key);
		} catch (Exception ignore) {
		}
		return null;
	}

	public static boolean isMIUI() {
		return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.code"));
	}

	public static boolean isEMUI() {
		return !TextUtils.isEmpty(getSystemProperty("ro.build.version.emui"));
	}

	public static boolean isMagic() {
		return !TextUtils.isEmpty(getSystemProperty("ro.build.version.magic"));
	}

	public static int alphaBlendColors(int color1, int color2, float alpha) {
		float alpha0 = 1f - alpha;
		int r = Math.round(((color1 >> 16) & 0xFF) * alpha0 + ((color2 >> 16) & 0xFF) * alpha);
		int g = Math.round(((color1 >> 8) & 0xFF) * alpha0 + ((color2 >> 8) & 0xFF) * alpha);
		int b = Math.round((color1 & 0xFF) * alpha0 + (color2 & 0xFF) * alpha);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public static boolean pickAccountForCompose(Activity activity, String accountID, String prefilledText) {
		Bundle args = new Bundle();
		if (prefilledText != null) args.putString("prefilledText", prefilledText);
		return pickAccountForCompose(activity, accountID, args);
	}

	public static boolean pickAccountForCompose(Activity activity, String accountID) {
		return pickAccountForCompose(activity, accountID, (String) null);
	}

	public static boolean pickAccountForCompose(Activity activity, String accountID, Bundle args) {
		if (AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1) {
			UiUtils.pickAccount(activity, accountID, 0, R.drawable.ic_fluent_compose_28_regular, session -> {
				args.putString("account", session.getID());
				Nav.go(activity, ComposeFragment.class, args);
			}, null);
			return true;
		} else {
			return false;
		}
	}

	// https://github.com/tuskyapp/Tusky/pull/3148
	public static void reduceSwipeSensitivity(ViewPager2 pager) {
		try {
			Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
			recyclerViewField.setAccessible(true);
			RecyclerView recyclerView = (RecyclerView) recyclerViewField.get(pager);
			Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
			touchSlopField.setAccessible(true);
			int touchSlop = touchSlopField.getInt(recyclerView);
			touchSlopField.set(recyclerView, touchSlop * 3);
		} catch (Exception ex) {
			Log.e("reduceSwipeSensitivity", Log.getStackTraceString(ex));
		}
	}

	public static View makeOverflowActionView(Context ctx) {
		// container needs tooltip, content description
		LinearLayout container = new LinearLayout(ctx, null, 0, R.style.Widget_Mastodon_ActionButton_Overflow) {
			@Override
			public CharSequence getAccessibilityClassName() {
				return Button.class.getName();
			}
		};
		// image needs, well, the image, and the paddings
		ImageView image = new ImageView(ctx, null, 0, R.style.Widget_Mastodon_ActionButton_Overflow);

		image.setDuplicateParentStateEnabled(true);
		image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		image.setClickable(false);
		image.setFocusable(false);
		image.setEnabled(false);

		// problem: as per overflow action button defaults, the padding on left and right is unequal
		// so (however the native overflow button manages this), the ripple background is off-center

		// workaround: set both paddings to the smaller, left one…
		int end = image.getPaddingEnd();
		int start = image.getPaddingStart();
		int paddingDiff = end - start; // what's missing to the long padding
		image.setPaddingRelative(start, image.getPaddingTop(), start, image.getPaddingBottom());

		// …and add the missing padding to the right on the container
		container.setPaddingRelative(0, 0, paddingDiff, 0);
		container.setBackground(null);
		container.setClickable(true);
		container.setFocusable(true);

		container.addView(image);

		// fucking finally
		return container;
	}

	/**
	 * Check to see if Android platform photopicker is available on the device\
	 *
	 * @return whether the device supports photopicker intents.
	 */
	@SuppressLint("NewApi")
	public static boolean isPhotoPickerAvailable(){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
			return true;
		}else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
			return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)>=2;
		}else
			return false;
	}

	@SuppressLint("InlinedApi")
	public static Intent getMediaPickerIntent(String[] mimeTypes, int maxCount){
		Intent intent;
		if(isPhotoPickerAvailable()){
			intent=new Intent(MediaStore.ACTION_PICK_IMAGES);
			if(maxCount>1)
				intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxCount);
		}else{
			intent=new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
		}
		if(mimeTypes.length>1){
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		}else if(mimeTypes.length==1){
			intent.setType(mimeTypes[0]);
		}else{
			intent.setType("*/*");
		}
		if(maxCount>1)
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		return intent;
	}

	public static void showFragmentForNotification(Context context, Notification n, String accountID, Bundle extras) {
		if (extras == null) extras = new Bundle();
		extras.putString("account", accountID);
		if (n.status!=null) {
			Status status=n.status;
			extras.putParcelable("status", Parcels.wrap(status.clone()));
			Nav.go((Activity) context, ThreadFragment.class, extras);
		} else if (n.report != null) {
			String domain = AccountSessionManager.getInstance().getAccount(accountID).domain;
			UiUtils.launchWebBrowser(context, "https://"+domain+"/admin/reports/"+n.report.id);
		} else if (n.account != null) {
			extras.putString("account", accountID);
			extras.putParcelable("profileAccount", Parcels.wrap(n.account));
			Nav.go((Activity) context, ProfileFragment.class, extras);
		}
	}

	/**
	 * Wraps a View.OnClickListener to filter multiple clicks in succession.
	 * Useful for buttons that perform some action that changes their state asynchronously.
	 * @param l
	 * @return
	 */
	public static View.OnClickListener rateLimitedClickListener(View.OnClickListener l){
		return new View.OnClickListener(){
			private long lastClickTime;

			@Override
			public void onClick(View v){
				if(SystemClock.uptimeMillis()-lastClickTime>500L){
					lastClickTime=SystemClock.uptimeMillis();
					l.onClick(v);
				}
			}
		};
	}

	@SuppressLint("DefaultLocale")
	public static String formatMediaDuration(int seconds){
		if(seconds>=3600)
			return String.format("%d:%02d:%02d", seconds/3600, seconds%3600/60, seconds%60);
		else
			return String.format("%d:%02d", seconds/60, seconds%60);
	}

	public static void beginLayoutTransition(ViewGroup sceneRoot){
		TransitionManager.beginDelayedTransition(sceneRoot, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.addTransition(new ChangeScroll())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
		);
	}

	public static Drawable getThemeDrawable(Context context, @AttrRes int attr){
		TypedArray ta=context.obtainStyledAttributes(new int[]{attr});
		Drawable d=ta.getDrawable(0);
		ta.recycle();
		return d;
	}

	public static WindowInsets applyBottomInsetToFixedView(View view, WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			view.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(40)) : 0);
			return insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
		}
		return insets;
	}

	public static void applyBottomInsetToFAB(View fab, WindowInsets insets){
		int inset;
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0 /*&& wantsOverlaySystemNavigation()*/){
			int bottomInset=insets.getSystemWindowInsetBottom();
			inset=bottomInset>0 ? Math.max(V.dp(40), bottomInset) : 0;
		}else{
			inset=0;
		}
		((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+inset;
	}

	public static String formatDuration(Context context, int seconds){
		if(seconds<3600){
			int minutes=seconds/60;
			return context.getResources().getQuantityString(R.plurals.x_minutes, minutes, minutes);
		}else if(seconds<24*3600){
			int hours=seconds/3600;
			return context.getResources().getQuantityString(R.plurals.x_hours, hours, hours);
		}else if(seconds>=7*24*3600 && seconds%(7*24*3600)<24*3600){
			int weeks=seconds/(7*24*3600);
			return context.getResources().getQuantityString(R.plurals.x_weeks, weeks, weeks);
		}else{
			int days=seconds/(24*3600);
			return context.getResources().getQuantityString(R.plurals.x_days, days, days);
		}
	}

	public static Uri getFileProviderUri(Context context, File file){
		return FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider", file);
	}

	public static void openSystemShareSheet(Context context, Object obj){
		Intent intent=new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		Account account;
		String url;
		String previewTitle;

		if(obj instanceof Account acc){
			account=acc;
			url=acc.url;
			previewTitle=context.getString(R.string.share_sheet_preview_profile, account.displayName);
		}else if(obj instanceof Status st){
			account=st.account;
			url=st.url;
			String postText=st.getStrippedText();
			if(TextUtils.isEmpty(postText)){
				previewTitle=context.getString(R.string.share_sheet_preview_profile, account.displayName);
			}else{
				if(postText.length()>100)
					postText=postText.substring(0, 100)+"...";
				previewTitle=context.getString(R.string.share_sheet_preview_post, account.displayName, postText);
			}
		}else{
			throw new IllegalArgumentException("Unsupported share object type");
		}

		intent.putExtra(Intent.EXTRA_TEXT, url);
		intent.putExtra(Intent.EXTRA_TITLE, previewTitle);
		ImageCache cache=ImageCache.getInstance(context);
		try{
			File ava=cache.getFile(new UrlImageLoaderRequest(account.avatarStatic));
			if(ava==null || !ava.exists())
				ava=cache.getFile(new UrlImageLoaderRequest(account.avatar));
			if(ava!=null && ava.exists()){
				intent.setClipData(ClipData.newRawUri(null, getFileProviderUri(context, ava)));
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			}
		}catch(IOException ignore){}
		context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_toot_title)));
	}

	private static final Pattern formatStringSubstitutionPattern = Pattern.compile("%(?:(\\d)\\$)?s");
	public static CharSequence generateFormattedString(String format, CharSequence... args) {
		if (format.startsWith(" ")) format = format.substring(1);
		if (format.endsWith(" ")) format = format.substring(0, format.length() - 1);

		Map<Integer, Integer> formatIndices = new HashMap<>();
		String[] partsInBetween = formatStringSubstitutionPattern.split(format, -1);
		SpannableStringBuilder text = new SpannableStringBuilder();

		Matcher m = formatStringSubstitutionPattern.matcher(format);
		int argsMaxIndex = 0;
		while (m.find()) {
			String group = m.groupCount() < 1 ? null : m.group(1);
			int index = formatIndices.size();
			try { index = Integer.parseInt(group); }
			catch (Exception ignored) {}
			formatIndices.put(index, argsMaxIndex++);
		}

		int formatOffset = formatIndices.size() > 0 ? Collections.min(formatIndices.keySet()) : 0;
		int argsOffset = 0;

		// say, string is just 'reacted with %s', but there are two arguments
		if (args.length > argsMaxIndex) {
			text.append(args[0], new TypefaceSpan("sans-serif-medium"), 0).append(' ');
			argsOffset++;
		}

		// join the args with the parts in between
		for (int i = 0; i < partsInBetween.length; i++) {
			text.append(partsInBetween[i]);
			Integer pos = formatIndices.get(i + formatOffset);
			if (pos != null && pos < args.length) {
				text.append(args[pos + argsOffset], new TypefaceSpan("sans-serif-medium"), 0);
			}
		}

		// add additional args to the end of the string
		if (args.length > argsMaxIndex + 1) {
			for (int i = argsMaxIndex + 1; i < args.length; i++) {
				text.append(' ').append(args[i], new TypefaceSpan("sans-serif-medium"), 0);
			}
		}

		return text;
	}

	public static void goToInstanceAboutFragment(String instanceUrl, String accountID ,Context context){
		try {
			new GetInstance()
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Instance result){
							Bundle args = new Bundle();
							args.putParcelable("instance", Parcels.wrap(result));
							args.putString("account", accountID);
							Nav.go((Activity) context, SettingsServerFragment.class, args);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(context);
						}
					})
					.wrapProgress((Activity) context, R.string.loading, true)
					.execRemote(instanceUrl);
		} catch (NullPointerException ignored) {
			// maybe the url was malformed?
			Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
		}
	}

	private static final String[] pronounsUrls= new String[] {
			"pronouns.within.lgbt/",
			"pronouns.cc/pronouns/",
			"pronouns.page/"
	};

	private static final String PRONOUN_CHARS="\\w*¿¡!?";
	private static final Pattern trimPronouns=
			Pattern.compile("[^"+PRONOUN_CHARS+"]*(["+PRONOUN_CHARS+"].*["+PRONOUN_CHARS+"]|["+PRONOUN_CHARS+"])\\W*");
	private static String extractPronounsFromField(String localizedPronouns, AccountField field) {
		if(!field.name.toLowerCase().contains(localizedPronouns) &&
				!field.name.toLowerCase().contains("pronouns")) return null;
		String text=HtmlParser.text(field.value);
		if(text.toLowerCase().contains("https://")){
			for(String pronounUrl : pronounsUrls){
				int index=text.indexOf(pronounUrl);
				int beginPronouns=index+pronounUrl.length();
				// we only want to display the info from the urls if they're not usernames
				if(index>-1 && beginPronouns<text.length() && text.charAt(beginPronouns)!='@'){
					return text.substring(beginPronouns);
				}
			}
			// maybe it's like "they and them (https://pronouns.page/...)"
			String[] parts=text.substring(0, text.toLowerCase().indexOf("https://"))
					.split(" ");
			if (parts.length==0) return null;
			text=String.join(" ", parts);
		}

		Matcher matcher=trimPronouns.matcher(text);
		if(!matcher.find()) return null;
		String pronouns=matcher.group(1);

		// crude fix to allow for pronouns like "it(/she)" or "(de) sie/ihr"
		int missingParens=0, missingBrackets=0;
		for(char c : pronouns.toCharArray()){
			if(c=='(') missingParens++;
			else if(c=='[') missingBrackets++;
			else if(c==')') missingParens--;
			else if(c==']') missingBrackets--;
		}
		if(missingParens > 0) pronouns+=")".repeat(missingParens);
		else if(missingParens < 0) pronouns="(".repeat(missingParens*-1)+pronouns;
		if(missingBrackets > 0) pronouns+="]".repeat(missingBrackets);
		else if(missingBrackets < 0) pronouns="[".repeat(missingBrackets*-1)+pronouns;

		// if ends with an un-closed custom emoji
		if(pronouns.matches("^.*\\s+:[a-zA-Z_]+$")) pronouns+=':';
		return pronouns;
	}

	// https://stackoverflow.com/questions/9475589/how-to-get-string-from-different-locales-in-android
	public static Context getLocalizedContext(Context context, Locale desiredLocale) {
		Configuration conf = context.getResources().getConfiguration();
		conf = new Configuration(conf);
		conf.setLocale(desiredLocale);
		return context.createConfigurationContext(conf);
	}

	public static Optional<String> extractPronouns(Context context, @Nullable Account account) {
		if (account==null || account.fields==null) return Optional.empty();
		String localizedPronouns=context.getString(R.string.sk_pronouns_label).toLowerCase();

		// higher = worse. the lowest number wins. also i'm sorry for writing this
		ToIntFunction<AccountField> comparePronounFields=(f)->{
			String t=f.name.toLowerCase();
			int localizedIndex = t.indexOf(localizedPronouns);
			int englishIndex = t.indexOf("pronouns");
			// neutralizing an english fallback failure if the localized pronoun already succeeded
			// -t.length() + t.length() = 0 -> so the low localized score doesn't get obscured
			if (englishIndex < 0) englishIndex = localizedIndex > -1 ? -t.length() : t.length();
			if (localizedIndex < 0) localizedIndex = t.length();
			return (localizedIndex + t.length()) + (englishIndex + t.length()) * 100;
		};

		// debugging:
//		List<Integer> ints = account.fields.stream().map(comparePronounFields::applyAsInt).collect(Collectors.toList());
//		List<AccountField> sorted = account.fields.stream().sorted(Comparator.comparingInt(comparePronounFields)).collect(Collectors.toList());

		return account.fields.stream()
				.sorted(Comparator.comparingInt(comparePronounFields))
				.map(f->UiUtils.extractPronounsFromField(localizedPronouns, f))
				.filter(Objects::nonNull)
				.findFirst();
	}

	public static void opacityIn(View v){
		v.animate().alpha(1).setDuration(400).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
	}

	public static void opacityOut(View v){
		opacityOut(v, ALPHA_PRESSED).start();
	}

	public static ViewPropertyAnimator opacityOut(View v, float alpha){
		return v.animate().alpha(alpha).setDuration(300).setInterpolator(CubicBezierInterpolator.DEFAULT);
	}

	public static void maybeShowTextCopiedToast(Context context){
		//show toast, android from S_V2 on has built-in popup, as documented in
		//https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
		if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.S_V2){
			Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show();
		}
	}

	public static boolean needShowClipboardToast(){
		return Build.VERSION.SDK_INT<=Build.VERSION_CODES.S_V2;
	}

	public static void setAllPaddings(View view, int paddingDp){
		int pad=V.dp(paddingDp);
		view.setPadding(pad, pad, pad, pad);
	}

	public static ViewGroup.MarginLayoutParams makeLayoutParams(int width, int height, int marginStart, int marginTop, int marginEnd, int marginBottom){
		ViewGroup.MarginLayoutParams lp=new ViewGroup.MarginLayoutParams(width>0 ? V.dp(width) : width, height>0 ? V.dp(height) : height);
		lp.topMargin=V.dp(marginTop);
		lp.bottomMargin=V.dp(marginBottom);
		lp.setMarginStart(V.dp(marginStart));
		lp.setMarginEnd(V.dp(marginEnd));
		return lp;
	}

	public static CharSequence fixBulletListInString(Context context, @StringRes int res){
		SpannableStringBuilder msg=new SpannableStringBuilder(context.getText(res));
		BulletSpan[] spans=msg.getSpans(0, msg.length(), BulletSpan.class);
		for(BulletSpan span:spans){
			BulletSpan betterSpan;
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q)
				betterSpan=new BulletSpan(V.dp(10), UiUtils.getThemeColor(context, R.attr.colorM3OnSurface));
			else
				betterSpan=new BulletSpan(V.dp(10), UiUtils.getThemeColor(context, R.attr.colorM3OnSurface), V.dp(1.5f));
			msg.setSpan(betterSpan, msg.getSpanStart(span), msg.getSpanEnd(span), msg.getSpanFlags(span));
			msg.removeSpan(span);
		}
		return msg;
	}

	public static void showProgressForAlertButton(Button button, boolean show){
		boolean shown=button.getTag(R.id.button_progress_orig_color)!=null;
		if(shown==show)
			return;
		button.setEnabled(!show);
		if(show){
			ColorStateList origColor=button.getTextColors();
			button.setTag(R.id.button_progress_orig_color, origColor);
			button.setTextColor(0);
			ProgressBar progressBar=(ProgressBar) LayoutInflater.from(button.getContext()).inflate(R.layout.progress_bar, null);
			Drawable progress=progressBar.getIndeterminateDrawable().mutate();
			progress.setTint(getThemeColor(button.getContext(), R.attr.colorM3OnSurface) & 0x60ffffff);
			if(progress instanceof Animatable a)
				a.start();
			LayerDrawable layerList=new LayerDrawable(new Drawable[]{progress});
			layerList.setLayerGravity(0, Gravity.CENTER);
			layerList.setLayerSize(0, V.dp(24), V.dp(24));
			layerList.setBounds(0, 0, button.getWidth(), button.getHeight());
			button.getOverlay().add(layerList);
		}else{
			button.getOverlay().clear();
			ColorStateList origColor=(ColorStateList) button.getTag(R.id.button_progress_orig_color);
			button.setTag(R.id.button_progress_orig_color, null);
			button.setTextColor(origColor);
		}
	}
}
