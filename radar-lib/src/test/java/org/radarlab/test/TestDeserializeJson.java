package org.radarlab.test;

import org.radarlab.core.AccountID;
import org.radarlab.core.AccountLine;
import org.radarlab.core.Currency;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestDeserializeJson {

    @Test
    public void testAccountLineParser(){
        String jsonStr = "{\n" +
                "    \"result\": {\n" +
                "        \"account\": \"rEVEyZNAVpE28TUxzRtVWmVRLnivKCbZnD\", \n" +
                "        \"lines\": [\n" +
                "            {\n" +
                "                \"account\": \"rBVskmUjtTFbHVtQmous9ckV9MQKA6Zu3W\", \n" +
                "                \"balance\": \"-102\", \n" +
                "                \"currency\": \"DNC\", \n" +
                "                \"limit\": \"0\", \n" +
                "                \"limit_peer\": \"1000000000\", \n" +
                "                \"no_ripple_peer\": true, \n" +
                "                \"quality_in\": 0, \n" +
                "                \"quality_out\": 0\n" +
                "            }, \n" +
                "            {\n" +
                "                \"account\": \"rGfddMog4QB3MkmzjeoHrS2oxwJ2JoYZKM\", \n" +
                "                \"balance\": \"-1766.3\", \n" +
                "                \"currency\": \"DNC\", \n" +
                "                \"limit\": \"0\", \n" +
                "                \"limit_peer\": \"1000000000\", \n" +
                "                \"no_ripple_peer\": true, \n" +
                "                \"quality_in\": 0, \n" +
                "                \"quality_out\": 0\n" +
                "            }, \n" +
                "            {\n" +
                "                \"account\": \"rpw536W3t5dZTPVe4VpGSDZLhBqc3fX9T8\", \n" +
                "                \"balance\": \"-7932.7\", \n" +
                "                \"currency\": \"DNC\", \n" +
                "                \"limit\": \"0\", \n" +
                "                \"limit_peer\": \"1000000000\", \n" +
                "                \"quality_in\": 0, \n" +
                "                \"quality_out\": 0\n" +
                "            }\n" +
                "        ], \n" +
                "        \"status\": \"success\"\n" +
                "    }\n" +
                "}";
        JSONObject json = new JSONObject(jsonStr);
        String address = json.getJSONObject("result").getString("account");
        JSONArray lines = json.getJSONObject("result").getJSONArray("lines");
        List<AccountLine> accountLineList = new ArrayList<>();
        for(int i=0;i<lines.length();i++){
            AccountLine accountLine = AccountLine.fromJSON(AccountID.fromAddress(address), lines.getJSONObject(i));
            System.out.println("Account*********"+accountLine.balance.issuer().address);
            System.out.println(accountLine.balance);
            System.out.println(accountLine.currency);
            System.out.println(accountLine.quality_in);
            System.out.println(accountLine.quality_out);
            System.out.println(accountLine.limit_peer);
            accountLineList.add(accountLine);
        }
        System.out.println(accountLineList);
    }

    @Test
    public void testCurrency(){
        Currency currency = Currency.VBC;
        System.out.println("currency:"+currency.toString());
    }
}
