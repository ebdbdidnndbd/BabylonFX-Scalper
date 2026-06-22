package com.babylonfx.scaler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class SignalService extends Service {
    private static final String CHANNEL_ID = "BabylonFX_Service_Channel";
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🏛️🦅 قناص بابل شغال")
                .setContentText("يتم فحص شارت الذهب دقيقة بدقيقة بالخلفية...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
        
        startForeground(1, notification);

        new Thread(() -> {
            Python py = Python.getInstance();
            PyObject sys = py.getModule("sys");
            sys.get("path").callAttr("append", this.getFilesDir().getAbsolutePath());

            while (isRunning) {
                try {
                    PyObject pyModule = py.getModule("rocket_brain");
                    PyObject result = pyModule.callAttr("check_gold_signal");
                    String res = result.toString();

                    if (res.startsWith("BUY")) {
                        String price = res.split("\\|")[1];
                        triggerNotification("📈 شراء الذهب فوراً (BUY)!", "سعر الدخول الحالي: " + price + "$ - اخطف الهدف واطلع.");
                    } else if (res.startsWith("SELL")) {
                        String price = res.split("\\|")[1];
                        triggerNotification("📉 بيع الذهب فوراً (SELL)!", "سعر الدخول الحالي: " + price + "$ - اخطف الهدف واطلع.");
                    }

                    Thread.sleep(60000); // فحص ميكانيكي دوري كل دقيقة
                } catch (Exception e) {
                    e.printStackTrace();
                    try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
                }
            }
        }).start();

        return START_STICKY;
    }

    private void triggerNotification(String title, String msg) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(msg)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();
        manager.notify((int) System.currentTimeMillis(), notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Babylon FX Run", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() { isRunning = false; super.onDestroy(); }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}

