package org.acc.http.proxy.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.certificate.CertificatePool;
import org.acc.http.proxy.pojo.CertificateInfo;
import org.acc.http.proxy.utils.MsgUtils;
import org.acc.http.proxy.utils.PromiseUtils;

import javax.net.ssl.SSLException;
import java.util.Objects;
import java.util.function.Consumer;

@Log4j2
public class HttpHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ChannelHandlerContext clientChannelHandlerContext;
    private final CertificatePool certificatePool;
    private final Consumer<FullHttpRequest> consumer;
    private final SslContext clientSslContext;

    public HttpHandler(CertificatePool certificatePool, Consumer<FullHttpRequest> consumer, SslContext clientSslContext) {
        this.certificatePool = certificatePool;
        this.consumer = consumer;
        this.clientSslContext = clientSslContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.clientChannelHandlerContext = ctx;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        throw new HandlerException(cause);
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

                httpHandle(fullHttpRequest, PromiseUtils.promise(host, port, clientChannelHandlerContext, new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline channelPipeline = ch.pipeline();

                        channelPipeline.addLast(new ExchangeHandler(clientChannelHandlerContext.channel()));
                        // 编码 FullHttpRequest对象
//                        channelPipeline.addLast(new HttpRequestEncoder());
                    }
                }));
                return;
            }

            if (Objects.nonNull(consumer)) {
                SslContext sslContext = sslContext(host, port);

                httpsHandleCapture(sslContext, PromiseUtils.promise(host, port, clientChannelHandlerContext, new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline channelPipeline = ch.pipeline();

                        channelPipeline.addFirst(clientSslContext.newHandler(ch.alloc()));

                        channelPipeline.addLast(new HttpClientCodec());
                        channelPipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 1024));
                        channelPipeline.addLast(new ExchangeHandler(clientChannelHandlerContext.channel()));
                    }
                }));
            } else {
                httpsHandle(PromiseUtils.promise(host, port, clientChannelHandlerContext, new ExchangeHandler(clientChannelHandlerContext.channel())));
            }
        }
    }

    /**
     * 直接转发http数据
     *
     * @param fullHttpRequest
     * @param promise
     */
    private void httpHandle(FullHttpRequest fullHttpRequest, Promise<Channel> promise) {
        // MsgUtils.fromHttpRequest(fullHttpRequest) 会将 ReferenceCounted 减1，所以这里先加1
        fullHttpRequest.retain();
        // copy一份ByteBuf, 防止 IllegalReferenceCountException
        ByteBuf byteBuf = ((ByteBuf) MsgUtils.fromHttpRequest(fullHttpRequest)).copy();
        ByteBuf byteBufRetain = byteBuf.retain();

        promise.addListener((FutureListener<Channel>) future -> {
            ChannelPipeline channelPipeline = clientChannelHandlerContext.pipeline();

            channelPipeline.remove(HandlerName.HTTP_HANDLER);
            channelPipeline.remove(HandlerName.HTTP_SERVER_CODEC);
            channelPipeline.remove(HandlerName.HTTP_OBJECT_AGGREGATOR);

            Channel channel = future.getNow();

            channelPipeline.addLast(new ExchangeHandler(channel));
            channel.writeAndFlush(byteBufRetain);
        });
    }

    /**
     * 直接转发https数据
     *
     * @param promise
     */
    private void httpsHandle(Promise<Channel> promise) {
        promise.addListener((FutureListener<Channel>) future -> {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));

            clientChannelHandlerContext.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                ChannelPipeline channelPipeline = clientChannelHandlerContext.pipeline();

                channelPipeline.remove(HandlerName.HTTP_HANDLER);
                channelPipeline.remove(HandlerName.HTTP_SERVER_CODEC);
                channelPipeline.remove(HandlerName.HTTP_OBJECT_AGGREGATOR);

                channelPipeline.addLast(new ExchangeHandler(future.getNow()));
            });
        });
    }

    /**
     * 捕获https请求内容
     *
     * @param promise
     */
    private void httpsHandleCapture(SslContext sslContext, Promise<Channel> promise) {
        promise.addListener((FutureListener<Channel>) future -> {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));

            clientChannelHandlerContext.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                ChannelPipeline channelPipeline = clientChannelHandlerContext.pipeline();

                channelPipeline.remove(HandlerName.HTTP_HANDLER);

                channelPipeline.addFirst(sslContext.newHandler(clientChannelHandlerContext.alloc()));
                //前面还有 new HttpServerCodec()、new HttpObjectAggregator(1024 * 1024)
                channelPipeline.addLast(new CaptureExchangeHandler(consumer, future));
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
}
