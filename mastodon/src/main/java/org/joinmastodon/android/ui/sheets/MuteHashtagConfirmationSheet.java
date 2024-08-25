package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Hashtag;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;


// MOSHIDON
public class MuteHashtagConfirmationSheet extends AccountRestrictionConfirmationSheet{
	public MuteHashtagConfirmationSheet(@NonNull Context context, Account user, AtomicReference<Duration> muteDuration, Hashtag hashtag, ConfirmCallback confirmCallback){
		super(context, user, confirmCallback);
		titleView.setText(R.string.mo_mute_hashtag);
		confirmBtn.setText(R.string.do_mute);
		secondaryBtn.setVisibility(View.GONE);
		icon.setImageResource(R.drawable.ic_fluent_speaker_off_24_regular);
		subtitleView.setText("#"+hashtag.name);
		addRow(R.drawable.ic_fluent_number_symbol_24_regular, R.string.mo_mute_hashtag_explanation_muted_home);
		addRow(R.drawable.ic_fluent_eye_off_24_regular, R.string.mo_mute_hashtag_explanation_discreet);
		addRow(R.drawable.ic_fluent_search_24_regular, R.string.mo_mute_hashtag_explanation_search);
		addDurationRow(context, muteDuration);
	}
}
