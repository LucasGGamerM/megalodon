package org.joinmastodon.android.fragments;

import static org.joinmastodon.android.GlobalUserPreferences.PrefixRepliesMode.ALWAYS;
import static org.joinmastodon.android.GlobalUserPreferences.PrefixRepliesMode.TO_OTHERS;
import static org.joinmastodon.android.api.requests.statuses.CreateStatus.DRAFTS_AFTER_INSTANT;
import static org.joinmastodon.android.api.requests.statuses.CreateStatus.getDraftInstant;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.icu.text.BreakIterator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.bottomSoftwareFoundation.bottom.Bottom;
import com.squareup.otto.Subscribe;
import com.twitter.twittertext.TwitterTextEmojiRegex;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.TweakedFileProvider;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.DeleteStatus;
import org.joinmastodon.android.api.requests.statuses.EditStatus;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.TakePictureRequestEvent;
import org.joinmastodon.android.events.ScheduledStatusCreatedEvent;
import org.joinmastodon.android.events.ScheduledStatusDeletedEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.fragments.account_list.AccountSearchFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.ContentType;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.CustomEmojiPopupKeyboard;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.PopupKeyboard;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.ui.text.ComposeAutocompleteSpan;
import org.joinmastodon.android.ui.text.ComposeHashtagOrMentionSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.utils.Tracking;
import org.joinmastodon.android.utils.TransferSpeedTracker;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewcontrollers.ComposeAutocompleteViewController;
import org.joinmastodon.android.ui.viewcontrollers.ComposeLanguageAlertViewController;
import org.joinmastodon.android.ui.viewcontrollers.ComposeMediaViewController;
import org.joinmastodon.android.ui.viewcontrollers.ComposePollViewController;
import org.joinmastodon.android.ui.views.ComposeEditText;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.joinmastodon.android.ui.views.SizeListenerLinearLayout;
import org.joinmastodon.android.utils.MastodonLanguage;
import org.joinmastodon.android.utils.StatusTextEncoder;
import org.parceler.Parcels;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.CustomTransitionsFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ComposeFragment extends MastodonToolbarFragment implements OnBackPressedListener, ComposeEditText.SelectionListener, HasAccountID, CustomTransitionsFragment {

	private static final int MEDIA_RESULT=717;
	public static final int IMAGE_DESCRIPTION_RESULT=363;
	private static final int AUTOCOMPLETE_ACCOUNT_RESULT=779;
	private static final int SCHEDULED_STATUS_OPENED_RESULT=161;

	private static final String GLITCH_LOCAL_ONLY_SUFFIX = "👁";
	private static final Pattern GLITCH_LOCAL_ONLY_PATTERN = Pattern.compile("[\\s\\S]*" + GLITCH_LOCAL_ONLY_SUFFIX + "[\uFE00-\uFE0F]*");

	private static final String TAG="ComposeFragment";
	public static final int CAMERA_PERMISSION_CODE = 626938;
	public static final int CAMERA_PIC_REQUEST_CODE = 6242069;

	private static final Pattern MENTION_PATTERN=Pattern.compile("(^|[^\\/\\w])@(([a-z0-9_]+)@[a-z0-9\\.\\-]+[a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	// from https://github.com/mastodon/mastodon-ios/blob/main/Mastodon/Helper/MastodonRegex.swift
	private static final Pattern AUTO_COMPLETE_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-z0-9_]+)(@[a-z0-9_\\.\\-]*)?|#([^\\s.]+)|:([a-z0-9_]+))", Pattern.CASE_INSENSITIVE);
	private static final Pattern HIGHLIGHT_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+))");

	@SuppressLint("NewApi") // this class actually exists on 6.0
	private final BreakIterator breakIterator=BreakIterator.getCharacterInstance();

	public LinearLayout mainLayout;
	private SizeListenerLinearLayout contentView;
	private TextView selfName, selfUsername, selfExtraText, extraText;
	private ImageView selfAvatar;
	private Account self;
	private String instanceDomain;

	private ComposeEditText mainEditText;
	private TextView charCounter;
	private String accountID;
	private int charCount, charLimit, trimmedCharCount;

	private Button publishButton, languageButton, scheduleTimeBtn;
	private PopupMenu contentTypePopup, visibilityPopup, draftOptionsPopup;
	private ImageButton publishButtonRelocated, mediaBtn, pollBtn, emojiBtn, spoilerBtn, draftsBtn, scheduleDraftDismiss, contentTypeBtn;
	private View sensitiveBtn;
	private TextView replyText;
	private LinearLayout scheduleDraftView;
	private ScrollView scrollView;
	private boolean initiallyScrolled = false;
	private TextView scheduleDraftText;
	private Button visibilityBtn;
	private LinearLayout bottomBar;
	private View autocompleteDivider;

	private List<EmojiCategory> customEmojis;
	private CustomEmojiPopupKeyboard emojiKeyboard;
	private Status replyTo;
	private Status quote;
	private String initialText;
	private String uuid;
	private EditText spoilerEdit;
	private View spoilerWrap;
	private boolean hasSpoiler;
	private boolean sensitive;
	private Instant scheduledAt = null;
	private ProgressBar sendProgress;
	private View sendingOverlay;
	private WindowManager wm;
	private StatusPrivacy statusVisibility=StatusPrivacy.PUBLIC;
	private boolean localOnly;
	private ComposeAutocompleteSpan currentAutocompleteSpan;
	private FrameLayout mainEditTextWrap;

	private ComposeLanguageAlertViewController.SelectedOption postLang;

	private ComposeAutocompleteViewController autocompleteViewController;
	private ComposePollViewController pollViewController=new ComposePollViewController(this);
	private ComposeMediaViewController mediaViewController=new ComposeMediaViewController(this);
	public Instance instance;

	public Status editingStatus;
	public ScheduledStatus scheduledStatus;
	private boolean redraftStatus;

	private Uri photoUri;

	private ContentType contentType;
	private MastodonLanguage.LanguageResolver languageResolver;

	private boolean creatingView;
	private boolean ignoreSelectionChanges=false;
	private MenuItem actionItem;
	private MenuItem draftMenuItem, undraftMenuItem, scheduleMenuItem, unscheduleMenuItem;
	private boolean wasDetached;

	private BackgroundColorSpan overLimitBG;
	private ForegroundColorSpan overLimitFG;

	public ComposeFragment(){
		super(R.layout.toolbar_fragment_with_progressbar);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(this);
		setRetainInstance(true);

		accountID=getArguments().getString("account");
		AccountSession session=AccountSessionManager.get(accountID);

		self=session.self;
		instanceDomain=session.domain;
		customEmojis=AccountSessionManager.getInstance().getCustomEmojis(instanceDomain);
		instance=AccountSessionManager.getInstance().getInstanceInfo(instanceDomain);
		languageResolver=new MastodonLanguage.LanguageResolver(instance);
		redraftStatus=getArguments().getBoolean("redraftStatus", false);
		contentType=session.getLocalPreferences().defaultContentType;
		if(getArguments().containsKey("editStatus")){
			editingStatus=Parcels.unwrap(getArguments().getParcelable("editStatus"));
		}
		if(getArguments().containsKey("replyTo")) {
			replyTo=Parcels.unwrap(getArguments().getParcelable("replyTo"));
		}
		if(getArguments().containsKey("quote")) {
			quote=Parcels.unwrap(getArguments().getParcelable("quote"));
		}
		if(instance==null){
			Nav.finish(this);
			return;
		}
		if(customEmojis.isEmpty()){
			AccountSessionManager.getInstance().updateInstanceInfo(instanceDomain);
		}

		Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
		if (bundle.containsKey("scheduledStatus")) scheduledStatus=Parcels.unwrap(bundle.getParcelable("scheduledStatus"));
		if (bundle.containsKey("scheduledAt")) scheduledAt=(Instant) bundle.getSerializable("scheduledAt");

		if(instance.maxTootChars>0)
			charLimit=instance.maxTootChars;
		else if(instance.configuration!=null && instance.configuration.statuses!=null && instance.configuration.statuses.maxCharacters>0)
			charLimit=instance.configuration.statuses.maxCharacters;
		else
			charLimit=500;

//		setTitle(editingStatus==null ? R.string.new_post : R.string.edit_post);
		if(savedInstanceState!=null)
			postLang=Parcels.unwrap(savedInstanceState.getParcelable("postLang"));
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
		mediaViewController.cancelAllUploads();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		wm=activity.getSystemService(WindowManager.class);

		overLimitBG=new BackgroundColorSpan(UiUtils.getThemeColor(activity, R.attr.colorM3ErrorContainer));
		overLimitFG=new ForegroundColorSpan(UiUtils.getThemeColor(activity, R.attr.colorM3Error));
	}

	@Override
	public void onDetach(){
		wasDetached=true;
		super.onDetach();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		creatingView=true;
		emojiKeyboard=new CustomEmojiPopupKeyboard(getActivity(), accountID, customEmojis, instanceDomain);
		emojiKeyboard.setListener(new CustomEmojiPopupKeyboard.Listener(){
			@Override
			public void onEmojiSelected(Emoji emoji){
				onCustomEmojiClick(emoji);
			}

			@Override
			public void onEmojiSelected(String emoji){
				if(getActivity().getCurrentFocus() instanceof EditText edit && edit == mainEditText){
					edit.getText().replace(edit.getSelectionStart(), edit.getSelectionEnd(), emoji);
				}
			}

			@Override
			public void onBackspace(){
				getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
			}
		});

		View view=inflater.inflate(R.layout.fragment_compose, container, false);

		if(GlobalUserPreferences.relocatePublishButton){
			publishButtonRelocated=view.findViewById(R.id.publish);
//			publishButton.setText(editingStatus==null || redraftStatus ? R.string.publish : R.string.save);
//			publishButton.setEllipsize(TextUtils.TruncateAt.END);
			publishButtonRelocated.setOnClickListener(v -> {
				if(GlobalUserPreferences.altTextReminders && editingStatus==null)
					checkAltTextsAndPublish();
				else
					publish();
			});
			publishButtonRelocated.setVisibility(View.VISIBLE);

			draftsBtn=view.findViewById(R.id.drafts_btn);
			draftsBtn.setVisibility(View.VISIBLE);
		} else {
			charCounter=view.findViewById(R.id.char_counter);
			charCounter.setVisibility(View.VISIBLE);
			charCounter.setText(String.valueOf(charLimit));
		}

		mainLayout=view.findViewById(R.id.compose_main_ll);
		mainEditText=view.findViewById(R.id.toot_text);
		mainEditTextWrap=view.findViewById(R.id.toot_text_wrap);
		scrollView=view.findViewById(R.id.scroll_view);

		selfName=view.findViewById(R.id.self_name);
		selfUsername=view.findViewById(R.id.self_username);
		selfAvatar=view.findViewById(R.id.self_avatar);
		selfExtraText=view.findViewById(R.id.self_extra_text);
		HtmlParser.setTextWithCustomEmoji(selfName, self.getDisplayName(), self.emojis);
		selfUsername.setText('@'+self.username+'@'+instanceDomain);
		if(self.avatar!=null)
			ViewImageLoader.load(selfAvatar, null, new UrlImageLoaderRequest(self.avatar));
		ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		};
		selfAvatar.setOutlineProvider(roundCornersOutline);
		selfAvatar.setClipToOutline(true);
		bottomBar=view.findViewById(R.id.bottom_bar);

		mediaBtn=view.findViewById(R.id.btn_media);
		pollBtn=view.findViewById(R.id.btn_poll);
		emojiBtn=view.findViewById(R.id.btn_emoji);
		spoilerBtn=view.findViewById(R.id.btn_spoiler);
		visibilityBtn=view.findViewById(R.id.btn_visibility);
		contentTypeBtn=view.findViewById(R.id.btn_content_type);
		scheduleDraftView=view.findViewById(R.id.schedule_draft_view);
		scheduleDraftText=view.findViewById(R.id.schedule_draft_text);
		scheduleDraftDismiss=view.findViewById(R.id.schedule_draft_dismiss);
		scheduleTimeBtn=view.findViewById(R.id.scheduled_time_btn);
		sensitiveBtn=view.findViewById(R.id.sensitive_item);
		replyText=view.findViewById(R.id.reply_text);

		PopupMenu attachPopup = new PopupMenu(getContext(), mediaBtn);
		attachPopup.inflate(R.menu.attach);
		if(UiUtils.isPhotoPickerAvailable())
			attachPopup.getMenu().findItem(R.id.media).setVisible(true);

		attachPopup.setOnMenuItemClickListener(i -> {
			if (i.getItemId() == R.id.camera){
				try {
					openCamera();
				} catch (IOException e){
					Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT);
				}

			} else {
				openFilePicker(i.getItemId() == R.id.media);
			}
			return true;
		});
		UiUtils.enablePopupMenuIcons(getContext(), attachPopup);
		mediaBtn.setOnClickListener(v->attachPopup.show());
		mediaBtn.setOnTouchListener(attachPopup.getDragToOpenListener());
		if (isInstancePixelfed()) pollBtn.setVisibility(View.GONE);
		pollBtn.setOnClickListener(v->togglePoll());
		emojiBtn.setOnClickListener(v->emojiKeyboard.toggleKeyboardPopup(mainEditText));
		spoilerBtn.setOnClickListener(v->toggleSpoiler());
		Drawable arrow=getResources().getDrawable(R.drawable.ic_baseline_arrow_drop_down_18, getActivity().getTheme()).mutate();
		arrow.setTint(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		visibilityBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null);

		localOnly = savedInstanceState != null ? savedInstanceState.getBoolean("localOnly") :
				editingStatus != null ? editingStatus.localOnly : replyTo != null && replyTo.localOnly;

		buildVisibilityPopup(visibilityBtn);
		visibilityBtn.setOnClickListener(v->visibilityPopup.show());
		visibilityBtn.setOnTouchListener(visibilityPopup.getDragToOpenListener());

		buildContentTypePopup(contentTypeBtn);
		contentTypeBtn.setOnClickListener(v->contentTypePopup.show());
		contentTypeBtn.setOnTouchListener(contentTypePopup.getDragToOpenListener());

		scheduleDraftDismiss.setOnClickListener(v->updateScheduledAt(null));
		scheduleTimeBtn.setOnClickListener(v->pickScheduledDateTime());

		sensitiveBtn.setOnClickListener(v->toggleSensitive());
		emojiKeyboard.setOnIconChangedListener(new PopupKeyboard.OnIconChangeListener(){
			@Override
			public void onIconChanged(int icon){
				emojiBtn.setSelected(icon!=PopupKeyboard.ICON_HIDDEN);
				updateNavigationBarColor(icon!=PopupKeyboard.ICON_HIDDEN);
				if(autocompleteViewController.getMode()==ComposeAutocompleteViewController.Mode.EMOJIS){
					contentView.layout(contentView.getLeft(), contentView.getTop(), contentView.getRight(), contentView.getBottom());
					if(icon==PopupKeyboard.ICON_HIDDEN)
						showAutocomplete();
					else
						hideAutocomplete();
				}
			}
		});

		contentView=(SizeListenerLinearLayout) view;
		contentView.addView(emojiKeyboard.getView());

		spoilerEdit=view.findViewById(R.id.content_warning);
		spoilerWrap=view.findViewById(R.id.content_warning_wrap);
		LayerDrawable spoilerBg=(LayerDrawable) spoilerWrap.getBackground().mutate();
		spoilerBg.setDrawableByLayerId(R.id.left_drawable, new SpoilerStripesDrawable(false));
		spoilerBg.setDrawableByLayerId(R.id.right_drawable, new SpoilerStripesDrawable(false));
		spoilerWrap.setBackground(spoilerBg);
		spoilerWrap.setClipToOutline(true);
		spoilerWrap.setOutlineProvider(OutlineProviders.roundedRect(8));
		if((savedInstanceState!=null && savedInstanceState.getBoolean("hasSpoiler", false)) || hasSpoiler){
			hasSpoiler=true;
			spoilerWrap.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
		}else if(editingStatus!=null && editingStatus.hasSpoiler()){
			hasSpoiler=true;
			spoilerWrap.setVisibility(View.VISIBLE);
			spoilerEdit.setText(getArguments().getString("sourceSpoiler", editingStatus.spoilerText));
			spoilerBtn.setSelected(true);
		}

		sensitive = savedInstanceState==null && editingStatus != null ? editingStatus.sensitive
				: savedInstanceState!=null && savedInstanceState.getBoolean("sensitive", false);
		if (sensitive) {
			sensitiveBtn.setVisibility(View.VISIBLE);
			sensitiveBtn.setSelected(true);
		}

		if (savedInstanceState != null) {
			statusVisibility = (StatusPrivacy) savedInstanceState.getSerializable("visibility");
		} else if (editingStatus != null && editingStatus.visibility != null) {
			statusVisibility = editingStatus.visibility;
		} else {
			loadDefaultStatusVisibility(savedInstanceState);
		}

		updateVisibilityIcon();
		visibilityPopup.getMenu().findItem(switch(statusVisibility){
			case PUBLIC -> R.id.vis_public;
			case UNLISTED -> R.id.vis_unlisted;
			case PRIVATE -> R.id.vis_followers;
			case DIRECT -> R.id.vis_private;
			case LOCAL -> R.id.vis_local;
		}).setChecked(true);
		visibilityPopup.getMenu().findItem(R.id.local_only).setChecked(localOnly);

		if (savedInstanceState != null && savedInstanceState.containsKey("contentType")) {
			contentType = (ContentType) savedInstanceState.getSerializable("contentType");
		} else if (getArguments().containsKey("sourceContentType")) {
			try {
				String val = getArguments().getString("sourceContentType");
				if (val != null) contentType = ContentType.valueOf(val);
			} catch (IllegalArgumentException ignored) {}
		}

		int typeIndex=contentType.ordinal();
		if (contentTypePopup.getMenu().findItem(typeIndex) != null)
			contentTypePopup.getMenu().findItem(typeIndex).setChecked(true);
		contentTypeBtn.setSelected(typeIndex != ContentType.UNSPECIFIED.ordinal() && typeIndex != ContentType.PLAIN.ordinal());

		autocompleteViewController=new ComposeAutocompleteViewController(getActivity(), accountID);
		autocompleteViewController.setCompletionSelectedListener(new ComposeAutocompleteViewController.AutocompleteListener(){
			@Override
			public void onCompletionSelected(String completion){
				onAutocompleteOptionSelected(completion);
			}

			@Override
			public void onSetEmojiPanelOpen(boolean open){
				if(open!=emojiKeyboard.isVisible())
					emojiKeyboard.toggleKeyboardPopup(mainEditText);
			}

			@Override
			public void onLaunchAccountSearch(){
				Bundle args=new Bundle();
				args.putString("account", accountID);
				Nav.goForResult(getActivity(), AccountSearchFragment.class, args, AUTOCOMPLETE_ACCOUNT_RESULT, ComposeFragment.this);
			}
		});
		View autocompleteView=autocompleteViewController.getView();
		autocompleteView.setVisibility(View.INVISIBLE);
		bottomBar.addView(autocompleteView, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(56)));
		autocompleteDivider=view.findViewById(R.id.bottom_bar_autocomplete_divider);

		pollViewController.setView(view, savedInstanceState);
		mediaViewController.setView(view, savedInstanceState);

		creatingView=false;

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		pollViewController.onSaveInstanceState(outState);
		mediaViewController.onSaveInstanceState(outState);
		outState.putBoolean("hasSpoiler", hasSpoiler);
		outState.putSerializable("visibility", statusVisibility);
		outState.putParcelable("postLang", Parcels.wrap(postLang));
		if(currentAutocompleteSpan!=null){
			Editable e=mainEditText.getText();
			outState.putInt("autocompleteStart", e.getSpanStart(currentAutocompleteSpan));
			outState.putInt("autocompleteEnd", e.getSpanEnd(currentAutocompleteSpan));
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == CAMERA_PERMISSION_CODE && (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST_CODE);
		} else {
			Toast.makeText(getContext(), R.string.permission_required, Toast.LENGTH_SHORT);
		}
	}

	@Override
	public void onResume(){
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		contentView.setSizeListener(emojiKeyboard::onContentViewSizeChanged);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		mainEditText.requestFocus();
		 view.postDelayed(()->{
		 	imm.showSoftInput(mainEditText, 0);
		 }, 100);
		sendProgress=view.findViewById(R.id.progress);
		sendProgress.setVisibility(View.GONE);

		mainEditText.setSelectionListener(this);
		mainEditText.addTextChangedListener(new TextWatcher(){
			private int lastChangeStart, lastChangeCount;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				if(s.length()==0)
					return;
				lastChangeStart=start;
				lastChangeCount=count;
			}

			@Override
			public void afterTextChanged(Editable s){
				if(s.length()==0){
					updateCharCounter();
					return;
				}
				int start=lastChangeStart;
				int count=lastChangeCount;
				// offset one char back to catch an already typed '@' or '#' or ':'
				int realStart=start;
				start=Math.max(0, start-1);
				CharSequence changedText=s.subSequence(start, realStart+count);
				String raw=changedText.toString();
				Editable editable=(Editable) s;
				// 1. find mentions, hashtags, and emoji shortcodes in any freshly inserted text, and put spans over them
				if(raw.contains("@") || raw.contains("#") || raw.contains(":")){
					Matcher matcher=AUTO_COMPLETE_PATTERN.matcher(changedText);
					while(matcher.find()){
						if(editable.getSpans(start+matcher.start(), start+matcher.end(), ComposeAutocompleteSpan.class).length>0)
							continue;
						ComposeAutocompleteSpan span;
						if(TextUtils.isEmpty(matcher.group(4))){ // not an emoji
							span=new ComposeHashtagOrMentionSpan();
						}else{
							span=new ComposeAutocompleteSpan();
						}
						editable.setSpan(span, start+matcher.start(), start+matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
					}
				}
				// 2. go over existing spans in the affected range, adjust end offsets and remove no longer valid spans
				ComposeAutocompleteSpan[] spans=editable.getSpans(realStart, realStart+count, ComposeAutocompleteSpan.class);
				for(ComposeAutocompleteSpan span:spans){
					int spanStart=editable.getSpanStart(span);
					int spanEnd=editable.getSpanEnd(span);
					if(spanStart==spanEnd){ // empty, remove
						editable.removeSpan(span);
						continue;
					}
					char firstChar=editable.charAt(spanStart);
					String spanText=s.subSequence(spanStart, spanEnd).toString();
					if(firstChar=='@' || firstChar=='#' || firstChar==':'){
						Matcher matcher=AUTO_COMPLETE_PATTERN.matcher(spanText);
						char prevChar=spanStart>0 ? editable.charAt(spanStart-1) : ' ';
						if(!matcher.find() || !Character.isWhitespace(prevChar)){ // invalid mention, remove
							editable.removeSpan(span);
						}else if(matcher.end()+spanStart<spanEnd){ // mention with something at the end, move the end offset
							editable.setSpan(span, spanStart, spanStart+matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
						}
					}else{
						editable.removeSpan(span);
					}
				}

				updateCharCounter();
			}
		});
		spoilerEdit.addTextChangedListener(new SimpleTextWatcher(e->updateCharCounter()));
		if(replyTo!=null || quote!=null){
			Status status = quote!=null ? quote : replyTo;
			View replyWrap = view.findViewById(R.id.reply_wrap);
			scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
				int scrollHeight = scrollView.getHeight();
				if (replyWrap.getMinimumHeight() != scrollHeight) {
					replyWrap.setMinimumHeight(scrollHeight);
					if (!initiallyScrolled) {
						initiallyScrolled = true;
						scrollView.post(() -> {
							int bottom = scrollView.getChildAt(0).getBottom();
							int delta = bottom - (scrollView.getScrollY() + scrollView.getHeight());
							int space = GlobalUserPreferences.reduceMotion ? 0 : Math.min(V.dp(70), delta);
							scrollView.scrollBy(0, delta - space);
							if (!GlobalUserPreferences.reduceMotion) {
								scrollView.postDelayed(() -> scrollView.smoothScrollBy(0, space), 130);
							}
						});
					}
				}
			});
			View originalPost=view.findViewById(R.id.original_post);
			extraText=view.findViewById(R.id.extra_text);
			originalPost.setVisibility(View.VISIBLE);
			originalPost.setOnClickListener(v->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("status", Parcels.wrap(status));
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				Nav.go(getActivity(), ThreadFragment.class, args);
			});

			ImageView avatar = view.findViewById(R.id.avatar);
			ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(status.account.avatar));
			ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
				@Override
				public void getOutline(View view, Outline outline){
					outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
				}
			};
			avatar.setOutlineProvider(roundCornersOutline);
			avatar.setClipToOutline(true);
			avatar.setOnClickListener(v->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("profileAccount", Parcels.wrap(status.account));
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				Nav.go(getActivity(), ProfileFragment.class, args);
			});

			Drawable visibilityIcon = getActivity().getDrawable(switch(status.visibility){
				case PUBLIC -> R.drawable.ic_fluent_earth_20_regular;
				case UNLISTED -> R.drawable.ic_fluent_lock_open_20_regular;
				case PRIVATE -> R.drawable.ic_fluent_lock_closed_20_filled;
				case DIRECT -> R.drawable.ic_fluent_mention_20_regular;
				case LOCAL -> R.drawable.ic_fluent_eye_20_regular;
			});
			ImageView moreBtn = view.findViewById(R.id.more);
			moreBtn.setImageDrawable(visibilityIcon);
			moreBtn.setBackground(null);

			TextView name = view.findViewById(R.id.name);
			name.setText(HtmlParser.parseCustomEmoji(status.account.getDisplayName(), status.account.emojis));
			UiUtils.loadCustomEmojiInTextView(name);

			String time = status==null || status.editedAt==null
					? UiUtils.formatRelativeTimestamp(getContext(), status.createdAt)
					: getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(getContext(), status.editedAt));

			((TextView) view.findViewById(R.id.username)).setText(status.account.getDisplayUsername());
			view.findViewById(R.id.separator).setVisibility(time==null ? View.GONE : View.VISIBLE);
			view.findViewById(R.id.time).setVisibility(time==null ? View.GONE : View.VISIBLE);
			if(time!=null) ((TextView) view.findViewById(R.id.time)).setText(time);

			if (status.hasSpoiler()) {
				TextView replyToSpoiler = view.findViewById(R.id.reply_to_spoiler);
				replyToSpoiler.setVisibility(View.VISIBLE);
				replyToSpoiler.setText(status.spoilerText);
				LayerDrawable spoilerBg=(LayerDrawable) replyToSpoiler.getBackground().mutate();
				spoilerBg.setDrawableByLayerId(R.id.left_drawable, new SpoilerStripesDrawable(false));
				spoilerBg.setDrawableByLayerId(R.id.right_drawable, new SpoilerStripesDrawable(false));
				replyToSpoiler.setBackground(spoilerBg);
				replyToSpoiler.setClipToOutline(true);
				replyToSpoiler.setOutlineProvider(OutlineProviders.roundedRect(8));
			}

			SpannableStringBuilder content = HtmlParser.parse(status.content, status.emojis, status.mentions, status.tags, accountID);
			LinkedTextView text = view.findViewById(R.id.text);
			if (content.length() > 0) {
				text.setText(content);
				UiUtils.loadCustomEmojiInTextView(text);
			} else {
				view.findViewById(R.id.display_item_text)
						.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(16)));
			}

			replyText.setText(HtmlParser.parseCustomEmoji(getString(quote!=null? R.string.sk_quoting_user : R.string.in_reply_to, status.account.getDisplayName()), status.account.emojis));
			UiUtils.loadCustomEmojiInTextView(replyText);
			int visibilityNameRes = switch (status.visibility) {
				case PUBLIC -> R.string.visibility_public;
				case UNLISTED -> R.string.sk_visibility_unlisted;
				case PRIVATE -> R.string.visibility_followers_only;
				case DIRECT -> R.string.visibility_private;
				case LOCAL -> R.string.sk_local_only;
			};
			replyText.setContentDescription(getString(R.string.in_reply_to, status.account.getDisplayName()) + ", " + getString(visibilityNameRes));
			replyText.setOnClickListener(v->{
				scrollView.smoothScrollTo(0, 0);
			});
			replyText.setOnClickListener(v->{
				scrollView.smoothScrollTo(0, 0);
			});


			ArrayList<String> mentions=new ArrayList<>();
			String ownID=AccountSessionManager.getInstance().getAccount(accountID).self.id;
			if(!status.account.id.equals(ownID))
				mentions.add('@'+status.account.acct);
			if(GlobalUserPreferences.mentionRebloggerAutomatically && status.rebloggedBy != null && !status.rebloggedBy.id.equals(ownID))
				mentions.add('@'+status.rebloggedBy.acct);
			for(Mention mention:status.mentions){
				if(mention.id.equals(ownID))
					continue;
				String m='@'+mention.acct;
				if(!mentions.contains(m))
					mentions.add(m);
			}
			initialText=mentions.isEmpty() ? "" : TextUtils.join(" ", mentions)+" ";
			if(savedInstanceState==null){
				mainEditText.setText(initialText);
				ignoreSelectionChanges=true;
				mainEditText.setSelection(mainEditText.length());
				ignoreSelectionChanges=false;
				if(!TextUtils.isEmpty(status.spoilerText)){
					hasSpoiler=true;
					spoilerWrap.setVisibility(View.VISIBLE);
					String prefix = (GlobalUserPreferences.prefixReplies == ALWAYS
							|| (GlobalUserPreferences.prefixReplies == TO_OTHERS && !ownID.equals(status.account.id)))
							&& !status.spoilerText.startsWith("re: ") ? "re: " : "";
					spoilerEdit.setText(prefix + status.spoilerText);
					spoilerBtn.setSelected(true);
				}
				if (status.language != null && !status.language.isEmpty()) setPostLanguage(status.language);
			}
		}else if (editingStatus==null || editingStatus.inReplyToId==null){
			replyText.setVisibility(View.GONE);
		}
		if(savedInstanceState==null){
			if(editingStatus!=null){
				initialText=getArguments().getString("sourceText", "");
				mainEditText.setText(initialText);
				ignoreSelectionChanges=true;
				mainEditText.setSelection(mainEditText.length());
				ignoreSelectionChanges=false;
				setPostLanguage(editingStatus.language);
				mediaViewController.onViewCreated(savedInstanceState);;
			}else{
				String prefilledText=getArguments().getString("prefilledText");
				if(!TextUtils.isEmpty(prefilledText)){
					mainEditText.setText(prefilledText);
					ignoreSelectionChanges=true;
					mainEditText.setSelection(mainEditText.length());
					ignoreSelectionChanges=false;
					initialText=prefilledText;
				}
				if (getArguments().containsKey("selectionStart") || getArguments().containsKey("selectionEnd")) {
					int selectionStart=getArguments().getInt("selectionStart", 0);
					int selectionEnd=getArguments().getInt("selectionEnd", selectionStart);
					mainEditText.setSelection(selectionStart, selectionEnd);
				}
				ArrayList<Uri> mediaUris=getArguments().getParcelableArrayList("mediaAttachments");
				if(mediaUris!=null && !mediaUris.isEmpty()){
					for(Uri uri:mediaUris){
						mediaViewController.addMediaAttachment(uri, null);
					}
				}
			}
		}

		updateSensitive();
		updateHeaders();

		if(editingStatus!=null){
			updateCharCounter();
			visibilityBtn.setEnabled(redraftStatus);
		}
		updateMediaPollStates();
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);
		if(savedInstanceState!=null && savedInstanceState.containsKey("autocompleteStart")){
			int start=savedInstanceState.getInt("autocompleteStart"), end=savedInstanceState.getInt("autocompleteEnd");
			currentAutocompleteSpan=new ComposeAutocompleteSpan();
			mainEditText.getText().setSpan(currentAutocompleteSpan, start, end, Editable.SPAN_EXCLUSIVE_INCLUSIVE);
			startAutocomplete(currentAutocompleteSpan);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(editingStatus==null ? R.menu.compose : R.menu.compose_edit, menu);
		actionItem = menu.findItem(R.id.publish);
		LinearLayout wrap=new LinearLayout(getActivity());
		getActivity().getLayoutInflater().inflate(R.layout.compose_action, wrap);
		actionItem.setActionView(wrap);
		actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		if(!GlobalUserPreferences.relocatePublishButton){
			publishButton = wrap.findViewById(R.id.publish_btn);
			publishButton.setOnClickListener(v -> {
				if(GlobalUserPreferences.altTextReminders && editingStatus==null)
					checkAltTextsAndPublish();
				else
					publish();
			});
			publishButton.setVisibility(View.VISIBLE);

			draftsBtn = wrap.findViewById(R.id.drafts_btn);
			draftsBtn.setVisibility(View.VISIBLE);
		}else{
			charCounter = wrap.findViewById(R.id.char_counter);
			charCounter.setVisibility(View.VISIBLE);
			charCounter.setText(String.valueOf(charLimit));
		}

//		draftsBtn=wrap.findViewById(R.id.drafts_btn);
		draftOptionsPopup=new PopupMenu(getContext(), draftsBtn);
		draftOptionsPopup.inflate(R.menu.compose_more);
		Menu draftOptionsMenu=draftOptionsPopup.getMenu();
		draftMenuItem=draftOptionsMenu.findItem(R.id.draft);
		undraftMenuItem=draftOptionsMenu.findItem(R.id.undraft);
		scheduleMenuItem=draftOptionsMenu.findItem(R.id.schedule);
		unscheduleMenuItem=draftOptionsMenu.findItem(R.id.unschedule);
		draftOptionsMenu.findItem(R.id.preview).setVisible(isInstanceAkkoma());
		draftOptionsPopup.setOnMenuItemClickListener(i->{
			int id=i.getItemId();
			if(id==R.id.draft) updateScheduledAt(getDraftInstant());
			else if(id==R.id.schedule) pickScheduledDateTime();
			else if(id==R.id.unschedule || id==R.id.undraft) updateScheduledAt(null);
			else if(id==R.id.drafts) navigateToUnsentPosts();
			else if(id==R.id.preview) publish(true);
			return true;
		});
		UiUtils.enablePopupMenuIcons(getContext(), draftOptionsPopup);


		languageButton = wrap.findViewById(R.id.language_btn);
		languageButton.setOnClickListener(v->showLanguageAlert());
		languageButton.setOnLongClickListener(v->{
			if(!getLocalPrefs().bottomEncoding){
				getLocalPrefs().bottomEncoding=true;
				getLocalPrefs().save();
			}
			return false;
		});
		if (!GlobalUserPreferences.relocatePublishButton)
			publishButton.post(()->publishButton.setMinimumWidth(publishButton.getWidth()));

		(GlobalUserPreferences.relocatePublishButton ? publishButtonRelocated : publishButton).setOnClickListener(v->{
			Consumer<Boolean> draftCheckComplete=(isDraft)->{
				if(GlobalUserPreferences.altTextReminders && !isDraft) checkAltTextsAndPublish();
				else publish();
			};

			boolean isAlreadyDraft=scheduledAt!=null && scheduledAt.isAfter(DRAFTS_AFTER_INSTANT);
			if(editingStatus!=null && scheduledAt!=null && isAlreadyDraft) {
				new M3AlertDialogBuilder(getActivity())
						.setTitle(R.string.sk_save_draft)
						.setMessage(R.string.sk_save_draft_message)
						.setPositiveButton(R.string.save, (d, w)->draftCheckComplete.accept(isAlreadyDraft))
						.setNegativeButton(R.string.publish, (d, w)->{
							updateScheduledAt(null);
							draftCheckComplete.accept(false);
						})
						.show();
			}else{
				draftCheckComplete.accept(isAlreadyDraft);
			}
		});
		draftsBtn.setOnClickListener(v-> draftOptionsPopup.show());
		draftsBtn.setOnTouchListener(draftOptionsPopup.getDragToOpenListener());
		updateScheduledAt(scheduledAt != null ? scheduledAt : scheduledStatus != null ? scheduledStatus.scheduledAt : null);

		Preferences prefs = AccountSessionManager.get(accountID).preferences;
		if (postLang != null) setPostLanguage(postLang);
		else setPostLanguage(prefs != null && prefs.postingDefaultLanguage != null && prefs.postingDefaultLanguage.length() > 0
				? languageResolver.fromOrFallback(prefs.postingDefaultLanguage)
				: languageResolver.getDefault());

		if(isInstancePixelfed()) spoilerBtn.setVisibility(View.GONE);
		if(isInstancePixelfed() || (editingStatus!=null && !redraftStatus)) {
			// editing an already published post
			draftsBtn.setVisibility(View.GONE);
		}

		updatePublishButtonState();
	}

	private void navigateToUnsentPosts() {
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putBoolean("hide_fab", true);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		imm.hideSoftInputFromWindow(draftsBtn.getWindowToken(), 0);
		if (hasDraft()) {
			Nav.go(getActivity(), ScheduledStatusListFragment.class, args);
		} else {
			// result for the previous ScheduledStatusList
			setResult(true, null);
			// finishing fragment in "onFragmentResult"
			Nav.goForResult(getActivity(), ScheduledStatusListFragment.class, args, SCHEDULED_STATUS_OPENED_RESULT, this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		emojiKeyboard.onConfigurationChanged();
	}

	@SuppressLint("NewApi")
	private void updateCharCounter(){
		Editable text=mainEditText.getText();

		String countableText=TwitterTextEmojiRegex.VALID_EMOJI_PATTERN.matcher(
				MENTION_PATTERN.matcher(
						HtmlParser.URL_PATTERN.matcher(text).replaceAll("$2xxxxxxxxxxxxxxxxxxxxxxx")
				).replaceAll("$1@$3")
		).replaceAll("x");
		charCount=0;
		breakIterator.setText(countableText);
		while(breakIterator.next()!=BreakIterator.DONE){
			charCount++;
		}

		if(hasSpoiler){
			charCount+=spoilerEdit.length();
		}
		if (localOnly && AccountSessionManager.get(accountID).getLocalPreferences().glitchInstance) {
			charCount -= GLITCH_LOCAL_ONLY_SUFFIX.length();
		}
		charCounter.setText(String.valueOf(charLimit-charCount));

		text.removeSpan(overLimitBG);
		text.removeSpan(overLimitFG);
		if(charCount>charLimit){
			charCounter.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error));
			int start=text.length()-(charCount-charLimit);
			int end=text.length();
			text.setSpan(overLimitFG, start, end, 0);
			text.setSpan(overLimitBG, start, end, 0);
		}else{
			charCounter.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		}

		trimmedCharCount=text.toString().trim().length();
		updatePublishButtonState();
	}

	private void resetPublishButtonText() {
		int publishText = editingStatus==null || redraftStatus ? R.string.publish : R.string.save;
		if(GlobalUserPreferences.relocatePublishButton){
			return;
		}
		AccountLocalPreferences prefs=AccountSessionManager.get(accountID).getLocalPreferences();
		if (publishText == R.string.publish && !TextUtils.isEmpty(prefs.publishButtonText)) {
			publishButton.setText(prefs.publishButtonText);
		} else {
			publishButton.setText(publishText);
		}
	}

	public void updatePublishButtonState(){
		uuid=null;
		if(GlobalUserPreferences.relocatePublishButton && publishButtonRelocated != null){
			publishButtonRelocated.setEnabled((!isInstancePixelfed() || !mediaViewController.isEmpty()) && (trimmedCharCount>0 || !mediaViewController.isEmpty()) && charCount<=charLimit && mediaViewController.getNonDoneAttachmentCount()==0 && (pollViewController.isEmpty() || pollViewController.getNonEmptyOptionsCount()>1));
		}

		if(publishButton==null)
			return;
		publishButton.setEnabled((!isInstancePixelfed() || !mediaViewController.isEmpty()) && (trimmedCharCount>0 || !mediaViewController.isEmpty()) && charCount<=charLimit && mediaViewController.getNonDoneAttachmentCount()==0 && (pollViewController.isEmpty() || pollViewController.getNonEmptyOptionsCount()>1));
	}

	private void onCustomEmojiClick(Emoji emoji){
		if(getActivity().getCurrentFocus() instanceof EditText edit){
			if(edit==mainEditText && currentAutocompleteSpan!=null && autocompleteViewController.getMode()==ComposeAutocompleteViewController.Mode.EMOJIS){
				Editable text=mainEditText.getText();
				int start=text.getSpanStart(currentAutocompleteSpan);
				int end=text.getSpanEnd(currentAutocompleteSpan);
				finishAutocomplete();
				text.replace(start, end, ':'+emoji.shortcode+':');
				return;
			}
			int start=edit.getSelectionStart();
			String prefix=start>0 && !Character.isWhitespace(edit.getText().charAt(start-1)) ? " :" : ":";
			edit.getText().replace(start, edit.getSelectionEnd(), prefix+emoji.shortcode+':');
		}
	}

	@Override
	protected void updateToolbar(){
		super.updateToolbar();
		int color=UiUtils.alphaBlendThemeColors(getActivity(), R.attr.colorM3Background, R.attr.colorM3Primary, 0.11f);
		getToolbar().setBackgroundColor(color);
		setStatusBarColor(color);
		bottomBar.setBackgroundColor(color);
		updateNavigationBarColor(emojiKeyboard.isVisible());
	}

	private void updateNavigationBarColor(boolean emojiKeyboardVisible){
		int color=UiUtils.alphaBlendThemeColors(getActivity(), R.attr.colorM3Background, R.attr.colorM3Primary, emojiKeyboardVisible ? 0.08f : 0.11f);
		setNavigationBarColor(color);
	}

	@Override
	protected int getNavigationIconDrawableResource(){
		return R.drawable.ic_fluent_dismiss_24_regular;
	}

	@Override
	public boolean wantsCustomNavigationIcon(){
		return true;
	}

	private void createScheduledStatusFinish(ScheduledStatus result) {
		wm.removeView(sendingOverlay);
		sendingOverlay=null;
		Toast.makeText(getContext(), scheduledAt.isAfter(DRAFTS_AFTER_INSTANT) ?
				R.string.sk_draft_saved : R.string.sk_post_scheduled, Toast.LENGTH_SHORT).show();
		Nav.finish(ComposeFragment.this);
		E.post(new ScheduledStatusCreatedEvent(result, accountID));
	}

	private void maybeDeleteScheduledPost(Runnable callback) {
		if (scheduledStatus != null) {
			new DeleteStatus.Scheduled(scheduledStatus.id).setCallback(new Callback<>() {
				@Override
				public void onSuccess(Object o) {
					E.post(new ScheduledStatusDeletedEvent(scheduledStatus.id, accountID));
					callback.run();
				}

				@Override
				public void onError(ErrorResponse error) {
					handlePublishError(error);
				}
			}).exec(accountID);
		} else {
			callback.run();
		}
	}

	private void checkAltTextsAndPublish(){
		int count=mediaViewController.getMissingAltTextAttachmentCount();
		if(count==0){
			publish();
		}else{
			String msg=getResources().getQuantityString(mediaViewController.areAllAttachmentsImages() ? R.plurals.alt_text_reminder_x_images : R.plurals.alt_text_reminder_x_attachments,
					count, switch(count){
						case 1 -> getString(R.string.count_one);
						case 2 -> getString(R.string.count_two);
						case 3 -> getString(R.string.count_three);
						case 4 -> getString(R.string.count_four);
						default -> String.valueOf(count);
					});
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.alt_text_reminder_title)
					.setMessage(msg)
					.setPositiveButton(R.string.alt_text_reminder_post_anyway, (dlg, item)->publish())
					.setNegativeButton(R.string.cancel, null)
					.show();
		}
	}

	private void publish(){
		publish(false);
	}

	private void publish(boolean preview){
		sendingOverlay=new View(getActivity());
		WindowManager.LayoutParams overlayParams=new WindowManager.LayoutParams();
		overlayParams.type=WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
		overlayParams.flags=WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
		overlayParams.width=overlayParams.height=WindowManager.LayoutParams.MATCH_PARENT;
		overlayParams.format=PixelFormat.TRANSLUCENT;
		overlayParams.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;
		overlayParams.token=mainEditText.getWindowToken();
		wm.addView(sendingOverlay, overlayParams);

		(GlobalUserPreferences.relocatePublishButton ? publishButtonRelocated : publishButton).setEnabled(false);
		V.setVisibilityAnimated(sendProgress, View.VISIBLE);

		mediaViewController.saveAltTextsBeforePublishing(
				()->actuallyPublish(preview),
				this::handlePublishError);
	}

	private void actuallyPublish(boolean preview){
		String text=mainEditText.getText().toString();
		if(GlobalUserPreferences.removeTrackingParams)
			text=Tracking.cleanUrlsInText(text);
		CreateStatus.Request req=new CreateStatus.Request();
		if("bottom".equals(postLang.encoding)){
			text=new StatusTextEncoder(Bottom::encode).encode(text);
			req.spoilerText="bottom-encoded emoji spam";
		}
		if(localOnly &&
				AccountSessionManager.get(accountID).getLocalPreferences().glitchInstance &&
				!GLITCH_LOCAL_ONLY_PATTERN.matcher(text).matches()){
			text+=" "+GLITCH_LOCAL_ONLY_SUFFIX;
		}
		req.status=text;
		req.localOnly=localOnly;
		req.visibility=localOnly && instance.isAkkoma() ? StatusPrivacy.LOCAL : statusVisibility;
		req.sensitive=sensitive;
		req.contentType=contentType==ContentType.UNSPECIFIED ? null : contentType;
		req.scheduledAt=scheduledAt;
		req.preview=preview;
		if(!mediaViewController.isEmpty()){
			req.mediaIds=mediaViewController.getAttachmentIDs();
			if(editingStatus != null){
				req.mediaAttributes=mediaViewController.getAttachmentAttributes();
			}
		}
		if(replyTo!=null || (editingStatus != null && editingStatus.inReplyToId!=null)){
			req.inReplyToId=editingStatus!=null ? editingStatus.inReplyToId : replyTo.id;
		}
		if(!pollViewController.isEmpty()){
			req.poll=pollViewController.getPollForRequest();
		}
		if(hasSpoiler && spoilerEdit.length()>0){
			req.spoilerText=spoilerEdit.getText().toString();
		}
		if(postLang!=null && postLang.language!=null){
			req.language=postLang.language.getLanguage();
		}
		if(quote != null){
			req.quoteId=quote.id;
		}
		if(uuid==null)
			uuid=UUID.randomUUID().toString();

		Callback<Status> resCallback=new Callback<>(){
			@Override
			public void onSuccess(Status result){
				if(preview){
					openPreview(result);
					return;
				}

				maybeDeleteScheduledPost(()->{
					wm.removeView(sendingOverlay);
					sendingOverlay=null;
					if(editingStatus==null || redraftStatus){
						E.post(new StatusCreatedEvent(result, accountID));
						if(replyTo!=null && !redraftStatus){
							replyTo.repliesCount++;
							E.post(new StatusCountersUpdatedEvent(replyTo));
						}
					}else{
						// pixelfed doesn't return the edited status :/
						Status editedStatus = result == null ? editingStatus : result;
						if (result == null) {
							editedStatus.text = req.status;
							editedStatus.spoilerText = req.spoilerText;
							editedStatus.sensitive = req.sensitive;
							editedStatus.language = req.language;
							// user will have to reload to see html
							editedStatus.content = req.status;
						}
						E.post(new StatusUpdatedEvent(editedStatus));
					}
					if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isStateSaved()){
						Nav.finish(ComposeFragment.this);
					}
					if(getArguments().getBoolean("navigateToStatus", false)){
						Bundle args=new Bundle();
						args.putString("account", accountID);
						args.putParcelable("status", Parcels.wrap(result));
						if(replyTo!=null) args.putParcelable("inReplyToAccount", Parcels.wrap(replyTo));
						Nav.go(getActivity(), ThreadFragment.class, args);
					}
				});
			}

			@Override
			public void onError(ErrorResponse error){
				handlePublishError(error);
			}
		};

		if(editingStatus!=null && !redraftStatus && !preview){
			new EditStatus(req, editingStatus.id)
					.setCallback(resCallback)
					.exec(accountID);
		}else if(req.scheduledAt == null || preview){
			new CreateStatus(req, uuid)
					.setCallback(resCallback)
					.exec(accountID);
		}else if(req.scheduledAt.isAfter(Instant.now().plus(10, ChronoUnit.MINUTES))){
			// checking for 10 instead of 5 minutes (as per mastodon) because i really don't want
			// bugs to occur because the client's clock is wrong by a minute or two - the api
			// returns a status instead of a scheduled status if scheduled time is less than 5
			// minutes into the future and this is 1. unexpected for the user and 2. hard to handle
			new CreateStatus.Scheduled(req, uuid)
					.setCallback(new Callback<>() {
						@Override
						public void onSuccess(ScheduledStatus result) {
							maybeDeleteScheduledPost(() -> {
								createScheduledStatusFinish(result);
							});
						}

						@Override
						public void onError(ErrorResponse error) {
							handlePublishError(error);
						}
					}).exec(accountID);
		}else{
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.sk_scheduled_too_soon_title)
					.setMessage(R.string.sk_scheduled_too_soon)
					.setPositiveButton(R.string.ok, (a, b)->{})
					.show();
			handlePublishError(null);
			(GlobalUserPreferences.relocatePublishButton ? publishButtonRelocated : publishButton).setEnabled(false);
		}

		if (replyTo == null) updateRecentLanguages();
	}

	private void handlePublishError(ErrorResponse error){
		wm.removeView(sendingOverlay);
		sendingOverlay=null;
		V.setVisibilityAnimated(sendProgress, View.GONE);
		(GlobalUserPreferences.relocatePublishButton ? publishButtonRelocated : publishButton).setEnabled(true);
		if(error instanceof MastodonErrorResponse me){
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.post_failed)
					.setMessage(me.error)
					.setPositiveButton(R.string.retry, (dlg, btn)->publish())
					.setNegativeButton(R.string.cancel, null)
					.show();
		}else if(error!=null){
			error.showToast(getActivity());
		}
	}

	private void openPreview(Status result){
		result.preview=true;
		wm.removeView(sendingOverlay);
		sendingOverlay=null;
		(GlobalUserPreferences.relocatePublishButton ? publishButtonRelocated : publishButton).setEnabled(true);
		V.setVisibilityAnimated(sendProgress, View.GONE);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		imm.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("status", Parcels.wrap(result));
		if(replyTo!=null){
			args.putParcelable("inReplyTo", Parcels.wrap(replyTo));
			args.putParcelable("inReplyToAccount", Parcels.wrap(replyTo.account));
		}
		Nav.go(getActivity(), ThreadFragment.class, args);
	}

	private void updateRecentLanguages() {
		if (postLang == null || postLang.language == null) return;
		String language = postLang.language.getLanguage();
		AccountLocalPreferences prefs = AccountSessionManager.get(accountID).getLocalPreferences();
		prefs.recentLanguages.remove(language);
		prefs.recentLanguages.add(0, language);
		if (postLang.encoding != null) {
			prefs.recentLanguages.remove(postLang.encoding);
			prefs.recentLanguages.add(0, postLang.encoding);
		}
		if ("bottom".equals(postLang.encoding) && !prefs.bottomEncoding) prefs.bottomEncoding = true;
		prefs.save();
	}

	private boolean hasDraft(){
		if(getArguments().getBoolean("hasDraft", false)) return true;
		if(editingStatus!=null){
			if(!mainEditText.getText().toString().equals(initialText))
				return true;
			List<String> existingMediaIDs=editingStatus.mediaAttachments.stream().map(a->a.id).collect(Collectors.toList());
			if(!existingMediaIDs.equals(mediaViewController.getAttachmentIDs()))
				return true;
			if(!statusVisibility.equals(editingStatus.visibility)) return true;
			if(scheduledStatus != null && !scheduledStatus.scheduledAt.equals(scheduledAt)) return true;
			if(sensitive != editingStatus.sensitive) return true;
			return pollViewController.isPollChanged();
		}
		boolean pollFieldsHaveContent=pollViewController.getNonEmptyOptionsCount()>0;
		return (mainEditText.length()>0 && !mainEditText.getText().toString().equals(initialText)) || !mediaViewController.isEmpty() || pollFieldsHaveContent;
	}

	@Override
	public boolean onBackPressed(){
		if(emojiKeyboard.isVisible()){
			emojiKeyboard.hide();
			return true;
		}
		if(hasDraft()){
			confirmDiscardDraftAndFinish();
			return true;
		}
		if(sendingOverlay!=null)
			return true;
		return false;
	}

	@Override
	public void onToolbarNavigationClick(){
		if(hasDraft()){
			confirmDiscardDraftAndFinish();
		}else{
			super.onToolbarNavigationClick();
		}
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result){
		if(reqCode==IMAGE_DESCRIPTION_RESULT && success){
			String attID=result.getString("attachment");
			String text=result.getString("text");
			mediaViewController.setAltTextByID(attID, text);
		}else if(reqCode==AUTOCOMPLETE_ACCOUNT_RESULT && success){
			Account acc=Parcels.unwrap(result.getParcelable("selectedAccount"));
			if(currentAutocompleteSpan==null)
				return;
			Editable e=mainEditText.getText();
			int start=e.getSpanStart(currentAutocompleteSpan);
			int end=e.getSpanEnd(currentAutocompleteSpan);
			e.removeSpan(currentAutocompleteSpan);
			e.replace(start, end, '@'+acc.acct+' ');
			finishAutocomplete();
		}
	}

	private void confirmDiscardDraftAndFinish(){
		boolean attachmentsPending=mediaViewController.areAnyAttachmentsNotDone();
		if(attachmentsPending) new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.sk_unfinished_attachments)
				.setMessage(R.string.sk_unfinished_attachments_message)
				.setPositiveButton(R.string.ok, (d, w)->{})
				.setNegativeButton(R.string.discard, (d, w)->Nav.finish(this))
				.show();
		else new M3AlertDialogBuilder(getActivity())
				.setTitle(editingStatus!=null ? R.string.sk_confirm_save_changes : R.string.sk_confirm_save_draft)
				.setPositiveButton(R.string.save, (d, w)->{
					updateScheduledAt(scheduledAt==null ? getDraftInstant() : scheduledAt);
					publish();
				})
				.setNegativeButton(R.string.discard, (d, w)->Nav.finish(this))
				.show();
	}


	/**
	 * Builds the correct intent for the device version to select media.
	 *
	 * <p>For Device version > T or R_SDK_v2, use the android platform photopicker via
	 * {@link MediaStore#ACTION_PICK_IMAGES}
	 *
	 * <p>For earlier versions use the built in docs ui via {@link Intent#ACTION_GET_CONTENT}
	 */
	private void openFilePicker(boolean photoPicker){
		Intent intent;
		boolean usePhotoPicker=photoPicker && UiUtils.isPhotoPickerAvailable();
		if(usePhotoPicker){
			intent=new Intent(MediaStore.ACTION_PICK_IMAGES);
			if(mediaViewController.getMaxAttachments()-mediaViewController.getMediaAttachmentsCount()>1)
				intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, mediaViewController.getMaxAttachments()-mediaViewController.getMediaAttachmentsCount());
		}else{
			intent=new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("*/*");
		}
		if(!usePhotoPicker && instance.configuration!=null &&
				instance.configuration.mediaAttachments!=null &&
				instance.configuration.mediaAttachments.supportedMimeTypes!=null &&
				!instance.configuration.mediaAttachments.supportedMimeTypes.isEmpty()){
			intent.putExtra(Intent.EXTRA_MIME_TYPES,
					instance.configuration.mediaAttachments.supportedMimeTypes.toArray(
							new String[0]));
		}else{
			if(!usePhotoPicker){
				// If photo picker is being used these are the default mimetypes.
				intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
			}
		}
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		startActivityForResult(intent, MEDIA_RESULT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==MEDIA_RESULT && resultCode==Activity.RESULT_OK){
			Uri single=data.getData();
			if(single!=null){
				mediaViewController.addMediaAttachment(single, null);
			}else{
				ClipData clipData=data.getClipData();
				for(int i=0;i<clipData.getItemCount();i++){
					mediaViewController.addMediaAttachment(clipData.getItemAt(i).getUri(), null);
				}
			}
		}

		if(requestCode==CAMERA_PIC_REQUEST_CODE && resultCode==Activity.RESULT_OK){
			onAddMediaAttachmentFromEditText(photoUri, null);
		}
	}

	@Subscribe
	public void onTakePictureRequest(TakePictureRequestEvent ev) {
		if(isVisible()) {
			try {
				openCamera();
			} catch (IOException e) {
				Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT);
			}

		}
	}

	private void openCamera() throws IOException {
		if (getContext().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			File photoFile = File.createTempFile("img", ".jpg");
			photoUri = UiUtils.getFileProviderUri(getContext(), photoFile);

			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
			if(getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
				startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST_CODE);
			} else {
				Toast.makeText(getContext(), R.string.mo_camera_not_available, Toast.LENGTH_SHORT);
			}
		} else {
			getActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
		}
	}


	public void updateMediaPollStates(){
		pollBtn.setSelected(pollViewController.isShown());
		mediaBtn.setEnabled(!pollViewController.isShown() && mediaViewController.canAddMoreAttachments());
		pollBtn.setEnabled(mediaViewController.isEmpty());
	}

	private void togglePoll(){
		pollViewController.toggle();
		updatePublishButtonState();
		updateMediaPollStates();
	}

	private void toggleSpoiler(){
		hasSpoiler=!hasSpoiler;
		if(hasSpoiler){
			spoilerWrap.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
			spoilerEdit.requestFocus();
		}else{
			spoilerWrap.setVisibility(View.GONE);
			spoilerEdit.setText("");
			spoilerBtn.setSelected(false);
			mainEditText.requestFocus();
			updateCharCounter();
			sensitiveBtn.setVisibility(mediaViewController.getMediaAttachmentsCount() > 0 ? View.VISIBLE : View.GONE);
		}
		updateSensitive();
	}

	private void toggleSensitive() {
		sensitive=!sensitive;
		sensitiveBtn.setSelected(sensitive);
	}

	public void updateSensitive() {
		sensitiveBtn.setVisibility(View.GONE);
		if (!mediaViewController.isEmpty()) sensitiveBtn.setVisibility(View.VISIBLE);
		if (mediaViewController.isEmpty()) sensitive = false;
	}

	private void pickScheduledDateTime() {
		LocalDateTime soon = LocalDateTime.now()
				.plus(15, ChronoUnit.MINUTES) // so 14:59 doesn't get rounded up to…
				.plus(1, ChronoUnit.HOURS) // …15:00, but rather 16:00
				.withMinute(0);
		new DatePickerDialog(getActivity(), (datePicker, year, arrayMonth, dayOfMonth) -> {
			new TimePickerDialog(getActivity(), (timePicker, hour, minute) -> {
				LocalDateTime at=LocalDateTime.of(year, arrayMonth + 1, dayOfMonth, hour, minute);
				updateScheduledAt(at.toInstant(ZoneId.systemDefault().getRules().getOffset(at)));
			}, soon.getHour(), soon.getMinute(), DateFormat.is24HourFormat(getActivity())).show();
		}, soon.getYear(), soon.getMonthValue() - 1, soon.getDayOfMonth()).show();
	}

	private void updateScheduledAt(Instant scheduledAt) {
		this.scheduledAt = scheduledAt;
		updatePublishButtonState();
		V.setVisibilityAnimated(scheduleDraftView, scheduledAt == null ? View.GONE : View.VISIBLE);
		draftMenuItem.setVisible(true);
		scheduleMenuItem.setVisible(true);
		undraftMenuItem.setVisible(false);
		unscheduleMenuItem.setVisible(false);
		if (scheduledAt != null) {
			DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
			if (scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)) {
				draftMenuItem.setVisible(false);
				undraftMenuItem.setVisible(true);
				scheduleTimeBtn.setVisibility(View.GONE);
				scheduleDraftText.setText(R.string.sk_compose_draft);
				scheduleDraftText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fluent_drafts_20_regular, 0, 0, 0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					scheduleDraftDismiss.setTooltipText(getString(R.string.sk_compose_no_draft));
				}
				scheduleDraftDismiss.setContentDescription(getString(R.string.sk_compose_no_draft));
				draftsBtn.setImageDrawable(getContext().getDrawable(GlobalUserPreferences.relocatePublishButton ? R.drawable.ic_fluent_drafts_24_regular : R.drawable.ic_fluent_drafts_20_filled));

				if(GlobalUserPreferences.relocatePublishButton){
					publishButtonRelocated.setImageResource(scheduledStatus != null && scheduledStatus.scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)
						? R.drawable.ic_fluent_save_24_selector : R.drawable.ic_fluent_drafts_24_selector);
				}else{
					publishButton.setText(scheduledStatus != null && scheduledStatus.scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)
							? R.string.save : R.string.sk_draft);
				}
			} else {
				scheduleMenuItem.setVisible(false);
				unscheduleMenuItem.setVisible(true);
				String at = scheduledAt.atZone(ZoneId.systemDefault()).format(formatter);
				scheduleTimeBtn.setVisibility(View.VISIBLE);
				scheduleTimeBtn.setText(at);
				scheduleDraftText.setText(R.string.sk_compose_scheduled);
				scheduleDraftText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					scheduleDraftDismiss.setTooltipText(getString(R.string.sk_compose_no_schedule));
				}
				scheduleDraftDismiss.setContentDescription(getString(R.string.sk_compose_no_schedule));
				draftsBtn.setImageDrawable(getContext().getDrawable(GlobalUserPreferences.relocatePublishButton ? R.drawable.ic_fluent_clock_24_filled : R.drawable.ic_fluent_clock_20_filled));
				if(GlobalUserPreferences.relocatePublishButton)
				{
					publishButtonRelocated.setImageResource(scheduledStatus != null && scheduledStatus.scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)
							? R.drawable.ic_fluent_save_24_selector : R.drawable.ic_fluent_clock_24_selector);
				}else{
					publishButton.setText(scheduledStatus != null && scheduledStatus.scheduledAt.equals(scheduledAt)
							? R.string.save : R.string.sk_schedule);
				}
			}
		} else {
			draftsBtn.setImageDrawable(getContext().getDrawable(GlobalUserPreferences.relocatePublishButton ? R.drawable.ic_fluent_clock_24_regular : R.drawable.ic_fluent_clock_20_regular));
			if(GlobalUserPreferences.relocatePublishButton){
				publishButtonRelocated.setImageResource(R.drawable.ic_fluent_send_24_regular);
			}
			resetPublishButtonText();
		}
	}

	private void updateHeaders() {
		UiUtils.setExtraTextInfo(getContext(), selfExtraText, false, false, localOnly, null);
		if (replyTo != null) UiUtils.setExtraTextInfo(getContext(), extraText, true, false, replyTo.localOnly || replyTo.visibility==StatusPrivacy.LOCAL, replyTo.account);
	}

	private void buildVisibilityPopup(View v){
		visibilityPopup=new PopupMenu(getActivity(), v);
		visibilityPopup.inflate(R.menu.compose_visibility);
		Menu m=visibilityPopup.getMenu();
		if(isInstancePixelfed()){
			m.findItem(R.id.vis_private).setVisible(false);
		}
		MenuItem localOnlyItem=visibilityPopup.getMenu().findItem(R.id.local_only);
		AccountLocalPreferences prefs=AccountSessionManager.get(accountID).getLocalPreferences();
		boolean prefsSaysSupported=prefs.localOnlySupported;
		if(isInstanceAkkoma()){
			m.findItem(R.id.vis_local).setVisible(true);
		}else if(localOnly || prefsSaysSupported){
			localOnlyItem.setVisible(true);
			localOnlyItem.setChecked(localOnly);
			Status status=editingStatus!=null ? editingStatus : replyTo;
			if(!prefsSaysSupported){
				prefs.localOnlySupported=true;
				if(GLITCH_LOCAL_ONLY_PATTERN.matcher(status.getStrippedText()).matches()){
					prefs.glitchInstance=true;
				}
				prefs.save();
			}
		}
		UiUtils.enablePopupMenuIcons(getActivity(), visibilityPopup);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic()) m.setGroupDividerEnabled(true);
		visibilityPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item){
				int id=item.getItemId();
				if(id==R.id.vis_public){
					statusVisibility=StatusPrivacy.PUBLIC;
				}else if(id==R.id.vis_unlisted){
					statusVisibility=StatusPrivacy.UNLISTED;
				}else if(id==R.id.vis_followers){
					statusVisibility=StatusPrivacy.PRIVATE;
				}else if(id==R.id.vis_private){
					statusVisibility=StatusPrivacy.DIRECT;
				}else if(id==R.id.vis_local){
					statusVisibility=StatusPrivacy.LOCAL;
				}
				if(id==R.id.local_only){
					localOnly=!item.isChecked();
					item.setChecked(localOnly);
				}else{
					item.setChecked(true);
				}
				updateVisibilityIcon();
				updateHeaders();
				return true;
			}
		});
	}

	@SuppressLint("ClickableViewAccessibility")
	private void buildContentTypePopup(View btn) {
		contentTypePopup=new PopupMenu(getActivity(), btn);
		Menu m = contentTypePopup.getMenu();
		for(ContentType value : ContentType.values()){
			if(!value.supportedByInstance(instance)) continue;
			m.add(0, value.ordinal(), Menu.NONE, value.getName());
		}
		m.setGroupCheckable(0, true, true);
		if (contentType!=ContentType.UNSPECIFIED || editingStatus!=null){
			// setting content type to null while editing will just leave it unchanged
			m.findItem(ContentType.UNSPECIFIED.ordinal()).setVisible(false);
		}

		contentTypePopup.setOnMenuItemClickListener(i->{
			uuid=null;
			int index=i.getItemId();
			contentType=ContentType.values()[index];
			btn.setSelected(index!=ContentType.UNSPECIFIED.ordinal() && index!=ContentType.PLAIN.ordinal());
			i.setChecked(true);
			return true;
		});

		if (!AccountSessionManager.get(accountID).getLocalPreferences().contentTypesEnabled) {
			btn.setVisibility(View.GONE);
		}
	}

	private void onVisibilityClick(View v){
		PopupMenu menu=new PopupMenu(getActivity(), v);
		menu.inflate(R.menu.compose_visibility);
		menu.setOnMenuItemClickListener(item->{
			int id=item.getItemId();
			if(id==R.id.vis_public){
				statusVisibility=StatusPrivacy.PUBLIC;
			}else if(id==R.id.vis_followers){
				statusVisibility=StatusPrivacy.PRIVATE;
			}else if(id==R.id.vis_private){
				statusVisibility=StatusPrivacy.DIRECT;
			}
			item.setChecked(true);
			updateVisibilityIcon();
			return true;
		});
		menu.show();
	}

	private void loadDefaultStatusVisibility(Bundle savedInstanceState) {
		if(replyTo != null) {
			statusVisibility = (replyTo.visibility == StatusPrivacy.PUBLIC && GlobalUserPreferences.defaultToUnlistedReplies ? StatusPrivacy.UNLISTED : replyTo.visibility);
		}

		AccountSessionManager asm = AccountSessionManager.getInstance();
		Preferences prefs=asm.getAccount(accountID).preferences;
		if (prefs != null) {
			// Only override the reply visibility if our preference is more private
			// (and we're not replying to ourselves, or not at all)
			if (prefs.postingDefaultVisibility.isLessVisibleThan(statusVisibility) &&
					(replyTo == null || !asm.isSelf(accountID, replyTo.account))) {
				statusVisibility = prefs.postingDefaultVisibility;
			}
		}
	}

	private void updateVisibilityIcon(){
		if(getActivity()==null)
			return;
		if(statusVisibility==null){ // TODO find out why this happens
			statusVisibility=StatusPrivacy.PUBLIC;
		}
		visibilityBtn.setText(switch(statusVisibility){
			case PUBLIC -> R.string.visibility_public;
			case UNLISTED -> R.string.sk_visibility_unlisted;
			case PRIVATE -> R.string.visibility_followers_only;
			case DIRECT -> R.string.visibility_private;
			case LOCAL -> R.string.sk_local_only;
		});
		Drawable icon=getResources().getDrawable(switch(statusVisibility){
			case PUBLIC -> R.drawable.ic_fluent_earth_16_regular;
			case UNLISTED -> R.drawable.ic_fluent_lock_open_16_regular;
			case PRIVATE -> R.drawable.ic_fluent_lock_closed_16_filled;
			case DIRECT -> R.drawable.ic_fluent_mention_16_regular;
			case LOCAL -> R.drawable.ic_fluent_eye_16_regular;
		}, getActivity().getTheme()).mutate();
		icon.setBounds(0, 0, V.dp(18), V.dp(18));
		visibilityBtn.setCompoundDrawableTintList(getContext().getResources().getColorStateList(R.color.m3_primary_selector, getContext().getTheme()));
		visibilityBtn.setCompoundDrawablesRelative(icon, null, visibilityBtn.getCompoundDrawablesRelative()[2], null);
	}

	@Override
	public void onSelectionChanged(int start, int end){
		if(ignoreSelectionChanges)
			return;
		if(start==end && mainEditText.length()>0){
			ComposeAutocompleteSpan[] spans=mainEditText.getText().getSpans(start, end, ComposeAutocompleteSpan.class);
			if(spans.length>0){
				assert spans.length==1;
				ComposeAutocompleteSpan span=spans[0];
				if(currentAutocompleteSpan==null && end==mainEditText.getText().getSpanEnd(span)){
					startAutocomplete(span);
				}else if(currentAutocompleteSpan!=null){
					Editable e=mainEditText.getText();
					String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
					autocompleteViewController.setText(spanText);
				}
			}else if(currentAutocompleteSpan!=null){
				finishAutocomplete();
			}
		}else if(currentAutocompleteSpan!=null){
			finishAutocomplete();
		}
	}

	@Override
	public String[] onGetAllowedMediaMimeTypes(){
		if(instance!=null && instance.configuration!=null && instance.configuration.mediaAttachments!=null && instance.configuration.mediaAttachments.supportedMimeTypes!=null)
			return instance.configuration.mediaAttachments.supportedMimeTypes.toArray(new String[0]);
		return new String[]{"image/jpeg", "image/gif", "image/png", "video/mp4"};
	}

	private String sanitizeMediaDescription(String description){
		if(description == null){
			return null;
		}

		// The Gboard android keyboard attaches this text whenever the user
		// pastes something from the keyboard's suggestion bar.
		// Due to different end user locales, the exact text may vary, but at
		// least in version 13.4.08, all of the translations contained the
		// string "Gboard".
		if (description.contains("Gboard")){
			return null;
		}

		return description;
	}

	@Override
	public boolean onAddMediaAttachmentFromEditText(Uri uri, String description){
		description = sanitizeMediaDescription(description);
		return mediaViewController.addMediaAttachment(uri, description);
	}

	private void startAutocomplete(ComposeAutocompleteSpan span){
		currentAutocompleteSpan=span;
		Editable e=mainEditText.getText();
		String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
		autocompleteViewController.setText(spanText);
		showAutocomplete();
	}

	private void finishAutocomplete(){
		if(currentAutocompleteSpan==null)
			return;
		autocompleteViewController.setText(null);
		currentAutocompleteSpan=null;
		hideAutocomplete();
	}

	private void showAutocomplete(){
		UiUtils.beginLayoutTransition(bottomBar);
		UiUtils.beginLayoutTransition(scheduleDraftView);
		View autocompleteView=autocompleteViewController.getView();
		bottomBar.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
		bottomBar.requestLayout();
		autocompleteView.setVisibility(View.VISIBLE);
		autocompleteDivider.setVisibility(View.VISIBLE);
	}

	private void hideAutocomplete(){
		UiUtils.beginLayoutTransition(bottomBar);
		UiUtils.beginLayoutTransition(scheduleDraftView);
		bottomBar.getLayoutParams().height=V.dp(56);
		bottomBar.requestLayout();
		autocompleteViewController.getView().setVisibility(View.INVISIBLE);
		autocompleteDivider.setVisibility(View.INVISIBLE);
	}

	private void onAutocompleteOptionSelected(String text){
		Editable e=mainEditText.getText();
		int start=e.getSpanStart(currentAutocompleteSpan);
		int end=e.getSpanEnd(currentAutocompleteSpan);
		if(start==-1 || end==-1)
			return;
		e.replace(start, end, text+" ");
		finishAutocomplete();
		InputConnection conn=mainEditText.getCurrentInputConnection();
		if(conn!=null)
			conn.finishComposingText();
	}

	@Override
	public CharSequence getTitle(){
		return getString(R.string.new_post);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}

	public boolean getWasDetached(){
		return wasDetached;
	}

	public boolean isCreatingView(){
		return creatingView;
	}

	@Override
	public String getAccountID(){
		return accountID;
	}

	public void addFakeMediaAttachment(Uri uri, String description){
		mediaViewController.addFakeMediaAttachment(uri, description);
	}

	private void showLanguageAlert(){
		AccountSession session=AccountSessionManager.get(accountID);
		ComposeLanguageAlertViewController vc=new ComposeLanguageAlertViewController(getActivity(), session.preferences!=null ? session.preferences.postingDefaultLanguage : null, postLang, mainEditText.getText().toString(), languageResolver, session);
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.language)
				.setView(vc.getView())
				.setPositiveButton(R.string.ok, (dialog, which)->setPostLanguage(vc.getSelectedOption()))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void setPostLanguage(String lang) {
		setPostLanguage(lang == null ? languageResolver.getDefault() : languageResolver.fromOrFallback(lang));
	}

	private void setPostLanguage(MastodonLanguage lang) {
		setPostLanguage(new ComposeLanguageAlertViewController.SelectedOption(lang));
	}

	private void setPostLanguage(ComposeLanguageAlertViewController.SelectedOption opt){
		postLang=opt;
		if (Objects.equals("bottom", opt.encoding)) {
			languageButton.setText("\uD83E\uDD7A\uD83D\uDC49\uD83D\uDC48");
			languageButton.setContentDescription(opt.encoding);
			return;
		}
		languageButton.setText(opt.language.getLanguageName());
		languageButton.setContentDescription(getActivity().getString(R.string.sk_post_language, opt.language.getDefaultName()));
	}

	@Override
	public Animator onCreateEnterTransition(View prev, View container){
		AnimatorSet anim=new AnimatorSet();
		if(getArguments().getBoolean("fromThreadFragment")){
			anim.playTogether(
					ObjectAnimator.ofFloat(container, View.ALPHA, 0f, 1f),
					ObjectAnimator.ofFloat(container, View.TRANSLATION_Y, V.dp(200), 0)
			);
		}else{
			anim.playTogether(
					ObjectAnimator.ofFloat(container, View.ALPHA, 0f, 1f),
					ObjectAnimator.ofFloat(container, View.TRANSLATION_X, V.dp(100), 0)
			);
		}
		anim.setDuration(300);
		anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
		return anim;
	}

	@Override
	public Animator onCreateExitTransition(View prev, View container){
		AnimatorSet anim=new AnimatorSet();
		anim.playTogether(
				ObjectAnimator.ofFloat(container, View.TRANSLATION_X, V.dp(100)),
				ObjectAnimator.ofFloat(container, View.ALPHA, 0)
		);
		anim.setDuration(200);
		anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
		return anim;
	}
}
