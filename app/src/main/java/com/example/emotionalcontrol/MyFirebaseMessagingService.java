package com.example.emotionalcontrol;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();

        NotificationHelper.showNotification(this, title, body);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM", "FCM Token: " + token);
        TokenUploader.uploadTokenToServer(token);
    }
}
