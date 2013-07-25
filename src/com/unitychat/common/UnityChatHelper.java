package com.unitychat.common;

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

import com.unitychat.R;
import com.unitychat.models.Friend;
import com.unitychat.models.Friend.Status;
import com.unitychat.widget.ChatDialog;
import com.unitychat.widget.ChatPopup;
import com.unitychat.widget.SlidingDrawer;
import com.unitychat.widget.SlidingDrawer.OnSlidingDrawerDismissListener;
import com.unitychat.widget.SlidingDrawer.OnSlidingDrawerShowListener;

import java.util.Timer;
import java.util.TimerTask;

public class UnityChatHelper {

    SlidingDrawer mUnityChat;
    View mUnityChatViewContainer;
    LinearLayout mUnityChatFriendContainerView;
    LinearLayout mUnityChatGameStatusView;
    Activity mActivity;
    UnityChatShowDismissListener showDismissListener;
    MediaHelper mediaHelper;

    boolean isInGame = false;

    boolean mHasTimer;
    long mDurationForTimer;

    public UnityChatHelper(Activity activity, int UnityChatResId, boolean withTimer,
                           long durationInMilli, boolean inGame, String gameName) {
        mActivity = activity;
        mUnityChat = (SlidingDrawer) activity.findViewById(UnityChatResId);
        mUnityChatViewContainer = mUnityChat.getContent();
        mUnityChatFriendContainerView = (LinearLayout) mUnityChatViewContainer.findViewById(R.drawer.friends);
        mUnityChatGameStatusView = (LinearLayout) mUnityChatViewContainer.findViewById(R.drawer.game_status_layout);
        mHasTimer = withTimer;
        mDurationForTimer = durationInMilli;
        isInGame = inGame;

        mUnityChat.setOnSlidingDrawerShowListener(mOnUnityChatShowListener);
        mUnityChat.setOnSlidingDrawerDismissListener(mOnUnityChatDismissListener);

        setupSubViews();
    }

    public void setupSubViews() {
        mUnityChatGameStatusView.setVisibility(isInGame ? View.VISIBLE : View.GONE);

        View publicChat = getNewDrawerChatItem();
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
        mUnityChatFriendContainerView.addView(publicChat);

        setupMediaPlayer();
    }

    public void setupMediaPlayer() {
        mediaHelper = new MediaHelper(mUnityChat, R.drawer.pause, R.drawer.play,
                R.drawer.next, R.drawer.prev, R.drawer.repeat, R.drawer.select_song, R.drawer.now_playing,
                R.layout.track_layout, R.track.track_number, R.track.track_name, R.drawer.media_refresh);
    }

    public void addNewFriend(final Friend friend) {
        View newFriend = getNewDrawerChatItem();
        TextView messages = (TextView) newFriend.findViewById(R.friend.new_messages);
        TextView id = (TextView) newFriend.findViewById(R.friend.id);
        CheckedTextView active = (CheckedTextView) newFriend.findViewById(R.friend.active);

        messages.setText("(" + "5" + ")"); // get number of messages
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
        mUnityChatFriendContainerView.addView(newFriend);
        mUnityChatFriendContainerView.requestLayout();
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private View getNewDrawerChatItem() {
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
        lp.width = width / 3 - 6;
        lp.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                50, mActivity.getResources().getDisplayMetrics());
        layout.setLayoutParams(lp);

        return layout;
    }

    public void showUnityChat() {
        mUnityChat.animateShow();
    }

    public void dismissUnityChat() {
        mUnityChat.animateDismiss();
    }

    public void setUnityChatShowOrHideListener(UnityChatShowDismissListener listener) {
        showDismissListener = listener;
    }

    public boolean isUnityChatShowing() {
        return mUnityChat.isShowing();
    }

    private void displayPopupChat(View v, Friend friend) {
        ChatPopup chatPopup = new ChatPopup(mActivity, friend, v);
        chatPopup.show();
    }

    private void displayDialogChat(View v, Friend friend) {
        ChatDialog chatPopup = new ChatDialog(mActivity, friend);
        chatPopup.show();
    }

    private OnSlidingDrawerShowListener mOnUnityChatShowListener = new OnSlidingDrawerShowListener() {
        @Override
        public void onSlidingDrawerShown() {
            if (mHasTimer) {
                Timer timer = new Timer();
                DropDownViewTimerTask dropDownTimer = new DropDownViewTimerTask();
                timer.schedule(dropDownTimer, mDurationForTimer);
            }

            if (!mediaHelper.isInitialized()) {
                mediaHelper.getMusic();
            }

            if (showDismissListener != null) {
                showDismissListener.UnityChatDidShow();
            }
        }
    };

    private OnSlidingDrawerDismissListener mOnUnityChatDismissListener = new OnSlidingDrawerDismissListener() {
        @Override
        public void onSlidingDrawerDismissed() {
            if (showDismissListener != null) {
                showDismissListener.UnityChatDidDismiss();
            }
        }
    };

    public static interface UnityChatShowDismissListener {
        public void UnityChatDidShow();

        public void UnityChatDidDismiss();
    }

    class DropDownViewTimerTask extends TimerTask {
        public void run() {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    dismissUnityChat();
                }
            });
        }
    }
}
