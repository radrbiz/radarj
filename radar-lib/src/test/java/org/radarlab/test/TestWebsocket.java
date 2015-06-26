package org.radarlab.test;

import org.json.JSONObject;
import org.junit.Test;
import org.radarlab.api.AccountImpl;
import org.radarlab.api.TransactionImpl;
import org.radarlab.client.api.exception.APIException;
import org.radarlab.client.ws.RadarWebSocketClient;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * Radar network provides websocket interface to make tx. This is a test class for websocket API with Radar.
 * The config file is searched by order:
 *   1, {PROJECT_RESOURCES}/radar.properties
 *   2, {PROJECT_RESOURCES}/radar.properties
 *
 * The config key is "websocket.servers", value is ip list, split by ","
 */
public class TestWebsocket {
    String addrForListen = "r4DJTKVx4gsgMK3XW8yzZvk5EWFbhJ8bqL";   // Change me !!

    @Test
    public void testSubscribe() {
        //Step 1: send subscribe request to Radar, return immediately
        //multiple accounts subscription is allowed, merge accounts to an array, or send subscription requests several times respectively .
        try {
            String subscribe = RadarWebSocketClient.request("{\n" +
                    "  \"id\": 10,\n" +
                    "  \"command\": \"subscribe\",\n" +
                    "  \"accounts\": [\"" + addrForListen + "\"]\n" +
                    "}");
            System.out.println("subscribe account:" + addrForListen + ", result=" + subscribe);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Step 2: Get the subscribe data.  Multiple accounts info would be packaged in subscribeQueue .
        //Note: All subscriptions will return. Need to determine the format, type, relevant account .
        int count = 0;
        while (count < 3) {
            long start = System.currentTimeMillis();
            try {
                //load data from queue. 5 seconds of timeout for each loop, then next fetching loop.  poll() method is easy to debug.
                String data = RadarWebSocketClient.subscribeQueue.poll(5, TimeUnit.SECONDS);
                //If using take() method, there is not need of timeout. take() will block and wait until something is returned.
                //String data = RadarWebSocketClient.subscribeQueue.take();

                System.out.println("Get Subscribe message:" + data);
                if (System.currentTimeMillis() - start < 1000 && data == null) {
                    Thread.sleep(2000);
                }
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testAccountTx() throws URISyntaxException, InterruptedException {
        String addr = addrForListen; // Change me !!

        String data = "{\"id\":1,\"command\":\"account_tx\",\"account\":\"" + addr + "\"," +
                "\"tx_type\":\"Payment\",\"binary\":false,\"forward\":false,\"ledger_index_max\":-1,\"ledger_index_min\":-1,\"limit\":20, \"marker\":{\n" +
                "            \"ledger\": 1223, \n" +
                "            \"seq\": 0\n" +
                "        }}";
        int count = 0;
        while(count < 2) {
            try {
                String resp = RadarWebSocketClient.request(data);
                System.out.println(resp);
                count++;
                Thread.sleep(3000);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
    @Test
    public void testAccountDividend(){
        String data = "{\"id\":0, \"account\":\"" + addrForListen + "\", \"command\":\"account_dividend\"}";
        try {
            String resp = RadarWebSocketClient.request(data);
            System.out.println(resp);
            Thread.sleep(1000);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    ////// -----------------------------  Test Account section -----------------------------
    static AccountImpl userImpl = new AccountImpl();

    @Test
    public void testGetAcctInfo() throws APIException {
        String json = userImpl.getAccountInfo(addrForListen);
        System.out.println(json);
    }

    @Test
    public void testAccountCurrencys() throws APIException {
        String json = userImpl.accountCurrencies(addrForListen);
        System.out.println(json);
    }

    @Test
    public void testAccountLines() throws APIException {
        String address = "r4DJTKVx4gsgMK3XW8yzZvk5EWFbhJ8bqL"; // Change me !!
        String peer = "rLeMULVGcGBX7m3ZgSmZ4EXQRLjaLbD6TT";    // Change me !!
        String json = userImpl.getAccountLines(address, peer);
        System.out.println(json);
    }

    @Test
    public void testAccountLinesCurrency() throws APIException {
        String json = userImpl.getAccountLinesCurrency(addrForListen);
        System.out.println(json);
    }

    @Test
    public void testSequence() {
        int json = userImpl.getUserCurrentSequence(addrForListen);
        System.out.println("getUserCurrentSequence() : " + json);
    }



    ////// -----------------------------  Test Transaction section -----------------------------
    static TransactionImpl txImpl = new TransactionImpl();

    @Test
    public void testBookOffer() throws APIException {
        JSONObject jsonObject = txImpl.bookOffers("CNY", addrForListen, "VBC", null, null, 20);
        System.out.println(jsonObject.toString());
    }


    @Test
    public void testMakeOffer() throws APIException {
        String seed = "ssCH---CHANGE-PRIVATE-SEED---"; // Change me !!
        /*
        String address = addrForListen;
        int sequence = 3;
        Amount takerGets = new Amount(new BigDecimal(100));
        Amount takerPays = new Amount(new BigDecimal(15), Currency.fromString("CNY"), AccountID.fromAddress(addrForListen));
        String json = txImpl.makeOffer(seed, takerGets, takerPays, sequence);
        System.out.println(json);
        */
    }

    @Test
    public void testAccountOffer() throws APIException {
        String address = addrForListen;
        String json = txImpl.accountOffers(address, 12, "VRP", null, "RUB", addrForListen, null);
        System.out.println(json);
    }

    @Test
    public void testOfferCancel() throws APIException {
        String seed = "snoPBrXtMeMyMHUVTgbuqAfg1SUTb"; // Change me !!
        String json = txImpl.offerCancel(seed, 2, 5589);
        System.out.println(json);
    }



}
