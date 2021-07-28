package com.gae.scaffolder.plugin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMPlugin";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New token: " + token);
        FCMPlugin.sendTokenRefresh(token);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

        Log.d(TAG, "==> MyFirebaseMessagingService onMessageReceived");
        
        if(remoteMessage.getNotification() != null){
            Log.d(TAG, "\tNotification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "\tNotification Message: " + remoteMessage.getNotification().getBody());
        }
        
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("wasTapped", false);
        
        if(remoteMessage.getNotification() != null){
            data.put("title", remoteMessage.getNotification().getTitle());
            data.put("body", remoteMessage.getNotification().getBody());
        }

        for (String key : remoteMessage.getData().keySet()) {
            Object value = remoteMessage.getData().get(key);
            Log.d(TAG, "\tKey: " + key + " Value: " + value);
            data.put(key, value);
        }
        
        Log.d(TAG, "\tNotification Data: " + data.toString());
        FCMPlugin.sendPushPayload(data);
        if(remoteMessage.getNotification() == null){
            Log.d(TAG, "==> MyFirebaseMessagingService AddingToTray");
            addNotificationToTray(remoteMessage, data);
        }
    }
    // [END receive_message]

    public void addNotificationToTray(RemoteMessage remoteMessage, Map<String, Object> data) {
        String title = data.get("title") != null ? data.get("title").toString() : null;
        String body = data.get("body") != null ? data.get("body").toString() : null;;

        if(title == null && data.get("link") !=null){
            // "link":"tuya://action?a=view&ct=You have a visitor&cc=Battery dootrbell ,someone is ringing the bell. &p={\"media\":13}&devId=d7237b993d524bae47liql&type=doorbell&msgId=d126710f1625217589&ts=1625217589000
            String link = (String) data.get("link");
            if (link.startsWith("tuya")) {
                String[] pairs = link.split("&");
                for (String pair : pairs) {
                    String[] pairs2 = pair.split("=");
                    if (pairs2[0].startsWith("cc")) {
                        body = pairs2[1];
                    }
                    if (pairs2[0] == "ct") {
                        title = pairs2[1];
                    }
                }
                if (title == null || title.length() < 1) {
                    title = "You have a visitor";
                }
                if (body == null || body.length() < 1) {
                    body = "Someone is ringing your doorbell";
                }
            }
        }

        Intent intent = new Intent(this, FCMPluginActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.putExtra("devId", (String) data.get("devId"));
        intent.putExtra("openVideo", true);
        for (String key : data.keySet()) {
            Object value = data.get(key);
            intent.putExtra(key, value.toString());
        }

        int requestCode = 104; // random
        PendingIntent pendingIntent =PendingIntent.getActivity(this,
                requestCode,intent,PendingIntent.FLAG_ONE_SHOT);
        String channelId =  "10001";


        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
        int resId = getResources().getIdentifier("doorbell", "raw", getApplicationContext().getPackageName());
        Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ getApplicationContext().getPackageName() + "/" + resId);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(_getResource("ic_launcher", "mipmap"))
                .setContentTitle(title)
                .setSound(alarmSound)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setContentText(body);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {

            NotificationChannel mChannel = new NotificationChannel(channelId,
                    "Doorbell Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription(body);
            mChannel.enableLights(true);
            mChannel.enableVibration(false);
            mChannel.setSound(alarmSound, attributes); // This is IMPORTANT
            mChannel.setLightColor(Color.RED );
            mBuilder.setChannelId(channelId) ;
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(mChannel);
        }

        assert mNotificationManager != null;
        mNotificationManager.notify(( int ) System. currentTimeMillis (), mBuilder.build());


    }

    private int _getResource(String name, String type) {
        String package_name = getApplication().getPackageName();
        Resources resources = getApplication().getResources();
        return resources.getIdentifier(name, type, package_name);
    }
}
