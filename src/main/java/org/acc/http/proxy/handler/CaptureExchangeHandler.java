package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private final Consumer<FullHttpRequest> consumer;
    private final List<FullHttpRequest> fullHttpRequests = new ArrayList<>();
    private final Future<Channel> future;


    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer, Future<Channel> future) {
        this.consumer = consumer;
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
