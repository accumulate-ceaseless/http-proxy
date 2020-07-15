package org.acc.http.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.certificate.Certificate;
import org.acc.http.proxy.certificate.CertificateName;
import org.acc.http.proxy.certificate.GenerateCertificateException;
import org.acc.http.proxy.utils.CertificateUtils;

import javax.net.ssl.SSLException;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Objects;

@Log4j2
public class HttpHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ChannelHandlerContext ctx;
    private final Bootstrap bootstrap = new Bootstrap();
    private Certificate certificate;

    public HttpHandler(Certificate certificate) {
        this.certificate = certificate;
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
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;

            int port = 80;

            String[] hostSplit = httpRequest.headers().get(HttpHeaderNames.HOST).split(":");
            String host = hostSplit[0];
            if (hostSplit.length > 1) {
                port = Integer.parseInt(hostSplit[1]);
            }

            Promise<Channel> promise = promise(host, port);

            // https连接
            if (httpRequest.method().equals(HttpMethod.CONNECT)) {
                promise.addListener((FutureListener<Channel>) future -> {
                    FullHttpResponse response =
                            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));

                    ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                        ChannelPipeline channelPipeline = ctx.pipeline();

                        SslContext sslContext = sslContext(host);
                        if (Objects.nonNull(sslContext)) {
                            channelPipeline.addFirst(sslContext.newHandler(ctx.alloc()));
                            channelPipeline.remove(HandlerName.HTTP_HANDLER);
                            channelPipeline.addLast(new ExchangeHandler(future.getNow()));
                        }
                    });
                });
            } else {
                Object object = fromHttpRequest(httpRequest);

                promise.addListener((FutureListener<Channel>) future -> {
                    ChannelPipeline channelPipeline = ctx.pipeline();

                    channelPipeline.remove(HandlerName.HTTP_CODEC);
                    channelPipeline.remove(HandlerName.HTTP_HANDLER);

                    channelPipeline.addLast(new ExchangeHandler(future.getNow()));
                    future.get().writeAndFlush(object);
                });
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private SslContext sslContext(String host) {
        try {
            KeyPair keyPair = certificate.generateKeyPair();

            CertificateUtils.generateDefaultRoot(certificate);

            X509Certificate rootCertificate = CertificateUtils.readRootCertificate(Paths.get(CertificateName.RootCertificateName));
            PrivateKey rootPrivateKey = CertificateUtils.readPrivateKey(Paths.get(CertificateName.RootCertificatePrivateKeyName));


            return SslContextBuilder.forServer(keyPair.getPrivate(), certificate.generate(CertificateName.Issuer,
                    rootPrivateKey,
                    rootCertificate.getNotBefore(),
                    rootCertificate.getNotAfter(),
                    keyPair.getPublic(),
                    Collections.singletonList(host))).build();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | GenerateCertificateException | SSLException e) {
            log.error(e);
        }

        return null;
    }

    /**
     * 将请求转化为原始数据
     *
     * @param httpRequest
     * @return
     */
    private Object fromHttpRequest(HttpRequest httpRequest) {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestEncoder());
        embeddedChannel.writeOutbound(httpRequest);
        Object object = embeddedChannel.readOutbound();
        embeddedChannel.close();

        return object;
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
                .remoteAddress(host, port)
                .handler(new ExchangeHandler(ctx.channel()))
                .connect()
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        promise.setSuccess(future.channel());
                    } else {
                        ctx.close();
                        future.cancel(true);
                    }
                });

        return promise;
    }
}
