package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;
import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private final Consumer<FullHttpRequest> consumer;
    private final Channel outputChannel;
    private FullHttpRequest fullHttpRequest;

    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer, Channel outputChannel) {
        this.consumer = consumer;
        this.outputChannel = outputChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            fullHttpRequest = (FullHttpRequest) msg;

            fullHttpRequest.retain();
            consumer.accept(fullHttpRequest);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (Objects.nonNull(fullHttpRequest) && outputChannel.isOpen()) {
            outputChannel.writeAndFlush(fullHttpRequest);
        }
        ctx.flush();
    }
}
