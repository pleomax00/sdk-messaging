package wigzo.android.sdk.messaging;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import wigzo.android.sdk.Wigzo;
import wigzo.android.sdk.DeviceId;

/**
 * Wigzo Messaging
 *
 */
public class WigzoMessaging extends WakefulBroadcastReceiver {
    private static final String TAG = "WigzoMessaging";

    protected static final String NOTIFICATION_SHOW_DIALOG = "wigzo.android.api.messaging.dialog";
    protected static final String EXTRA_MESSAGE = "wigzo.android.api.messaging.message";

    protected static final String EVENT_OPEN    = "[CLY]_push_open";
    protected static final String EVENT_ACTION  = "[CLY]_push_action";

    protected static String[] buttonNames = new String[]{"Open", "Review"};

    protected static final int NOTIFICATION_TYPE_UNKNOWN  = 0;
    protected static final int NOTIFICATION_TYPE_MESSAGE  = 1;
    protected static final int NOTIFICATION_TYPE_URL      = 1 << 1;
    protected static final int NOTIFICATION_TYPE_REVIEW   = 1 << 2;

    protected static final int NOTIFICATION_TYPE_SILENT           = 1 << 3;

    protected static final int NOTIFICATION_TYPE_SOUND_DEFAULT    = 1 << 4;
    protected static final int NOTIFICATION_TYPE_SOUND_URI        = 1 << 5;
    protected static final int NOTIFICATION_TYPE_CATGORY          = 1 << 6;
    protected static final int NOTIFICATION_TYPE_PICTURE          = 1 << 7;

    /**
     * Action for Wigzo Messaging BroadcastReceiver.
     * Once message is arrived, Wigzo Messaging will send a broadcast notification with action "APP_PACKAGE_NAME.wigzo.messaging"
     * to which you can subscribe via BroadcastReceiver.
     */
    public static String BROADCAST_RECEIVER_ACTION_MESSAGE = "wigzo.android.api.messaging.broadcast.message";
    public static String getBroadcastAction (Context context) {
        try {
            ComponentName name = new ComponentName(context, WigzoMessagingService.class);
            Bundle data = context.getPackageManager().getServiceInfo(name, PackageManager.GET_META_DATA).metaData;
            return data.getString("broadcast_action");
        } catch (PackageManager.NameNotFoundException ignored) {
            Log.w(TAG, "Set broadcast_action metadata for WigzoMessagingService in AndroidManifest.xml to receive broadcasts about received messages.");
            return null;
        }
    }


    private static Context context;

    /**
     * Activity used for messages displaying.
     * When message arrives, Wigzo displays it either as a Notification, or as a AlertDialog. In any case,
     * this activity is used as a final destination.
     */
    private static Class<? extends Activity> activityClass;

    public static void setActivity(Activity activity) {
        setActivity(activity, activity.getClass());
    }

    public static void setActivity(Activity activity, Class<? extends Activity> claz) {
        context = activity.getApplicationContext();
        activityClass = claz == null ? activity.getClass() : claz;
    }
    protected static Context getContext() { return context; }
    protected static Class<? extends Activity> getActivityClass() { return activityClass; }

    @Override
    public void onReceive (Context context, Intent intent) {
        if (Wigzo.sharedInstance().isLoggingEnabled()) {
            Log.i(TAG, "Starting service @ " + SystemClock.elapsedRealtime());
        }

        ComponentName comp = new ComponentName(context.getPackageName(), WigzoMessagingService.class.getName());
        startWakefulService(context, intent.setComponent(comp));
        setResultCode(Activity.RESULT_OK);
    }



    private static final String PREFERENCES_NAME = "wigzo.android.api.messaging";
    private static final String PROPERTY_REGISTRATION_ID = "wigzo.android.api.messaging.registration.id";
    private static final String PROPERTY_REGISTRATION_VERSION = "wigzo.android.api.messaging.version";
    private static final String PROPERTY_REGISTRATION_SENDER = "wigzo.android.api.messaging.sender";
    private static final String PROPERTY_APPLICATION_TITLE = "wigzo.android.api.messaging.app.title";
    private static final String PROPERTY_SERVER_URL = "wigzo.android.api.messaging.server.url";
    private static final String PROPERTY_APP_KEY = "wigzo.android.api.messaging.app.key";
    private static final String PROPERTY_DEVICE_ID = "wigzo.android.api.messaging.device.id";
    private static final String PROPERTY_ORGANIZATIOND = "OrgId";
    private static final String PROPERTY_DEVICE_ID_MODE = "wigzo.android.api.messaging.device.id.mode";
    private static final String PROPERTY_ACTIVITY_CLASS = "wigzo.android.api.messaging.activity.class";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static GoogleCloudMessaging gcm;


