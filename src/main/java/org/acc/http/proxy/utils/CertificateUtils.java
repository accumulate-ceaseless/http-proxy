package org.acc.http.proxy.utils;

import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.certificate.Certificate;
import org.acc.http.proxy.certificate.CertificateName;
import org.acc.http.proxy.certificate.GenerateCertificateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
public final class CertificateUtils {
    private static KeyFactory keyFactory;

    static {
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
    }

    /**
     * 生成默认的根证书
     *
     * @param certificate
     * @return
     * @throws GenerateCertificateException
     */
    public static byte[] generateDefaultRoot(Certificate certificate) throws GenerateCertificateException {
        try {
            KeyPair keyPair = certificate.generateKeyPair();

            // 20年
            X509Certificate x509Certificate = certificate.generateRoot("C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=Proxy",
                    new Date(),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365 * 20)),
                    keyPair);

            byte[] bytes = x509Certificate.getEncoded();

            writePrivateKey(keyPair.getPrivate(), Paths.get(CertificateName.rootCertificatePrivateKeyName));

            return bytes;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateEncodingException e) {
            throw new GenerateCertificateException("生成默认的根证书失败", e);
        }
    }

    /**
     * 写入私钥
     *
     * @param privateKey
     * @param path
     */
    public static void writePrivateKey(PrivateKey privateKey, Path path) {
        try {
            Files.write(path, new PKCS8EncodedKeySpec(privateKey.getEncoded()).getEncoded());
        } catch (IOException e) {
            log.error("写入密钥失败", e);
        }
    }

    /**
     * 读取私钥
     *
     * @param path
     * @return
     */
    public static PrivateKey readPrivateKey(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytes);

            return keyFactory.generatePrivate(privateKeySpec);
        } catch (IOException | InvalidKeySpecException e) {
            log.error("加载私钥失败", e);
        }

        return null;
    }
}
