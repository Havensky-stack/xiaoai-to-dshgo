package io.github.dsh.voice.agent;

import org.json.JSONObject;

public interface Tool {
    int SAFE = 0, ASK = 1, DANGER = 2;

    String name();
    String description();
    JSONObject parameters();
    int risk();
    JSONObject execute(JSONObject args) throws Exception;
}
