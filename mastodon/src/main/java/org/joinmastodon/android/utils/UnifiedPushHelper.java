package org.joinmastodon.android.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.unifiedpush.android.connector.UnifiedPush;

public class UnifiedPushHelper {

	/**
	 * @param context
	 * @return `true` if UnifiedPush is used
	 */
	public static boolean isUnifiedPushEnabled(@NonNull Context context) {
		return UnifiedPush.getAckDistributor(context) != null;
	}

	/**
	 * If any distributor is installed on the device
	 * @param context
	 * @return `true` if at least one is installed
	 */
	public static boolean hasAnyDistributorInstalled(@NonNull Context context) {
		return !UnifiedPush.getDistributors(context).isEmpty();
	}

	public static void registerAllAccounts(@NonNull Context context) {
		for (AccountSession accountSession : AccountSessionManager.getInstance().getLoggedInAccounts()){
			UnifiedPush.register(
					context,
					accountSession.getID(),
					null,
					null
			);
		}
	}

	public static void unregisterAllAccounts(@NonNull Context context) {
		for (AccountSession accountSession : AccountSessionManager.getInstance().getLoggedInAccounts()){
			UnifiedPush.unregister(
					context,
					accountSession.getID()
			);
			// use FCM again
			accountSession.getPushSubscriptionManager().registerAccountForPush(null);
		}
	}
}
