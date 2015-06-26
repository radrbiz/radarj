package org.radarlab.api;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.radarlab.client.api.exception.APIException;
import org.radarlab.client.ws.RadarWebSocketClient;
import org.radarlab.core.AccountID;
import org.radarlab.core.AccountLine;
import org.radarlab.core.IssuerLine;

import java.util.*;


/**
 * User account implements, defined by WebSocket API of Radar.
 */
public class AccountImpl {
    private static final Logger logger = Logger.getLogger(AccountImpl.class);
    
    /**
     * API impl: get account basic info
     *
     * {
     * "id": 2,
     * "command": "account_info",
     * "account": "r9cZA1mLK5R5Am25ArfXFmqgNwjZgnfk59",
     * "strict": true,
     * "ledger_index": "validated"
     * }
     * @param address
     * @return
     */
    public String getAccountInfo(String address) throws APIException {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("id", 0);
        requestData.put("command", "account_info");
        requestData.put("account", address);
        requestData.put("strict", true);
        requestData.put("ledger_index", "validated");
        String data = new Gson().toJson(requestData);
        String accountInfo = null;
        try {
            accountInfo = RadarWebSocketClient.request(data);
        } catch (APIException e) {
            if (e.code.compareTo(APIException.ErrorCode.ADDRESS_NOT_FOUND) == 0) {
                accountInfo = formatNotFoundUser(address).toString();
            } else {
                throw new APIException(e.code, e.getMessage());
            }
        }
        return accountInfo;
    }

    /**
     * API impl: get account receive_currencies and send_currencies
     */
    public String accountCurrencies(String address) throws APIException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("command", "account_currencies");
        data.put("account", address);

