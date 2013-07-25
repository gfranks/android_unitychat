package com.unitychat.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.unitychat.R;
import com.unitychat.models.Friend;
import com.unitychat.models.Friend.Status;

public class ChatDialog extends Dialog {

    private Activity mActivity;
    private Friend mFriend;

    public ChatDialog(Activity activity, Friend friend) {
        super(activity, R.style.Dialog_BG2);
        this.mActivity = activity;
        this.mFriend = friend;

        setChatDialog();
    }

    private void setChatDialog() {
        setContentView(R.layout.private_chat_dialog);
        setCanceledOnTouchOutside(true);
        setCancelable(true);

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                300, mActivity.getResources().getDisplayMetrics());
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        setupDialogViews();
    }

    private void setupDialogViews() {
        final TextView messages = (TextView) findViewById(R.chat.messages);
        final EditText input = (EditText) findViewById(R.chat.input);

        Button challenge = (Button) findViewById(R.chat.challenge);
        challenge.setCompoundDrawablesWithIntrinsicBounds(
                mFriend.getStatus() == Status.INACTIVE ? R.drawable.grey_light
                        : R.drawable.green_light, 0, 0, 0);
        challenge.setText("Challenge " + mFriend.getId());
        challenge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // display game options
            }
        });

        Button sendMessage = (Button) findViewById(R.chat.send);
        sendMessage.setOnClickListener(new View.OnClickListener() {
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
}
