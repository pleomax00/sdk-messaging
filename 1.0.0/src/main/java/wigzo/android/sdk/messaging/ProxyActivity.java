package wigzo.android.sdk.messaging;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;


public class ProxyActivity extends Activity {

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent (Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart () {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            final Message msg = extras.getParcelable(WigzoMessaging.EXTRA_MESSAGE);

            if (msg != null) {
                if (extras.containsKey(WigzoMessaging.NOTIFICATION_SHOW_DIALOG)) {
                    WigzoMessaging.recordMessageOpen(msg.getId());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(msg.getNotificationTitle(this))
                            .setMessage(msg.getNotificationMessage());

                    if (msg.hasLink()) {
                        builder.setCancelable(true)
                                .setPositiveButton(WigzoMessaging.buttonNames[0], new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        WigzoMessaging.recordMessageAction(msg.getId());
                                        finish();
                                        Intent activity = msg.getIntent(ProxyActivity.this, WigzoMessaging.getActivityClass());
                                        if(activity != null)
                                            startActivity(activity);
                                    }
                                })
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        finish();
                                    }
                                });
                    } else if (msg.hasReview()) {
                        builder.setCancelable(true)
                                .setPositiveButton(WigzoMessaging.buttonNames[1], new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        WigzoMessaging.recordMessageAction(msg.getId());
                                        finish();
                                        Intent activity = msg.getIntent(ProxyActivity.this, WigzoMessaging.getActivityClass());
                                        if(activity != null)
                                            startActivity(activity);
                                    }
                                })
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        finish();
                                    }
                                });
                    } else if (msg.hasMessage()) {
                        WigzoMessaging.recordMessageAction(msg.getId());
                        builder.setCancelable(true);
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        });
                    }else if (msg.hasCatgory()) {
                        WigzoMessaging.recordMessageAction(msg.getCatgory());
                        builder.setCancelable(true);
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();

                            }
                        });}
                    else {
                        throw new IllegalStateException("Wigzo Message with UNKNOWN type in ProxyActivity");
                    }

                    builder.create().show();
                } else {
                    WigzoMessaging.recordMessageAction(msg.getId());
                    Intent activity = msg.getIntent(this, WigzoMessaging.getActivityClass());

                    if (activity != null) {
                        activity.putExtra("catgory", msg.getCatgory());
                        startActivity(activity);
                    }
                    else
                        throw new IllegalStateException("Wigzo Message with UNKNOWN type in ProxyActivity");
                }
            }
        }
    }

    @Override
    protected void onStop () {
        super.onStop();
    }
}