    public static void init(Activity activity, Class<? extends Activity> activityClass, String sender, String[] buttonNames) {
        setActivity(activity, activityClass);

        if (gcm != null) {
            return;
        }

        if (buttonNames != null) {
            WigzoMessaging.buttonNames = buttonNames;
        }

        if (checkPlayServices(activity) ) {
            gcm = GoogleCloudMessaging.getInstance(activity);
            String registrationId = getRegistrationId(activity, sender);
            if (registrationId.isEmpty()) {
                registerInBackground(activity, sender);
            } else {
                Wigzo.sharedInstance().onRegistrationId(registrationId);
            }
        } else {
            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.w(TAG, "No valid Google Play Services APK found.");
            }
        }
    }

    public static void storeConfiguration(Context context, String serverURL, String appKey, String deviceID, DeviceId.Type idMode, String orgId) {
        String label = "App";
        try {
            label = context.getString(context.getApplicationInfo().labelRes);
        } catch (Throwable t) {
            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.wtf(TAG, "Couldn't find android:label='@string/app_name' resource, please set it in AndroidManifest.xml", t);
            }
        }

        if (Wigzo.sharedInstance().isLoggingEnabled()) {
            Log.i(TAG, "Storing configuration: " + label + ", " + serverURL + ", " + appKey + ", " + deviceID + ", " + idMode + ", " +orgId);
        }

        SharedPreferences.Editor editor = getGCMPreferences(context).edit()
                .putString(PROPERTY_APPLICATION_TITLE, label)
                .putString(PROPERTY_SERVER_URL, serverURL)
                .putString(PROPERTY_APP_KEY, appKey)
                .putString(PROPERTY_ORGANIZATIOND , orgId)
                .putString(PROPERTY_DEVICE_ID, deviceID)
                .putInt(PROPERTY_DEVICE_ID_MODE, idMode == null ? -1 : idMode.ordinal());

        if (activityClass != null) {
            editor.putString(PROPERTY_ACTIVITY_CLASS, activityClass.getName());
        }

        editor.commit();
    }

    @SuppressWarnings("unchecked")
    protected static boolean initWigzo(Context context) {
        String serverURL = getGCMPreferences(context).getString(PROPERTY_SERVER_URL, null);
        String appKey = getGCMPreferences(context).getString(PROPERTY_APP_KEY, null);
        String orgId = getGCMPreferences(context).getString(PROPERTY_ORGANIZATIOND, null);
        String deviceID = getGCMPreferences(context).getString(PROPERTY_DEVICE_ID, null);
        String activityClassName = getGCMPreferences(context).getString(PROPERTY_ACTIVITY_CLASS, null);

        DeviceId.Type idMode = null;
        int mode = getGCMPreferences(context).getInt(PROPERTY_DEVICE_ID_MODE, -1);
        if (mode != -1) idMode = DeviceId.Type.values()[mode];

        if (serverURL == null || appKey == null) {
            return false;
        } else {
            Wigzo.sharedInstance().init(context, serverURL, appKey, deviceID, idMode, orgId);
            try {
                activityClass = (Class<? extends Activity>) Class.forName(activityClassName);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Could not find class " + activityClassName, e);
            }
            return true;
        }
    }

    private static void registerInBackground(final Context context, final String sender) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    String registrationId = gcm.register(sender);
                    Wigzo.sharedInstance().onRegistrationId(registrationId);
                    storeRegistrationId(context, registrationId, sender);
                } catch (IOException ex) {
                    Log.e(TAG, "Failed to register for GCM identificator: " + ex.getMessage());
                }
                return null;
            }
        }.execute(null, null, null);
    }

    private static void storeRegistrationId(Context context, String regId, String sender) {
        int appVersion = getAppVersion(context);
        if (Wigzo.sharedInstance().isLoggingEnabled()) {
            Log.i(TAG, "Saving regId " + regId + " for sender ID " + sender + " for app version " + appVersion);
        }
        getGCMPreferences(context).edit()
                .putString(PROPERTY_REGISTRATION_ID, regId)
                .putString(PROPERTY_REGISTRATION_SENDER, sender)
                .putInt(PROPERTY_REGISTRATION_VERSION, appVersion).commit();
    }


    private static SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static boolean checkPlayServices(Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.w(TAG, "Unable to install Play Services.");
            }
            return false;
        }
        return true;
    }

    private static String getRegistrationId(Activity activity, String sender) {
        final SharedPreferences preferences = getGCMPreferences(activity);
        String registrationId = preferences.getString(PROPERTY_REGISTRATION_ID, "");

        if (registrationId.isEmpty()) {
            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.i(TAG, "Registration not found.");
            }
            return "";
        }

        int registeredVersion = preferences.getInt(PROPERTY_REGISTRATION_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(activity.getApplicationContext());
        if (registeredVersion != currentVersion) {
            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.i(TAG, "App version changed.");
            }
            return "";
        }

        String registeredSender = preferences.getString(PROPERTY_REGISTRATION_SENDER, "");
        if (!registeredSender.equals(sender)) {
            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.i(TAG, "Sender ID changed.");
            }
            return "";
        }

        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    protected static String getAppTitle(Context context) {
        return getGCMPreferences(context).getString(PROPERTY_APPLICATION_TITLE, "");
    }

    public static void recordMessageOpen(String messageId) {
        if (!Wigzo.sharedInstance().isInitialized()) {
            WigzoMessaging.initWigzo(getContext());
        }
        Map<String, String> segmentation = new HashMap<String, String>();
        segmentation.put("i", messageId);
        Wigzo.sharedInstance().recordEvent(EVENT_OPEN, segmentation, 1);
    }

    public static void recordMessageAction(String messageId) {
        if (!Wigzo.sharedInstance().isInitialized()) {
            WigzoMessaging.initWigzo(getContext());
        }
        Map<String, String> segmentation = new HashMap<String, String>();
        segmentation.put("i", messageId);
        Wigzo.sharedInstance().recordEvent(EVENT_ACTION, segmentation, 1);
    }
}
