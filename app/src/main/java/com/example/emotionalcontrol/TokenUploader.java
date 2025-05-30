package com.example.emotionalcontrol;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class TokenUploader {
    private static final String SERVER_URL = "http://192.168.0.101:5000/register_token";

    public static void uploadTokenToServer(String token) {
        Log.d("FCM", "Uploading token to server: " + token);

        JSONObject json = new JSONObject();
        try {
            json.put("token", token);
        } catch (JSONException e) {
            Log.e("FCM", "JSON error: " + e.getMessage());
            return;
        }

        // Используем глобальный context
        RequestQueue requestQueue = Volley.newRequestQueue(AppContextProvider.getContext());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, SERVER_URL, json,
                response -> Log.d("FCM", "Token uploaded successfully: " + response),
                error -> Log.e("FCM", "Token upload failed: " + error.getMessage())
        );

        requestQueue.add(request);
    }

}
