package org.radarlab.client.ws;

import org.radarlab.api.APIException;
import org.radarlab.client.Config;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * interact with radar servers
 */
public class RadarWebSocketClient {
    private static final String[] servers;

    private static final ConcurrentMap<String, WSClient> clientMap = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, BlockingQueue<String>> queues = new ConcurrentHashMap<>();
    public static final BlockingQueue<String> subscribeQueue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
    private static final AtomicLong requestID = new AtomicLong(0L);
    private static final Logger logger = Logger.getLogger(RadarWebSocketClient.class);

    static {
        String serverStr = Config.getInstance().getProperty("websocket.servers");
        logger.info("Load websocket.servers from config, serverStr=" + serverStr);
        if(StringUtils.isBlank(serverStr)){
            logger.error("Property \"websocket.servers\" not found.");
            throw new RuntimeException("Property \"websocket.servers\" not found.");
        }
        servers = serverStr.split(",");
        for (String server : servers) {
            try {
                URI uri = new URI(server);
                WSClient wsClient = new WSClient(uri);
                wsClient.connect();
                clientMap.put(server, wsClient);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Send json data to server
     */
    public static String request(String data) throws InterruptedException {
        Long requestId = requestID.getAndIncrement();
        Long currentDataRequestId = 0L;
        try {
            JSONObject json = new JSONObject(data);
            currentDataRequestId = json.getLong("id");
            json.put("id", requestId);
            data = json.toString();
        } catch (Exception ex) {

        }
        String server = servers[RandomUtils.nextInt(0, servers.length)];
        queues.put(requestId, new LinkedBlockingQueue<>(1));
        Channel channel = clientMap.get(server).getChannel();
        if(!channel.isActive() || !channel.isOpen()){
            channel = clientMap.get(server).connect();
        }
        //logger.info("****Active:"+channel.isActive() + "----Open:"+channel.isOpen());
        WebSocketFrame frame = new TextWebSocketFrame(data);
        channel.writeAndFlush(frame);
        String result = WebSocketClientHandler.getMessage(requestId);
        if(result == null){
            return null;
        }
        JSONObject json = new JSONObject(result);
        json.put("id", currentDataRequestId);
        return json.toString();
    }

    /**
     * Send json data to server, if failed, throws the APIException for details.
     */
    public static String req(String data) throws APIException {
        Long requestId = requestID.getAndIncrement();
        Long currentDataRequestId;
        try {
            JSONObject json = new JSONObject(data);
            currentDataRequestId = json.getLong("id");
            json.put("id", requestId);
            data = json.toString();
        } catch (Exception ex) {
            throw new APIException(APIException.ErrorCode.MALFORMED_REQUEST_DATA, "Invalid \"id\" property, must be a number");
        }
        String server = servers[RandomUtils.nextInt(0, servers.length)];
        queues.put(requestId, new LinkedBlockingQueue<>(1));
        Channel channel = clientMap.get(server).getChannel();
        if(channel==null || !channel.isActive() || !channel.isOpen()){
            channel = clientMap.get(server).connect();
        }
        //logger.debug("****˜Active:" + channel.isActive() + "----Open:" + channel.isOpen());
        logger.debug("***request to ws:" + data);
        WebSocketFrame frame = new TextWebSocketFrame(data);
        channel.writeAndFlush(frame);
        String result = WebSocketClientHandler.getMessage(requestId);
        //logger.debug("request from remote:" + result);
        if(result == null){
            throw new APIException(APIException.ErrorCode.REMOTE_ERROR, "websocket error on reponse.");
        }
        JSONObject json = new JSONObject(result);
        json.put("id", currentDataRequestId);
        if(json.has("status") && !json.getString("status").equals("error"))
            return json.toString();
        else{
            String error = json.getString("error");
            if("actNotFound".equals(error))
                throw new APIException(APIException.ErrorCode.ADDRESS_NOT_FOUND, json.getString("error_message"));
            else if("noCurrent".equals(error) || "noNetwork".equals(error)){
                throw new APIException(APIException.ErrorCode.REMOTE_ERROR, json.getString("error_message"));
            }else{
                throw new APIException(APIException.ErrorCode.UNKNOWN_ERROR, json.getString("error_message"));
            }
        }
    }
}
