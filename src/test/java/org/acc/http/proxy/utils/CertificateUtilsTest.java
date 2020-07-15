package org.acc.http.proxy.utils;

import org.acc.http.proxy.certificate.CertificateImpl;
import org.junit.jupiter.api.Test;

public class CertificateUtilsTest {
    @Test
    public void generateDefaultRootTest() throws Exception {
        CertificateUtils.generateDefaultRoot(new CertificateImpl());
    }
}
