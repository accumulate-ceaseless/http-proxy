package org.acc.http.proxy.utils;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class MsgUtils {
    /**
     * 将请求转化为原始数据
     *
     * @param fullHttpRequest
     * @return
     */
    public static Object fromHttpRequest(FullHttpRequest fullHttpRequest) {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestEncoder());
        embeddedChannel.writeOutbound(fullHttpRequest);
        Object object = embeddedChannel.readOutbound();
        embeddedChannel.close();

        return object;
    }
}
