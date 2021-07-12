package com.gae.scaffolder.plugin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
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
            addNotificationToTray(remoteMessage, data);
        }
    }
    // [END receive_message]
    
    public void addNotificationToTray(RemoteMessage remoteMessage, Map<String, Object> data) {
        String title = data.get("title") != null ? data.get("title").toString() : null;
        String body = data.get("body") != null ? data.get("body").toString() : null;;

        if(title == null && data.get("link") !=null){
            // Trying to extract title from link (in case of tuya)
            // TODO: Need to extract it from link.
            // "link":"tuya://action?a=view&ct=You have a visitor&cc=Battery dootrbell ,someone is ringing the bell. &p={\"media\":13}&devId=d7237b993d524bae47liql&type=doorbell&msgId=d126710f1625217589&ts=1625217589000"
            title = "Doorbell";
            body = "Someone at doorbell";
        }
        Intent intent = new Intent(this, FCMPluginActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        for (String key : data.keySet()) {
            Object value = data.get(key);
            intent.putExtra(key, value.toString());
        }

        int requestCode = 104; // random
        PendingIntent pendingIntent =PendingIntent.getActivity(this,
                requestCode,intent,PendingIntent.FLAG_ONE_SHOT);
        String channelId =  "alerts";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder;
        notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(_getResource("ic_launcher", "mipmap"))
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        // .setSound(defaultSoundUri)
                        .setChannelId(channelId)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);
                NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        notificationManager.notify(requestCode, notificationBuilder.build());
        // [END receive_message]
    }

    private int _getResource(String name, String type) {
        String package_name = getApplication().getPackageName();
        Resources resources = getApplication().getResources();
        return resources.getIdentifier(name, type, package_name);
    }
}
