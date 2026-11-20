package io.github.dsh.voice.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry register(Tool t) { tools.put(t.name(), t); return this; }

    public JSONArray definitions() {
        JSONArray arr = new JSONArray();
        for (Tool t : tools.values()) {
            try {
                JSONObject f = new JSONObject();
                f.put("name", t.name());
                f.put("description", t.description());
                f.put("parameters", t.parameters());
                JSONObject o = new JSONObject();
                o.put("type", "function");
                o.put("function", f);
                arr.put(o);
            } catch (Exception ignored) { }
        }
        return arr;
    }

    public String nameList() {
        List<String> n = new ArrayList<>(tools.keySet());
        return n.toString();
    }

    public String execute(String name, String argsJson) {
        try {
            Tool t = tools.get(name);
            if (t == null) return "{\"ok\":false,\"error\":\"unknown tool: " + name + "\"}";
            JSONObject args = (argsJson == null || argsJson.isEmpty()) ? new JSONObject() : new JSONObject(argsJson);
            JSONObject r = t.execute(args);
            if (r == null) r = new JSONObject();
            if (!r.has("ok")) r.put("ok", true);
            return r.toString();
        } catch (Throwable e) {
            try {
                return new JSONObject().put("ok", false).put("error", String.valueOf(e.getMessage())).toString();
            } catch (Exception ignored) {
                return "{\"ok\":false}";
            }
        }
    }
}
