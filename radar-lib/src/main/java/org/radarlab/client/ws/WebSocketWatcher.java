package org.radarlab.client.ws;

import com.google.gson.Gson;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.apache.commons.lang3.RandomUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ac
 * since 15/5/26.
 */
public class WebSocketWatcher implements Runnable {
    private static final Logger logger = Logger.getLogger(WebSocketWatcher.class);

    @Override
    public void run() {
        logger.info("Start to watch ws servers.");
        Collection<WSClient> clients = RadarWebSocketClient.clients();
        for (WSClient client : clients) {
            if (client.getStatus() == 0) {
                long requestId = RandomUtils.nextLong(0, 100L);
                RadarWebSocketClient.queues.put(requestId, new LinkedBlockingQueue<>(1));
                try {
                    Channel channel = client.getChannel();
                    if (channel != null && !(channel.isActive() || channel.isOpen())) {
                        logger.info("------Reconnecting websock:" + client.getType() + ", address:" + client.getUri().toString());
//                        channel.close();
                        channel = client.connect();
                    }
                    Map<String, Object> para = new HashMap<>();
                    para.put("id", requestId);
                    para.put("command", "ledger");
                    para.put("ledger_index", "validated");
                    WebSocketFrame frame = new TextWebSocketFrame(new Gson().toJson(para));
                    channel.writeAndFlush(frame);
                    String result = WebSocketClientHandler.getMessage(requestId);
                    logger.info("Result from ws:" + result);
                    if(result != null){
                        JSONObject json = new JSONObject(result);
                        if(json.has("status") && json.getString("status").equals("success")){
                            client.setStatus(1);
                            RadarWebSocketClient.updateClient(client);
                            logger.info("Client:" + client.getType() + " is OK now.");
                        }else{
                            logger.info("Client:" + client.getType() + " is not back to normal yet," + result);
                        }
                    }else{
                        logger.info("Client:" + client.getType() + " is not back to normal yet, status is timeout.");
                    }
                } catch (Exception e) {
                    logger.error("WebSocketWatcher throws exception:", e);
                    logger.info("Client:" + client.getType() + " is not back to normal yet.");
                }
            }else{
                logger.info("Client:" + client.getType() + " is running ok.");
            }
        }
    }
}
