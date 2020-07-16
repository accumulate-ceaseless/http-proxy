package org.acc.http.proxy.certificate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CertificatePoolTest {
    private CertificatePool certificatePool = new CertificatePool(new CertificateImpl());

    @Disabled
    @Test
    public void tt() {
        certificatePool.getCertificateInfo("123", 443);
        certificatePool.getCertificateInfo("456", 443);
        certificatePool.getCertificateInfo("123", 443);
    }
}
