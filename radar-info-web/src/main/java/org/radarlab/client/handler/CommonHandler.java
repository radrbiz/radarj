package org.radarlab.client.handler;

import com.google.gson.Gson;
import org.radarlab.client.ClientProcessor;
import org.radarlab.client.util.HttpClient;
import org.radarlab.core.exception.RadarException;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommonHandler implements ClientProcessor {

    public static final CommonHandler instance = new CommonHandler();
    @Override
    public String processResponse(Map<String, String> params) throws RadarException {
        String type = params.get("type");
        Map<String, Object> postData = new HashMap<>();
        Map<String, Object> para = new HashMap<>();
        if("tx".equals(type)){
            postData.put("method", "tx");
            para.put("transaction", params.get("address"));
            para.put("binary", false);
        }else if("ledgerInfo".equals(type)){
            postData.put("method", "ledger");
            para.put("ledger_index", NumberUtils.createInteger(params.get("address")));
            para.put("accounts", false);
            para.put("expand", true);
            para.put("full", false);
            para.put("transactions", true);
        }else if("accountTxs".equals(type)){
            postData.put("method", "account_tx");
            para.put("account", params.get("address"));
            para.put("binary", false);
//            para.put("count", false);
//            para.put("descending", false);
//            para.put("forward", false);
//            para.put("ledger_index_max", -1);
            para.put("ledger_index_min", -1);
            para.put("limit", 20);
//            para.put("offset", 1);
        }else if("accountOffers".equals(type)){
            postData.put("method", "account_offers");
            para.put("account", params.get("address"));
            para.put("ledger_index", "current");
        }else if("accountLines".equals(type)){
            postData.put("method", "account_lines");
            para.put("account", params.get("address"));
            para.put("ledger_index", "current");
        }else if("accountInfo".equals(type)){
            postData.put("method", "account_info");
            para.put("account", params.get("address"));
            para.put("ledger_index", "validated");
            para.put("strict", true);
        }
        postData.put("params", Collections.singletonList(para));
        String data = new Gson().toJson(postData);
        System.out.println(data);
        String result = HttpClient.post(URI, data).getResponseString();
//        System.out.println(result);
        return result;
    }
}
