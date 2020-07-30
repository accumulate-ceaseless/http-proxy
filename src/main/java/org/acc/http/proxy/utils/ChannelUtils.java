package org.acc.http.proxy.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

import java.util.Objects;

public final class ChannelUtils {
    public static void close(Channel channel) {
        if (Objects.nonNull(channel) && channel.isActive()) {
            channel.writeAndFlush(Unpooled.buffer()).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
