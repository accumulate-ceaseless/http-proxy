package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.utils.ChannelUtils;
import org.acc.http.proxy.utils.ThrowableUtils;

import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private Consumer<FullHttpRequest> consumer;
    private final Channel outputChannel;

    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer, Channel outputChannel) {
        this.consumer = consumer;
        this.outputChannel = outputChannel;
    }

    public CaptureExchangeHandler(Channel outputChannel) {
        this.outputChannel = outputChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            consumer.accept(fullHttpRequest);

            fullHttpRequest.retain();
            ChannelUtils.writeAndFlush(outputChannel, fullHttpRequest);

            ReferenceCountUtil.release(fullHttpRequest);
        } else if (msg instanceof FullHttpResponse) {
            ChannelUtils.writeAndFlush(outputChannel, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ThrowableUtils.message(this.getClass(), cause);
        ctx.close();
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
