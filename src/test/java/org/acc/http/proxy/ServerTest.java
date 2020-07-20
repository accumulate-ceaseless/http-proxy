package org.acc.http.proxy;

import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

//@Disabled
@Log4j2
public class ServerTest {
    private Server server = new Server();

    @Test
    public void runTest() {
        server.run(8001);
    }

    @Test
    public void runWithConsumerTest() {
        server.runWithConsumer(8001, httpRequest -> {
//            log.info("请求 {}", httpRequest);
            log.info("外面主机{}， 请求地址 {}", httpRequest.headers().get(HttpHeaderNames.HOST), httpRequest.uri());
        });
    }
}
