package org.radarlab.client.handler;

import com.google.gson.Gson;
import org.radarlab.client.ClientProcessor;
import org.radarlab.client.util.HttpClient;
import org.radarlab.core.exception.RadarException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LedgerData implements ClientProcessor {
    public static final LedgerData instance = new LedgerData();

    @Override
    public String processResponse(Map<String, String> params) throws RadarException {
        Integer index = NumberUtils.createInteger(params.get("index"));
        Gson gson = new Gson();
        Map<String, Object> postData = new HashMap<>();
        Map<String, Object> para = new HashMap<>();
        postData.put("method", "ledger_data");
        para.put("ledger_index", index);
        para.put("binary", false);
        para.put("limit", 256);
        postData.put("params", Collections.singletonList(para));
        String data = gson.toJson(postData);
        HttpClient.Response response = HttpClient.post(URI, data);
        JSONObject json = new JSONObject(response.getResponseString());
        postData = new HashMap<>();
        para = new HashMap<>();
        postData.put("method", "ledger");
        para.put("ledger_index", index);
        para.put("accounts", false);
        para.put("full", false);
        para.put("expand", true);
        para.put("transactions", true);
        para.put("dividend", true);
        postData.put("params", Collections.singletonList(para));
        data = gson.toJson(postData);
        response = HttpClient.post(URI, data);
        JSONObject tmp = new JSONObject(response.getResponseString());
        JSONObject ledger = tmp.getJSONObject("result").getJSONObject("ledger");

        json.getJSONObject("result").put("transactions", ledger.getJSONArray("transactions"));
        json.getJSONObject("result").put("totalCoins", ledger.getString("total_coins"));
        if (ledger.has("totalCoinsVBC"))
            json.getJSONObject("result").put("totalCoinsVBC", ledger.getString("total_coinsVBC"));

        return json.toString();
    }
}
