package org.acc.http.proxy.certificate;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

@Log4j2
public class CertificateTest {
    private Certificate certificate = new CertificateImpl();

    @Test
    public void generateTest() throws Exception {
        X509Certificate x509Certificate = certificate.generate("www.baidu.com");
        log.info(x509Certificate);
    }
}
