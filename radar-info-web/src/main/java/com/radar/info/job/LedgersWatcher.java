package com.radar.info.job;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.radarlab.client.api.exception.APIException;
import org.radarlab.client.ws.RadarWebSocketClient;
import org.radarlab.core.exception.RadarException;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LedgersWatcher implements InitializingBean{
    public static final ConcurrentMap<Integer, String> ledgers = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(LedgersWatcher.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(new CacheUpdater(), 0, 10, TimeUnit.SECONDS);
    }

    private class CacheUpdater implements Runnable{
        @Override
        public void run() {
            int ledgerClosed = ledgerClosed();
            try {
                for (int i = ledgerClosed; i > ledgerClosed - 30; i--) {
                    if (ledgers.containsKey(i)) {
                        continue;
                    }
                    logger.info("add ledger:" + i);
                    ledgers.put(i, ledger(i, false, false, true, false, true));
                    Integer[] ks = new Integer[ledgers.keySet().size()];
                    ledgers.keySet().toArray(ks);
                    List<Integer> keys = Arrays.asList(ks);
                    Collections.sort(keys, (o1, o2) -> o2 - o1);
                    if (keys.size() > 30) {
                        ledgers.keySet().forEach(key -> {
                            if (keys.indexOf(key) > 29) {
                                ledgers.remove(key);
                            }
                        });
                    }
                }
                logger.info("wath complete, now ledgers contains:" + ledgers.size() + " ledgers");
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
    public static String ledger(int ledgerIndex) throws APIException {
        return ledger(ledgerIndex, false, true, true, false, false);
    }

    /**
     * load ledger info
     *
     * @param ledgerIndex  ledger index number
     * @param full         if get full data
     * @param expand       if expand
     * @param transactions if contains txns
     * @param accounts     if contains accounts
     * @param dividend     if contains dividend txns
     * @return ledger data
     * @throws RadarException
     */
    public static String ledger(
            int ledgerIndex, boolean full, boolean expand,
            boolean transactions, boolean accounts, boolean dividend) throws APIException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 2);
        params.put("command", "ledger");
        params.put("ledger_index", ledgerIndex);
        params.put("full", full);
        params.put("expand", expand);
        params.put("transactions", transactions);
        params.put("accounts", accounts);
        params.put("dividend", dividend);
        String data = new Gson().toJson(params);
        return RadarWebSocketClient.request(RadarWebSocketClient.WebsocketServerType.FULL, data);
    }

    /**
     * get ledger closed value of radard
     *
     * @return
     */
    public static int ledgerClosed() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        params.put("command", "ledger_closed");

        String data = new Gson().toJson(params);
        try {
            String res = RadarWebSocketClient.request(RadarWebSocketClient.WebsocketServerType.FULL, data);
            JSONObject resJson = new JSONObject(res);
            if (resJson.has("status") && resJson.getString("status").equalsIgnoreCase("success")) {
                JSONObject jsonObjectResult = resJson.getJSONObject("result");
                return jsonObjectResult.getInt("ledger_index");
            }
        } catch (Exception e) {
            System.out.println("getLedgerClosed error:" + e.getMessage());
            logger.info("getLedgerClosed exception");
            logger.info("getLedgerClosed error:" + e.getMessage());
        }

        return 0;

    }

}
