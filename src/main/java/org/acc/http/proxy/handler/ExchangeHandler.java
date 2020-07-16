package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.log4j.Log4j2;

import java.util.function.Consumer;

@Log4j2
public class ExchangeHandler extends ChannelInboundHandlerAdapter {
    // 目标通道（netty代理客户端与远程服务器建立的通道）
    private final Channel targetChannel;
    private Consumer<HttpRequest> consumer;

    public ExchangeHandler(Channel targetChannel) {
        this.targetChannel = targetChannel;
    }

    public ExchangeHandler(Channel targetChannel, Consumer<HttpRequest> consumer) {
        this.targetChannel = targetChannel;
        this.consumer = consumer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        log.error(cause);
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;
            consumer.accept(httpRequest);
        }

        targetChannel.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        targetChannel.flush();
    }
}
