package org.acc.http.proxy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class ServerTest {
    private Server server = new Server();

    @Test
    public void runTest() {
        server.startUp(8001);
    }
}
