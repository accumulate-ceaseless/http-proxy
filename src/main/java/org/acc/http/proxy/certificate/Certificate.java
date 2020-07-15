package org.acc.http.proxy.certificate;

import java.security.cert.X509Certificate;

public interface Certificate {
    X509Certificate generate(String host) throws GenerateCertificateException;
}
