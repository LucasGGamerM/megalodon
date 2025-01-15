package org.joinmastodon.android;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.webkit.WebView;

import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.utils.UnifiedPushHelper;

import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.utils.NetworkUtils;
import me.grishka.appkit.utils.V;

public class MastodonApp extends Application{

	@SuppressLint("StaticFieldLeak") // it's not a leak
	public static Context context;

	@Override
	public void onCreate(){
		super.onCreate();
		context=getApplicationContext();
		V.setApplicationContext(context);
		ImageCache.Parameters params=new ImageCache.Parameters();
		params.diskCacheSize=100*1024*1024;
		params.maxMemoryCacheSize=Integer.MAX_VALUE;
		ImageCache.setParams(params);
		NetworkUtils.setUserAgent("MoshidonAndroid/"+BuildConfig.VERSION_NAME);

		if (UnifiedPushHelper.isUnifiedPushEnabled(this)){
			UnifiedPushHelper.registerAllAccounts(this);
		} else {
			PushSubscriptionManager.tryRegisterFCM();
		}
		GlobalUserPreferences.load();
		if(BuildConfig.DEBUG){
			WebView.setWebContentsDebuggingEnabled(true);
		}
	}
}
