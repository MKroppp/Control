package com.example.emotionalcontrol;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.DataOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 2000;
    private static final int REQUEST_CODE_CAPTURE_SCREEN = 1000;
    private MediaProjectionManager mProjectionManager;
    //private Interpreter interpreter;
    private Button startButton, stopButton;
    private Executor executor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.btn_stop_service);
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        requestPermissions();

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        startButton.setOnClickListener(v -> startScreenCapture());
        stopButton.setOnClickListener(v -> stopScreenCapture());

        IntentFilter filter = new IntentFilter("com.example.emotionalcontrol.IMAGE_CAPTURED");
        BroadcastReceiver imageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imagePath = intent.getStringExtra("image_path");
                /*if (imagePath != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    processImage(bitmap);
                }*/
                if (imagePath != null) {
                    uploadImageToServer(imagePath);
                }
            }
        };
        registerReceiver(imageReceiver, filter, Context.RECEIVER_EXPORTED);
        /*loadModel();*/

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM token failed", task.getException());
                        return;
                    }

                    // Получен токен
                    String token = task.getResult();
                    Log.d("FCM", "FCM Token вручную: " + token);
                    TokenUploader.uploadTokenToServer(token);
                });
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO

        };
        boolean granted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        if (!granted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startScreenCapture() {
        Intent screenCaptureIntent = mProjectionManager.createScreenCaptureIntent();
        startActivityForResult(screenCaptureIntent, REQUEST_CODE_CAPTURE_SCREEN);
    }

    private void stopScreenCapture() {
        Intent intent = new Intent(this, ScreenMonitoringService.class);
        stopService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE_SCREEN && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, ScreenMonitoringService.class);
            intent.putExtra("media_projection_data", data);
            ContextCompat.startForegroundService(this, intent);
        } else {
            Toast.makeText(this, "Захват экрана не разрешён", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToServer(String imagePath) {
        File file = new File(imagePath);
        if (!file.exists()) {
            Log.e(TAG, "Файл не существует: " + imagePath);
            return;
        }

        Log.d(TAG, "Отправка изображения на сервер: " + imagePath);

        executor.execute(() -> {
            try {
                String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                URL url = new URL("http://192.168.0.101:5000/upload");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + file.getName() + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: image/png" + lineEnd);
                outputStream.writeBytes(lineEnd);

                FileInputStream inputStream = new FileInputStream(file);
                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                inputStream.close();
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Изображение успешно отправлено!");
                    deleteScreenshot(imagePath);
                } else {
                    Log.e(TAG, "Ошибка при отправке изображения, код ответа: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка отправки изображения", e);
            }
        });
    }

    private void deleteScreenshot(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "Файл удален: " + imagePath);
            } else {
                Log.e(TAG, "Не удалось удалить файл: " + imagePath);
            }
        }
    }


    /*private void processImage(Bitmap bitmap) {
        if (bitmap == null) return;

        executor.execute(() -> {

            int width = interpreter.getInputTensor(0).shape()[1];
            int height = interpreter.getInputTensor(0).shape()[2];

            ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap, width, height);

            int[] outputShape = interpreter.getOutputTensor(0).shape();
            float[][] result = new float[outputShape[0]][outputShape[1]];

            interpreter.run(byteBuffer, result);

            Log.d(TAG, "Результат модели: " + Arrays.deepToString(result));

            int classIndex = argmax(result[0]);
            float confidence = result[0][classIndex];

            Log.d(TAG, "Предсказанный класс: " + classIndex + ", уверенность: " + confidence);

            if ((classIndex == 1 || classIndex == 3 || classIndex == 4) && confidence >= 0.5) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Обнаружен нежелательный контент!", Toast.LENGTH_SHORT).show());
            }
        });
    }*/

    /*private MappedByteBuffer loadModelFile(String modelPath) throws Exception {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getLength());
    }*/

    /*private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap, int width, int height) {

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        int inputSize = width * height * 3 * 4; // RGB

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(inputSize);
        byteBuffer.order(java.nio.ByteOrder.nativeOrder());

        int[] pixels = new int[width * height];
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int pixel : pixels) {
            byteBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
            byteBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
            byteBuffer.putFloat((pixel & 0xFF) / 255.0f);        // B
        }

        scaledBitmap.recycle();
        byteBuffer.rewind();
        return byteBuffer;
    }*/

    /*private int argmax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }*/

    /*private void loadModel() {
        try {
            MappedByteBuffer model = loadModelFile("2.tflite");
            interpreter = new Interpreter(model);
            int[] inputShape = interpreter.getInputTensor(0).shape();
            Log.d(TAG, "Размер входного тензора: " + Arrays.toString(inputShape));

            Log.d(TAG, "Модель успешно загружена");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки модели", e);
        }
    }*/
}
