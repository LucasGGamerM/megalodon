package org.joinmastodon.android.api;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.BaseModel;
import org.joinmastodon.android.model.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import okhttp3.Call;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class MastodonAPIRequest<T> extends APIRequest<T>{
	private static final String TAG="MastodonAPIRequest";

	private static MastodonAPIController unauthenticatedApiController=new MastodonAPIController(null);

	private String domain;
	private AccountSession account;
	private String path;
	private String method;
	private Object requestBody;
	private List<Pair<String, String>> queryParams;
	Class<T> respClass;
	TypeToken<T> respTypeToken;
	Call okhttpCall;
	Token token;
	boolean canceled, isRemote;
	Map<String, String> headers;
	long timeout;
	private ProgressDialog progressDialog;
	protected boolean removeUnsupportedItems;
	@Nullable Context context;

	public MastodonAPIRequest(HttpMethod method, String path, Class<T> respClass){
		this.path=path;
		this.method=method.toString();
		this.respClass=respClass;
	}

	public MastodonAPIRequest(HttpMethod method, String path, TypeToken<T> respTypeToken){
		this.path=path;
		this.method=method.toString();
		this.respTypeToken=respTypeToken;
	}

	@Override
	public synchronized void cancel(){
		if(BuildConfig.DEBUG)
			Log.d(TAG, "canceling request "+this);
		canceled=true;
		if(okhttpCall!=null){
			okhttpCall.cancel();
		}
	}

	@Override
	public APIRequest<T> exec(){
		throw new UnsupportedOperationException("Use exec(accountID) or execNoAuth(domain)");
	}

	public MastodonAPIRequest<T> exec(String accountID){
		try{
			account=AccountSessionManager.getInstance().getAccount(accountID);
			domain=account.domain;
			account.getApiController().submitRequest(this);
		}catch(Exception x){
			Log.e(TAG, "exec: this shouldn't happen, but it still did", x);
			invokeErrorCallback(new MastodonErrorResponse(x.getLocalizedMessage(), -1, x));
		}
		return this;
	}

	public MastodonAPIRequest<T> execNoAuth(String domain){
		this.domain=domain;
		unauthenticatedApiController.submitRequest(this);
		return this;
	}

	public MastodonAPIRequest<T> exec(String domain, Token token){
		this.domain=domain;
		this.token=token;
		unauthenticatedApiController.submitRequest(this);
		return this;
	}

	public MastodonAPIRequest<T> execRemote(String domain) {
		return execRemote(domain, null);
	}

	public MastodonAPIRequest<T> execRemote(String domain, @Nullable AccountSession remoteSession) {
		this.isRemote = true;
		return Optional.ofNullable(remoteSession)
				.or(() -> AccountSessionManager.getInstance().getLoggedInAccounts().stream()
						.filter(acc -> acc.domain.equals(domain))
						.findAny())
				.map(AccountSession::getID)
				.map(this::exec)
				.orElseGet(() -> this.execNoAuth(domain));
	}

	public MastodonAPIRequest<T> wrapProgress(Context context, @StringRes int message, boolean cancelable){
		return wrapProgress(context, message, cancelable, null);
	}

	public MastodonAPIRequest<T> wrapProgress(Context context, @StringRes int message, boolean cancelable, Consumer<ProgressDialog> transform){
		progressDialog=new ProgressDialog(context);
		progressDialog.setMessage(context.getString(message));
		progressDialog.setCancelable(cancelable);
		if (transform != null) transform.accept(progressDialog);
		if(cancelable){
			progressDialog.setOnCancelListener(dialog->cancel());
		}
		progressDialog.show();
		return this;
	}

	protected void setRequestBody(Object body){
		requestBody=body;
	}

	protected void addQueryParameter(String key, String value){
		if(queryParams==null)
			queryParams=new ArrayList<>();
		queryParams.add(new Pair<>(key, value));
	}

	protected void addHeader(String key, String value){
		if(headers==null)
			headers=new HashMap<>();
		headers.put(key, value);
	}

	public MastodonAPIRequest<T> setTimeout(long timeout){
		this.timeout=timeout;
		return this;
	}

	protected String getPathPrefix(){
		return "/api/v1";
	}

	public Uri getURL(){
		Uri.Builder builder=new Uri.Builder()
				.scheme("https")
				.authority(domain)
				.path(getPathPrefix()+path);
		if(queryParams!=null){
			for(Pair<String, String> param:queryParams){
				builder.appendQueryParameter(param.first, param.second);
			}
		}
		return builder.build();
	}

	public String getMethod(){
		return method;
	}

	public RequestBody getRequestBody() throws IOException{
		if(requestBody instanceof RequestBody rb)
			return rb;
		return requestBody==null ? null : new JsonObjectRequestBody(requestBody);
	}

	@Override
	public MastodonAPIRequest<T> setCallback(Callback<T> callback){
		super.setCallback(callback);
		return this;
	}

	public MastodonAPIRequest<T> setContext(Context context) {
		this.context = context;
		return this;
	}

	@Nullable
	public Context getContext() {
		return context;
	}

	@CallSuper
	public void validateAndPostprocessResponse(T respObj, Response httpResponse) throws IOException{
		if(respObj instanceof BaseModel){
			((BaseModel) respObj).isRemote = isRemote;
			((BaseModel) respObj).postprocess();
		}else if(respObj instanceof List){
			if(removeUnsupportedItems){
				Iterator<?> itr=((List<?>) respObj).iterator();
				while(itr.hasNext()){
					Object item=itr.next();
					if(item instanceof BaseModel){
						try{
							((BaseModel) item).isRemote = isRemote;
							((BaseModel) item).postprocess();
						}catch(ObjectValidationException x){
							Log.w(TAG, "Removing invalid object from list", x);
							itr.remove();
						}
					}
				}
				// no idea why we're post-processing twice, but well, as long
				// as upstream does it like this, i don't wanna break anything
				for(Object item:((List<?>) respObj)){
					if(item instanceof BaseModel){
						((BaseModel) item).isRemote = isRemote;
						((BaseModel) item).postprocess();
					}
				}
			}else{
				for(Object item:((List<?>) respObj)){
					if(item instanceof BaseModel) {
						((BaseModel) item).isRemote = isRemote;
						((BaseModel) item).postprocess();
					}
				}
			}
		}
	}

	void onError(ErrorResponse err){
		if(!canceled)
			invokeErrorCallback(err);
	}

	void onError(String msg, int httpStatus, Throwable exception){
		if(!canceled)
			invokeErrorCallback(new MastodonErrorResponse(msg, httpStatus, exception));
	}

	void onSuccess(T resp){
		if(!canceled)
			invokeSuccessCallback(resp);
	}

	@Override
	protected void onRequestDone(){
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}

	public enum HttpMethod{
		GET,
		POST,
		PUT,
		DELETE,
		PATCH
	}
}
