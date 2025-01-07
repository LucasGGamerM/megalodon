package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.SetPrivateNote;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.requests.instance.GetInstance;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.account_list.BlockedAccountsListFragment;
import org.joinmastodon.android.fragments.account_list.FollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowingListFragment;
import org.joinmastodon.android.fragments.account_list.MutedAccountsListFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.fragments.settings.SettingsServerFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.sheets.DecentralizationExplainerSheet;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CoverImageView;
import org.joinmastodon.android.ui.views.CustomDrawingOrderLinearLayout;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.joinmastodon.android.utils.ProvidesAssistContent;
import org.parceler.Parcels;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;
import me.grishka.appkit.views.UsableRecyclerView;

public class ProfileFragment extends LoaderFragment implements OnBackPressedListener, ScrollableToTop, HasFab, ProvidesAssistContent.ProvidesWebUri {
	private static final int AVATAR_RESULT=722;
	private static final int COVER_RESULT=343;

	private ImageView avatar;
	private CoverImageView cover;
	private View avatarBorder;
	private TextView name, username, usernameDomain, bio, followersCount, followersLabel, followingCount, followingLabel;
	private ImageView lockIcon, botIcon;
	private ProgressBarButton actionButton, notifyButton;
	private ViewPager2 pager;
	private NestedRecyclerScrollView scrollView;
	private AccountTimelineFragment postsFragment, postsWithRepliesFragment, mediaFragment;
	private PinnedPostsListFragment pinnedPostsFragment;
	private TabLayout tabbar;
	private SwipeRefreshLayout refreshLayout;
	private View followersBtn, followingBtn;
	private EditText nameEdit, bioEdit;
	private ProgressBar actionProgress, notifyProgress;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private TextView followsYouView;
	private ViewGroup rolesView;
	private LinearLayout countersLayout;
	private View nameEditWrap, bioEditWrap, usernameWrap;
	private View tabsDivider;
	private View actionButtonWrap;
	private CustomDrawingOrderLinearLayout scrollableContent;

	private Account account, remoteAccount;
	private String accountID;
	private String domain;
	private Relationship relationship;
	private boolean isOwnProfile;
	private List<AccountField> fields=new ArrayList<>();

	private boolean isInEditMode, editDirty;
	private Uri editNewAvatar, editNewCover;
	private String profileAccountID;
	private boolean refreshing;
	private ImageButton fab;
	private WindowInsets childInsets;
	private PhotoViewer currentPhotoViewer;
	private boolean editModeLoading;
	private ElevationOnScrollListener onScrollListener;
	private Drawable tabsColorBackground;
	private boolean tabBarIsAtTop;
	private Animator tabBarColorAnim;
	private MenuItem editSaveMenuItem;
	private boolean savingEdits;

	private int maxFields = ProfileAboutFragment.MAX_FIELDS;

	// from ProfileAboutFragment
	public UsableRecyclerView list;
	private AboutAdapter adapter;
	private ItemTouchHelper dragHelper=new ItemTouchHelper(new ReorderCallback());
	private ListImageLoaderWrapper imgLoader;

