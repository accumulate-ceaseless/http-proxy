package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.utils.ChannelUtils;
import org.acc.http.proxy.utils.ReleaseUtils;
import org.acc.http.proxy.utils.ThrowableUtils;

import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private Consumer<FullHttpRequest> consumer;
    private final Channel outputChannel;

    private final String desc;

    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer, Channel outputChannel, String desc) {
        this.consumer = consumer;
        this.outputChannel = outputChannel;
        this.desc = desc;
    }

    public CaptureExchangeHandler(Channel outputChannel, String desc) {
        this.outputChannel = outputChannel;
        this.desc = desc;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            consumer.accept(fullHttpRequest);

            ChannelUtils.writeAndFlush(outputChannel, fullHttpRequest);
            ReleaseUtils.release(fullHttpRequest);
        } else if (msg instanceof FullHttpResponse) {
            ChannelUtils.writeAndFlush(outputChannel, msg);
        }
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ThrowableUtils.message(desc, this.getClass(), cause);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
