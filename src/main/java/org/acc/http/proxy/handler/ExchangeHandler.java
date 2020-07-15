package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ExchangeHandler extends ChannelInboundHandlerAdapter {
    private Channel exchangeChannel;

    public ExchangeHandler(Channel exchangeChannel) {
        this.exchangeChannel = exchangeChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        exchangeChannel.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        exchangeChannel.flush();
    }
}
