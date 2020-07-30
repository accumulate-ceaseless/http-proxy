package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.utils.ChannelUtils;
import org.acc.http.proxy.utils.ThrowableUtils;

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
        ThrowableUtils.message(this.getClass(), cause);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ChannelUtils.writeAndFlush(outputChannel, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ChannelUtils.close(outputChannel);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