	// profile note
	private FrameLayout noteWrap;
	private EditText noteEdit;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
		domain=AccountSessionManager.getInstance().getAccount(accountID).domain;
		if (getArguments().containsKey("remoteAccount")) {
			remoteAccount = Parcels.unwrap(getArguments().getParcelable("remoteAccount"));
			if(!getArguments().getBoolean("noAutoLoad", false))
				loadData();
		} else if(getArguments().containsKey("profileAccount")){
			account=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
			profileAccountID=account.id;
			isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
			loaded=true;
			if(!isOwnProfile)
				loadRelationship();
			else if (isInstanceAkkoma()) {
				maxFields = (int) getInstance().get().pleroma.metadata.fieldsLimits.maxFields;
			}
		}else{
			profileAccountID=getArguments().getString("profileAccountID");
			if(!getArguments().getBoolean("noAutoLoad", false))
				loadData();
		}
	}

	private String getPrefilledText() {
		return account == null || AccountSessionManager.getInstance().isSelf(accountID, account)
				? null : '@'+account.acct+' ';
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View content=inflater.inflate(R.layout.fragment_profile, container, false);

		avatar=content.findViewById(R.id.avatar);
		cover=content.findViewById(R.id.cover);
		avatarBorder=content.findViewById(R.id.avatar_border);
		name=content.findViewById(R.id.name);
		usernameWrap=content.findViewById(R.id.username_wrap);
		username=content.findViewById(R.id.username);
		usernameDomain=content.findViewById(R.id.username_domain);
		lockIcon=content.findViewById(R.id.lock_icon);
		botIcon=content.findViewById(R.id.bot_icon);
		bio=content.findViewById(R.id.bio);
		followersCount=content.findViewById(R.id.followers_count);
		followersLabel=content.findViewById(R.id.followers_label);
		followersBtn=content.findViewById(R.id.followers_btn);
		followingCount=content.findViewById(R.id.following_count);
		followingLabel=content.findViewById(R.id.following_label);
		followingBtn=content.findViewById(R.id.following_btn);
		actionButton=content.findViewById(R.id.profile_action_btn);
		notifyButton=content.findViewById(R.id.notify_btn);
		pager=content.findViewById(R.id.pager);
		scrollView=content.findViewById(R.id.scroller);
		tabbar=content.findViewById(R.id.tabbar);
		refreshLayout=content.findViewById(R.id.refresh_layout);
		nameEdit=content.findViewById(R.id.name_edit);
		bioEdit=content.findViewById(R.id.bio_edit);
		nameEditWrap=content.findViewById(R.id.name_edit_wrap);
		bioEditWrap=content.findViewById(R.id.bio_edit_wrap);
		actionProgress=content.findViewById(R.id.action_progress);
		notifyProgress=content.findViewById(R.id.notify_progress);
		fab=content.findViewById(R.id.fab);
		followsYouView=content.findViewById(R.id.follows_you);
		countersLayout=content.findViewById(R.id.profile_counters);
		tabsDivider=content.findViewById(R.id.tabs_divider);
		actionButtonWrap=content.findViewById(R.id.profile_action_btn_wrap);
		scrollableContent=content.findViewById(R.id.scrollable_content);
		list=content.findViewById(R.id.metadata);
		rolesView=content.findViewById(R.id.roles);

		avatarBorder.setOutlineProvider(OutlineProviders.roundedRect(26));
		avatarBorder.setClipToOutline(true);
		avatar.setOutlineProvider(OutlineProviders.roundedRect(24));
		avatar.setClipToOutline(true);

		noteEdit=content.findViewById(R.id.note_edit);
		noteWrap=content.findViewById(R.id.note_edit_wrap);

		noteEdit.setOnFocusChangeListener((v, hasFocus)->{
			if(hasFocus){
				hideFab();
				return;
			}
			showFab();
			savePrivateNote(noteEdit.getText().toString());
		});

		FrameLayout sizeWrapper=new FrameLayout(getActivity()){
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
				pager.getLayoutParams().height=MeasureSpec.getSize(heightMeasureSpec)-getPaddingTop()-getPaddingBottom()-V.dp(48);
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		};

		tabViews=new FrameLayout[4];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.profile_posts;
				case 1 -> R.id.profile_posts_with_replies;
				case 2 -> R.id.profile_pinned_posts;
				case 3 -> R.id.profile_media;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			sizeWrapper.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		UiUtils.reduceSwipeSensitivity(pager);
		pager.setOffscreenPageLimit(4);
		pager.setUserInputEnabled(!GlobalUserPreferences.disableSwipe);
		pager.setAdapter(new ProfilePagerAdapter());
		pager.getLayoutParams().height=getResources().getDisplayMetrics().heightPixels;

		scrollView.setScrollableChildSupplier(this::getScrollableRecyclerView);
		scrollView.getViewTreeObserver().addOnGlobalLayoutListener(this::updateMetadataHeight);

		sizeWrapper.addView(content);

		tabbar.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		tabbar.setTabTextSize(V.dp(14));
		tabLayoutMediator=new TabLayoutMediator(tabbar, pager, (tab, position)->tab.setText(switch(position){
			case 0 -> R.string.profile_featured;
			case 1 -> R.string.profile_timeline;
			case 2 -> R.string.profile_about;
			default -> throw new IllegalStateException();
		}));
		tabLayoutMediator=new TabLayoutMediator(tabbar, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				tab.setText(switch(position){
					case 0 -> R.string.posts;
					case 1 -> R.string.posts_and_replies;
					case 2 -> R.string.sk_pinned_posts;
					case 3 -> R.string.media;
					default -> throw new IllegalStateException();
				});
				if (position == 4) tab.view.setVisibility(View.GONE);
			}
		});
		tabbar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
			@Override
			public void onTabSelected(TabLayout.Tab tab){}

			@Override
			public void onTabUnselected(TabLayout.Tab tab){}

			@Override
			public void onTabReselected(TabLayout.Tab tab){
				if(getFragmentForPage(tab.getPosition()) instanceof ScrollableToTop stt)
					stt.scrollToTop();
			}
		});

		cover.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setEmpty();
			}
		});

		actionButton.setOnClickListener(this::onActionButtonClick);
		actionButton.setOnLongClickListener(this::onActionButtonLongClick);
		notifyButton.setOnClickListener(this::onNotifyButtonClick);
		avatar.setOnClickListener(this::onAvatarClick);
		cover.setOnClickListener(this::onCoverClick);
		refreshLayout.setOnRefreshListener(this);
		fab.setOnClickListener(this::onFabClick);
		fab.setOnLongClickListener(v->UiUtils.pickAccountForCompose(getActivity(), accountID, getPrefilledText()));

		if(savedInstanceState!=null){
			postsFragment=(AccountTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "posts");
			postsWithRepliesFragment=(AccountTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "postsWithReplies");
			mediaFragment=(AccountTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "media");
			pinnedPostsFragment=(PinnedPostsListFragment) getChildFragmentManager().getFragment(savedInstanceState, "pinnedPosts");
		}

		if(loaded){
			bindHeaderView();
			dataLoaded();
			tabLayoutMediator.attach();
		}else{
			fab.setVisibility(View.GONE);
		}

		followersBtn.setOnClickListener(this::onFollowersOrFollowingClick);
		followingBtn.setOnClickListener(this::onFollowersOrFollowingClick);

		content.findViewById(R.id.username_wrap).setOnClickListener(v->{
			new DecentralizationExplainerSheet(getActivity(), accountID, account).show();
		});

		content.findViewById(R.id.username_wrap).setOnLongClickListener(v->{
			String usernameString=account.acct;
			if(!usernameString.contains("@")){
				usernameString+="@"+domain;
			}
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, "@"+usernameString));
			UiUtils.maybeShowTextCopiedToast(getActivity());
			return true;
		});

		// from ProfileAboutFragment
		list.setItemAnimator(new BetterItemAnimator());
		list.setDrawSelectorOnTop(true);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		imgLoader=new ListImageLoaderWrapper(getActivity(), list, new RecyclerViewDelegate(list), null);
		list.setAdapter(adapter=new AboutAdapter());
		list.setClipToPadding(false);

		scrollableContent.setDrawingOrderCallback((count, pos)->{
			// The header is the first child, draw it last to overlap everything for the photo viewer transition to look nice
			if(pos==count-1)
				return 0;
			// Offset the order of other child views to compensate
			return pos+1;
		});

		int colorBackground=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background);
		int colorPrimary=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		refreshLayout.setProgressBackgroundColorSchemeColor(UiUtils.alphaBlendColors(colorBackground, colorPrimary, 0.11f));
		refreshLayout.setColorSchemeColors(colorPrimary);

		nameEdit.addTextChangedListener(new SimpleTextWatcher(e->editDirty=true));
		bioEdit.addTextChangedListener(new SimpleTextWatcher(e->editDirty=true));


