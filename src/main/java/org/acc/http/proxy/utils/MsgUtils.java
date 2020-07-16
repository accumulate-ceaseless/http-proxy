package org.acc.http.proxy.utils;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;

public final class MsgUtils {
    /**
     * 将请求转化为原始数据
     *
     * @param httpRequest
     * @return
     */
    public static Object fromHttpRequest(HttpRequest httpRequest) {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestEncoder());
        embeddedChannel.writeOutbound(httpRequest);
        Object object = embeddedChannel.readOutbound();
        embeddedChannel.close();

        return object;
    }
}
