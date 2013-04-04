package com.drawerchat.widget;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.drawerchat.R;
import com.drawerchat.models.Friend;
import com.drawerchat.models.Friend.Status;
import com.drawerchat.widget.SlidingDrawer.OnSlidingDrawerDismissListener;
import com.drawerchat.widget.SlidingDrawer.OnSlidingDrawerShowListener;

public class DrawerChatHelper {

	SlidingDrawer mDrawerChat;
	View mDrawerChatViewContainer;
	LinearLayout mDrawerChatFriendContainerView;
	LinearLayout mDrawerChatGameStatusView;
	Activity mActivity;
	DrawerChatShowDismissListener showDismissListener;
	
	boolean isInGame = false;
    
    boolean mHasTimer;
    long mDurationForTimer;
    
    public DrawerChatHelper(Activity activity, int drawerChatResId, boolean withTimer, 
    		long durationInMilli, boolean inGame, String gameName){
    	mActivity = activity;
    	mDrawerChat = (SlidingDrawer) activity.findViewById(drawerChatResId);
    	mDrawerChatViewContainer = mDrawerChat.getContent();
    	mDrawerChatFriendContainerView = (LinearLayout) mDrawerChatViewContainer.findViewById(R.id.friends);
    	mDrawerChatGameStatusView = (LinearLayout) mDrawerChatViewContainer.findViewById(R.id.game_status_layout);
    	mHasTimer = withTimer;
    	mDurationForTimer = durationInMilli;
    	isInGame = inGame;
    	
    	setupSubViews();
    }
    
    public void setupSubViews() {
    	mDrawerChatGameStatusView.setVisibility(isInGame ? View.VISIBLE : View.GONE);
    	
    	View publicChat = getNewDrawerItem();
    	TextView text = (TextView) publicChat.findViewById(R.friend.id);
    	text.setText("Public Chat");
    	LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) text.getLayoutParams();
    	lp.gravity = Gravity.CENTER;
    	text.setLayoutParams(lp);
    	publicChat.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// go to public chat
				
			}
    	});
    	mDrawerChatFriendContainerView.addView(publicChat);
    }
    
    public void addNewFriend(final Friend friend) {
    	View newFriend = getNewDrawerItem();
    	TextView messages = (TextView) newFriend.findViewById(R.friend.new_messages);
    	TextView id = (TextView) newFriend.findViewById(R.friend.id);
    	CheckedTextView active = (CheckedTextView) newFriend.findViewById(R.friend.active);
    	
    	messages.setText("("+"5"+")"); // get number of messages
    	messages.setVisibility(View.VISIBLE);
    	
    	id.setText(friend.getId());
    	LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) id.getLayoutParams();
    	lp.gravity = Gravity.CENTER;
    	id.setLayoutParams(lp);
    	
    	active.setChecked(friend.getStatus() == Status.INACTIVE ? false : true);
    	active.setVisibility(View.VISIBLE);
    	
    	newFriend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				displayPopupChat(v, friend);
				displayDialogChat(v, friend);
			}
    	});
    	mDrawerChatFriendContainerView.addView(newFriend);
    	mDrawerChatFriendContainerView.requestLayout();
    }
    
    @SuppressLint("NewApi") 
    @SuppressWarnings("deprecation")
	private View getNewDrawerItem() {
    	LayoutInflater layoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = layoutInflater.inflate(R.layout.friend_layout, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = 3;
        
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        
        int width;
        try { 
        	Point size = new Point();
        	display.getSize(size); 
        	width = size.x; 
        } catch (NoSuchMethodError e) { 
        	width = display.getWidth(); 
        }
        lp.width = width/3 - 6;
        lp.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				50, mActivity.getResources().getDisplayMetrics());
        layout.setLayoutParams(lp);
    	
    	return layout;
    }
    
    public void showDrawerChat() {
    	mDrawerChat.setOnSlidingDrawerShowListener(mOnDrawerChatShowListener);
    	mDrawerChat.setOnSlidingDrawerDismissListener(mOnDrawerChatDismissListener);
    	mDrawerChat.animateShow();
    }
    
    public void dismissDrawerChat() {
    	mDrawerChat.animateDismiss();
    }
    
    public void setDrawerChatShowOrHideListener(DrawerChatShowDismissListener listener) {
    	showDismissListener = listener;
    }
    
    public boolean isDrawerChatShowing() {
    	return mDrawerChat.isShowing();
    }
    
    private void displayPopupChat(View v, Friend friend) {
        ChatPopup chatPopup = new ChatPopup(mActivity, friend, v);
        chatPopup.show();
    }
    
    private void displayDialogChat(View v, Friend friend) {
        ChatDialog chatPopup = new ChatDialog(mActivity, friend);
        chatPopup.show();
    }
    
    private OnSlidingDrawerShowListener mOnDrawerChatShowListener = new OnSlidingDrawerShowListener() {
		@Override
		public void onSlidingDrawerShown() {
			if (mHasTimer) {
				Timer timer = new Timer();
				DropDownViewTimerTask dropDownTimer = new DropDownViewTimerTask();
				timer.schedule(dropDownTimer, mDurationForTimer);
			}
			
			if (showDismissListener != null) {
				showDismissListener.drawerChatDidShow();
			}
		}
	};
    
	private OnSlidingDrawerDismissListener mOnDrawerChatDismissListener = new OnSlidingDrawerDismissListener() {	
		@Override
		public void onSlidingDrawerDismissed() {
			if (showDismissListener != null) {
				showDismissListener.drawerChatDidDismiss();
			}
		}
	};

    public static interface DrawerChatShowDismissListener {
    	public void drawerChatDidShow();
    	public void drawerChatDidDismiss();
    }
    
    class DropDownViewTimerTask extends TimerTask {
    	public void run() {
    		mActivity.runOnUiThread(new Runnable() {
    			public void run() {
    				dismissDrawerChat();
    			}
    		});
    	}
    }
}