//		qrCodeButton.setOnClickListener(v->{
//			Bundle args=new Bundle();
//			args.putString("account", accountID);
//			args.putParcelable("targetAccount", Parcels.wrap(account));
//			ProfileQrCodeFragment qf=new ProfileQrCodeFragment();
//			qf.setArguments(args);
//			qf.show(getChildFragmentManager(), "qrDialog");
//		});

		return sizeWrapper;
	}

	private void showPrivateNote(){
		noteWrap.setVisibility(View.VISIBLE);
		noteEdit.setText(relationship.note);
	}

	private void hidePrivateNote(){
		noteEdit.setText(null);
		noteWrap.setVisibility(View.GONE);
	}

	private void savePrivateNote(String note){
		if(note!=null && note.equals(relationship.note)){
			updateRelationship();
			invalidateOptionsMenu();
			return;
		}
		new SetPrivateNote(profileAccountID, note).setCallback(new Callback<>() {
			@Override
			public void onSuccess(Relationship result) {
				updateRelationship(result);
				invalidateOptionsMenu();
				if(!TextUtils.isEmpty(result.note))
					Toast.makeText(MastodonApp.context, R.string.mo_personal_note_saved, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onError(ErrorResponse error) {
				error.showToast(getContext());
			}
		}).exec(accountID);
	}

	private void onAccountLoaded(Account result) {
		account=result;
		isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
		bindHeaderView();
		dataLoaded();
		if(!tabLayoutMediator.isAttached())
			tabLayoutMediator.attach();
		if(!isOwnProfile)
			loadRelationship();
		else
			AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
		if(refreshing){
			refreshing=false;
			refreshLayout.setRefreshing(false);
			if(postsFragment.loaded)
				postsFragment.onRefresh();
			if(postsWithRepliesFragment.loaded)
				postsWithRepliesFragment.onRefresh();
			if(pinnedPostsFragment.loaded)
				pinnedPostsFragment.onRefresh();
			if(mediaFragment.loaded)
				mediaFragment.onRefresh();
		}
		V.setVisibilityAnimated(fab, View.VISIBLE);
	}

	@Override
	protected void doLoadData(){
		if (remoteAccount != null) {
			UiUtils.lookupAccountHandle(getContext(), accountID, remoteAccount.getFullyQualifiedName(), (c, args) -> {
				if (getContext() == null) return;
				if (args == null || !args.containsKey("profileAccount")) {
					Toast.makeText(getContext(), getContext().getString(
							R.string.sk_error_loading_profile, domain
					), Toast.LENGTH_SHORT).show();
					Nav.finish(this);
					return;
				}
				onAccountLoaded(Parcels.unwrap(args.getParcelable("profileAccount")));
			});
			return;
		}

		currentRequest=new GetAccountByID(profileAccountID)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(Account result){
						if(getActivity()==null) return;
						onAccountLoaded(result);
					}
				})
				.exec(accountID);
	}

	@Override
	public void onRefresh(){
		if(isInEditMode){
			refreshing=false;
			refreshLayout.setRefreshing(false);
			return;
		}
		if(refreshing)
			return;
		refreshing=true;
		doLoadData();
	}

	@Override
	public void dataLoaded(){
		if(getActivity()==null)
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(account));
		args.putBoolean("__is_tab", true);
		if(postsFragment==null){
			postsFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.DEFAULT, true);
		}
		if(postsWithRepliesFragment==null){
			postsWithRepliesFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.INCLUDE_REPLIES, false);
		}
		if(mediaFragment==null){
			mediaFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.MEDIA, false);
		}
		if(pinnedPostsFragment==null){
			pinnedPostsFragment=new PinnedPostsListFragment();
			pinnedPostsFragment.setArguments(args);
		}
		setFields(fields);
		pager.getAdapter().notifyDataSetChanged();
		super.dataLoaded();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
		// To avoid the callback triggering on first layout with position=0 before anything is instantiated
		pager.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				pager.getViewTreeObserver().removeOnPreDrawListener(this);
				pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
					@Override
					public void onPageSelected(int position){
						Fragment _page=getFragmentForPage(position);
						if(_page instanceof BaseRecyclerFragment<?> page && page.isAdded()){
							if(!page.loaded && !page.isDataLoading())
								page.loadData();
						}
					}

					@Override
					public void onPageScrollStateChanged(int state){
						if(isInEditMode)
							return;
						refreshLayout.setEnabled(state!=ViewPager2.SCROLL_STATE_DRAGGING);
					}
				});
				return true;
			}
		});

		tabsColorBackground=((LayerDrawable)tabbar.getBackground()).findDrawableByLayerId(R.id.color_overlay);

		onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, getToolbar());
		scrollView.setOnScrollChangeListener(this::onScrollChanged);
		scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				scrollView.getViewTreeObserver().removeOnPreDrawListener(this);

				tabBarIsAtTop=!scrollView.canScrollVertically(1) && scrollView.getHeight()>0;
				if (UiUtils.isTrueBlackTheme()) tabBarIsAtTop=false;
				tabsColorBackground.setAlpha(tabBarIsAtTop ? 20 : 0);
				tabbar.setTranslationZ(tabBarIsAtTop ? V.dp(3) : 0);
				tabsDivider.setAlpha(tabBarIsAtTop ? 0 : 1);

				return true;
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		if(postsFragment==null)
			return;
		if(postsFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "posts", postsFragment);
		if(postsWithRepliesFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "postsWithReplies", postsWithRepliesFragment);
		if(mediaFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "media", mediaFragment);
		if(pinnedPostsFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "pinnedPosts", pinnedPostsFragment);
	}

	@Override
	public void onHidden(){
		if (relationship != null && !noteEdit.getText().toString().equals(relationship.note)){
			savePrivateNote(noteEdit.getText().toString());
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(contentView!=null){
			if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
				int insetBottom=insets.getSystemWindowInsetBottom();
				childInsets=insets.inset(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+insetBottom;
				applyChildWindowInsets();
				insets=insets.inset(0, 0, 0, insetBottom);
			}else{
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16);
			}
		}
		super.onApplyWindowInsets(insets);
	}

	private void applyChildWindowInsets(){
		if(postsFragment!=null && postsFragment.isAdded() && childInsets!=null){
			postsFragment.onApplyWindowInsets(childInsets);
			postsWithRepliesFragment.onApplyWindowInsets(childInsets);
			pinnedPostsFragment.onApplyWindowInsets(childInsets);
			mediaFragment.onApplyWindowInsets(childInsets);
		}
	}

	@SuppressLint("SetTextI18n")
	private void bindHeaderView(){
		setTitle(account.getDisplayName());
		setSubtitle(getResources().getQuantityString(R.plurals.x_posts, (int)(account.statusesCount%1000), account.statusesCount));
		ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(
				TextUtils.isEmpty(account.avatar) ? getSession().getDefaultAvatarUrl() :
						GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic,
				V.dp(100), V.dp(100)));
		ViewImageLoader.load(cover, null, new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.header : account.headerStatic, 1000, 1000));
		SpannableStringBuilder ssb=new SpannableStringBuilder(account.getDisplayName());
		if(AccountSessionManager.get(accountID).getLocalPreferences().customEmojiInNames)
			HtmlParser.parseCustomEmoji(ssb, account.emojis);
		name.setText(ssb);
		setTitle(ssb);

		if (account.roles != null && !account.roles.isEmpty()) {
			rolesView.setVisibility(View.VISIBLE);
			rolesView.removeAllViews();
			name.setPadding(0, 0, V.dp(12), 0);
			for (Account.Role role : account.roles) {
				TextView roleText = new TextView(getActivity(), null, 0, R.style.role_label);
				roleText.setText(role.name);
				roleText.setGravity(Gravity.CENTER_VERTICAL);
				if (!TextUtils.isEmpty(role.color) && role.color.startsWith("#")) try {
					GradientDrawable bg = (GradientDrawable) roleText.getBackground().mutate();
					bg.setStroke(V.dp(1), Color.parseColor(role.color));
				} catch (Exception ignored) {}
				rolesView.addView(roleText);
			}
		}

