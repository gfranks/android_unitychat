package com.unitychat.widget;

import com.unitychat.R;
import com.unitychat.models.Friend;
import com.unitychat.models.Friend.Status;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ChatPopup extends PopupWindow {

	private Activity mActivity;
	private Friend mFriend;
	private View mParent;
	private View mLayout;
	
	int OFFSET_X;
    int OFFSET_Y;
	
	public ChatPopup(Activity activity, Friend friend, View parent) {
		super(activity);
		this.mActivity = activity;
		this.mFriend = friend;
		this.mParent = parent;
		
		setChatPopup();
	}
	
	public void show() {
		showAsDropDown(mParent);
//		showAtLocation(layout, Gravity.NO_GRAVITY, OFFSET_X, OFFSET_Y);
	}
	
	@SuppressLint("NewApi") 
	@SuppressWarnings("deprecation")
	private void setChatPopup() {
		LayoutInflater layoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLayout = layoutInflater.inflate(R.layout.private_chat_popup, null);
        
		setContentView(mLayout);
        setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				300, mActivity.getResources().getDisplayMetrics()));
        setHeight((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				450, mActivity.getResources().getDisplayMetrics()));
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        
        setupPopupViews();
        
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = 0, height = 0;
        try { 
        	Point size = new Point();
        	display.getSize(size); 
        	width = size.x; 
        	height = size.y; 
        } catch (NoSuchMethodError e) { 
        	width = display.getWidth(); 
        	height = display.getHeight(); 
        }
        
        moveIndicatorToView();
        OFFSET_X = mParent.getLeft();
        OFFSET_Y = height - mParent.getHeight()*5 - 5;
	}
	
	private void setupPopupViews() {
		final TextView messages = (TextView) mLayout
				.findViewById(R.chat.messages);
		final EditText input = (EditText) mLayout.findViewById(R.chat.input);

		Button challenge = (Button) mLayout.findViewById(R.chat.challenge);
		challenge.setCompoundDrawablesWithIntrinsicBounds(
				mFriend.getStatus() == Status.INACTIVE ? R.drawable.grey_light
						: R.drawable.green_light, 0, 0, 0);
		challenge.setText("Challenge " + mFriend.getId());
		challenge.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// display game options
			}
		});

		Button sendMessage = (Button) mLayout.findViewById(R.chat.send);
		sendMessage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String previousMessages;
				if (messages.getText().toString().contains("No Messages")) {
					previousMessages = "";
				} else {
					previousMessages = messages.getText() + "\n";
				}
				// get current user to add to new message
				messages.setText(previousMessages + "You: "
						+ input.getText().toString());

				input.setText(null);
				InputMethodManager keyboard = (InputMethodManager) mActivity
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
			}
		});
	}

	private void moveIndicatorToView() {
		int[] location = new int[2];
		mParent.getLocationInWindow(location);
		ImageView indicator = (ImageView) mLayout
				.findViewById(R.chat.private_chat_popup_indicator);
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) indicator
				.getLayoutParams();
		lp.leftMargin = mParent.getWidth() / 2 + 5;
		indicator.setLayoutParams(lp);
	}
}
