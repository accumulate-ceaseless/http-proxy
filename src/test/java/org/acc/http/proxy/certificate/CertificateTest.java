package org.acc.http.proxy.certificate;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

@Log4j2
public class CertificateTest {
    private Certificate certificate = new CertificateImpl();

    @Test
    public void generateTest() throws Exception {
        log.info(certificate.generate("www.baidu.com"));
    }
}
