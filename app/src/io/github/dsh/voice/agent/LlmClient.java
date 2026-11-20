package io.github.dsh.voice.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class LlmClient {
    private final Settings cfg;

    public LlmClient(Settings cfg) { this.cfg = cfg; }

    public JSONObject chat(org.json.JSONArray messages, org.json.JSONArray tools) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", cfg.model());
        body.put("messages", messages);
        if (tools != null && tools.length() > 0) body.put("tools", tools);
        body.put("temperature", 0.3);

        HttpURLConnection c = (HttpURLConnection) new URL(cfg.baseUrl() + "/chat/completions").openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(120000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + cfg.apiKey());

        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes("UTF-8"));
        }
        int code = c.getResponseCode();
        if (code / 100 != 2) {
            StringBuilder err = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getErrorStream(), "UTF-8"))) {
                String l; while ((l = r.readLine()) != null) err.append(l);
            }
            throw new Exception("HTTP " + code + ": " + err);
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
            String l; while ((l = r.readLine()) != null) sb.append(l);
        }
        return new JSONObject(sb.toString());
    }
}
