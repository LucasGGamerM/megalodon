package org.joinmastodon.android.fragments.onboarding;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Objects;

import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class CustomWelcomeFragment extends InstanceCatalogFragment {
	private View headerView;
	protected MergeRecyclerAdapter mergeAdapter;

	public CustomWelcomeFragment() {
		super(R.layout.fragment_welcome_custom, 1);
	}

	@Override
	public void onAttach(Context context){
		super.onAttach(context);
		setRefreshEnabled(false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		dataLoaded();
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();

		if (!canGoBack()) {
			ImageView toolbarLogo=new ImageView(getActivity());
			toolbarLogo.setScaleType(ImageView.ScaleType.CENTER);
			toolbarLogo.setImageResource(R.drawable.logo);
			toolbarLogo.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary)));

			FrameLayout logoWrap=new FrameLayout(getActivity());
			FrameLayout.LayoutParams logoParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			logoParams.setMargins(0, V.dp(2), 0, 0);
			logoWrap.addView(toolbarLogo, logoParams);

			getToolbar().addView(logoWrap, new Toolbar.LayoutParams(Gravity.CENTER));
		} else {
			setTitle(R.string.add_account);
		}
	}

	@Override
	protected void proceedWithAuthOrSignup(Instance instance) {
		AccountSessionManager.getInstance().authenticate(getActivity(), instance);
	}

	@Override
	protected void updateFilteredList(){
		String query=getCurrentSearchQuery();
		boolean addFakeInstance=query.length()>0 && query.matches("^\\S+\\.[^\\.]+$");
		if(addFakeInstance){
			fakeInstance.domain=fakeInstance.normalizedDomain=query;
			fakeInstance.description=getString(R.string.loading_instance);
			if(filteredData.size()>0 && filteredData.get(0)==fakeInstance){
				if(list.findViewHolderForAdapterPosition(1) instanceof InstanceViewHolder ivh){
					ivh.rebind();
				}
			}
			if(filteredData.isEmpty()){
				filteredData.add(fakeInstance);
				adapter.notifyItemInserted(0);
			}
		}
		ArrayList<CatalogInstance> prevData=new ArrayList<>(filteredData);
		filteredData.clear();
		if(query.length()>0){
			boolean foundExactMatch=false;
			for(CatalogInstance inst:data){
				if(inst.normalizedDomain.contains(query)){
					filteredData.add(inst);
					if(inst.normalizedDomain.equals(query))
						foundExactMatch=true;
				}
			}
			if(!foundExactMatch && addFakeInstance) {
				filteredData.add(0, fakeInstance);
				adapter.notifyItemChanged(0);
			}
		}
		UiUtils.updateList(prevData, filteredData, list, adapter, Objects::equals);
		for(int i=0;i<list.getChildCount();i++){
			list.getChildAt(i).invalidateOutline();
		}
	}

	protected String getCurrentSearchQuery(){
		String[] parts=currentSearchQuery.split("@");
		return parts.length>0 ? parts[parts.length-1] : "";
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
		list.setItemAnimator(new BetterItemAnimator());
		((UsableRecyclerView) list).setSelector(null);
	}

	@Override
	protected void doLoadData(int offset, int count) {}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		headerView=getActivity().getLayoutInflater().inflate(R.layout.header_welcome_custom, list, false);
		searchEdit=headerView.findViewById(R.id.search_edit);
		searchEdit.setOnEditorActionListener(this::onSearchEnterPressed);

		headerView.findViewById(R.id.more).setVisibility(View.GONE);
//		headerView.findViewById(R.id.visibility).setVisibility(View.GONE);
//		headerView.findViewById(R.id.unread_indicator).setVisibility(View.GONE);
//		headerView.findViewById(R.id.separator).setVisibility(View.GONE);
//		headerView.findViewById(R.id.time).setVisibility(View.GONE);
		// FIXME: make a custom header
		((TextView) headerView.findViewById(R.id.time_and_username)).setText(R.string.mo_app_username);
		((TextView) headerView.findViewById(R.id.name)).setText(R.string.mo_app_name);
		((ImageView) headerView.findViewById(R.id.avatar)).setImageDrawable(getActivity().getDrawable(R.mipmap.ic_launcher));
		((FragmentStackActivity) getActivity()).invalidateSystemBarColors(this);

		searchEdit.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				nextButton.setEnabled(false);
				chosenInstance = null;
				searchEdit.removeCallbacks(searchDebouncer);
				searchEdit.postDelayed(searchDebouncer, 300);
			}

			@Override
			public void afterTextChanged(Editable s){}
		});

		mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		mergeAdapter.addAdapter(adapter=new InstancesAdapter());
		View spacer = new Space(getActivity());
		spacer.setMinimumHeight(V.dp(8));
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(spacer));
		return mergeAdapter;
	}

	private class InstancesAdapter extends UsableRecyclerView.Adapter<InstanceViewHolder> {
		public InstancesAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public InstanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new InstanceViewHolder();
		}

		@Override
		public void onBindViewHolder(InstanceViewHolder holder, int position){
			holder.bind(filteredData.get(position));
			chosenInstance = filteredData.get(position);
			if (chosenInstance != fakeInstance) nextButton.setEnabled(true);
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return filteredData.size();
		}

		@Override
		public int getItemViewType(int position){
			return -1;
		}
	}

	private class InstanceViewHolder extends BindableViewHolder<CatalogInstance> implements UsableRecyclerView.Clickable{
		private final TextView title, description, userCount, lang;

		public InstanceViewHolder(){
			super(getActivity(), R.layout.item_instance_custom, list);
			title=findViewById(R.id.title);
			description=findViewById(R.id.description);
			userCount=findViewById(R.id.user_count);
			lang=findViewById(R.id.lang);
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N){
				UiUtils.fixCompoundDrawableTintOnAndroid6(userCount);
				UiUtils.fixCompoundDrawableTintOnAndroid6(lang);
			}
		}

		@Override
		public void onBind(CatalogInstance item){
			title.setText(item.normalizedDomain);
			description.setText(item.description);
			if (item == fakeInstance) {
				userCount.setVisibility(View.GONE);
				lang.setVisibility(View.GONE);
			} else {
				userCount.setVisibility(View.VISIBLE);
				lang.setVisibility(View.VISIBLE);
				userCount.setText(UiUtils.abbreviateNumber(item.totalUsers));
				lang.setText(item.language.toUpperCase());
			}
		}

		@Override
		public void onClick(){
			if(chosenInstance==null)
				nextButton.setEnabled(true);
			chosenInstance=item;
			loadInstanceInfo(chosenInstance.domain, false);
			onNextClick(null);
		}
	}
}
