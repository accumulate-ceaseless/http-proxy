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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public final class CertificatePool {
    private final Map<String, CertificateInfo> certificateInfoMap = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    private final Certificate certificate;

    public CertificatePool(Certificate certificate) {
        this.certificate = certificate;
    }

    public CertificateInfo getCertificateInfo(String host, int port) {
        lock.lock();

        try {
            String key = host + ":" + port;

            CertificateInfo certificateInfo = certificateInfoMap.get(key);
            if (Objects.nonNull(certificateInfo)) {
                return certificateInfo;
            }

            X509Certificate rootCertificate = CertificateUtils.readRootCertificate(Paths.get(CertificateName.RootCertificateName));
            PrivateKey rootPrivateKey = CertificateUtils.readPrivateKey(Paths.get(CertificateName.RootCertificatePrivateKeyName));

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
                log.info("证书池超最大容量-执行清理");
            }

            certificateInfoMap.put(key, certificateInfo);

            return certificateInfo;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | GenerateCertificateException e) {
            log.error(e);
        } finally {
            lock.unlock();
        }

        return null;
    }
}
