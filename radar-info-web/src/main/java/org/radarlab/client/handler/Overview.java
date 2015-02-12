package org.radarlab.client.handler;

import com.google.gson.Gson;
import org.radarlab.client.ClientProcessor;
import org.radarlab.client.util.HttpClient;
import org.radarlab.core.exception.RadarException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Overview implements ClientProcessor{

    public static final Overview instance = new Overview();
    @Override
    public String processResponse(Map<String, String> params) throws RadarException {
        String resp = HttpClient.get(MODEL_SERVER).getResponseString();
        JSONObject json = new JSONObject(resp);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> result = new HashMap<>();
        result.put("ledger_index", json.getLong("ledger_index"));
        result.put("ledger_time", format.format(new Date(json.getLong("ledger_time"))));
        try {
            result.put("totalCoins", json.getString("totalCoins"));
            result.put("totalCoinsVBC", json.getString("totalCoinsVBC"));
        }catch (Exception ex){
            result.put("totalCoins", json.getInt("totalCoins"));
            result.put("totalCoinsVBC", json.getInt("totalCoinsVBC"));
        }
        result.put("account_count", json.getLong("account_count") + 1214971 + 9);
        result.put("tx_count", json.getLong("tx_count"));
        return  new Gson().toJson(result);
    }
}
