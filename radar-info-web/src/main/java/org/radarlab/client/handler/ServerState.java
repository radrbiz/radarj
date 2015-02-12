package org.radarlab.client.handler;

import com.google.gson.Gson;
import org.radarlab.client.ClientProcessor;
import org.radarlab.client.util.HttpClient;
import org.radarlab.core.exception.RadarException;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerState implements ClientProcessor {

    public static final ServerState instance = new ServerState();
    private static final Logger logger = Logger.getLogger(ServerState.class);

    @Override
    public String processResponse(Map<String, String> params) throws RadarException {
        logger.info("get server state");

        Map<String, Object> postData = new HashMap<>();
        Map<String, Object> para = new HashMap<>();
        postData.put("method", "server_state");
        postData.put("params", Collections.singletonList(para));
        String data = new Gson().toJson(postData);
        HttpClient.Response response = HttpClient.post(URI, data);
        JSONObject json = new JSONObject(response.getResponseString());
        json.getJSONObject("result").getJSONObject("state").put("address", URI);
        if(!ADMIN_URI.startsWith("http://-")) {
            try {
                postData.clear();
                postData.put("method", "peers");
                postData.put("params", Collections.singletonList(para));
                data = new Gson().toJson(postData);
                logger.info("get data from:" + ADMIN_URI + ", data=" + data);
                response = HttpClient.post(ADMIN_URI, data);
                logger.info("get peers from : " + ADMIN_URI + ", result=" + response.getResponseString());
                JSONObject peers = new JSONObject(response.getResponseString());
                if (peers.getJSONObject("result").has("peers")) {
                    json.getJSONObject("result").put("peers", peers.getJSONObject("result").getJSONArray("peers"));
                }
            } catch (Exception e) {
            }
        }
        return json.toString();
    }
}


