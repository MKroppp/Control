package com.example.emotionalcontrol;

import static android.app.Activity.RESULT_OK;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenMonitoringService extends Service {
    private static final String TAG = "ScreenMonitoringService";
    private static final String CHANNEL_ID = "screen_monitoring";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private long lastScreenshotTime = 0;
    private static final int SCREENSHOT_INTERVAL_MS = 3000;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, new Notification.Builder(this, CHANNEL_ID).setContentTitle("Monitoring").build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = projectionManager.getMediaProjection(RESULT_OK, intent.getParcelableExtra("media_projection_data"));
            startCapture();
        }
        return START_STICKY;
    }

    private String saveBitmapToFile(Bitmap bitmap) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String fileName = "screenshot_" + timestamp + ".png";

            java.io.File file = new java.io.File(getExternalFilesDir(null), fileName);
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения файла", e);
            return null;
        }
    }

    private void startCapture() {
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "MediaProjection Stopped");
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
                }
                stopSelf();
            }
        }, null);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                0, imageReader.getSurface(), new VirtualDisplay.Callback() {
                    @Override
                    public void onStopped() {
                        super.onStopped();
                        Log.d(TAG, "Virtual Display Stopped");
                        stopSelf();
                    }
                }, null);

        imageReader.setOnImageAvailableListener(reader -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScreenshotTime < SCREENSHOT_INTERVAL_MS) {
                // Пропускаем кадр, если прошло меньше 3 секунд
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    image.close();
                }
                return;
            }
            lastScreenshotTime = currentTime;

            Image image = reader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                if (buffer != null) {
                    buffer.rewind();
                    bitmap.copyPixelsFromBuffer(buffer);
                }
                Intent broadcast = new Intent("com.example.emotionalcontrol.IMAGE_CAPTURED");
                String filePath = saveBitmapToFile(bitmap);
                if (filePath != null) {
                    broadcast.putExtra("image_path", filePath);
                    sendBroadcast(broadcast);
                }
                image.close();
            }
        }, null);

    }

    /*private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
    }*/

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Screen Monitoring", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }

}