package org.joinmastodon.android.fragments;

import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Instance;

import java.util.Optional;

public interface HasAccountID {
    String getAccountID();

    default AccountSession getSession() {
        return AccountSessionManager.getInstance().getAccount(getAccountID());
    }

    default boolean isInstanceAkkoma() {
        return getInstance().map(Instance::isAkkoma).orElse(false);
    }

    default boolean isInstancePixelfed() {
        return getInstance().map(Instance::isPixelfed).orElse(false);
    }

	default boolean isInstanceIceshrimp() {
		return getInstance().map(Instance::isIceshrimp).orElse(false);
	}

	default boolean isInstanceIceshrimpJs() {
		return getInstance().map(Instance::isIceshrimpJs).orElse(false);
	}

    default Optional<Instance> getInstance() {
        return getSession().getInstance();
    }

	default AccountLocalPreferences getLocalPrefs() {
		return AccountSessionManager.get(getAccountID()).getLocalPreferences();
	}
}
