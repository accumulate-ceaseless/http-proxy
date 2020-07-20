package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.ssl.SslContext;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private final Consumer<FullHttpRequest> consumer;
    private final ChannelHandlerContext channelHandlerContext;
    private final SslContext clientSslContext;

    private AtomicInteger atomicInteger = new AtomicInteger();

    private List<FullHttpRequest> fullHttpRequests = new ArrayList<>();

    private Future<Channel> future;


    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer,
                                  ChannelHandlerContext channelHandlerContext,
                                  SslContext clientSslContext,
                                  Future<Channel> future) {
        this.consumer = consumer;
        this.channelHandlerContext = channelHandlerContext;
        this.clientSslContext = clientSslContext;
        this.future = future;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            consumer.accept(fullHttpRequest);
            fullHttpRequests.add(fullHttpRequest);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        synchronized (this) {
            Channel channel = future.get();
            fullHttpRequests.forEach(channel::writeAndFlush);
            fullHttpRequests.clear();
        }
    }
}
