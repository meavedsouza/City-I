package com.city_i.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.city_i.MainActivity;
import com.city_i.R;
import com.city_i.dashboard.IssueDetailActivity;
import com.city_i.models.NotificationModel;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.Map;
import java.util.Random;

public class NotificationService extends FirebaseMessagingService {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "civic_connect_channel";
    private static final String CHANNEL_NAME = "CivicConnect Notifications";
    private static final String CHANNEL_DESC = "Notifications for civic issue updates";

    private static NotificationService instance;
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        context = getApplicationContext();
        createNotificationChannel();
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        // Send token to your server if needed
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            sendNotification(title, body, null);
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            String body = data.get("body");
            String issueId = data.get("issueId");
            String type = data.get("type");
            String priority = data.get("priority");

            // Create notification model
            NotificationModel notification = new NotificationModel();
            notification.setId(generateNotificationId());
            notification.setTitle(title != null ? title : "CivicConnect Update");
            notification.setMessage(body != null ? body : "New update on your reported issue");
            notification.setIssueId(issueId);
            notification.setType(type != null ? type : "update");
            notification.setPriority(priority != null ? priority : "normal");
            notification.setTimestamp(new Date());
            notification.setRead(false);

            // Save to local database
            saveNotification(notification);

            // Send notification
            sendNotification(title, body, issueId);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNotification(String title, String message, String issueId) {
        if (title == null) title = "CivicConnect";
        if (message == null) message = "New update available";

        Intent intent;

        if (issueId != null && !issueId.isEmpty()) {
            // Open specific issue detail
            intent = new Intent(this, IssueDetailActivity.class);
            intent.putExtra("ISSUE_ID", issueId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            // Open main activity
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Default notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Large icon (app logo)
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        // Add actions based on notification type
        if (issueId != null) {
            // Add "View Details" action
            NotificationCompat.Action viewAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_view,
                    "View Details",
                    pendingIntent
            ).build();

            notificationBuilder.addAction(viewAction);
        }

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        int notificationId = generateNotificationId();
        notificationManager.notify(notificationId, notificationBuilder.build());

        Log.d(TAG, "Notification sent with ID: " + notificationId);
    }

    // Public static methods for sending notifications from anywhere
    public static void sendLocalNotification(Context context, String title, String message) {
        sendLocalNotification(context, title, message, null);
    }

    public static void sendLocalNotification(Context context, String title, String message, String issueId) {
        if (instance == null) {
            instance = new NotificationService();
            instance.context = context;
            instance.createNotificationChannel();
        }

        instance.sendNotification(title, message, issueId);
    }

    public static void sendIssueStatusNotification(Context context, String issueId,
                                                   String issueTitle, String newStatus) {
        String title = "Issue Status Updated";
        String message = "Your issue \"" + issueTitle + "\" is now " + newStatus;

        sendLocalNotification(context, title, message, issueId);
    }

    public static void sendPriorityAlert(Context context, String issueId, String priority) {
        String title = "⚠️ High Priority Alert";
        String message = "An issue near you has been marked as " + priority + " priority";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setColor(Color.RED)
                .setLights(Color.RED, 1000, 1000)
                .setVibrate(new long[]{0, 500, 200, 500});

        if (issueId != null) {
            Intent intent = new Intent(context, IssueDetailActivity.class);
            intent.putExtra("ISSUE_ID", issueId);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setContentIntent(pendingIntent);
        }

        NotificationManagerCompat.from(context).notify(generateNotificationId(), builder.build());
    }

    public static void sendGreenImpactNotification(Context context, int issuesResolved) {
        String title = "🌱 Green Impact Update";
        String message = "Your reports have helped resolve " + issuesResolved + " issues!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_eco)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(Color.GREEN);

        NotificationManagerCompat.from(context).notify(generateNotificationId(), builder.build());
    }

    public static void sendDailyDigest(Context context, int newIssues, int resolvedIssues) {
        String title = "📊 Daily Civic Digest";
        String message = newIssues + " new issues reported, " + resolvedIssues + " resolved today";

        sendLocalNotification(context, title, message);
    }

    // Utility methods
    private static int generateNotificationId() {
        return new Random().nextInt(9999 - 1000) + 1000;
    }

    private void saveNotification(NotificationModel notification) {
        // Save notification to local database (Room/SQLite)
        // For hackathon, you can use SharedPreferences or simple file storage
        // TODO: Implement database storage
    }

    // Notification settings
    public static boolean areNotificationsEnabled(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void setNotificationSound(Context context, Uri soundUri) {
        // Implement notification sound preference
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        // Implement vibration preference
    }

    // Clear all notifications
    public static void clearAllNotifications(Context context) {
        NotificationManagerCompat.from(context).cancelAll();
    }

    // Channel management (for Android O+)
    public static void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}