package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.utils.MsgUtils;

import java.util.function.Consumer;

@Log4j2
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private final Channel outputChannel;
    private final Consumer<FullHttpRequest> consumer;

    public CaptureExchangeHandler(Channel outputChannel, Consumer<FullHttpRequest> consumer) {
        this.outputChannel = outputChannel;
        this.consumer = consumer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 只转发明文 FullHttpRequest 或 FullHttpMessage
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            consumer.accept(fullHttpRequest);
            outputChannel.write(MsgUtils.fromHttpRequest(fullHttpRequest));
        }else if (msg instanceof FullHttpMessage) {
            FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;

            outputChannel.write(MsgUtils.fromHttpResponse(fullHttpResponse));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        outputChannel.flush();
    }
}
