package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;

import androidx.annotation.NonNull;

public class BlockDomainConfirmationSheet extends AccountRestrictionConfirmationSheet{
	public BlockDomainConfirmationSheet(@NonNull Context context, Account user, ConfirmCallback confirmCallback, ConfirmCallback blockUserConfirmCallback){
		super(context, user, confirmCallback);
		titleView.setText(R.string.block_domain_confirm_title);
		confirmBtn.setText(R.string.do_block_server);
		secondaryBtn.setText(context.getString(R.string.block_user_x_instead, user.getDisplayUsername()));
		icon.setImageResource(R.drawable.ic_fluent_shield_24_regular);
		subtitleView.setText(user.getDomain());
		addRow(R.drawable.ic_campaign_24px, R.string.users_cant_see_blocked);
		addRow(R.drawable.ic_fluent_eye_off_24_regular, R.string.you_wont_see_server_posts);
		addRow(R.drawable.ic_fluent_person_delete_24_regular, R.string.server_followers_will_be_removed);
		addRow(R.drawable.ic_fluent_arrow_reply_24_regular, R.string.server_cant_mention_or_follow_you);
		addRow(R.drawable.ic_fluent_history_24_regular, R.string.server_can_interact_with_older);

		secondaryBtn.setOnClickListener(v->{
			if(loading)
				return;
			loading=true;
			secondaryBtn.setProgressBarVisible(true);
			blockUserConfirmCallback.onConfirmed(this::dismiss, ()->{
				secondaryBtn.setProgressBarVisible(false);
				loading=false;
			});
		});
	}
}
