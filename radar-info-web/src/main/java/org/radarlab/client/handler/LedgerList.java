package org.radarlab.client.handler;

import com.google.gson.Gson;
import org.radarlab.client.ClientProcessor;
import org.radarlab.client.util.Convert;
import org.radarlab.client.util.HttpClient;
import org.radarlab.core.exception.RadarException;
import org.radarlab.core.hash.B58;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LedgerList implements ClientProcessor {

    public static final LedgerList instance = new LedgerList();
    private static final Logger logger = Logger.getLogger(LedgerList.class);

    private static final Map<Integer, Map<String, Object>> ledgers = new ConcurrentHashMap<>();

    private static volatile Integer maxIndex = 0;

    @Override
    public String processResponse(Map<String, String> params) throws RadarException {
        String data = "{\"method\": \"ledger_closed\",\"params\": [{}]}";
        HttpClient.Response response = HttpClient.post(URI, data);
        JSONObject obj = new JSONObject(response.getResponseString());
        int index = obj.getJSONObject("result").getInt("ledger_index");
        int curIndex = getLedger(index);
        if(maxIndex>0 && curIndex - maxIndex > 50){
            maxIndex = curIndex - 50;
        }
        if(curIndex - maxIndex >1 && maxIndex > 0){
            for(int i=maxIndex +1;i<curIndex;i++){
                getLedger(i);
            }
        }
        if(curIndex > maxIndex){
            maxIndex = curIndex;
        }

        Integer[] ks = new Integer[ledgers.keySet().size()];
        ledgers.keySet().toArray(ks);
        List<Integer> keys = Arrays.asList(ks);
        Collections.sort(keys, (o1, o2) -> o2 - o1);
        if (keys.size() > 50) {
            ledgers.keySet().forEach(key -> {
                if (keys.indexOf(key) > 49) {
                    ledgers.remove(key);
                }
            });
        }
        List<Map<String, Object>> list = new ArrayList<>();
        keys.forEach(key->{
            if(list.size() <=50 &&ledgers.get(key) != null)
                list.add(ledgers.get(key));
        });
        return new Gson().toJson(list);
    }

    private int getLedger(int index) throws RadarException {
        Gson gson = new Gson();
        Map<String, Object> postData = new HashMap<>();
        Map<String, Object> para = new HashMap<>();
        postData.put("method", "ledger");
        para.put("ledger_index", index);
        para.put("accounts", false);
        para.put("full", false);
        para.put("expand", false);
        para.put("transactions", false);
        postData.put("params", Collections.singletonList(para));
        String data = gson.toJson(postData);
        HttpClient.Response response = HttpClient.post(URI, data);
//        logger.info("method=ledger, response=" + response.getResponseString());
        JSONObject json = new JSONObject(response.getResponseString());
        try {
            if (json.has("result") && json.getJSONObject("result").has("ledger")) {
                json = json.getJSONObject("result").getJSONObject("ledger");

                if (json.has("ledger_index")) {
                    int currentIndex = json.getInt("ledger_index");
                    if (!ledgers.containsKey(currentIndex)) {
                        Map ledger = new HashMap<>();
                        ledger.put("ledger_index", json.getInt("ledger_index"));
                        ledger.put("ledger_hash", json.getString("ledger_hash"));
                        ledger.put("close_time_human", json.getString("close_time_human"));
                        ledger.put("creator_address", B58.getInstance().encodeToString(Convert.hexToBytes(json.getString("account_hash"))));
                        ledger.put("closed", json.getBoolean("closed"));
                        ledger.put("total_coins", json.getString("total_coins"));
                        if (json.has("total_coinsVBC"))
                            ledger.put("total_coinsVBC", json.getString("total_coinsVBC"));
                        System.out.println(json.getString("transaction_hash"));
                        ledger.put("transaction_hash", json.getString("transaction_hash"));
                        ledgers.put(currentIndex, ledger);
                    }
                    return currentIndex;
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return 0;
    }
}
