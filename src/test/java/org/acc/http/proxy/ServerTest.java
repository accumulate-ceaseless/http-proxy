package org.acc.http.proxy;

import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

//@Disabled
@Log4j2
public class ServerTest {
    private Server server = new Server();

    @Test
    public void startUpTest() {
        server.run(8001);
    }

    @Test
    public void startUpConsume() {
        server.runWithConsumer(8001, httpRequest -> {
            log.info("{}", httpRequest);
            log.info("主机{}， 请求地址 {}", httpRequest.headers().get(HttpHeaderNames.HOST), httpRequest.uri());
        });
    }
}
