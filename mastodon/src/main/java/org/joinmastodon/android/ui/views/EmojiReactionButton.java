package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class EmojiReactionButton extends ProgressBarButton {
	private final Handler handler=new Handler();

	public EmojiReactionButton(Context context){
		super(context);
	}

	public EmojiReactionButton(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public EmojiReactionButton(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// allow long click even if button is disabled
		int action=event.getAction();
		if(action==MotionEvent.ACTION_DOWN && !isEnabled())
			handler.postDelayed(this::performLongClick, ViewConfiguration.getLongPressTimeout());
		if(action==MotionEvent.ACTION_UP)
			handler.removeCallbacksAndMessages(null);
		return super.onTouchEvent(event);
	}
}
