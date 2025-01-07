package org.joinmastodon.android.model;

import org.joinmastodon.android.GlobalUserPreferences;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

@Parcel
public class EmojiReaction {
    public List<Account> accounts;
    public List<String> accountIds;
    public int count;
    public boolean me;
    public String name;
    public String url;
	public String staticUrl;

	public transient ImageLoaderRequest request;
	public transient boolean pendingChange=false;

	public String getUrl(boolean playGifs){
		String idealUrl=playGifs ? url : staticUrl;
		if(idealUrl==null) return url==null ? staticUrl : url;
		return idealUrl;
	}

	public static EmojiReaction of(Emoji info, Account me){
		EmojiReaction reaction=new EmojiReaction();
		reaction.me=true;
		reaction.count=1;
		reaction.name=info.shortcode;
		reaction.url=info.url;
		reaction.staticUrl=info.staticUrl;
		reaction.accounts=new ArrayList<>(Collections.singleton(me));
		reaction.accountIds=new ArrayList<>(Collections.singleton(me.id));
		reaction.request=new UrlImageLoaderRequest(info.url, 0, V.sp(24));
		return reaction;
	}

	public static EmojiReaction of(String emoji, Account me){
		EmojiReaction reaction=new EmojiReaction();
		reaction.me=true;
		reaction.count=1;
		reaction.name=emoji;
		reaction.accounts=new ArrayList<>(Collections.singleton(me));
		reaction.accountIds=new ArrayList<>(Collections.singleton(me.id));
		return reaction;
	}

	public void add(Account self){
		if(accounts==null) accounts=new ArrayList<>();
		if(accountIds==null) accountIds=new ArrayList<>();
		count++;
		me=true;
		accounts.add(self);
		accountIds.add(self.id);
	}

    public EmojiReaction copy() {
		EmojiReaction r=new EmojiReaction();
		r.accounts=accounts;
		r.accountIds=accountIds;
		r.count=count;
		r.me=me;
		r.name=name;
		r.url=url;
		r.staticUrl=staticUrl;
		r.request=request;
		r.pendingChange=pendingChange;
		return r;
    }
}
