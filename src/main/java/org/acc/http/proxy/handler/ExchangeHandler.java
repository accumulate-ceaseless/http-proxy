package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;
import java.util.function.Consumer;

@Log4j2
public class ExchangeHandler extends ChannelInboundHandlerAdapter {
    private Channel exchangeChannel;
    private Consumer<HttpRequest> consumer;

    public ExchangeHandler(Channel exchangeChannel) {
        this.exchangeChannel = exchangeChannel;
    }

    public ExchangeHandler(Channel exchangeChannel, Consumer<HttpRequest> consumer) {
        this.exchangeChannel = exchangeChannel;
        this.consumer = consumer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (Objects.nonNull(consumer) && msg instanceof HttpRequest) {
            consumer.accept((HttpRequest) msg);
        }
        exchangeChannel.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        exchangeChannel.flush();
    }
}
