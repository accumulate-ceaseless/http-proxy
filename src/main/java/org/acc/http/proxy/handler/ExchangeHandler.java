package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

/**
 * 负责交换通道数据，不捕获内容
 */
@Log4j2
public class ExchangeHandler extends ChannelInboundHandlerAdapter {
    private final Channel outputChannel;

    public ExchangeHandler(Channel targetChannel) {
        this.outputChannel = targetChannel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        outputChannel.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        outputChannel.flush();
    }
}
