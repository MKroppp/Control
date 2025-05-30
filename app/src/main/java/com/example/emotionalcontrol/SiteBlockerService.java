package com.example.emotionalcontrol;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import java.util.Arrays;
import java.util.List;
import android.util.Log;

public class SiteBlockerService extends AccessibilityService {
    private static final String PREFS_NAME = "AppSettings";
    private static final String BLOCK_SITES_KEY = "block_sites";
    private static final String BLOCKED_SITES_LIST = "blocked_sites";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("SiteBlockerService", "onAccessibilityEvent вызван");

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d("SiteBlockerService", "Тип события: TYPE_WINDOW_CONTENT_CHANGED");

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean isBlocked = prefs.getBoolean(BLOCK_SITES_KEY, false);
            String blockedSites = prefs.getString(BLOCKED_SITES_LIST, "");

            // Преобразуем строку в список сайтов
            List<String> blockedSitesList = Arrays.asList(blockedSites.split(","));

            if (isBlocked) {
                String eventText = event.getText().toString().toLowerCase();
                Log.d("SiteBlockerService", "Текст на экране: " + eventText);

                for (String site : blockedSitesList) {
                    if (!site.trim().isEmpty() && eventText.contains(site.trim())) {
                        Log.d("SiteBlockerService", "Обнаружен запрещенный сайт: " + site);
                        performGlobalAction(GLOBAL_ACTION_BACK); // Закрывает браузер
                        Toast.makeText(this, "Доступ к " + site + " заблокирован", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
