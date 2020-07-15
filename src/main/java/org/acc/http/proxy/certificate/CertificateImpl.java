package org.acc.http.proxy.certificate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import static javax.management.timer.Timer.ONE_DAY;

@Log4j2
public class CertificateImpl implements Certificate {
    private final Provider provider = new BouncyCastleProvider();

    public CertificateImpl() {
        Security.addProvider(provider);
    }

    @Override
    public X509Certificate generate(String host) throws GenerateCertificateException {
        try {
            long millis = System.currentTimeMillis();
            Key key = generateKey();

            X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
                    new X500Name("C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=ProxyRoot"),
                    BigInteger.valueOf(millis),
                    new Date(millis - 10 * ONE_DAY),
                    new Date(millis + 3650 * ONE_DAY),
                    new X500Name("C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + host),
                    SubjectPublicKeyInfo.getInstance(new ASN1InputStream(key.getPublicKey().getEncoded()).readObject())
            );

            byte[] signatureBytes = signatureBytes(key);

            X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(new ContentSigner() {
                private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                {
                    byteArrayOutputStream.write(signatureBytes);
                }

                @Override
                public AlgorithmIdentifier getAlgorithmIdentifier() {
                    return new AlgorithmIdentifier(PKCSObjectIdentifiers.sha1WithRSAEncryption);
                }

                @Override
                public OutputStream getOutputStream() {
                    return byteArrayOutputStream;
                }

                @Override
                public byte[] getSignature() {
                    return signatureBytes;
                }
            });

            return (X509Certificate) CertificateFactory
                    .getInstance("X509")
                    .generateCertificate(new ByteArrayInputStream(x509CertificateHolder.getEncoded()));
        } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | CertificateException | IOException e) {
            throw new GenerateCertificateException("生成证书失败", e);
        }
    }

    /**
     * 签名数据
     *
     * @param key
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    private byte[] signatureBytes(Key key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(key.getPrivateKey());
        signature.update(key.publicKey.getEncoded());

        return signature.sign();
    }

    /**
     * 生成密钥
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    private Key generateKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", provider.getName());
        keyPairGenerator.initialize(2048, new SecureRandom());

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        return new Key(keyPair.getPrivate(), keyPair.getPublic());
    }

    @Data
    @AllArgsConstructor
    private static class Key {
        private PrivateKey privateKey;
        private PublicKey publicKey;
    }
}
