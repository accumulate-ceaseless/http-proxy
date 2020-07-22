package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private final Consumer<FullHttpRequest> consumer;
    private final Future<Channel> future;
    private FullHttpRequest fullHttpRequest;


    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer, Future<Channel> future) {
        this.consumer = consumer;
        this.future = future;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            fullHttpRequest = (FullHttpRequest) msg;

            fullHttpRequest.retain();
            consumer.accept(fullHttpRequest);
        }
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (Objects.nonNull(fullHttpRequest)) {
            future.get().writeAndFlush(fullHttpRequest);
        }
    }
}
