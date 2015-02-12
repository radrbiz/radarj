package org.radarlab.client.api.render;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class ErrorRender {
    public static String render(int code, String message){
        Map<String, Object> render = new HashMap<>();
        render.put("code", code);
        render.put("message", message);
        return new Gson().toJson(render);
    }
}
