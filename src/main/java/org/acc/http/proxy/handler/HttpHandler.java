package org.acc.http.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.certificate.CertificatePool;
import org.acc.http.proxy.pojo.CertificateInfo;
import org.acc.http.proxy.utils.MsgUtils;

import javax.net.ssl.SSLException;
import java.util.Objects;
import java.util.function.Consumer;

@Log4j2
public class HttpHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ChannelHandlerContext clientContext;
    private final CertificatePool certificatePool;
    private final Consumer<FullHttpRequest> consumer;
    private final SslContext clientSslContext;
    private Bootstrap bootstrap = new Bootstrap();

    public HttpHandler(CertificatePool certificatePool, Consumer<FullHttpRequest> consumer, SslContext clientSslContext) {
        this.certificatePool = certificatePool;
        this.consumer = consumer;
        this.clientSslContext = clientSslContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.clientContext = ctx;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
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

                httpHandle(fullHttpRequest, host, port);
                return;
            }

            if (Objects.nonNull(consumer)) {
                httpsHandleCapture(sslContext(host, port), host, port);
            } else {
                httpsHandle(host, port);
            }
        }
    }

    /**
     * 直接转发http数据
     *
     * @param fullHttpRequest
     * @param host
     * @param port
     */
    private void httpHandle(FullHttpRequest fullHttpRequest, String host, int port) {
        fullHttpRequest.retain();
        ByteBuf byteBuf = ((ByteBuf) MsgUtils.fromHttpRequest(fullHttpRequest));

        bootstrap.group(clientContext.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ExchangeHandler(clientContext.channel()))
                .connect(host, port)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ChannelPipeline channelPipeline = clientContext.pipeline();

                        channelPipeline.remove(HandlerName.HTTP_HANDLER);
                        channelPipeline.remove(HandlerName.HTTP_SERVER_CODEC);
                        channelPipeline.remove(HandlerName.HTTP_OBJECT_AGGREGATOR);

                        Channel channel = future.channel();

                        channelPipeline.addLast(new ExchangeHandler(channel));
                        channel.writeAndFlush(byteBuf);
                    } else {
                        clientContext.close();
                    }
                });
    }

    /**
     * 直接转发https数据
     *
     * @param host
     * @param port
     */
    private void httpsHandle(String host, int port) {
        bootstrap.group(clientContext.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ExchangeHandler(clientContext.channel()))
                .connect(host, port)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));

                        clientContext.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                            ChannelPipeline channelPipeline = clientContext.pipeline();

                            channelPipeline.remove(HandlerName.HTTP_HANDLER);
                            channelPipeline.remove(HandlerName.HTTP_SERVER_CODEC);
                            channelPipeline.remove(HandlerName.HTTP_OBJECT_AGGREGATOR);

                            channelPipeline.addLast(new ExchangeHandler(future.channel()));
                        });
                    } else {
                        clientContext.close();
                    }
                });
    }

    /**
     * 捕获https请求内容
     *
     * @param sslContext
     * @param host
     * @param port
     */
    private void httpsHandleCapture(SslContext sslContext, String host, int port) {
        bootstrap.group(clientContext.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline channelPipeline = ch.pipeline();

                        channelPipeline.addFirst(clientSslContext.newHandler(ch.alloc()));

                        channelPipeline.addLast(new HttpClientCodec());
                        channelPipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 512));
                        channelPipeline.addLast(new ExchangeHandler(clientContext.channel()));
                    }
                })
                .connect(host, port)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));

                        clientContext.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                            ChannelPipeline channelPipeline = clientContext.pipeline();

                            channelPipeline.remove(HandlerName.HTTP_HANDLER);

                            channelPipeline.addFirst(sslContext.newHandler(clientContext.alloc()));
                            //前面还有 new HttpServerCodec()、new HttpObjectAggregator(1024 * 1024 * 512)
                            channelPipeline.addLast(new CaptureExchangeHandler(consumer, future.channel()));
                        });
                    } else {
                        clientContext.close();
                    }
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
}
