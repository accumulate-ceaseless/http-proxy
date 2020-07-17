package org.acc.http.proxy.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 代理服务器与目标服务器之间的通道处理器：将目标服务返回的数据转发到客户端
 */
@Log4j2
public class TargetHandler extends ChannelInboundHandlerAdapter {
    // 客户端与代理服务器建立的通道
    private final Channel clientChannel;
    private String host;

    public TargetHandler(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    public TargetHandler(Channel clientChannel, String host) {
        this.clientChannel = clientChannel;
        this.host = host;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        log.error(cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("响应");

        if (Objects.nonNull(host)) {
            ByteBuf byteBuf = (ByteBuf) msg;
            log.info("明文拦截, 目标服务器返回数据 {} - {}", host, byteBuf.toString(StandardCharsets.UTF_8));
        }

        clientChannel.writeAndFlush(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        clientChannel.flush();
    }
}
