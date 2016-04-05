package wigzo.android.sdk.messaging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Wigzo Messaging service message representation.
 */
public class Message implements Parcelable {
    private static final String TAG = "Wigzo|Message";

    private Bundle data;
    private int type;

    public Message(Bundle data) {
        this.data = data;
        this.type = setType();
    }

    public String getId() { return data.getString("c.i"); }
    public String getLink() { return data.getString("c.l"); }
    public String getReview() { return data.getString("c.r"); }
    public String getCatgory(){ return  data.getString("c.c");}
    public String getMessage() { return data.getString("message"); }
    public String getPicture(){return  data.getString("picture");}
    public String getSoundUri() { return data.getString("sound"); }
    public Bundle getData() { return data; }
    public int getType() { return type; }

    /**
     * Depending on message contents, it can represent different types of actions.
     * @return message type according to message contents.
     */
    private int setType() {
        int t = WigzoMessaging.NOTIFICATION_TYPE_UNKNOWN;

        if (getMessage() != null && !"".equals(getMessage())) {
            t |= WigzoMessaging.NOTIFICATION_TYPE_MESSAGE;
        }

        if(getCatgory()!=null && !"".equals(getCatgory())){
            t |= WigzoMessaging.NOTIFICATION_TYPE_CATGORY;
        }

        if (getReview() != null) {
            t |= WigzoMessaging.NOTIFICATION_TYPE_REVIEW;
        }

        if ("true".equals(data.getString("c.s"))) {
            t |= WigzoMessaging.NOTIFICATION_TYPE_SILENT;
        }

        if (getSoundUri() != null && !"".equals(getSoundUri())) {
            if ("default".equals(getSoundUri())) t |= WigzoMessaging.NOTIFICATION_TYPE_SOUND_DEFAULT;
            else t |= WigzoMessaging.NOTIFICATION_TYPE_SOUND_URI;
        }

        if (getLink() != null && !"".equals(getLink())) {
            t |= WigzoMessaging.NOTIFICATION_TYPE_URL;
        }
        if(getPicture() !=null && !"".equals(getPicture())){
            t |= WigzoMessaging.NOTIFICATION_TYPE_PICTURE;
        }
        return t;
    }

    public boolean hasLink() { return (type & WigzoMessaging.NOTIFICATION_TYPE_URL) > 0; }
    public boolean hasReview() { return (type & WigzoMessaging.NOTIFICATION_TYPE_REVIEW) > 0; }
    public boolean hasMessage() { return (type & WigzoMessaging.NOTIFICATION_TYPE_MESSAGE) > 0; }
    public boolean hasCatgory() { return (type & WigzoMessaging.NOTIFICATION_TYPE_CATGORY) > 0;}
    public boolean isSilent() { return (type & WigzoMessaging.NOTIFICATION_TYPE_SILENT) > 0; }
    public boolean hasSoundUri() { return (type & WigzoMessaging.NOTIFICATION_TYPE_SOUND_URI) > 0; }
    public boolean hasSoundDefault() { return (type & WigzoMessaging.NOTIFICATION_TYPE_SOUND_DEFAULT) > 0; }
    public boolean hasPictureUrl() { return  (type & WigzoMessaging.NOTIFICATION_TYPE_PICTURE) > 0;}
    public boolean isUnknown() { return type == WigzoMessaging.NOTIFICATION_TYPE_UNKNOWN; }

    /**
     * Message is considered valid only when it has Wigzo ID and its type is determined
     * @return whether this message is valid or not
     */
    public boolean isValid() {
        String id = data.getString("c.i");
        return !isUnknown() && id != null && id.length() == 24;
    }

    /**
     * Depending on message contents, different intents can be run.
     * @return Intent
     */
    public Intent getIntent(Context context, Class <? extends Activity> activityClass) {
        if (hasLink()) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse(getLink()));
        } else if (hasReview()) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ("".equals(getReview()) ? context.getPackageName() : getReview())));
        } else if (hasMessage()) {
            Intent intent = new Intent(context, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            return intent;
        }else if (hasCatgory()){
            Intent intent = new Intent(context, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("catgory",getCatgory());

        }
        return null;
    }

    public String getNotificationTitle(Context context) {
        return WigzoMessaging.getAppTitle(context);
    }

    /**
     * @return Message for Notification or AlertDialog
     */
    public String getNotificationMessage() {
        if (hasLink()) {
            return hasMessage() ? getMessage() : "";
        } else if (hasReview()) {
            return hasMessage() ? getMessage() : "";
        } else if (hasMessage()) {
            return getMessage();
        }else if (hasCatgory()){
            return  getCatgory();
        }else if(hasPictureUrl()){
            return  getPicture();
        }
        return null;
    }

    @Override
    public String toString() {
        return data == null ? "empty" : data.toString();
    }

    @Override
    public int describeContents () {
        return 0;
    }

    @Override
    public void writeToParcel (Parcel dest, int flags) {
        dest.writeBundle(data);
    }
    public static final Creator<Message> CREATOR = new Creator<Message>() {
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    Message(Parcel in) {
        data = in.readBundle();
        type = setType();
    }
}
