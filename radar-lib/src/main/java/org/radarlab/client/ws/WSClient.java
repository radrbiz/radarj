package org.radarlab.client.ws;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.apache.log4j.Logger;
import org.radarlab.client.api.exception.APIException;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongUnaryOperator;


/**
 * Created by Andy
 * since 14/12/10.
 */
public class WSClient extends ReentrantLock {
    private static final Logger logger = Logger.getLogger(WSClient.class);

    private Channel channel;
    private URI uri;
    private EventLoopGroup group = null;

    private int status = 1;//1 normal, 0 offline
    private static final int MAX_RETRY_TIMES = 100;
    private long timestamp;
    private String type;
    public static final AtomicLong timeout = new AtomicLong(0L);

    private final Marker marker = new Marker();

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        if(status == 1)
            timeout.set(0);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public WSClient(URI uri){
        this.uri = uri;
    }

    public WSClient(URI uri, String type) {
        this.uri = uri;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long updateStatus(){
        return timeout.getAndUpdate(marker);
    }

    public void setTimes(Long time){
        timeout.set(time);
    }

    public Channel getChannel() {
        return channel;
    }

    public URI getUri() {
        return uri;
    }

    public Channel connect() throws APIException {
        boolean lock = false;
        try {
            if (lock = this.tryLock(10, TimeUnit.MILLISECONDS)) {
                if (channel != null && channel.isActive() && channel.isOpen()) {
                    return channel;
                }
                try {
                    if (group != null) {
                        group.shutdownGracefully();
                    }
                    group = new NioEventLoopGroup();
                    final WebSocketClientHandler handler =
                            new WebSocketClientHandler(
                                    WebSocketClientHandshakerFactory.newHandshaker(
                                            uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), Integer.MAX_VALUE));
                    Bootstrap b = new Bootstrap();
                    b.group(group)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) {
                                    ChannelPipeline p = ch.pipeline();
                                    p.addLast(
                                            new HttpClientCodec(),
                                            new HttpObjectAggregator(1000000),
                                            handler);
                                }
                            });
                    final CountDownLatch channelLatch = new CountDownLatch(1);
                    ChannelFuture f = b.connect(uri.getHost(), uri.getPort());
                    f.addListener(cf -> {
                        if(cf.isSuccess()) {
                            channel = ((ChannelFuture)cf).channel();
                            channelLatch.countDown();
                        } else {
                            throw new APIException(APIException.ErrorCode.REMOTE_ERROR, "connection established failed.");
                        }
                    });
                    try {
                        channelLatch.await(10, TimeUnit.SECONDS);
                    } catch(InterruptedException ex) {
                        throw new APIException(APIException.ErrorCode.REMOTE_ERROR,
                                "Interrupted while waiting for connection to arrive.");
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                return channel;
            }else{
                throw new APIException(APIException.ErrorCode.REMOTE_ERROR, "Websocket is still connecting...");
            }
        } catch (InterruptedException e) {
            throw new APIException(APIException.ErrorCode.REMOTE_ERROR, e.getMessage());
        }finally {
            if(lock){
                unlock();
            }
        }
    }
    private class Marker implements LongUnaryOperator {
        @Override
        public long applyAsLong(long operand) {
            if(operand > MAX_RETRY_TIMES){
                status = 0;
            }
            return ++operand;
        }
    }
}
