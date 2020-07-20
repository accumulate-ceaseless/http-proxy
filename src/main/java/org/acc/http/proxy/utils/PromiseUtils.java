package org.acc.http.proxy.utils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Promise;

public final class PromiseUtils {
    public static Promise<Channel> promise(String host, int port, ChannelHandlerContext ctx, ChannelHandler channelHandler) {
        Promise<Channel> promise = ctx.executor().newPromise();

        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(channelHandler)
                .connect(host, port)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        promise.setSuccess(future.channel());
                    } else {
                        ctx.close();
                        future.cancel(true);
                    }
                });

        return promise;
    }
}
