package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.Future;
import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final Consumer<FullHttpRequest> consumer;
    private final Future<Channel> future;
    private FullHttpRequest fullHttpRequest;


    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer, Future<Channel> future) {
        this.consumer = consumer;
        this.future = future;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        consumer.accept(msg);

        msg.retain();
        this.fullHttpRequest = msg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        future.get().writeAndFlush(fullHttpRequest);
    }
}
