package com.example.emotionalcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "AppSettings";
    private static final String BLOCK_SITES_KEY = "block_sites";
    private static final String BLOCKED_SITES_LIST = "blocked_sites";
    private static final String TIME_LIMIT_KEY = "time_limit_minutes";

    private EditText timeLimitInput;
    private Button saveTimeLimitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        timeLimitInput = findViewById(R.id.time_limit_input);
        saveTimeLimitButton = findViewById(R.id.save_time_limit_button);

        // Загружаем сохраненное состояние
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isBlocked = prefs.getBoolean(BLOCK_SITES_KEY, false);
        String blockedSites = prefs.getString(BLOCKED_SITES_LIST, "");
        int savedTimeLimit = prefs.getInt(TIME_LIMIT_KEY, 0);


        if (savedTimeLimit > 0) {
            timeLimitInput.setText(String.valueOf(savedTimeLimit));
        }


        // Сохраняем лимит времени
        saveTimeLimitButton.setOnClickListener(v -> {
            String input = timeLimitInput.getText().toString().trim();
            if (!input.isEmpty()) {
                int minutes = Integer.parseInt(input);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(TIME_LIMIT_KEY, minutes);
                editor.apply();
                Toast.makeText(this, "Лимит времени сохранён: " + minutes + " мин", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Введите корректное значение времени", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
