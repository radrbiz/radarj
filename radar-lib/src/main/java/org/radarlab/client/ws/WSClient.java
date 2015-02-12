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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;


/**
 * Websocket client wrap
 */
public class WSClient {

    private Channel channel;
    private URI uri;
    private EventLoopGroup group = null;

    public WSClient(URI uri){
        this.uri = uri;
    }

    public Channel getChannel() {
        return channel;
    }
    public Channel connect(){
        if(group != null)
            group.shutdownGracefully();

        group = new NioEventLoopGroup();
        try {
            final WebSocketClientHandler handler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), Integer.MAX_VALUE));

            final boolean ssl = "wss".equalsIgnoreCase(uri.getScheme());
            final SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            } else {
                sslCtx = null;
            }

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(1000000),
                                    handler);
                        }
                    });
            ChannelFuture cf = b.connect(uri.getHost(), uri.getPort());
            channel = cf.sync().channel();

            handler.handshakeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }
        return channel;
    }
}
