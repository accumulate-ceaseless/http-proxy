package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;

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
            outputChannel.writeAndFlush(fullHttpRequest);

            ReferenceCountUtil.release(fullHttpRequest);
            ctx.flush();
        } else if (msg instanceof FullHttpResponse) {
            if (outputChannel.isOpen()) {
                outputChannel.writeAndFlush(msg);
                ctx.flush();
            } else {
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(this.getClass().getSimpleName(), cause);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
