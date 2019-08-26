package com.mkyong;

import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

//https://www.agilegroup.co.jp/technote/springboot-fileupload-error-handling.html
@SpringBootApplication
public class SpringBootWebApplication {

    private int maxUploadSizeInMb = 250 * 1024 * 1024; // 250 MB

    public static void main(String[] args) throws Exception {
    	TrustManager[] trustAllCerts = new TrustManager[] {
    		       new X509TrustManager() {
    		          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    		            return null;
    		          }

    		          public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

    		          public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

    		       }
    		    };

    		    SSLContext sc = SSLContext.getInstance("SSL");
    		    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    		    // Create all-trusting host name verifier
    		    HostnameVerifier allHostsValid = new HostnameVerifier() {
    		        public boolean verify(String hostname, SSLSession session) {
    		          return true;
    		        }
    		    };
    		    // Install the all-trusting host verifier
    		    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    		    /*
    		     * end of the fix
    		     */
        SpringApplication.run(SpringBootWebApplication.class, args);
    }

    //Tomcat large file upload connection reset
    //http://www.mkyong.com/spring/spring-file-upload-and-connection-reset-issue/
    @Bean
    public TomcatEmbeddedServletContainerFactory tomcatEmbedded() {

        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

        tomcat.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
            if ((connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?>)) {
                //-1 means unlimited
                ((AbstractHttp11Protocol<?>) connector.getProtocolHandler()).setMaxSwallowSize(-1);
            }
        });

        return tomcat;

    }

}