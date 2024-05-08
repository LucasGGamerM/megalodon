package org.joinmastodon.android.fragments.settings;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.ToNumberPolicy;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HasAccountID;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;
import org.joinmastodon.android.utils.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class SettingsAboutAppFragment extends BaseSettingsFragment<Void> implements HasAccountID{
	private static final String TAG="SettingsAboutAppFragment";
	private ListItem<Void> mediaCacheItem, copyCrashLogItem;
	private CheckableListItem<Void> enablePreReleasesItem;
	private AccountSession session;
	private boolean timelineCacheCleared=false;
	private File crashLogFile=new File(MastodonApp.context.getFilesDir(), "crash.log");

	// MOSHIDON
	private ListItem<Void> clearRecentEmojisItem, exportItem;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.about_app, getString(R.string.mo_app_name)));
		session=AccountSessionManager.get(accountID);

		String lastModified=crashLogFile.exists()
				? DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(crashLogFile.lastModified()))
				: getString(R.string.sk_settings_crash_log_unavailable);
		List<ListItem<Void>> items=new ArrayList<>(List.of(
				new ListItem<>(R.string.sk_settings_donate, 0, R.drawable.ic_fluent_heart_24_regular, i->UiUtils.launchWebBrowser(getActivity(), getString(R.string.mo_donate_url))),
				new ListItem<>(R.string.mo_settings_contribute, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), getString(R.string.mo_repo_url))),
				new ListItem<>(R.string.settings_tos, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/terms")),
				new ListItem<>(R.string.settings_privacy_policy, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), getString(R.string.privacy_policy_url)), 0, true),
				exportItem=new ListItem<>(R.string.export_settings_title, R.string.export_settings_summary, R.drawable.ic_fluent_arrow_export_24_filled, this::onExportClick),
				clearRecentEmojisItem=new ListItem<>(R.string.mo_clear_recent_emoji, 0, this::onClearRecentEmojisClick),
				mediaCacheItem=new ListItem<>(R.string.settings_clear_cache, 0, this::onClearMediaCacheClick),
				new ListItem<>(getString(R.string.sk_settings_clear_timeline_cache), session.domain, this::onClearTimelineCacheClick),
				copyCrashLogItem=new ListItem<>(getString(R.string.sk_settings_copy_crash_log), lastModified, 0, this::onCopyCrashLog)
		));

		if(GithubSelfUpdater.needSelfUpdating()){
			items.add(enablePreReleasesItem=new CheckableListItem<>(R.string.sk_updater_enable_pre_releases, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.enablePreReleases, i->toggleCheckableItem(enablePreReleasesItem)));
		}

		copyCrashLogItem.isEnabled=crashLogFile.exists();
		onDataLoaded(items);
		updateMediaCacheItem();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		GlobalUserPreferences.enablePreReleases=enablePreReleasesItem!=null && enablePreReleasesItem.checked;
		GlobalUserPreferences.save();
		if(timelineCacheCleared) getActivity().recreate();
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(super.getAdapter());

		TextView versionInfo=new TextView(getActivity());
		versionInfo.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(32)));
		versionInfo.setTextAppearance(R.style.m3_label_medium);
		versionInfo.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
		versionInfo.setGravity(Gravity.CENTER);
		versionInfo.setText(getString(R.string.mo_settings_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
		versionInfo.setOnClickListener(v->{
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("", BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")"));
			if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.S_V2){
				new Snackbar.Builder(getActivity())
						.setText(R.string.app_version_copied)
						.show();
			}
		});
		adapter.addAdapter(new SingleViewRecyclerAdapter(versionInfo));

		return adapter;
	}

	private void onClearMediaCacheClick(ListItem<?> item){
		MastodonAPIController.runInBackground(()->{
			Activity activity=getActivity();
			ImageCache.getInstance(getActivity()).clear();
			activity.runOnUiThread(()->{
				Toast.makeText(activity, R.string.media_cache_cleared, Toast.LENGTH_SHORT).show();
				updateMediaCacheItem();
			});
		});
	}

	private void onClearTimelineCacheClick(ListItem<?> item){
		session.getCacheController().putHomeTimeline(List.of(), true);
		Toast.makeText(getContext(), R.string.sk_timeline_cache_cleared, Toast.LENGTH_SHORT).show();
		timelineCacheCleared=true;
	}

	private void onClearRecentEmojisClick(ListItem<?> item){
		getLocalPrefs().recentCustomEmoji=new ArrayList<>();
		getLocalPrefs().save();
		Toast.makeText(getContext(), R.string.mo_recent_emoji_cleared, Toast.LENGTH_SHORT).show();
	}

	private void onExportClick(ListItem<?> item){
		Gson gson = new Gson();
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("versionName", BuildConfig.VERSION_NAME);
		jsonObject.addProperty("versionCode", BuildConfig.VERSION_CODE);

		// GlobalUserPreferences
		//TODO: remove prefs that should not be exported
		JsonElement je = gson.toJsonTree(GlobalUserPreferences.getPrefs().getAll());
		jsonObject.add("GlobalUserPreferences", je);

		// add account local prefs
		for(AccountSession accountSession: AccountSessionManager.getInstance().getLoggedInAccounts()) {
            Map<String, ?> prefs = accountSession.getRawLocalPreferences().getAll();
			//TODO: remove prefs that should not be exported
			JsonElement accountPrefs = gson.toJsonTree(prefs);
			jsonObject.add(accountSession.self.id, accountPrefs);
		}

		try {
			File file = new File(getContext().getCacheDir(), "moshidon-exported-settings.json");
			FileWriter writer = new FileWriter(file);
			writer.write(jsonObject.toString());
			writer.flush();
			writer.close();

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("application/json");
			Uri outputUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
			intent.putExtra(Intent.EXTRA_STREAM, outputUri);
			startActivity(Intent.createChooser(intent, getContext().getString(R.string.export_settings_share)));
		} catch (IOException e) {
			Toast.makeText(getContext(), getContext().getString(R.string.export_settings_fail), Toast.LENGTH_SHORT).show();
			Log.w(TAG, e);
		}
	}

	private void updateMediaCacheItem(){
		long size=ImageCache.getInstance(getActivity()).getDiskCache().size();
		mediaCacheItem.subtitle=UiUtils.formatFileSize(getActivity(), size, false);
		mediaCacheItem.isEnabled=size>0;
		rebindItem(mediaCacheItem);
	}

	@Override
	public String getAccountID(){
		return accountID;
	}

	private void onCopyCrashLog(ListItem<?> item){
		if(!crashLogFile.exists()) return;
		try(InputStream is=new FileInputStream(crashLogFile)){
			BufferedReader reader=new BufferedReader(new InputStreamReader(is));
			StringBuilder sb=new StringBuilder();
			String line;
			while ((line=reader.readLine())!=null) sb.append(line).append("\n");
			UiUtils.copyText(list, sb.toString());
		} catch(IOException e){
			Log.e(TAG, "Error reading crash log", e);
		}
	}
}