//		boolean isSelf=AccountSessionManager.getInstance().isSelf(accountID, account);

//		String acct = ((isSelf || account.isRemote)
//					? account.getFullyQualifiedName()
//					: account.acct);

		username.setText("@"+account.username);

		String domain=account.getDomain();
		if(TextUtils.isEmpty(domain))
			domain=AccountSessionManager.get(accountID).domain;
		usernameDomain.setText(domain);

		lockIcon.setVisibility(account.locked ? View.VISIBLE : View.GONE);
		lockIcon.setImageTintList(ColorStateList.valueOf(username.getCurrentTextColor()));

		botIcon.setVisibility(account.bot ? View.VISIBLE : View.GONE);
		botIcon.setImageTintList(ColorStateList.valueOf(username.getCurrentTextColor()));

		CharSequence parsedBio=HtmlParser.parse(account.note, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
		if(TextUtils.isEmpty(parsedBio)){
			bio.setVisibility(View.GONE);
		}else{
			bio.setVisibility(View.VISIBLE);
			bio.setText(parsedBio);
		}
		followersCount.setText(UiUtils.abbreviateNumber(account.followersCount));
		followingCount.setText(UiUtils.abbreviateNumber(account.followingCount));
		followersLabel.setText(getResources().getQuantityString(R.plurals.followers, (int)Math.min(999, account.followersCount)));
		followingLabel.setText(getResources().getQuantityString(R.plurals.following, (int)Math.min(999, account.followingCount)));

		if (account.followersCount < 0) followersBtn.setVisibility(View.GONE);
		if (account.followingCount < 0) followingBtn.setVisibility(View.GONE);
		if (account.followersCount < 0 || account.followingCount < 0)
			countersLayout.findViewById(R.id.profile_counters_separator).setVisibility(View.GONE);

		UiUtils.loadCustomEmojiInTextView(name);
		UiUtils.loadCustomEmojiInTextView(bio);

		notifyButton.setVisibility(View.GONE);
		if(AccountSessionManager.getInstance().isSelf(accountID, account)){
			actionButton.setText(R.string.edit_profile);
			TypedArray ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Tonal, new int[]{android.R.attr.background});
			actionButton.setBackground(ta.getDrawable(0));
			ta.recycle();
			ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Tonal, new int[]{android.R.attr.textColor});
			actionButton.setTextColor(ta.getColorStateList(0));
			ta.recycle();
		}else{
			actionButton.setVisibility(View.GONE);
		}

		fields.clear();

		if (account.createdAt != null) {
			AccountField joined=new AccountField();
			joined.parsedName=joined.name=getString(R.string.profile_joined);
			joined.parsedValue=joined.value=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDateTime.ofInstant(account.createdAt, ZoneId.systemDefault()));
			fields.add(joined);
		}

		for(AccountField field:account.fields){
			field.parsedValue=ssb=HtmlParser.parse(field.value, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID, account);
			field.valueEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			ssb=new SpannableStringBuilder(field.name);
			HtmlParser.parseCustomEmoji(ssb, account.emojis);
			field.parsedName=ssb;
			field.nameEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			field.emojiRequests=new ArrayList<>(field.nameEmojis.length+field.valueEmojis.length);
			for(CustomEmojiSpan span:field.nameEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			for(CustomEmojiSpan span:field.valueEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			fields.add(field);
		}

		setFields(fields);
	}

	private void updateToolbar(){
		getToolbar().setOnClickListener(v->scrollToTop());
		getToolbar().setNavigationContentDescription(R.string.back);
		if(onScrollListener!=null){
			onScrollListener.setViews(getToolbar());
		}
	}

	@Override
	protected boolean wantsToolbarMenuIconsTinted() {
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(isOwnProfile && isInEditMode){
			editSaveMenuItem=menu.add(0, R.id.save, 0, R.string.save_changes);
			editSaveMenuItem.setIcon(R.drawable.ic_fluent_save_24_regular);
			editSaveMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			editSaveMenuItem.setVisible(!isActionButtonInView());
			return;
		}
		if(relationship==null && !isOwnProfile)
			return;
		inflater.inflate(isOwnProfile ? R.menu.profile_own : R.menu.profile, menu);
		if(isOwnProfile){
			UiUtils.enableOptionsMenuIcons(getActivity(), menu, R.id.scheduled, R.id.bookmarks);
		}else{
			UiUtils.enableOptionsMenuIcons(getActivity(), menu, R.id.edit_note);
		}
		boolean hasMultipleAccounts = AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1;
		menu.findItem(R.id.open_with_account).setVisible(hasMultipleAccounts);

		if(isOwnProfile) {
			if (isInstancePixelfed()) menu.findItem(R.id.scheduled).setVisible(false);
			menu.findItem(R.id.favorites).setIcon(GlobalUserPreferences.likeIcon ? R.drawable.ic_fluent_heart_20_regular : R.drawable.ic_fluent_star_20_regular);
			UiUtils.insetPopupMenuIcon(getContext(), menu.findItem(R.id.favorites));
			return;
		}

		menu.findItem(R.id.manage_user_lists).setTitle(getString(R.string.sk_lists_with_user, account.getShortUsername()));
		MenuItem mute=menu.findItem(R.id.mute);
		mute.setTitle(getString(relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getShortUsername()));
		mute.setIcon(relationship.muting ? R.drawable.ic_fluent_speaker_2_24_regular : R.drawable.ic_fluent_speaker_off_24_regular);
		UiUtils.insetPopupMenuIcon(getContext(), mute);
		menu.findItem(R.id.block).setTitle(getString(relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getShortUsername()));
		menu.findItem(R.id.report).setTitle(getString(R.string.report_user, account.getShortUsername()));
		menu.findItem(R.id.manage_user_lists).setVisible(relationship.following);
		menu.findItem(R.id.soft_block).setVisible(relationship.followedBy && !relationship.following);
		MenuItem hideBoosts=menu.findItem(R.id.hide_boosts);
		if (relationship.following) {
			hideBoosts.setTitle(getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user, account.getShortUsername()));
			hideBoosts.setIcon(relationship.showingReblogs ? R.drawable.ic_fluent_arrow_repeat_all_off_24_regular : R.drawable.ic_fluent_arrow_repeat_all_24_regular);
			UiUtils.insetPopupMenuIcon(getContext(), hideBoosts);
			hideBoosts.setVisible(true);
		} else {
			hideBoosts.setVisible(false);
		}
		MenuItem blockDomain=menu.findItem(R.id.block_domain);
		if(!account.isLocal()){
			blockDomain.setTitle(getString(relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
			blockDomain.setVisible(true);
		}else{
			blockDomain.setVisible(false);
		}
		boolean canAddNote = noteWrap.getVisibility()==View.GONE && (relationship.note==null || relationship.note.isEmpty());
		menu.findItem(R.id.edit_note).setTitle(canAddNote ? R.string.sk_add_note : R.string.sk_delete_note);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if(id==R.id.share){
			UiUtils.openSystemShareSheet(getActivity(), account);
		}else if(id==R.id.mute){
			UiUtils.confirmToggleMuteUser(getActivity(), accountID, account, relationship.muting, this::updateRelationship);
		}else if(id==R.id.block){
			UiUtils.confirmToggleBlockUser(getActivity(), accountID, account, relationship.blocking, this::updateRelationship);
		}else if(id==R.id.soft_block){
			UiUtils.confirmSoftBlockUser(getActivity(), accountID, account, this::updateRelationship);
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(account));
			args.putParcelable("relationship", Parcels.wrap(relationship));
			Nav.go(getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(getActivity(), account.url);
		}else if(id==R.id.block_domain){
			UiUtils.confirmToggleBlockDomain(getActivity(), accountID, account.getDomain(), relationship.domainBlocking, ()->{
				relationship.domainBlocking=!relationship.domainBlocking;
				updateRelationship();
			});
		}else if(id==R.id.hide_boosts){
			new SetAccountFollowed(account.id, true, !relationship.showingReblogs, relationship.notifying)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, false)
					.exec(accountID);
		}else if(id==R.id.bookmarks){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), BookmarkedStatusListFragment.class, args);
		}else if(id==R.id.favorites){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), FavoritedStatusListFragment.class, args);
		}else if(id==R.id.manage_user_lists){
			final Bundle args=new Bundle();
			args.putString("account", accountID);
			if (!isOwnProfile) {
				args.putString("profileAccount", profileAccountID);
				args.putString("profileDisplayUsername", account.getDisplayUsername());
			}
			Nav.go(getActivity(), ListsFragment.class, args);
		}else if(id==R.id.muted_accounts){
			final Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			Nav.go(getActivity(), MutedAccountsListFragment.class, args);
		}else if(id==R.id.blocked_accounts){
			final Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			Nav.go(getActivity(), BlockedAccountsListFragment.class, args);
		}else if(id==R.id.followed_hashtags){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), FollowedHashtagsFragment.class, args);
		}else if(id==R.id.scheduled){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), ScheduledStatusListFragment.class, args);
		}else if(id==R.id.save){
			if(isInEditMode)
				saveAndExitEditMode();
		}else if(id==R.id.edit_note){
			if(noteWrap.getVisibility()==View.GONE){
				showPrivateNote();
				UiUtils.beginLayoutTransition(scrollableContent);
				noteEdit.requestFocus();
				noteEdit.postDelayed(()->{
					InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
					imm.showSoftInput(noteEdit, 0);
				}, 100);
			}else if(relationship.note.isEmpty() && noteEdit.getText().toString().isEmpty()){
				hidePrivateNote();
				noteEdit.clearFocus();
				noteEdit.postDelayed(()->{
					InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
					imm.hideSoftInputFromWindow(noteEdit.getWindowToken(), 0);
				}, 100);
				UiUtils.beginLayoutTransition(scrollableContent);
			}else{
				new M3AlertDialogBuilder(getActivity())
						.setMessage(getContext().getString(R.string.sk_private_note_confirm_delete, account.getDisplayUsername()))
						.setPositiveButton(R.string.delete, (dlg, btn)->savePrivateNote(null))
						.setNegativeButton(R.string.cancel, null)
						.show();
			}
			invalidateOptionsMenu();
		}else if(id==R.id.open_with_account){
			UiUtils.pickAccount(getActivity(), accountID, R.string.sk_open_with_account, R.drawable.ic_fluent_person_swap_24_regular, session ->UiUtils.openURL(
					getActivity(), session.getID(), account.url, false
			), null);
		}
		return true;
	}

	private void loadRelationship(){
		new GetAccountRelationships(Collections.singletonList(account.id))
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						if(!result.isEmpty()){
							relationship=result.get(0);
							updateRelationship();
						}
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(accountID);
	}

	private void updateRelationship(){
		if(getActivity()==null) return;
		if(relationship.note!=null && !relationship.note.isEmpty()) showPrivateNote();
		else hidePrivateNote();
		invalidateOptionsMenu();
		actionButton.setVisibility(View.VISIBLE);
		notifyButton.setVisibility(relationship.following && !isInstanceIceshrimpJs() ? View.VISIBLE : View.GONE); // always hide notify button on Iceshrimp-JS because it's unsupported on the server
		UiUtils.setRelationshipToActionButtonM3(relationship, actionButton);
		actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		notifyProgress.setIndeterminateTintList(notifyButton.getTextColors());
		followsYouView.setVisibility(relationship.followedBy ? View.VISIBLE : View.GONE);
		notifyButton.setSelected(relationship.notifying);
		notifyButton.setContentDescription(getString(relationship.notifying ? R.string.sk_user_post_notifications_on : R.string.sk_user_post_notifications_off, '@'+account.username));
		noteEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		UiUtils.beginLayoutTransition(scrollableContent);
	}

	public ImageButton getFab() {
		return fab;
	}

	@Override
	public void showFab() {
		if (getFragmentForPage(pager.getCurrentItem()) instanceof HasFab fabulous) fabulous.showFab();
	}

	@Override
	public void hideFab() {
		if (getFragmentForPage(pager.getCurrentItem()) instanceof HasFab fabulous) fabulous.hideFab();
	}

	@Override
	public boolean isScrolling() {
		return getFragmentForPage(pager.getCurrentItem()) instanceof HasFab fabulous
				&& fabulous.isScrolling();
	}

	private void onScrollChanged(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){
		if(scrollY>cover.getHeight()){
			cover.setTranslationY(scrollY-(cover.getHeight()));
			cover.setTranslationZ(V.dp(10));
			cover.setTransform(cover.getHeight()/2f);
		}else{
			cover.setTranslationY(0f);
			cover.setTranslationZ(0f);
			cover.setTransform(scrollY/2f);
		}
		cover.invalidate();
		if(currentPhotoViewer!=null){
			currentPhotoViewer.offsetView(0, oldScrollY-scrollY);
		}
		onScrollListener.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY);

		boolean newTabBarIsAtTop=!scrollView.canScrollVertically(1);
		if(newTabBarIsAtTop!=tabBarIsAtTop){
			if(UiUtils.isTrueBlackTheme()) newTabBarIsAtTop=false;
			tabBarIsAtTop=newTabBarIsAtTop;

			if(tabBarIsAtTop){
				// ScrollView would sometimes leave 1 pixel unscrolled, force it into the correct scrollY
				int maxY=scrollView.getChildAt(0).getHeight()-scrollView.getHeight();
				if(scrollView.getScrollY()!=maxY)
					scrollView.scrollTo(0, maxY);
			}

			if(tabBarColorAnim!=null)
				tabBarColorAnim.cancel();
			AnimatorSet set=new AnimatorSet();
			set.playTogether(
					ObjectAnimator.ofInt(tabsColorBackground, "alpha", tabBarIsAtTop ? 20 : 0),
					ObjectAnimator.ofFloat(tabbar, View.TRANSLATION_Z, tabBarIsAtTop ? V.dp(3) : 0),
					ObjectAnimator.ofFloat(getToolbar(), View.TRANSLATION_Z, tabBarIsAtTop ? 0 : V.dp(3)),
					ObjectAnimator.ofFloat(tabsDivider, View.ALPHA, tabBarIsAtTop ? 0 : 1)
			);
			set.setDuration(150);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					tabBarColorAnim=null;
				}
			});
			tabBarColorAnim=set;
			set.start();
		}
		if(isInEditMode && editSaveMenuItem!=null){
			boolean buttonInView=isActionButtonInView();
			if(buttonInView==editSaveMenuItem.isVisible()){
				editSaveMenuItem.setVisible(!buttonInView);
			}
		}
	}

	private Fragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> postsFragment;
			case 1 -> postsWithRepliesFragment;
			case 2 -> pinnedPostsFragment;
			case 3 -> mediaFragment;
			default -> throw new IllegalStateException();
		};
	}

	private RecyclerView getScrollableRecyclerView(){
		return isInEditMode ? list :
				getFragmentForPage(pager.getCurrentItem()).getView().findViewById(R.id.list);
	}

	private void onActionButtonClick(View v){
		if(isOwnProfile){
			if(!isInEditMode)
				loadAccountInfoAndEnterEditMode();
			else
				saveAndExitEditMode();
		}else{
			UiUtils.performAccountAction(getActivity(), account, accountID, relationship, actionButton, this::setActionProgressVisible, this::updateRelationship);
		}
	}

	private boolean onActionButtonLongClick(View v) {
		if (isOwnProfile || AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
		UiUtils.pickAccount(getActivity(), accountID, R.string.sk_follow_as, R.drawable.ic_fluent_person_add_28_regular, session -> {
			UiUtils.lookupAccount(getActivity(), account, session.getID(), accountID, acc -> {
				if (acc == null) return;
				new SetAccountFollowed(acc.id, true, true).setCallback(new Callback<>() {
					@Override
					public void onSuccess(Relationship relationship) {
						Toast.makeText(
								getActivity(),
								getString(R.string.sk_followed_as, session.self.getShortUsername()),
								Toast.LENGTH_SHORT
						).show();
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(getActivity());
					}
				}).exec(session.getID());
			});
		}, null);
		return true;
	}

	private void setActionProgressVisible(boolean visible){
		actionButton.setTextVisible(!visible);
		actionProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(visible)
			actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		actionButton.setClickable(!visible);
	}

	private void setNotifyProgressVisible(boolean visible){
		notifyButton.setTextVisible(!visible);
		notifyProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(visible)
			notifyProgress.setIndeterminateTintList(notifyButton.getTextColors());
		notifyButton.setClickable(!visible);
	}

	private void loadAccountInfoAndEnterEditMode(){
		if(editModeLoading)
			return;
		editModeLoading=true;
		setActionProgressVisible(true);
		new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						editModeLoading=false;
						if(getActivity()==null)
							return;
						enterEditMode(result);
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						editModeLoading=false;
						if(getActivity()==null)
							return;
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
	}

	private void updateMetadataHeight() {
		ViewGroup.LayoutParams params = list.getLayoutParams();
		int desiredHeight = isInEditMode ? scrollView.getHeight() : ViewGroup.LayoutParams.WRAP_CONTENT;
		if (params.height == desiredHeight) return;
		params.height = desiredHeight;
		list.requestLayout();
	}

	private void enterEditMode(Account account){
		if(isInEditMode)
			throw new IllegalStateException();
		isInEditMode=true;
		adapter.notifyDataSetChanged();
		dragHelper.attachToRecyclerView(list);
		editDirty=false;
		invalidateOptionsMenu();
		actionButton.setText(R.string.save_changes);
		pager.setVisibility(View.GONE);
		tabbar.setVisibility(View.GONE);
		Drawable overlay=getResources().getDrawable(R.drawable.edit_avatar_overlay, getActivity().getTheme()).mutate();
		avatar.setForeground(overlay);
		updateMetadataHeight();

		Toolbar toolbar=getToolbar();
		Drawable close=getToolbarContext().getDrawable(R.drawable.ic_fluent_dismiss_24_regular).mutate();
		close.setTint(UiUtils.getThemeColor(getToolbarContext(), R.attr.colorM3OnSurfaceVariant));
		toolbar.setNavigationIcon(close);
		toolbar.setNavigationContentDescription(R.string.discard);

		ViewGroup parent=contentView.findViewById(R.id.scrollable_content);
		TransitionManager.beginDelayedTransition(parent, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
		);

		name.setVisibility(View.GONE);
		rolesView.setVisibility(View.GONE);
		usernameWrap.setVisibility(View.GONE);
		bio.setVisibility(View.GONE);
		countersLayout.setVisibility(View.GONE);

		nameEditWrap.setVisibility(View.VISIBLE);
		nameEdit.setText(account.displayName);

		bioEditWrap.setVisibility(View.VISIBLE);
		bioEdit.setText(account.source.note);

		refreshLayout.setEnabled(false);
		editDirty=false;
		V.setVisibilityAnimated(fab, View.GONE);

		fields = account.source.fields;
		adapter.notifyDataSetChanged();
	}

	private void exitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		isInEditMode=false;

		invalidateOptionsMenu();
		actionButton.setText(R.string.edit_profile);
		avatar.setForeground(null);

		Toolbar toolbar=getToolbar();
		if(canGoBack()){
			Drawable back=getToolbarContext().getDrawable(R.drawable.ic_fluent_arrow_left_24_regular).mutate();
			back.setTint(UiUtils.getThemeColor(getToolbarContext(), R.attr.colorM3OnSurfaceVariant));
			toolbar.setNavigationIcon(back);
			toolbar.setNavigationContentDescription(0);
		}else{
			toolbar.setNavigationIcon(null);
		}
		editSaveMenuItem=null;

		ViewGroup parent=contentView.findViewById(R.id.scrollable_content);
		TransitionManager.beginDelayedTransition(parent, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
		);
		nameEditWrap.setVisibility(View.GONE);
		bioEditWrap.setVisibility(View.GONE);
		name.setVisibility(View.VISIBLE);
		rolesView.setVisibility(View.VISIBLE);
		usernameWrap.setVisibility(View.VISIBLE);
		bio.setVisibility(View.VISIBLE);
		countersLayout.setVisibility(View.VISIBLE);
		refreshLayout.setEnabled(true);
		pager.setVisibility(View.VISIBLE);
		tabbar.setVisibility(View.VISIBLE);
		updateMetadataHeight();

		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		imm.hideSoftInputFromWindow(content.getWindowToken(), 0);
		V.setVisibilityAnimated(fab, View.VISIBLE);
		bindHeaderView();
		V.setVisibilityAnimated(fab, View.VISIBLE);
	}

	private void saveAndExitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		setActionProgressVisible(true);
		savingEdits=true;
		new UpdateAccountCredentials(nameEdit.getText().toString(), bioEdit.getText().toString(), editNewAvatar, editNewCover, fields)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						savingEdits=false;
						account=result;
						AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
						if(getActivity()==null) return;
						exitEditMode();
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						savingEdits=false;
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
	}

	private void updateRelationship(Relationship r){
		relationship=r;
		updateRelationship();
	}

	@Override
	public boolean onBackPressed(){
		if(noteEdit.hasFocus()) {
			savePrivateNote(noteEdit.getText().toString());
		}
		if(isInEditMode){
			if(savingEdits)
				return true;
			if(editDirty){
				new M3AlertDialogBuilder(getActivity())
						.setTitle(R.string.discard_changes)
						.setPositiveButton(R.string.discard, (dlg, btn)->exitEditMode())
						.setNegativeButton(R.string.cancel, null)
						.show();
			}else{
				exitEditMode();
			}
			return true;
		}
		return false;
	}

	private List<Attachment> createFakeAttachments(String url, Drawable drawable){
		Attachment att=new Attachment();
		att.type=Attachment.Type.IMAGE;
		att.url=url;
		att.meta=new Attachment.Metadata();
		att.meta.width=drawable.getIntrinsicWidth();
		att.meta.height=drawable.getIntrinsicHeight();
		return Collections.singletonList(att);
	}

	private void onNotifyButtonClick(View v) {
		UiUtils.performToggleAccountNotifications(getActivity(), account, accountID, relationship, actionButton, this::setNotifyProgressVisible, this::updateRelationship);
	}

	private void onAvatarClick(View v){
		if(isInEditMode){
			startImagePicker(AVATAR_RESULT);
		}else{
			Drawable ava=avatar.getDrawable();
			if(ava==null)
				return;
			int radius=V.dp(25);
			currentPhotoViewer=new PhotoViewer(getActivity(), createFakeAttachments(TextUtils.isEmpty(account.avatar) ? getSession().getDefaultAvatarUrl() : account.avatar, ava), 0,
					null, accountID, new SingleImagePhotoViewerListener(avatar, avatarBorder, new int[]{radius, radius, radius, radius}, this, ()->currentPhotoViewer=null, ()->ava, null, null));
		}
	}

	private void onCoverClick(View v){
		if(isInEditMode){
			startImagePicker(COVER_RESULT);
		}else{
			Drawable drawable=cover.getDrawable();
			if(drawable==null || drawable instanceof ColorDrawable)
				return;
			currentPhotoViewer=new PhotoViewer(getActivity(), createFakeAttachments(account.header, drawable), 0,
					null, accountID, new SingleImagePhotoViewerListener(cover, cover, null, this, ()->currentPhotoViewer=null, ()->drawable, ()->avatarBorder.setTranslationZ(2), ()->avatarBorder.setTranslationZ(0)));
		}
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		if(getPrefilledText() != null) args.putString("prefilledText", getPrefilledText());
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void startImagePicker(int requestCode){
		Intent intent=UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1);
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode==Activity.RESULT_OK){
			if(requestCode==AVATAR_RESULT){
				editNewAvatar=data.getData();
				ViewImageLoader.loadWithoutAnimation(avatar, null, new UrlImageLoaderRequest(editNewAvatar, V.dp(100), V.dp(100)));
				editDirty=true;
			}else if(requestCode==COVER_RESULT){
				editNewCover=data.getData();
				ViewImageLoader.loadWithoutAnimation(cover, null, new UrlImageLoaderRequest(editNewCover, V.dp(1000), V.dp(1000)));
				editDirty=true;
			}
		}
	}

	@Override
	public void scrollToTop(){
		getScrollableRecyclerView().scrollToPosition(0);
		scrollView.smoothScrollTo(0, 0);
	}

	private void onFollowersOrFollowingClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(account));
		Class<? extends Fragment> cls;
		if(v.getId()==R.id.followers_btn)
			cls=FollowerListFragment.class;
		else if(v.getId()==R.id.following_btn)
			cls=FollowingListFragment.class;
		else
			return;
		Nav.go(getActivity(), cls, args);
	}

	private boolean isActionButtonInView(){
		return actionButton.getVisibility()==View.VISIBLE && actionButtonWrap.getTop()+actionButtonWrap.getHeight()>scrollView.getScrollY();
	}

	private class ProfilePagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=new FrameLayout(parent.getContext());
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
			Fragment fragment=getFragmentForPage(position);
			FrameLayout fragmentView=tabViews[position];
			fragmentView.setVisibility(View.VISIBLE);
			if(fragmentView.getParent() instanceof ViewGroup parent)
				parent.removeView(fragmentView);
			((FrameLayout)holder.itemView).addView(fragmentView);
			if(!fragment.isAdded()){
				getChildFragmentManager().beginTransaction().add(fragmentView.getId(), fragment).commit();
				holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						getChildFragmentManager().executePendingTransactions();
						if(fragment.isAdded()){
							holder.itemView.getViewTreeObserver().removeOnPreDrawListener(this);
							applyChildWindowInsets();
						}
						return true;
					}
				});
			}
		}

		@Override
		public int getItemCount(){
			return loaded ? tabViews.length : 0;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}

	@Override
	public void onProvideAssistContent(AssistContent assistContent) {
		callFragmentToProvideAssistContent(getFragmentForPage(pager.getCurrentItem()), assistContent);
	}

	@Override
	public String getAccountID() {
		return accountID;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return Uri.parse(account.url);
	}

	// from ProfileAboutFragment
	public void setFields(List<AccountField> fields){
		this.fields=fields;
		if(isInEditMode){
			isInEditMode=false;
//			dragHelper.attachToRecyclerView(null);
		}
		if(adapter!=null)
			adapter.notifyDataSetChanged();
	}

	private class AboutAdapter extends UsableRecyclerView.Adapter<BaseViewHolder> implements ImageLoaderRecyclerAdapter {
		public AboutAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return switch(viewType){
				case 0 -> new AboutViewHolder();
				case 1 -> new EditableAboutViewHolder();
				case 2 -> new AddRowViewHolder();
				default -> throw new IllegalStateException("Unexpected value: "+viewType);
			};
		}

		@Override
		public void onBindViewHolder(BaseViewHolder holder, int position){
			if(position<fields.size()){
				holder.bind(fields.get(position));
			}else{
				holder.bind(null);
			}
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			if(isInEditMode){
				int size=fields.size();
				if(size<maxFields)
					size++;
				return size;
			}
			return fields.size();
		}

		@Override
		public int getItemViewType(int position){
			if(isInEditMode){
				return position==fields.size() ? 2 : 1;
			}
			return 0;
		}

		@Override
		public int getImageCountForItem(int position){
			return isInEditMode || fields.get(position).emojiRequests==null ? 0 : fields.get(position).emojiRequests.size();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return fields.get(position).emojiRequests.get(image);
		}
	}

	private abstract class BaseViewHolder extends BindableViewHolder<AccountField> {
		public BaseViewHolder(int layout){
			super(getActivity(), layout, list);
		}

		@Override
		public void onBind(AccountField item){
		}
	}

	private class AboutViewHolder extends BaseViewHolder implements ImageLoaderViewHolder{
		private final TextView title;
		private final LinkedTextView value;

		public AboutViewHolder(){
			super(R.layout.item_profile_about);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
		}

		@Override
		public void onBind(AccountField item){
			super.onBind(item);
			title.setText(item.parsedName);
			value.setText(item.parsedValue);
			if(item.verifiedAt!=null){
				int textColor=UiUtils.getThemeColor(getContext(), R.attr.colorM3Success);
				value.setTextColor(textColor);
				value.setLinkTextColor(textColor);
				Drawable check=getResources().getDrawable(R.drawable.ic_fluent_checkmark_starburst_20_regular, getActivity().getTheme()).mutate();
				check.setTint(textColor);
				value.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, check, null);
			}else{
				value.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
				value.setLinkTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorAccent));
				value.setCompoundDrawables(null, null, null, null);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			CustomEmojiSpan span=index>=item.nameEmojis.length ? item.valueEmojis[index-item.nameEmojis.length] : item.nameEmojis[index];
			span.setDrawable(image);
			title.setText(title.getText());
			value.setText(value.getText());
			toolbarTitleView.setText(toolbarTitleView.getText());
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}

	private class EditableAboutViewHolder extends BaseViewHolder {
		private final EditText title;
		private final EditText value;
		private boolean ignoreTextChange;

		public EditableAboutViewHolder(){
			super(R.layout.onboarding_profile_field);
			title=findViewById(R.id.title);
			value=findViewById(R.id.content);
			findViewById(R.id.dragger_thingy).setOnLongClickListener(v->{
				dragHelper.startDrag(this);
				return true;
			});
			title.addTextChangedListener(new SimpleTextWatcher(e->{
				item.name=e.toString();
				if(!ignoreTextChange)
					editDirty=true;
			}));
			value.addTextChangedListener(new SimpleTextWatcher(e->{
				item.value=e.toString();
				if(!ignoreTextChange)
					editDirty=true;
			}));
			findViewById(R.id.delete).setOnClickListener(this::onRemoveRowClick);
		}

		@Override
		public void onBind(AccountField item){
			super.onBind(item);
			ignoreTextChange=true;
			title.setText(item.name);
			value.setText(item.value);
			ignoreTextChange=false;
		}

		private void onRemoveRowClick(View v){
			int pos=getAbsoluteAdapterPosition();
			fields.remove(pos);
			adapter.notifyItemRemoved(pos);
			for(int i=0;i<list.getChildCount();i++){
				BaseViewHolder vh=(BaseViewHolder) list.getChildViewHolder(list.getChildAt(i));
				vh.rebind();
			}
			list.measure(0, 0);
		}
	}

	private class AddRowViewHolder extends BaseViewHolder implements UsableRecyclerView.Clickable{
		public AddRowViewHolder(){
			super(R.layout.item_profile_about_add_row);
		}

		@Override
		public void onClick(){
			fields.add(new AccountField());
			if(fields.size()==maxFields){ // replace this row with new row
				adapter.notifyItemChanged(fields.size()-1);
			}else{
				adapter.notifyItemInserted(fields.size()-1);
				rebind();
			}
			list.measure(0, 0);
		}
	}

	private class ReorderCallback extends ItemTouchHelper.SimpleCallback{
		public ReorderCallback(){
			super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target){
			if(target instanceof AddRowViewHolder)
				return false;
			int fromPosition=viewHolder.getAbsoluteAdapterPosition();
			int toPosition=target.getAbsoluteAdapterPosition();
			if (fromPosition<toPosition) {
				for (int i=fromPosition;i<toPosition;i++) {
					Collections.swap(fields, i, i+1);
				}
			} else {
				for (int i=fromPosition;i>toPosition;i--) {
					Collections.swap(fields, i, i-1);
				}
			}
			adapter.notifyItemMoved(fromPosition, toPosition);
			((BindableViewHolder<?>)viewHolder).rebind();
			((BindableViewHolder<?>)target).rebind();
			return true;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction){

		}

		@Override
		public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState){
			super.onSelectedChanged(viewHolder, actionState);
			if(actionState==ItemTouchHelper.ACTION_STATE_DRAG){
				viewHolder.itemView.setTag(me.grishka.appkit.R.id.item_touch_helper_previous_elevation, viewHolder.itemView.getElevation()); // prevents the default behavior of changing elevation in onDraw()
				viewHolder.itemView.animate().translationZ(V.dp(1)).setDuration(200).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
			}
		}

		@Override
		public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder){
			super.clearView(recyclerView, viewHolder);
			viewHolder.itemView.animate().translationZ(0).setDuration(100).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
		}

		@Override
		public boolean isLongPressDragEnabled(){
			return false;
		}
	}
}