        String postData = new Gson().toJson(data);
        String json = RadarWebSocketClient.request(postData);
        return json;
    }


    /**
     * API impl: get account AccountLines info
     * {
     * "id": 1,
     * "command": "account_lines",
     * "account": "r9cZA1mLK5R5Am25ArfXFmqgNwjZgnfk59",
     * "ledger": "current"
     * }
     *
     * @param address
     * @return
     * @throws APIException
     */
    public String getAccountLines(String address, String peer) throws APIException {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("id", 0);
        requestData.put("command", "account_lines");
        requestData.put("account", address);
        requestData.put("ledger", "current");
        requestData.put("peer", peer);
        String data = new Gson().toJson(requestData);
        try {
//            List<JSONObject> accountLineList = new ArrayList<>();
//            Map<String, IssuerLine> issuerLines = new HashMap<>();


            TreeMap<String, IssuerLine> resultMap = new TreeMap<>();
            String accountInfo;
            try {
                accountInfo = RadarWebSocketClient.request(data);
                JSONObject json = new JSONObject(accountInfo);
                JSONArray lines = json.getJSONObject("result").getJSONArray("lines");
                for (int i = 0; i < lines.length(); i++) {
                    JSONObject line = lines.getJSONObject(i);
                    AccountLine accountLine = AccountLine.fromJSON(AccountID.fromAddress(address), line);
                    String currency = line.getString("currency");
                    Double balance = line.getDouble("balance");
                    String account = line.getString("account");
                    String issuer = address;
                    Double limit = line.getDouble("limit");
                    if (balance > 0) {
                        issuer = account;
                    } else if (balance == 0 && limit > 0) {
                        issuer = account;
                    }
                    String key = currency+"#"+issuer;
                    IssuerLine issuerLine = new IssuerLine();
                    if(resultMap.containsKey(key)){
                        issuerLine = resultMap.get(key);
                    }else{
                        issuerLine.setCurrency(currency);
                        issuerLine.setIssuer(issuer);
                        resultMap.putIfAbsent(key, issuerLine);
                    }
                    issuerLine.setAmount(issuerLine.getAmount() + balance);
                    issuerLine.getLines().add(line);
                }
            } catch (APIException e) {
                if (e.code.compareTo(APIException.ErrorCode.ADDRESS_NOT_FOUND) == 0) {
                    //
                } else {
                    throw new APIException(e.code, e.getMessage());
                }
            }

            JSONObject linesMap = new JSONObject();
            linesMap.put("issuer_lines", resultMap.values());
            linesMap.put("account", address);

            return linesMap.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new APIException(APIException.ErrorCode.REMOTE_ERROR, "Can not retrive account info through method \"account_info\"");
        }
    }


    /**
     * @param address
     * @return
     */
    public int getUserCurrentSequence(String address) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("id", 0);
        requestData.put("command", "account_info");
        requestData.put("account", address);
        String data = new Gson().toJson(requestData);
        String accountInfo;

        try {
            accountInfo = RadarWebSocketClient.request(data);
            JSONObject json = new JSONObject(accountInfo);
            String status = json.getString("status");
            if (!status.equalsIgnoreCase("success")) {  //result not success
                return -1;
            }
            JSONObject jsonAccountData = json.getJSONObject("result").getJSONObject("account_data");
            int sequence = jsonAccountData.getInt("Sequence");
            return sequence;
        } catch (APIException e) {
            e.printStackTrace();
        }

        return -1;
    }


    /**
     * {
     * "id": 1,
     * "command": "account_lines",
     * "account": "r9cZA1mLK5R5Am25ArfXFmqgNwjZgnfk59",
     * "ledger": "current"
     * }
     *
     * @param address
     * @return
     * @throws APIException
     */
    public String getAccountLinesCurrency(String address) throws APIException {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("id", 0);
        requestData.put("command", "account_lines");
        requestData.put("account", address);
        requestData.put("ledger", "current");
        String data = new Gson().toJson(requestData);
        try {
            String accountInfo = RadarWebSocketClient.request(data);
            JSONObject json = new JSONObject(accountInfo);
            JSONArray lines = json.getJSONObject("result").getJSONArray("lines");
            List<JSONObject> accountLineList = new ArrayList<>();
//            List<JSONObject> issuerLineList = new ArrayList<>();
            Map<String, IssuerLine> issuerLines = new HashMap<>();
            for (int i = 0; i < lines.length(); i++) {
                JSONObject line = lines.getJSONObject(i);
                AccountLine accountLine = AccountLine.fromJSON(AccountID.fromAddress(address), line);
                if (!accountLine.balance.issuer().address.equals(address)) {
                    IssuerLine il = issuerLines.get(accountLine.currency.humanCode());
                    if (il == null) {
                        il = new IssuerLine();
                        List<JSONObject> issuerLineList = new ArrayList<>();
                        issuerLineList.add(line);
                        il.setAmount(accountLine.balance.doubleValue());
                        il.setCurrency(accountLine.currency.humanCode());
                        il.setLines(issuerLineList);
                        issuerLines.put(accountLine.currency.humanCode(), il);
                    } else {
                        il.setAmount(il.getAmount() + (accountLine.balance.doubleValue()));
                        il.getLines().add(line);
                    }
                } else
                    accountLineList.add(line);
            }
            JSONObject linesMap = new JSONObject();
            List<String> curr = new ArrayList<>();
            curr.add("VRP - Radar");
            curr.add("VBC - Radar");
            issuerLines.keySet().forEach(c -> curr.add(c));
            linesMap.put("currencies", new JSONArray(new Gson().toJson(curr)));
//            linesMap.put("sequence", json)
            return linesMap.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new APIException(APIException.ErrorCode.REMOTE_ERROR, "Can not retrive account info through method \"account_info\"");


        }
    }


    private static JSONObject formatNotFoundUser(String address) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 10);  //
        jsonObject.put("status", "success");

        JSONObject jsonObjectResult = new JSONObject();
        jsonObjectResult.put("ledger_current_index",0);
        jsonObjectResult.put("validated",false);

        JSONObject jsonObjectData = new JSONObject();
        jsonObjectData.put("Account",address);
        jsonObjectData.put("Balance","0");
        jsonObjectData.put("BalanceVBC", "0");
        jsonObjectData.put("inactive", true);

        jsonObjectResult.put("account_data", jsonObjectData);
        jsonObject.put("result", jsonObjectResult);
        return jsonObject;
    }




}
