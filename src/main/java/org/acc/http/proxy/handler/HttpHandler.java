package org.acc.http.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.certificate.CertificatePool;
import org.acc.http.proxy.pojo.CertificateInfo;
import org.acc.http.proxy.utils.MsgUtils;

import javax.net.ssl.SSLException;
import java.util.Objects;
import java.util.function.Consumer;

@Log4j2
public class HttpHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ChannelHandlerContext ctx;
    private final Bootstrap bootstrap = new Bootstrap();
    private final CertificatePool certificatePool;
    private final Consumer<FullHttpRequest> consumer;

    private SslContext clientSslContext;

    public HttpHandler(CertificatePool certificatePool, Consumer<FullHttpRequest> consumer) {
        this.certificatePool = certificatePool;
        this.consumer = consumer;

        if (Objects.nonNull(consumer)) {
            initClientSslContext();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            int port = 80;

            String[] hostSplit = fullHttpRequest.headers().get(HttpHeaderNames.HOST).split(":");
            String host = hostSplit[0];
            if (hostSplit.length > 1) {
                port = Integer.parseInt(hostSplit[1]);
            }

            // http连接
            if (!fullHttpRequest.method().equals(HttpMethod.CONNECT)) {
                if (Objects.nonNull(consumer)) {
                    // 如果是http请求，无需解密，直接获取
                    consumer.accept(fullHttpRequest);
                }

                httpHandle(fullHttpRequest, promise(host, port));
                return;
            }

            if (Objects.isNull(consumer)) {
                httpsHandle(promise(host, port));
                return;
            }

            SslContext sslContext = sslContext(host, port);
            if (Objects.isNull(sslContext)) {
                ctx.close();
                return;
            }

            httpsHandle(sslContext(host, port), promiseSsl(host, port));
        }
    }

    private void httpsHandle(Promise<Channel> promise) {
        promise.addListener((FutureListener<Channel>) future -> {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));

            ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                ChannelPipeline channelPipeline = ctx.pipeline();

                channelPipeline.remove(HandlerName.HTTP_HANDLER);
                channelPipeline.remove(HandlerName.HTTP_SERVER_CODEC);
                channelPipeline.remove(HandlerName.HTTP_OBJECT_AGGREGATOR);

                channelPipeline.addLast(new ExchangeHandler(future.getNow()));
            });
        });
    }

    private void httpsHandle(SslContext sslContext, Promise<Channel> promise) {
        promise.addListener((FutureListener<Channel>) future -> {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));

            ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                ChannelPipeline channelPipeline = ctx.pipeline();

                channelPipeline.remove(HandlerName.HTTP_HANDLER);

                channelPipeline.addFirst(sslContext.newHandler(ctx.alloc()));
                channelPipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                channelPipeline.addLast(new CaptureExchangeHandler(future.getNow(), consumer));
            });
        });
    }

    private SslContext sslContext(String host, int port) {
        try {
            CertificateInfo certificateInfo = certificatePool.getCertificateInfo(host, port);

            if (Objects.nonNull(certificateInfo)) {
                return SslContextBuilder.forServer(certificateInfo.getKeyPair().getPrivate(), certificateInfo.getX509Certificate()).build();
            }
        } catch (SSLException e) {
            log.error(e);
        }

        return null;
    }

    private void httpHandle(HttpRequest httpRequest, Promise<Channel> promise) {
        Object object = MsgUtils.fromHttpRequest(httpRequest);

        promise.addListener((FutureListener<Channel>) future -> {
            ChannelPipeline channelPipeline = ctx.pipeline();
            channelPipeline.remove(HandlerName.HTTP_SERVER_CODEC);
            channelPipeline.remove(HandlerName.HTTP_OBJECT_AGGREGATOR);
            channelPipeline.remove(HandlerName.HTTP_HANDLER);

            Channel channel = future.getNow();

            channelPipeline.addLast(new ExchangeHandler(channel));
            future.getNow().writeAndFlush(object);
        });
    }

    /**
     * 创建一个连接
     *
     * @param host
     * @param port
     * @return
     */
    private Promise<Channel> promise(String host, int port) {
        Promise<Channel> promise = ctx.executor().newPromise();

        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ExchangeHandler(ctx.channel()))
                .connect(host, port)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        promise.setSuccess(future.channel());
                    } else {
                        log.warn("{}:{} 代理请求失败", host, port);
                        ctx.close();
                        future.cancel(true);
                    }
                });

        return promise;
    }

    /**
     * 创建一个连接
     *
     * @param host
     * @param port
     * @return
     */
    private Promise<Channel> promiseSsl(String host, int port) {
        Promise<Channel> promise = ctx.executor().newPromise();

        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline channelPipeline = ch.pipeline();

                        channelPipeline.addLast(new SslHandler(clientSslContext.newEngine(ch.alloc())));
                        channelPipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                        channelPipeline.addLast(new CaptureExchangeHandler(ctx.channel(), consumer));
                    }
                })
                .connect(host, port)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        promise.setSuccess(future.channel());
                    } else {
                        log.warn("{}:{} 代理请求失败", host, port);
                        ctx.close();
                        future.cancel(true);
                    }
                });

        return promise;
    }

    private void initClientSslContext() {
        try {
            clientSslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (SSLException e) {
            log.error(e);
            System.exit(-1);
        }
    }
}
