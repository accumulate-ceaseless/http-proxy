package org.acc.http.proxy.certificate;

import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.pojo.CertificateInfo;
import org.acc.http.proxy.utils.CertificateUtils;

import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public final class CertificatePool {
    private final ConcurrentHashMap<String, CertificateInfo> certificateInfoMap = new ConcurrentHashMap<>();
    private X509Certificate rootCertificate;
    private PrivateKey rootPrivateKey;

    private final Certificate certificate;

    public CertificatePool(Certificate certificate) {
        this.certificate = certificate;

        initRootCertificateInfo();
    }

    private void initRootCertificateInfo() {
        rootCertificate = CertificateUtils.readRootCertificate(Paths.get(CertificateName.RootCertificateName));
        rootPrivateKey = CertificateUtils.readPrivateKey(Paths.get(CertificateName.RootCertificatePrivateKeyName));
    }

    public CertificateInfo getCertificateInfo(String host, int port) {
        try {
            String key = host + ":" + port;

            CertificateInfo certificateInfo = certificateInfoMap.get(key);
            if (Objects.nonNull(certificateInfo)) {
                return certificateInfo;
            }

            KeyPair keyPair = certificate.generateKeyPair();

            X509Certificate x509Certificate = certificate.generate(CertificateName.Issuer,
                    rootPrivateKey,
                    rootCertificate.getNotBefore(),
                    rootCertificate.getNotAfter(),
                    keyPair.getPublic(),
                    Collections.singletonList(host));

            certificateInfo = new CertificateInfo(keyPair, x509Certificate);

            // 超过5000就清空
            if (certificateInfoMap.size() >= 5000) {
                certificateInfoMap.clear();
            }

            certificateInfoMap.put(key, certificateInfo);

            return certificateInfo;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | GenerateCertificateException e) {
            log.error(e);
        }

        return null;
    }
}
