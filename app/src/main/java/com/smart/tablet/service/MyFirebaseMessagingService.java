package com.smart.tablet.service;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.smart.tablet.Constants;
import com.smart.tablet.entities.Setting;
import com.smart.tablet.tasks.StoreSetting;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String MESSAGE_RECEIVED = MyFirebaseMessagingService.class.getName() + ":MESSAGE_RECEIVED";
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        storeToken(s);
    }

    private void storeToken(String token) {
        new StoreSetting(this, new Setting(Constants.DEVICE_ID, token))
                .execute();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();

            if(data.get("type") != null) {
                if (data.get("type").equals("notification")) {
                    Intent i = new Intent(MESSAGE_RECEIVED);

                    for (Map.Entry<String, String> entry :
                            data.entrySet()) {
                        i.putExtra(entry.getKey(), entry.getValue());
                    }

                    sendBroadcast(i);
                } else if (data.get("type").equals("command")) {
                    String command = data.get("command");
                    Intent intent = null;

                    switch (command) {
                        case Constants.COMMAND_EXECUTE_SEND_REPORT:
                            intent = new Intent(this, SendAnalytics.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                            break;
                        case Constants.COMMAND_PING:
                            intent = new Intent(this, PingResponse.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                            break;
                    }
                }
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();

        Log.d(TAG, "notification deleted");
    }
}