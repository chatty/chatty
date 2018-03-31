
package chatty.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;


/**
 * Based on https://stackoverflow.com/a/34111150/2375667
 * 
 * @author tduva
 */
public class SSLUtil {
    
    private static final Logger LOGGER = Logger.getLogger(SSLUtil.class.getName());
    
    private static void addCert(KeyStore keyStore, String certFile) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (InputStream caInput = new BufferedInputStream(
                SSLUtil.class.getResourceAsStream(certFile))) {

            Certificate crt = cf.generateCertificate(caInput);
            System.out.println("Added Cert for " + ((X509Certificate) crt)
                    .getSubjectDN());

            keyStore.setCertificateEntry(certFile, crt);
        }
    }
    
    public static SSLContext getSSLContextWithLE() throws Exception {
        // Load existing certs
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Path ksPath = Paths.get(System.getProperty("java.home"),
                "lib", "security", "cacerts");
        keyStore.load(Files.newInputStream(ksPath),
                "changeit".toCharArray());

        addCert(keyStore, "DSTRootCAX3.crt");
        addCert(keyStore, "isrgrootx1.crt");

//        if (false) { // enable to see
//            System.out.println("Truststore now trusting: ");
//            PKIXParameters params = new PKIXParameters(keyStore);
//            params.getTrustAnchors().stream()
//                    .map(TrustAnchor::getTrustedCert)
//                    .map(X509Certificate::getSubjectDN)
//                    .forEach(System.out::println);
//            System.out.println();
//        }

        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
    
    public static void addLetsEncrypt() {
        try {
            SSLContext.setDefault(getSSLContextWithLE());
        } catch (Exception e) {
            LOGGER.warning("Failed adding cert: "+e);
        }
    }

    public static void main(String[] args) throws IOException {
        addLetsEncrypt();
        
        // signed by default trusted CAs.
        testUrl(new URL("https://google.com"));
        testUrl(new URL("https://www.thawte.com"));

        // signed by letsencrypt
        testUrl(new URL("https://helloworld.letsencrypt.org"));
        // signed by LE's cross-sign CA
        testUrl(new URL("https://letsencrypt.org"));
        // expired
        testUrl(new URL("https://tv.eurosport.com/"));
        // self-signed
        testUrl(new URL("https://www.pcwebshop.co.uk/"));

    }

    static void testUrl(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        try {
            connection.connect();
            System.out.println("Headers of " + url + " => "
                    + connection.getHeaderFields());
        } catch (SSLHandshakeException e) {
            System.out.println("Untrusted: " + url);
        }
    }

}