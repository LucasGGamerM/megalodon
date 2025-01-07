package org.joinmastodon.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.EmojiUpdatedEvent;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class CustomEmojiPopupKeyboard extends PopupKeyboard{
	private List<EmojiCategory> emojis;
	private UsableRecyclerView list;
	private ListImageLoaderWrapper imgLoader;
	private MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
	private String domain;
	private int spanCount=6;
	private Listener listener;
	private boolean forReaction;
	// Generated using https://github.com/mathiasbynens/emoji-test-regex-pattern
	private static Pattern emojiRegex = Pattern.compile("[#*0-9]\\x{FE0F}?\\x{20E3}|[\\xA9\\xAE\\x{203C}\\x{2049}\\x{2122}\\x{2139}\\x{2194}-\\x{2199}\\x{21A9}\\x{21AA}\\x{231A}\\x{231B}\\x{2328}\\x{23CF}\\x{23ED}-\\x{23EF}\\x{23F1}\\x{23F2}\\x{23F8}-\\x{23FA}\\x{24C2}\\x{25AA}\\x{25AB}\\x{25B6}\\x{25C0}\\x{25FB}\\x{25FC}\\x{25FE}\\x{2600}-\\x{2604}\\x{260E}\\x{2611}\\x{2614}\\x{2615}\\x{2618}\\x{2620}\\x{2622}\\x{2623}\\x{2626}\\x{262A}\\x{262E}\\x{262F}\\x{2638}-\\x{263A}\\x{2640}\\x{2642}\\x{2648}-\\x{2653}\\x{265F}\\x{2660}\\x{2663}\\x{2665}\\x{2666}\\x{2668}\\x{267B}\\x{267E}\\x{267F}\\x{2692}\\x{2694}-\\x{2697}\\x{2699}\\x{269B}\\x{269C}\\x{26A0}\\x{26A7}\\x{26AA}\\x{26B0}\\x{26B1}\\x{26BD}\\x{26BE}\\x{26C4}\\x{26C8}\\x{26CF}\\x{26D1}\\x{26E9}\\x{26F0}-\\x{26F5}\\x{26F7}\\x{26F8}\\x{26FA}\\x{2702}\\x{2708}\\x{2709}\\x{270F}\\x{2712}\\x{2714}\\x{2716}\\x{271D}\\x{2721}\\x{2733}\\x{2734}\\x{2744}\\x{2747}\\x{2757}\\x{2763}\\x{27A1}\\x{2934}\\x{2935}\\x{2B05}-\\x{2B07}\\x{2B1B}\\x{2B1C}\\x{2B55}\\x{3030}\\x{303D}\\x{3297}\\x{3299}\\x{1F004}\\x{1F170}\\x{1F171}\\x{1F17E}\\x{1F17F}\\x{1F202}\\x{1F237}\\x{1F321}\\x{1F324}-\\x{1F32C}\\x{1F336}\\x{1F37D}\\x{1F396}\\x{1F397}\\x{1F399}-\\x{1F39B}\\x{1F39E}\\x{1F39F}\\x{1F3CD}\\x{1F3CE}\\x{1F3D4}-\\x{1F3DF}\\x{1F3F5}\\x{1F3F7}\\x{1F43F}\\x{1F4FD}\\x{1F549}\\x{1F54A}\\x{1F56F}\\x{1F570}\\x{1F573}\\x{1F576}-\\x{1F579}\\x{1F587}\\x{1F58A}-\\x{1F58D}\\x{1F5A5}\\x{1F5A8}\\x{1F5B1}\\x{1F5B2}\\x{1F5BC}\\x{1F5C2}-\\x{1F5C4}\\x{1F5D1}-\\x{1F5D3}\\x{1F5DC}-\\x{1F5DE}\\x{1F5E1}\\x{1F5E3}\\x{1F5E8}\\x{1F5EF}\\x{1F5F3}\\x{1F5FA}\\x{1F6CB}\\x{1F6CD}-\\x{1F6CF}\\x{1F6E0}-\\x{1F6E5}\\x{1F6E9}\\x{1F6F0}\\x{1F6F3}]\\x{FE0F}?|[\\x{261D}\\x{270C}\\x{270D}\\x{1F574}\\x{1F590}][\\x{FE0F}\\x{1F3FB}-\\x{1F3FF}]?|[\\x{26F9}\\x{1F3CB}\\x{1F3CC}\\x{1F575}][\\x{FE0F}\\x{1F3FB}-\\x{1F3FF}]?(?:\\x{200D}[\\x{2640}\\x{2642}]\\x{FE0F}?)?|[\\x{270A}\\x{270B}\\x{1F385}\\x{1F3C2}\\x{1F3C7}\\x{1F442}\\x{1F443}\\x{1F446}-\\x{1F450}\\x{1F466}\\x{1F467}\\x{1F46B}-\\x{1F46D}\\x{1F472}\\x{1F474}-\\x{1F476}\\x{1F478}\\x{1F47C}\\x{1F483}\\x{1F485}\\x{1F48F}\\x{1F491}\\x{1F4AA}\\x{1F57A}\\x{1F595}\\x{1F596}\\x{1F64C}\\x{1F64F}\\x{1F6C0}\\x{1F6CC}\\x{1F90C}\\x{1F90F}\\x{1F918}-\\x{1F91F}\\x{1F930}-\\x{1F934}\\x{1F936}\\x{1F977}\\x{1F9B5}\\x{1F9B6}\\x{1F9BB}\\x{1F9D2}\\x{1F9D3}\\x{1F9D5}\\x{1FAC3}-\\x{1FAC5}\\x{1FAF0}\\x{1FAF2}-\\x{1FAF8}][\\x{1F3FB}-\\x{1F3FF}]?|[\\x{1F3C3}\\x{1F6B6}\\x{1F9CE}][\\x{1F3FB}-\\x{1F3FF}]?(?:\\x{200D}(?:[\\x{2640}\\x{2642}]\\x{FE0F}?(?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|\\x{27A1}\\x{FE0F}?))?|[\\x{1F3C4}\\x{1F3CA}\\x{1F46E}\\x{1F470}\\x{1F471}\\x{1F473}\\x{1F477}\\x{1F481}\\x{1F482}\\x{1F486}\\x{1F487}\\x{1F645}-\\x{1F647}\\x{1F64B}\\x{1F64D}\\x{1F64E}\\x{1F6A3}\\x{1F6B4}\\x{1F6B5}\\x{1F926}\\x{1F935}\\x{1F937}-\\x{1F939}\\x{1F93D}\\x{1F93E}\\x{1F9B8}\\x{1F9B9}\\x{1F9CD}\\x{1F9CF}\\x{1F9D4}\\x{1F9D6}-\\x{1F9DD}][\\x{1F3FB}-\\x{1F3FF}]?(?:\\x{200D}[\\x{2640}\\x{2642}]\\x{FE0F}?)?|[\\x{1F46F}\\x{1F9DE}\\x{1F9DF}](?:\\x{200D}[\\x{2640}\\x{2642}]\\x{FE0F}?)?|[\\x{23E9}-\\x{23EC}\\x{23F0}\\x{23F3}\\x{25FD}\\x{2693}\\x{26A1}\\x{26AB}\\x{26C5}\\x{26CE}\\x{26D4}\\x{26EA}\\x{26FD}\\x{2705}\\x{2728}\\x{274C}\\x{274E}\\x{2753}-\\x{2755}\\x{2795}-\\x{2797}\\x{27B0}\\x{27BF}\\x{2B50}\\x{1F0CF}\\x{1F18E}\\x{1F191}-\\x{1F19A}\\x{1F201}\\x{1F21A}\\x{1F22F}\\x{1F232}-\\x{1F236}\\x{1F238}-\\x{1F23A}\\x{1F250}\\x{1F251}\\x{1F300}-\\x{1F320}\\x{1F32D}-\\x{1F335}\\x{1F337}-\\x{1F343}\\x{1F345}-\\x{1F34A}\\x{1F34C}-\\x{1F37C}\\x{1F37E}-\\x{1F384}\\x{1F386}-\\x{1F393}\\x{1F3A0}-\\x{1F3C1}\\x{1F3C5}\\x{1F3C6}\\x{1F3C8}\\x{1F3C9}\\x{1F3CF}-\\x{1F3D3}\\x{1F3E0}-\\x{1F3F0}\\x{1F3F8}-\\x{1F407}\\x{1F409}-\\x{1F414}\\x{1F416}-\\x{1F425}\\x{1F427}-\\x{1F43A}\\x{1F43C}-\\x{1F43E}\\x{1F440}\\x{1F444}\\x{1F445}\\x{1F451}-\\x{1F465}\\x{1F46A}\\x{1F479}-\\x{1F47B}\\x{1F47D}-\\x{1F480}\\x{1F484}\\x{1F488}-\\x{1F48E}\\x{1F490}\\x{1F492}-\\x{1F4A9}\\x{1F4AB}-\\x{1F4FC}\\x{1F4FF}-\\x{1F53D}\\x{1F54B}-\\x{1F54E}\\x{1F550}-\\x{1F567}\\x{1F5A4}\\x{1F5FB}-\\x{1F62D}\\x{1F62F}-\\x{1F634}\\x{1F637}-\\x{1F641}\\x{1F643}\\x{1F644}\\x{1F648}-\\x{1F64A}\\x{1F680}-\\x{1F6A2}\\x{1F6A4}-\\x{1F6B3}\\x{1F6B7}-\\x{1F6BF}\\x{1F6C1}-\\x{1F6C5}\\x{1F6D0}-\\x{1F6D2}\\x{1F6D5}-\\x{1F6D7}\\x{1F6DC}-\\x{1F6DF}\\x{1F6EB}\\x{1F6EC}\\x{1F6F4}-\\x{1F6FC}\\x{1F7E0}-\\x{1F7EB}\\x{1F7F0}\\x{1F90D}\\x{1F90E}\\x{1F910}-\\x{1F917}\\x{1F920}-\\x{1F925}\\x{1F927}-\\x{1F92F}\\x{1F93A}\\x{1F93F}-\\x{1F945}\\x{1F947}-\\x{1F976}\\x{1F978}-\\x{1F9B4}\\x{1F9B7}\\x{1F9BA}\\x{1F9BC}-\\x{1F9CC}\\x{1F9D0}\\x{1F9E0}-\\x{1F9FF}\\x{1FA70}-\\x{1FA7C}\\x{1FA80}-\\x{1FA88}\\x{1FA90}-\\x{1FABD}\\x{1FABF}-\\x{1FAC2}\\x{1FACE}-\\x{1FADB}\\x{1FAE0}-\\x{1FAE8}]|\\x{26D3}\\x{FE0F}?(?:\\x{200D}\\x{1F4A5})?|\\x{2764}\\x{FE0F}?(?:\\x{200D}[\\x{1F525}\\x{1FA79}])?|\\x{1F1E6}[\\x{1F1E8}-\\x{1F1EC}\\x{1F1EE}\\x{1F1F1}\\x{1F1F2}\\x{1F1F4}\\x{1F1F6}-\\x{1F1FA}\\x{1F1FC}\\x{1F1FD}\\x{1F1FF}]|\\x{1F1E7}[\\x{1F1E6}\\x{1F1E7}\\x{1F1E9}-\\x{1F1EF}\\x{1F1F1}-\\x{1F1F4}\\x{1F1F6}-\\x{1F1F9}\\x{1F1FB}\\x{1F1FC}\\x{1F1FE}\\x{1F1FF}]|\\x{1F1E8}[\\x{1F1E6}\\x{1F1E8}\\x{1F1E9}\\x{1F1EB}-\\x{1F1EE}\\x{1F1F0}-\\x{1F1F5}\\x{1F1F7}\\x{1F1FA}-\\x{1F1FF}]|\\x{1F1E9}[\\x{1F1EA}\\x{1F1EC}\\x{1F1EF}\\x{1F1F0}\\x{1F1F2}\\x{1F1F4}\\x{1F1FF}]|\\x{1F1EA}[\\x{1F1E6}\\x{1F1E8}\\x{1F1EA}\\x{1F1EC}\\x{1F1ED}\\x{1F1F7}-\\x{1F1FA}]|\\x{1F1EB}[\\x{1F1EE}-\\x{1F1F0}\\x{1F1F2}\\x{1F1F4}\\x{1F1F7}]|\\x{1F1EC}[\\x{1F1E6}\\x{1F1E7}\\x{1F1E9}-\\x{1F1EE}\\x{1F1F1}-\\x{1F1F3}\\x{1F1F5}-\\x{1F1FA}\\x{1F1FC}\\x{1F1FE}]|\\x{1F1ED}[\\x{1F1F0}\\x{1F1F2}\\x{1F1F3}\\x{1F1F7}\\x{1F1F9}\\x{1F1FA}]|\\x{1F1EE}[\\x{1F1E8}-\\x{1F1EA}\\x{1F1F1}-\\x{1F1F4}\\x{1F1F6}-\\x{1F1F9}]|\\x{1F1EF}[\\x{1F1EA}\\x{1F1F2}\\x{1F1F4}\\x{1F1F5}]|\\x{1F1F0}[\\x{1F1EA}\\x{1F1EC}-\\x{1F1EE}\\x{1F1F2}\\x{1F1F3}\\x{1F1F5}\\x{1F1F7}\\x{1F1FC}\\x{1F1FE}\\x{1F1FF}]|\\x{1F1F1}[\\x{1F1E6}-\\x{1F1E8}\\x{1F1EE}\\x{1F1F0}\\x{1F1F7}-\\x{1F1FB}\\x{1F1FE}]|\\x{1F1F2}[\\x{1F1E6}\\x{1F1E8}-\\x{1F1ED}\\x{1F1F0}-\\x{1F1FF}]|\\x{1F1F3}[\\x{1F1E6}\\x{1F1E8}\\x{1F1EA}-\\x{1F1EC}\\x{1F1EE}\\x{1F1F1}\\x{1F1F4}\\x{1F1F5}\\x{1F1F7}\\x{1F1FA}\\x{1F1FF}]|\\x{1F1F4}\\x{1F1F2}|\\x{1F1F5}[\\x{1F1E6}\\x{1F1EA}-\\x{1F1ED}\\x{1F1F0}-\\x{1F1F3}\\x{1F1F7}-\\x{1F1F9}\\x{1F1FC}\\x{1F1FE}]|\\x{1F1F6}\\x{1F1E6}|\\x{1F1F7}[\\x{1F1EA}\\x{1F1F4}\\x{1F1F8}\\x{1F1FA}\\x{1F1FC}]|\\x{1F1F8}[\\x{1F1E6}-\\x{1F1EA}\\x{1F1EC}-\\x{1F1F4}\\x{1F1F7}-\\x{1F1F9}\\x{1F1FB}\\x{1F1FD}-\\x{1F1FF}]|\\x{1F1F9}[\\x{1F1E6}\\x{1F1E8}\\x{1F1E9}\\x{1F1EB}-\\x{1F1ED}\\x{1F1EF}-\\x{1F1F4}\\x{1F1F7}\\x{1F1F9}\\x{1F1FB}\\x{1F1FC}\\x{1F1FF}]|\\x{1F1FA}[\\x{1F1E6}\\x{1F1EC}\\x{1F1F2}\\x{1F1F3}\\x{1F1F8}\\x{1F1FE}\\x{1F1FF}]|\\x{1F1FB}[\\x{1F1E6}\\x{1F1E8}\\x{1F1EA}\\x{1F1EC}\\x{1F1EE}\\x{1F1F3}\\x{1F1FA}]|\\x{1F1FC}[\\x{1F1EB}\\x{1F1F8}]|\\x{1F1FD}\\x{1F1F0}|\\x{1F1FE}[\\x{1F1EA}\\x{1F1F9}]|\\x{1F1FF}[\\x{1F1E6}\\x{1F1F2}\\x{1F1FC}]|\\x{1F344}(?:\\x{200D}\\x{1F7EB})?|\\x{1F34B}(?:\\x{200D}\\x{1F7E9})?|\\x{1F3F3}\\x{FE0F}?(?:\\x{200D}(?:\\x{26A7}\\x{FE0F}?|\\x{1F308}))?|\\x{1F3F4}(?:\\x{200D}\\x{2620}\\x{FE0F}?|\\x{E0067}\\x{E0062}(?:\\x{E0065}\\x{E006E}\\x{E0067}|\\x{E0073}\\x{E0063}\\x{E0074}|\\x{E0077}\\x{E006C}\\x{E0073})\\x{E007F})?|\\x{1F408}(?:\\x{200D}\\x{2B1B})?|\\x{1F415}(?:\\x{200D}\\x{1F9BA})?|\\x{1F426}(?:\\x{200D}[\\x{2B1B}\\x{1F525}])?|\\x{1F43B}(?:\\x{200D}\\x{2744}\\x{FE0F}?)?|\\x{1F441}\\x{FE0F}?(?:\\x{200D}\\x{1F5E8}\\x{FE0F}?)?|\\x{1F468}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F468}\\x{1F469}]\\x{200D}(?:\\x{1F466}(?:\\x{200D}\\x{1F466})?|\\x{1F467}(?:\\x{200D}[\\x{1F466}\\x{1F467}])?)|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F468}|\\x{1F466}(?:\\x{200D}\\x{1F466})?|\\x{1F467}(?:\\x{200D}[\\x{1F466}\\x{1F467}])?)|\\x{1F3FB}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F468}[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F468}[\\x{1F3FC}-\\x{1F3FF}]))?|\\x{1F3FC}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F468}[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F468}[\\x{1F3FB}\\x{1F3FD}-\\x{1F3FF}]))?|\\x{1F3FD}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F468}[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F468}[\\x{1F3FB}\\x{1F3FC}\\x{1F3FE}\\x{1F3FF}]))?|\\x{1F3FE}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F468}[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F468}[\\x{1F3FB}-\\x{1F3FD}\\x{1F3FF}]))?|\\x{1F3FF}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F468}[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F468}[\\x{1F3FB}-\\x{1F3FE}]))?)?|\\x{1F469}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?[\\x{1F468}\\x{1F469}]|\\x{1F466}(?:\\x{200D}\\x{1F466})?|\\x{1F467}(?:\\x{200D}[\\x{1F466}\\x{1F467}])?|\\x{1F469}\\x{200D}(?:\\x{1F466}(?:\\x{200D}\\x{1F466})?|\\x{1F467}(?:\\x{200D}[\\x{1F466}\\x{1F467}])?))|\\x{1F3FB}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:[\\x{1F468}\\x{1F469}]|\\x{1F48B}\\x{200D}[\\x{1F468}\\x{1F469}])[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}[\\x{1F468}\\x{1F469}][\\x{1F3FC}-\\x{1F3FF}]))?|\\x{1F3FC}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:[\\x{1F468}\\x{1F469}]|\\x{1F48B}\\x{200D}[\\x{1F468}\\x{1F469}])[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}[\\x{1F468}\\x{1F469}][\\x{1F3FB}\\x{1F3FD}-\\x{1F3FF}]))?|\\x{1F3FD}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:[\\x{1F468}\\x{1F469}]|\\x{1F48B}\\x{200D}[\\x{1F468}\\x{1F469}])[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}[\\x{1F468}\\x{1F469}][\\x{1F3FB}\\x{1F3FC}\\x{1F3FE}\\x{1F3FF}]))?|\\x{1F3FE}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:[\\x{1F468}\\x{1F469}]|\\x{1F48B}\\x{200D}[\\x{1F468}\\x{1F469}])[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}[\\x{1F468}\\x{1F469}][\\x{1F3FB}-\\x{1F3FD}\\x{1F3FF}]))?|\\x{1F3FF}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:[\\x{1F468}\\x{1F469}]|\\x{1F48B}\\x{200D}[\\x{1F468}\\x{1F469}])[\\x{1F3FB}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}[\\x{1F468}\\x{1F469}][\\x{1F3FB}-\\x{1F3FE}]))?)?|\\x{1F62E}(?:\\x{200D}\\x{1F4A8})?|\\x{1F635}(?:\\x{200D}\\x{1F4AB})?|\\x{1F636}(?:\\x{200D}\\x{1F32B}\\x{FE0F}?)?|\\x{1F642}(?:\\x{200D}[\\x{2194}\\x{2195}]\\x{FE0F}?)?|\\x{1F93C}(?:[\\x{1F3FB}-\\x{1F3FF}]|\\x{200D}[\\x{2640}\\x{2642}]\\x{FE0F}?)?|\\x{1F9D1}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F384}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{1F91D}\\x{200D}\\x{1F9D1}|\\x{1F9D1}\\x{200D}\\x{1F9D2}(?:\\x{200D}\\x{1F9D2})?|\\x{1F9D2}(?:\\x{200D}\\x{1F9D2})?)|\\x{1F3FB}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F384}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F9D1}[\\x{1F3FC}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F9D1}[\\x{1F3FB}-\\x{1F3FF}]))?|\\x{1F3FC}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F384}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F9D1}[\\x{1F3FB}\\x{1F3FD}-\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F9D1}[\\x{1F3FB}-\\x{1F3FF}]))?|\\x{1F3FD}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F384}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F9D1}[\\x{1F3FB}\\x{1F3FC}\\x{1F3FE}\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F9D1}[\\x{1F3FB}-\\x{1F3FF}]))?|\\x{1F3FE}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F384}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F9D1}[\\x{1F3FB}-\\x{1F3FD}\\x{1F3FF}]|\\x{1F91D}\\x{200D}\\x{1F9D1}[\\x{1F3FB}-\\x{1F3FF}]))?|\\x{1F3FF}(?:\\x{200D}(?:[\\x{2695}\\x{2696}\\x{2708}]\\x{FE0F}?|[\\x{1F9AF}\\x{1F9BC}\\x{1F9BD}](?:\\x{200D}\\x{27A1}\\x{FE0F}?)?|[\\x{1F33E}\\x{1F373}\\x{1F37C}\\x{1F384}\\x{1F393}\\x{1F3A4}\\x{1F3A8}\\x{1F3EB}\\x{1F3ED}\\x{1F4BB}\\x{1F4BC}\\x{1F527}\\x{1F52C}\\x{1F680}\\x{1F692}\\x{1F9B0}-\\x{1F9B3}]|\\x{2764}\\x{FE0F}?\\x{200D}(?:\\x{1F48B}\\x{200D})?\\x{1F9D1}[\\x{1F3FB}-\\x{1F3FE}]|\\x{1F91D}\\x{200D}\\x{1F9D1}[\\x{1F3FB}-\\x{1F3FF}]))?)?|\\x{1FAF1}(?:\\x{1F3FB}(?:\\x{200D}\\x{1FAF2}[\\x{1F3FC}-\\x{1F3FF}])?|\\x{1F3FC}(?:\\x{200D}\\x{1FAF2}[\\x{1F3FB}\\x{1F3FD}-\\x{1F3FF}])?|\\x{1F3FD}(?:\\x{200D}\\x{1FAF2}[\\x{1F3FB}\\x{1F3FC}\\x{1F3FE}\\x{1F3FF}])?|\\x{1F3FE}(?:\\x{200D}\\x{1FAF2}[\\x{1F3FB}-\\x{1F3FD}\\x{1F3FF}])?|\\x{1F3FF}(?:\\x{200D}\\x{1FAF2}[\\x{1F3FB}-\\x{1F3FE}])?)?");
	private final boolean playGifs;
	private final AccountLocalPreferences lp;
	private EmojiCategory recentEmojiCategory;

	public CustomEmojiPopupKeyboard(Activity activity, String accountID, List<EmojiCategory> emojis, String domain){
		this(activity, accountID, emojis, domain, false);
	}

	public CustomEmojiPopupKeyboard(Activity activity, String accountID, List<EmojiCategory> emojis, String domain, boolean forReaction){
		super(activity);
		this.emojis=emojis;
		this.domain=domain;
		this.forReaction=forReaction;
		lp=AccountSessionManager.get(accountID).getLocalPreferences();
		playGifs=GlobalUserPreferences.playGifs;
	}

	@Override
	protected View onCreateView(){
		GridLayoutManager lm=new GridLayoutManager(activity, spanCount);
		list=new UsableRecyclerView(activity){
			@Override
			protected void onMeasure(int widthSpec, int heightSpec){
				// it's important to do this in onMeasure so the child views will be measured with correct paddings already set
				spanCount=Math.round((MeasureSpec.getSize(widthSpec)-V.dp(32-8))/(float)V.dp(48+8));
				lm.setSpanCount(spanCount);
				invalidateItemDecorations();
				super.onMeasure(widthSpec, heightSpec);
			}
		};
		lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){
			@Override
			public int getSpanSize(int position){
				if(adapter.getItemViewType(position)==0)
					return lm.getSpanCount();
				return 1;
			}
		});
		list.setLayoutManager(lm);
		list.setPadding(V.dp(16), 0, V.dp(16), 0);
		imgLoader=new ListImageLoaderWrapper(activity, list, new RecyclerViewDelegate(list), null);

		if (!forReaction){
			List<Emoji> recentEmoji=new ArrayList<>(lp.recentCustomEmoji);
			if(!recentEmoji.isEmpty())
				adapter.addAdapter(new SingleCategoryAdapter(recentEmojiCategory=new EmojiCategory(activity.getString(R.string.mo_emoji_recent), recentEmoji)));
			for(EmojiCategory category : emojis)
				adapter.addAdapter(new SingleCategoryAdapter(category));
		}
		list.setAdapter(adapter);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(view instanceof TextView){ // section header
					outRect.left=outRect.right=V.dp(-16);
				}else{
					EmojiViewHolder evh=(EmojiViewHolder) parent.getChildViewHolder(view);
					int col=evh.positionWithinCategory%spanCount;
					if(col<spanCount-1){
						outRect.right=V.dp(8);
					}
					outRect.bottom=V.dp(8);
				}
			}
		});
		list.setSelector(null);
		list.setClipToPadding(false);
		new StickyHeadersOverlay(activity, 0).install(list);

		LinearLayout ll=new LinearLayout(activity) {
			@Override
			public boolean onInterceptTouchEvent(MotionEvent e){
				if (e.getAction() == MotionEvent.ACTION_MOVE) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				return false;
			}
		};
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setElevation(V.dp(3));
		ll.setBackgroundResource(R.drawable.bg_m3_surface1);

		ll.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		if(forReaction){
			FrameLayout topPanel=new FrameLayout(activity);
			topPanel.setPadding(V.dp(16), V.dp(12), V.dp(16), V.dp(12));
			topPanel.setBackgroundResource(R.drawable.bg_m3_surface2);
			ll.addView(topPanel, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			
			InputMethodManager imm=(InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			EditText input=new EditText(activity);
			input.setHint(R.string.sk_enter_emoji_hint);
			input.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count){
					// Only check the emoji regex if the text field was empty before
					if(start == 0){
						if(emojiRegex.matcher(s.toString()).find()){
							imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
							listener.onEmojiSelected(s.toString());
							input.getText().clear();
						}
					}
					for(int i=0; i<adapter.getAdapterCount(); i++){
						SingleCategoryAdapter currentAdapter=(SingleCategoryAdapter) adapter.getAdapterAt(i);
						currentAdapter.getFilter().filter(s.toString());
					}
				}

				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void afterTextChanged(Editable s) {}
			});
			input.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
				@Override
				public void onViewAttachedToWindow(@NonNull View view){}

				@Override
				public void onViewDetachedFromWindow(@NonNull View view){
					input.getText().clear();
				}
			});
			topPanel.addView(input);

		}else{ // in compose fragment
			FrameLayout bottomPanel=new FrameLayout(activity);
			bottomPanel.setPadding(V.dp(16), V.dp(8), V.dp(16), V.dp(8));
			bottomPanel.setBackgroundResource(R.drawable.bg_m3_surface2);
			ll.addView(bottomPanel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			ImageButton hideKeyboard=new ImageButton(activity);
			hideKeyboard.setImageResource(R.drawable.ic_fluent_keyboard_dock_24_regular);
			hideKeyboard.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(activity, R.attr.colorM3OnSurfaceVariant)));
			hideKeyboard.setBackground(UiUtils.getThemeDrawable(activity, android.R.attr.actionBarItemBackground));
			hideKeyboard.setOnClickListener(v->hide());
			bottomPanel.addView(hideKeyboard, new FrameLayout.LayoutParams(V.dp(48), V.dp(48), Gravity.START | Gravity.CENTER_VERTICAL));

			ImageButton backspace=new ImageButton(activity);
			backspace.setImageResource(R.drawable.ic_fluent_backspace_24_regular);
			backspace.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(activity, R.attr.colorM3OnSurfaceVariant)));
			backspace.setBackground(UiUtils.getThemeDrawable(activity, android.R.attr.actionBarItemBackground));
			backspace.setOnClickListener(v->listener.onBackspace());
			bottomPanel.addView(backspace, new FrameLayout.LayoutParams(V.dp(48), V.dp(48), Gravity.END | Gravity.CENTER_VERTICAL));
		}

		return ll;
	}

	public void setListener(Listener listener){
		this.listener=listener;
	}

	@SuppressLint("NotifyDataSetChanged")
	@Subscribe
	public void onEmojiUpdated(EmojiUpdatedEvent ev){
		if(ev.instanceDomain.equals(domain)){
			emojis=AccountSessionManager.getInstance().getCustomEmojis(domain);
			adapter.notifyDataSetChanged();
		}
	}

	public void customToggleKeyboardPopup(){
		if (adapter.getAdapterCount() == 0){
			List<Emoji> recentEmoji=new ArrayList<>(lp.recentCustomEmoji);
			if(!recentEmoji.isEmpty())
				adapter.addAdapter(new SingleCategoryAdapter(recentEmojiCategory=new EmojiCategory(activity.getString(R.string.mo_emoji_recent), recentEmoji)));
			for(EmojiCategory category : emojis)
				adapter.addAdapter(new SingleCategoryAdapter(category));
		}
		super.toggleKeyboardPopup(null);
	}

	private class SingleCategoryAdapter extends UsableRecyclerView.Adapter<RecyclerView.ViewHolder> implements ImageLoaderRecyclerAdapter, Filterable{
		private EmojiCategory category;

		private final EmojiCategory originalCategory;
		private List<ImageLoaderRequest> requests;

		public SingleCategoryAdapter(EmojiCategory category){
			super(imgLoader);
			this.category=category;
			this.originalCategory=new EmojiCategory(category);
			requests=category.emojis.stream().map(e->new UrlImageLoaderRequest(e.getUrl(playGifs), V.dp(24), V.dp(24))).collect(Collectors.toList());
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return viewType==0 ? new SectionHeaderViewHolder() : new EmojiViewHolder(category==recentEmojiCategory, requests);
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position){
			if(category.emojis.size() == 0) {
				holder.itemView.setVisibility(View.GONE);
			}
			if(holder instanceof EmojiViewHolder evh){
				evh.bind(category.emojis.get(position-1));
				evh.positionWithinCategory=position-1;
			}else if(holder instanceof SectionHeaderViewHolder shvh){
				shvh.bind(TextUtils.isEmpty(category.title) ? domain : category.title);
			}

			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			if(category.emojis.size() == 0) return 0;
			return category.emojis.size()+1;
		}

		@Override
		public int getItemViewType(int position){
			return position==0 ? 0 : 1;
		}

		@Override
		public int getImageCountForItem(int position){
			return position>0 ? 1 : 0;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return requests.get(position-1);
		}

		@Override
		public Filter getFilter(){
			return emojiFilter;
		}
		private final Filter emojiFilter = new Filter(){
			@Override
			protected FilterResults performFiltering(CharSequence charSequence){
				List<Emoji> filteredEmoji=new ArrayList<>();
				String search=charSequence.toString().toLowerCase().trim();

				if(charSequence==null || charSequence.length()==0){
					filteredEmoji.addAll(originalCategory.emojis);
				}else{
					for(Emoji emoji : originalCategory.emojis){
						if(emoji.shortcode.toLowerCase().contains(search)){
							filteredEmoji.add(emoji);
						}
					}

				}
				FilterResults results=new FilterResults();
				results.values=filteredEmoji;
				return results;
			}
			@Override
			protected void publishResults(CharSequence charSequence, FilterResults filterResults){
				category.emojis.clear();
				category.emojis.addAll((List) filterResults.values);
				requests=category.emojis.stream().map(e->new UrlImageLoaderRequest(e.getUrl(playGifs), V.dp(24), V.dp(24))).collect(Collectors.toList());
				notifyDataSetChanged();
			}
		};
	}

	private class SectionHeaderViewHolder extends BindableViewHolder<String> implements StickyHeadersOverlay.HeaderViewHolder{
		private Drawable background;

		public SectionHeaderViewHolder(){
			super(activity, R.layout.item_emoji_section, list);
			background=new ColorDrawable(UiUtils.alphaBlendThemeColors(activity, R.attr.colorM3Surface, R.attr.colorM3Primary, .08f));
			itemView.setBackground(background);
		}

		@Override
		public void onBind(String item){
			((TextView)itemView).setText(item);
			setStickyFactor(0);
		}

		@Override
		public void setStickyFactor(float factor){
			background.setAlpha(Math.round(255*factor));
		}
	}

	private class EmojiViewHolder extends BindableViewHolder<Emoji> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable, UsableRecyclerView.LongClickable{
		public int positionWithinCategory;
		public final boolean isRecentEmojiCategory;
		private List<ImageLoaderRequest> requests;

		public EmojiViewHolder(boolean isRecentEmojiCategory, List<ImageLoaderRequest> requests){
			super(new ImageView(activity));
			ImageView img=(ImageView) itemView;
			img.setLayoutParams(new RecyclerView.LayoutParams(V.dp(48), V.dp(48)));
			img.setScaleType(ImageView.ScaleType.FIT_CENTER);
			int pad=V.dp(6);
			img.setPadding(pad, pad, pad, pad);
			img.setBackgroundResource(R.drawable.bg_custom_emoji);
			this.isRecentEmojiCategory=isRecentEmojiCategory;
			this.requests=requests;
		}

		@Override
		public void onBind(Emoji item){

		}

		@Override
		public void setImage(int index, Drawable image){
			((ImageView)itemView).setImageDrawable(image);
			if(image instanceof Animatable)
				((Animatable) image).start();
		}

		@Override
		public void clearImage(int index){
			((ImageView)itemView).setImageDrawable(null);
		}

		@Override
		public boolean onLongClick(){
			if(!isRecentEmojiCategory || requests.size() < getAbsoluteAdapterPosition()-1 || 0 > getAbsoluteAdapterPosition()-1) return false;
			requests.remove(getAbsoluteAdapterPosition()-1);
			getBindingAdapter().notifyItemRemoved(getAbsoluteAdapterPosition());
			getBindingAdapter().notifyItemChanged(0);
			recentEmojiCategory.emojis.remove(item);
			imgLoader.updateImages();
			itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			lp.recentCustomEmoji.remove(item);
			lp.save();
			return true;
		}

		@Override
		public void onClick(){
			Iterator<Emoji> iterator=lp.recentCustomEmoji.iterator();
			while(iterator.hasNext())
				if(Objects.equals(iterator.next().shortcode, item.shortcode))
					iterator.remove();
			if(lp.recentCustomEmoji.size() < 21)
				lp.recentCustomEmoji.add(0, item);
			lp.save();
			listener.onEmojiSelected(item);
		}
	}

	public interface Listener{
		void onEmojiSelected(Emoji customEmoji);
		void   onEmojiSelected(String emoji);
		void onBackspace();
	}
}
