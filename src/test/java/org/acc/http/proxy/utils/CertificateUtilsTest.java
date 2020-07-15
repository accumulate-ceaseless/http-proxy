package org.acc.http.proxy.utils;

import org.acc.http.proxy.certificate.CertificateImpl;
import org.acc.http.proxy.certificate.CertificateName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.security.cert.X509Certificate;

public class CertificateUtilsTest {
    @Disabled
    @Test
    public void generateDefaultRootTest() throws Exception {
        CertificateUtils.generateDefaultRoot(new CertificateImpl());
    }

    @Test
    public void subjectTest() {
        X509Certificate certificate = CertificateUtils.readRootCertificate(Paths.get(CertificateName.RootCertificateName));

        CertificateUtils.subject(certificate);
    }
}
