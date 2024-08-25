package org.joinmastodon.android.ui.sheets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.drawables.EmptyDrawable;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.AutoOrientationLinearLayout;
import org.joinmastodon.android.ui.views.ProgressBarButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public abstract class AccountRestrictionConfirmationSheet extends BottomSheet{
	private LinearLayout contentWrap;
	protected Button cancelBtn;
	protected ProgressBarButton confirmBtn, secondaryBtn;
	protected TextView titleView, subtitleView;
	protected ImageView icon;
	protected boolean loading;

	public AccountRestrictionConfirmationSheet(@NonNull Context context, Account user, ConfirmCallback confirmCallback){
		super(context);
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_restrict_account, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		contentWrap=findViewById(R.id.content_wrap);
		titleView=findViewById(R.id.title);
		subtitleView=findViewById(R.id.text);
		cancelBtn=findViewById(R.id.btn_cancel);
		confirmBtn=findViewById(R.id.btn_confirm);
		secondaryBtn=findViewById(R.id.btn_secondary);
		icon=findViewById(R.id.icon);

		contentWrap.setDividerDrawable(new EmptyDrawable(1, V.dp(8)));
		contentWrap.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		confirmBtn.setOnClickListener(v->{
			if(loading)
				return;
			loading=true;
			confirmBtn.setProgressBarVisible(true);
			confirmCallback.onConfirmed(this::dismiss, ()->{
				confirmBtn.setProgressBarVisible(false);
				loading=false;
			});
		});
		cancelBtn.setOnClickListener(v->{
			if(!loading)
				dismiss();
		});
	}

	protected void addRow(@DrawableRes int icon, CharSequence text, View view) {
		TextView tv=new TextView(getContext());
		tv.setTextAppearance(R.style.m3_body_large);
		tv.setTextColor(UiUtils.getThemeColor(getContext(), R.attr.colorM3OnSurfaceVariant));
		tv.setCompoundDrawableTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getContext(), R.attr.colorM3Primary)));
		tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		tv.setText(text);
		InsetDrawable drawable=new InsetDrawable(getContext().getResources().getDrawable(icon, getContext().getTheme()), V.dp(8));
		drawable.setBounds(0, 0, V.dp(40), V.dp(40));
		tv.setCompoundDrawablesRelative(drawable, null, null, null);
		tv.setCompoundDrawablePadding(V.dp(16));

		if(view==null){
			contentWrap.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			return;
		}

		AutoOrientationLinearLayout layout = new AutoOrientationLinearLayout(getContext());
		// allow complete row to trigger child click listener
		if(view.hasOnClickListeners())
			layout.setOnClickListener(v -> view.performClick());
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity=Gravity.CENTER;
		lp.weight=1f;
		layout.addView(tv, lp);
		layout.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		contentWrap.addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	protected void addRow(@DrawableRes int icon, @StringRes int text, View view){
		addRow(icon, getContext().getString(text), view);
	}

	protected void addRow(@DrawableRes int icon, CharSequence text){
		addRow(icon, text, null);
	}

	protected void addRow(@DrawableRes int icon, @StringRes int text){
		addRow(icon, getContext().getString(text));
	}

	public void addDurationRow(@NonNull Context context, AtomicReference<Duration> muteDuration) {
		//Moshidon: add row to choose a duration, e.g. for muting accounts
		Button muteDurationBtn=new Button(getContext());
		muteDurationBtn.setOnClickListener(v->getMuteDurationDialog(context, muteDuration, muteDurationBtn).show());
		muteDurationBtn.setText(R.string.sk_duration_indefinite);
		addRow(R.drawable.ic_fluent_clock_20_regular, R.string.sk_mute_label, muteDurationBtn);
	}

	@NonNull
	private M3AlertDialogBuilder getMuteDurationDialog(@NonNull Context context, AtomicReference<Duration> muteDuration, Button button){
		M3AlertDialogBuilder builder=new M3AlertDialogBuilder(context);
		builder.setTitle(R.string.sk_mute_label);
		builder.setIcon(R.drawable.ic_fluent_clock_20_regular);
		List<Duration> durations =List.of(Duration.ZERO,
				Duration.ofMinutes(5),
				Duration.ofMinutes(30),
				Duration.ofHours(1),
				Duration.ofHours(6),
				Duration.ofDays(1),
				Duration.ofDays(3),
				Duration.ofDays(7),
				Duration.ofDays(7));

		String[] choices = {context.getString(R.string.sk_duration_indefinite),
				context.getString(R.string.sk_duration_minutes_5),
				context.getString(R.string.sk_duration_minutes_30),
				context.getString(R.string.sk_duration_hours_1),
				context.getString(R.string.sk_duration_hours_6),
				context.getString(R.string.sk_duration_days_1),
				context.getString(R.string.sk_duration_days_3),
				context.getString(R.string.sk_duration_days_7)};

		builder.setSingleChoiceItems(choices, durations.indexOf(muteDuration.get()), (dialog, which) -> {});

		builder.setPositiveButton(R.string.ok, (dialog, which)->{
			int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
			muteDuration.set(durations.get(selected));
			button.setText(choices[selected]);
		});
		builder.setNegativeButton(R.string.cancel, null);

		return builder;
	}


	public interface ConfirmCallback{
		void onConfirmed(Runnable onSuccess, Runnable onError);
	}
}
