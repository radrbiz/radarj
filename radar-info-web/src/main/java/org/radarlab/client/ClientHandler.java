package org.radarlab.client;

import org.radarlab.client.api.render.ErrorRender;
import org.radarlab.client.handler.*;
import org.radarlab.core.exception.RadarException;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private static final HashMap<String, ClientProcessor> processors = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ClientHandler.class);
    private static final ConcurrentMap<String, RateLimitStatus> limitMap = new ConcurrentHashMap<>();

    static {
        processors.put("ledger", LedgerList.instance);
        processors.put("ledgerData", LedgerData.instance);
        processors.put("serverState", ServerState.instance);
        processors.put("accountInfo", CommonHandler.instance);
        processors.put("ledgerInfo", CommonHandler.instance);
        processors.put("accountTxs", CommonHandler.instance);
        processors.put("accountLines", CommonHandler.instance);
        processors.put("accountOffers", CommonHandler.instance);
        processors.put("overview", Overview.instance);
        processors.put("tx", CommonHandler.instance);
    }

    public static String getClientIP(ChannelHandlerContext ctx, HttpRequest request) {
        if (request == null)
            return null;
        String s = request.headers().get("X-Forwarded-For");
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s))
            s = request.headers().get("Proxy-Client-IP");
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s))
            s = request.headers().get("WL-Proxy-Client-IP");
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s))
            s = request.headers().get("HTTP_CLIENT_IP");
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s))
            s = request.headers().get("HTTP_X_FORWARDED_FOR");
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s)) {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            InetAddress inetaddress = socketAddress.getAddress();
            s = inetaddress.getHostAddress();
        }
        if ("127.0.0.1".equals(s) || "0:0:0:0:0:0:0:1".equals(s))
            try {
                s = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException unknownhostexception) {
            }
        if (s != null && !"".equals(s.trim()) && s.split(",").length > 0) {
            s = s.split(",")[0];
        }
        return s;
    }

    private String processResponse(String requestType, Map<String, String> params) throws RadarException {
        if (processors.get(requestType) == null) {
            throw new RadarException("No processor found.");
        }
        return processors.get(requestType).processResponse(params);
    }

    public RateLimitStatus updateRegisterRate(String key) {
        try {
            RateLimitStatus rate = limitMap.get(key);
            if (limitMap.containsKey(key)) {
                if (rate.getRemaining_hits().getAndDecrement() > 0) {
                    int timer = (int) (rate.getReset_time_in_seconds() - (System.currentTimeMillis() / 1000));
                    if (timer <= 0) {
                        rate = new RateLimitStatus();
                        rate.setHourly_limit(5000);
                        rate.setRemaining_hits(new AtomicInteger(5000));
                        rate.setReset_time_in_seconds((int) ((System.currentTimeMillis() + 60 * 60 * 1000) / 1000));
                        rate.setReset_time(String.valueOf(System.currentTimeMillis()));
                        limitMap.put(key, rate);
                    } else
                        limitMap.put(key, rate);//set remain time.
                    return rate;
                }
            } else {
                synchronized (ClientHandler.class) {
                    rate = limitMap.get(key);
                    if(rate == null) {
                        rate = new RateLimitStatus();
                        rate.setHourly_limit(5000);
                        rate.setRemaining_hits(new AtomicInteger(5000));
                        rate.setReset_time_in_seconds((int) ((System.currentTimeMillis() + 60 * 60 * 1000) / 1000));
                        rate.setReset_time(String.valueOf(System.currentTimeMillis()));
                        limitMap.put(key, rate);
                        return rate;
                    }else{
                        return updateRegisterRate(key);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Update ratelimit error-->" + e, e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws RadarException {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            logger.info("request from:" + req.getUri());

            boolean keepAlive = HttpHeaders.isKeepAlive(req);
            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            Map<String, String> params = new HashMap<>();
            HttpMethod method = req.getMethod();
            if (method.name().equals(HttpMethod.POST.name())) {
                String ip = getClientIP(ctx, req);
                if (ip != null) {
                    RateLimitStatus rate = updateRegisterRate(ip);
                    if (rate == null) {
                        throw new RadarException("Rate status error");
                    }
                    if (rate != null) {
                        logger.info(req.getUri() + ": key-->" + ip + ", remains access time:" + rate.getRemaining_hits());
                    }
                    if (rate != null && rate.getRemaining_hits().get() <= 0) {
                        throw new RadarException("Too many requests.");
                    }
                }
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), req, CharsetUtil.UTF_8);
                List<InterfaceHttpData> dataList = postDecoder.getBodyHttpDatas();
                dataList.forEach(data -> {
                    if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        Attribute attribute = (Attribute) data;
                        try {
                            params.put(data.getName() == null ? "json" : data.getName(), attribute.getValue());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }else if(req.getUri().endsWith("/")){
                sendRedirect(ctx, req.getUri() + "index.html");
                return;
            }else if (req.getUri().contains("/ledger/")) {

                long start = System.currentTimeMillis();
                String uri = req.getUri();
                String ledgerIndex = uri.substring(uri.indexOf("/ledger/") + 8, uri.length());
                RandomAccessFile raf = null;
                try {
                    String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

                    String filePath = classPath.substring(0, (classPath.contains("lib") ? classPath.lastIndexOf("lib") : classPath.lastIndexOf("target"))) + "src/webapp/ledger.html";
                    raf = new RandomAccessFile(filePath, "r");
                    long fileLength = raf.length();
                    byte[] bytes = new byte[(int) fileLength];
                    raf.read(bytes);
                    String html = new String(bytes, "utf-8");
                    html = html.replaceAll("\\$index", ledgerIndex);
                    html = html.replaceAll("\\$platform", org.radarlab.client.util.Config.getInstance().getProperty("client.name"));
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(html.getBytes("utf-8")));
                    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                            String.valueOf(fileLength));
                    Channel ch = ctx.channel();
                    if (!req.getMethod().equals(HttpMethod.HEAD)) {
                        ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
                    }

                    if (!keepAlive) {
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.write(response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    ctx.flush();
//                    ctx.close();
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                logger.info("cost:" + (System.currentTimeMillis() - start));
                return;
            }else if(req.getUri().contains("/search/")){
                String data = req.getUri().substring(req.getUri().indexOf("/search/") + 8, req.getUri().length());
                RandomAccessFile raf = null;
                try {
                    String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

                    String filePath = classPath.substring(0, (classPath.contains("lib") ? classPath.lastIndexOf("lib") : classPath.lastIndexOf("target"))) + "src/webapp/index.html";
                    raf = new RandomAccessFile(filePath, "r");
                    long fileLength = raf.length();
                    byte[] bytes = new byte[(int) fileLength];
                    raf.read(bytes);
                    String html = new String(bytes, "utf-8");
                    if(NumberUtils.isNumber(data))
                        html = html.replaceAll("\\$sendReq", "sendReq('ledgerInfo', "+data+", 'ledger', 'ledger_detail', 'Ledger Info');");
                    else if(data.length()>36){
                        html = html.replaceAll("\\$sendReq", "sendReq('tx', '"+data+"', 'tx', 'txInfo', 'Transaction Info');");
                    }else{
                        String replace = "sendReq('accountInfo', '"+data+"', 'account', 'account_info', \"Account Info\");\n" +
                                "                sendReq('accountTxs', '"+data+"', 'account', 'account_txs', \"Account Transactions\");\n" +
                                "                sendReq('accountLines', '"+data+"', 'account', 'account_lines', 'Account Lines');\n" +
                                "                sendReq('accountOffers', '"+data+"', 'account', 'account_offers', 'Account Offers');";
                        html = html.replaceAll("\\$sendReq",replace);
                    }
                    html = html.replaceAll("\\$platform", org.radarlab.client.util.Config.getInstance().getProperty("client.name"));
                    html = html.replaceAll("\\./", "../");
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(html.getBytes("utf-8")));
                    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                            String.valueOf(fileLength));
                    Channel ch = ctx.channel();
                    if (!req.getMethod().equals(HttpMethod.HEAD)) {
                        ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
                    }
                    if (!keepAlive) {
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.write(response);
                    }
                    return;
                } catch (Exception e2) {
                    e2.printStackTrace();
                } finally {
                    ctx.flush();
                    ctx.close();
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            else if (req.getUri().contains(".html")) {
                String name = req.getUri().substring(req.getUri().lastIndexOf("/") + 1, req.getUri().lastIndexOf(".html"));
                //index page
                RandomAccessFile raf = null;
                try {
                    String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

                    String filePath = classPath.substring(0, (classPath.contains("lib") ? classPath.lastIndexOf("lib") : classPath.lastIndexOf("target"))) + "src/webapp/" + name + ".html";
                    raf = new RandomAccessFile(filePath, "r");
                    long fileLength = raf.length();
                    byte[] bytes = new byte[(int) fileLength];
                    raf.read(bytes);
                    String html = new String(bytes, "utf-8");
                    html = html.replaceAll("\\$platform", org.radarlab.client.util.Config.getInstance().getProperty("client.name"));
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(html.getBytes("utf-8")));
                    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                            String.valueOf(fileLength));
                    Channel ch = ctx.channel();
                    if (!req.getMethod().equals(HttpMethod.HEAD)) {
                        ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
                    }
                    if (!keepAlive) {
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.write(response);
                    }
                    return;
                } catch (Exception e2) {
                    e2.printStackTrace();
                } finally {
                    ctx.flush();
                    ctx.close();
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (req.getUri().contains("static")) {
                String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                String filePath = classPath.substring(0, (classPath.contains("lib") ? classPath.lastIndexOf("lib") : classPath.lastIndexOf("target")))
                        + "src/webapp/" + req.getUri().substring(req.getUri().lastIndexOf("static"), req.getUri().length());
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(filePath, "r");
                    long fileLength = raf.length();
                    byte[] bytes = new byte[(int) fileLength];
                    raf.read(bytes);
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(bytes));
                    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                            String.valueOf(fileLength));
                    Channel ch = ctx.channel();
                    if (!req.getMethod().equals(HttpMethod.HEAD)) {
                        ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
                    }
                    if (!keepAlive) {
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.write(response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    ctx.flush();
                    ctx.close();
                    if (raf != null)
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
                return;
            } else {
                throw new RadarException(405, "Method not Allowed.");
            }
            String type = params.get("type");
            if (StringUtils.isBlank(type)) {
                throw new RadarException(400, "Can not find request type.");
            }
            byte[] bytes = new byte[0];
            try {
                bytes = this.processResponse(type, params).getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(bytes));
            response.headers().set(CONTENT_TYPE, "application/json");
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());


            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                ctx.write(response);
            }
            ctx.flush();
        }
    }

    private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(LOCATION, newUri);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String errorResponse;
        if (cause instanceof RadarException) {
            RadarException exception = (RadarException) cause;
            errorResponse = ErrorRender.render(exception.getCode(), exception.getMessage());
        } else if (cause instanceof ReadTimeoutException || cause instanceof WriteTimeoutException) {
            ctx.flush();
            ctx.close();
            return;
        } else {
            errorResponse = ErrorRender.render(500, "Internal Server Error.");
        }
        byte[] bytes = new byte[0];
        try {
            bytes = errorResponse.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(bytes));
        response.headers().set(CONTENT_TYPE, "application/json");
        ctx.write(response);
        logger.error("catch exception-->" + cause.getMessage());
        cause.printStackTrace();
        ctx.flush();
        ctx.close();
    }

    public static void main(String[] args) {
        String uri = "http://localhost/ledger/1232323";
        System.out.println(uri.substring(uri.indexOf("/ledger/") + 8, uri.length()));
        ;
    }
}
