package com.mlsd.proxse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.util.Log;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Build;

import androidx.annotation.RequiresApi;

/**
 * @hide
 */
public class ProxyService extends Service {

    private static ProxyServer server = null;

    /**
     * Keep these values up-to-date with PacManager.java
     */
    public static final String KEY_PROXY = "keyProxy";
    public static final String HOST = "localhost";
    // STOPSHIP This being a static port means it can be hijacked by other apps.
    public static final int PORT = 9998;
    public static final String EXCL_LIST = "";

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final String TAG = "ProxyService";
    public static final String CHANNEL_ID = "ProxyServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        /*if (server == null) {
            server = new ProxyServer();
            server.startServer();
        }*/
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stopServer();
            server = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IProxyCallback.Stub() {
            @Override
            public void getProxyData(IBinder callback) throws RemoteException {
                if (server != null) {
                    IProxyDataListener dataListener = IProxyDataListener.Stub.asInterface(callback);
                    if (dataListener != null) {
                        server.setCallback(dataListener);
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService();
                    }
                    //Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    //Toast.makeText(getApplicationContext(), "Foreground service is stopped.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    /* Used to build and start foreground service. */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForegroundService() {
        Log.d(TAG, "Start foreground service.");

        // Create notification default intent.
        createNotificationChannel();
        Intent intent = new Intent(this, ProxSeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(getText(R.string.notification_title))
                    .setContentText(getText(R.string.notification_message))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .setTicker(getText(R.string.ticker_text))
                    .build();
        }

        // Start foreground service.
        if (server == null) {
            server = new ProxyServer();
            server.startServer();
        }
        startForeground(1, notification);
    }

    private void stopForegroundService() {
        Log.d(TAG, "Stop foreground service.");

        if (server != null) {
            server.stopServer();
            server = null;
        }
        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}