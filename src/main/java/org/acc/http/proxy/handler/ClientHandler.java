package org.acc.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.utils.MsgUtils;

import java.util.function.Consumer;

/**
 * 客户端与代理服务器通道的处理器
 */
@Log4j2
public class ClientHandler extends ChannelInboundHandlerAdapter {
    // 代理服务器与目标服务器建立的通道
    private final Channel targetChannel;

    // 获取客户端提交的请求
    private Consumer<HttpRequest> consumer;

    public ClientHandler(Channel targetChannel) {
        this.targetChannel = targetChannel;
    }

    public ClientHandler(Channel targetChannel, Consumer<HttpRequest> consumer) {
        this.targetChannel = targetChannel;
        this.consumer = consumer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        log.error(cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;

            consumer.accept(httpRequest);
            targetChannel.write(MsgUtils.fromHttpRequest(httpRequest));
        } else if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;

            targetChannel.write(httpContent.content());
        } else {
            targetChannel.write(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        targetChannel.flush();
    }
}
