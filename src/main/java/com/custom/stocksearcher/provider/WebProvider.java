package com.custom.stocksearcher.provider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * wrapper RestTemplate & Jsoup
 */
@Component
public class WebProvider {
    String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36";

    public Document getHtmlDoc(String url, boolean isNeedClean) throws Exception {
        SSLUtil.trustAllHttpsCertificates();
        String html = Jsoup.connect(url).userAgent(userAgent).ignoreContentType(true).get().toString();
        return isNeedClean ?
                Jsoup.parseBodyFragment(Jsoup.clean(html, Safelist.basic())) :
                Jsoup.parseBodyFragment(html);
    }

    public <T> T getUrlToObject(String url, Class<T> c) {
        SSLUtil.trustAllHttpsCertificates();

        return new RestTemplate().getForObject(url, c);
    }

}

/**
 * for pass ssl
 */
class SSLUtil {
    HostnameVerifier hv = (urlHostName, session) -> true;

    public static void trustAllHttpsCertificates() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
            javax.net.ssl.TrustManager tm = new miTM();
            trustAllCerts[0] = tm;
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, null);
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class miTM implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }
}
