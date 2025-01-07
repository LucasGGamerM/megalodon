package org.joinmastodon.android.model;

import android.view.Menu;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.R;

public enum ContentType {
	@SerializedName("text/plain")
	PLAIN,
	@SerializedName("text/html")
	HTML,
	@SerializedName("text/markdown")
	MARKDOWN,
	@SerializedName("text/bbcode")
	BBCODE, // akkoma
	@SerializedName("text/x.misskeymarkdown")
	MISSKEY_MARKDOWN, // akkoma/*key
	@SerializedName("")
	UNSPECIFIED;

	public int getName() {
		return switch(this) {
			case PLAIN -> R.string.sk_content_type_plain;
			case HTML -> R.string.sk_content_type_html;
			case MARKDOWN -> R.string.sk_content_type_markdown;
			case BBCODE -> R.string.sk_content_type_bbcode;
			case MISSKEY_MARKDOWN -> R.string.sk_content_type_mfm;
			case UNSPECIFIED -> R.string.sk_content_type_unspecified;
		};
	}

	public boolean supportedByInstance(Instance i) {
		return i.isAkkoma() || i.isIceshrimp() || (this!=BBCODE && this!=MISSKEY_MARKDOWN);
	}
}
