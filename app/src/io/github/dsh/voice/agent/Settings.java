package io.github.dsh.voice.agent;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private final SharedPreferences sp;

    public Settings(Context c) {
        sp = c.getSharedPreferences("cfg", Context.MODE_PRIVATE);
    }

    public String baseUrl() {
        String v = sp.getString("base_url", "");
        if (v == null || v.isEmpty()) v = "https://api.deepseek.com/v1";
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    public String apiKey() { return sp.getString("api_key", ""); }
    public String model() {
        String v = sp.getString("model", "");
        return (v == null || v.isEmpty()) ? "deepseek-chat" : v;
    }

    public boolean ready() { return !baseUrl().isEmpty() && !apiKey().isEmpty(); }

    public void save(String baseUrl, String key, String model) {
        sp.edit().putString("base_url", baseUrl.trim())
                 .putString("api_key", key.trim())
                 .putString("model", model.trim()).apply();
    }
}
