package de.jlo.talendcomp.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;

public class ElasticClient {
	
	private RestHighLevelClient highLevelClient = null;
	private RestClient lowLevelClient = null;
	private List<HttpHost> hostList = new ArrayList<HttpHost>();
	private static final String DEFAULT_PORT = "9200";
	private String user = null;
	private String password = null;
	private boolean useAuthentication = false;
	private int timeout = 10000;
	private String pathToCertificate = null;

	/**
	 * Set the hosts in form of semicolon delimited list of hosts
	 * @param nodes host1:port1;host2;host3:port3
	 * @param encrypted
	 */
	public void setNodes(String nodes, Boolean encrypted) throws Exception {
		String protocol = "http";
		if (encrypted != null && encrypted.booleanValue()) {
			protocol = "https";
		}
		hostList = new ArrayList<HttpHost>();
		if (isEmpty(nodes) == false) {
			StringTokenizer st = new StringTokenizer(nodes, ";,|");
			while (st.hasMoreTokens()) {
				String hostAndPort = st.nextToken().trim();
				if (isEmpty(hostAndPort) == false) {
					try {
						String host = null;
						String port = null;
						int pos = hostAndPort.indexOf(':');
						if (pos != -1) {
							host = hostAndPort.substring(0, pos);
							port = hostAndPort.substring(pos + 1);
						} else {
							host = hostAndPort;
						}
						if (isEmpty(port)) {
							port = DEFAULT_PORT;
						}
						URL url = new URL(protocol + "://" + host + ":" + port);
						hostList.add(new HttpHost(url.getHost(), url.getPort(), url.getProtocol()));
					} catch (MalformedURLException e) {
						throw new Exception("setNodes for url: " + protocol + "://" + hostAndPort + "failed: " + e.getMessage(), e);
					}
				}
			}
		}
	}

	public static boolean isEmpty(String s) {
		if (s == null) {
			return true;
		}
		if (s.trim().isEmpty()) {
			return true;
		}
		if (s.trim().equalsIgnoreCase("null")) {
			return true;
		}
		return false;
	}
	
	private SSLContext buildSSLContext() throws Exception {
		if (pathToCertificate == null) {
			return null;
		}
		File f = new File(pathToCertificate);
		if (f.canRead() == false) {
			throw new Exception("Certificate file: " + f.getAbsolutePath() + " cannot be read");
		}
		if (f.getName().endsWith(".crt")) {
			Path caCertificatePath = Paths.get(f.getAbsolutePath());
			CertificateFactory factory =
			    CertificateFactory.getInstance("X.509");
			Certificate trustedCa;
			try (InputStream is = Files.newInputStream(caCertificatePath)) {
			    trustedCa = factory.generateCertificate(is);
			}
			KeyStore trustStore = KeyStore.getInstance("pkcs12");
			trustStore.load(null, null);
			trustStore.setCertificateEntry("ca", trustedCa);
			SSLContextBuilder sslContextBuilder = SSLContexts.custom()
			    .loadTrustMaterial(trustStore, null);
			final SSLContext sslContext = sslContextBuilder.build();
			return sslContext;
		} else {
			throw new Exception("Currently ony *.crt files are allowed.");
		}
	}
	
	public void setupClient() throws Exception {
		if (hostList.isEmpty()) {
			throw new IllegalStateException("No hosts defined! Please setup at least one host."); 
		}
		HttpHost[] httpHosts = new HttpHost[hostList.size()];
		for (int i = 0; i < hostList.size(); i++) {
			httpHosts[i] = hostList.get(i);
		}
		RestClientBuilder rcb = RestClient.builder(httpHosts);
		rcb.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
			
		    @Override
		    public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
		        return requestConfigBuilder
		        		.setSocketTimeout(timeout)
		        		.setConnectTimeout(timeout)
                        .setRedirectsEnabled(true)
                        .setRelativeRedirectsAllowed(true)
		        		.setContentCompressionEnabled(true)
						;
		    }
		    
		});
		SSLContext sslContext = buildSSLContext();
		if (sslContext != null) {
			rcb.setHttpClientConfigCallback(new HttpClientConfigCallback() {
				
		        @Override
		        public HttpAsyncClientBuilder customizeHttpClient(
		            HttpAsyncClientBuilder httpClientBuilder) {
		            return httpClientBuilder.setSSLContext(sslContext);
		        }
		        
		    });
		}
		Header[] defaultHeaders = null;
		if (useAuthentication && user != null) {
			if (isEmpty(password)) {
				throw new IllegalStateException("Password not set!");
			}
			defaultHeaders = new Header[3];
			defaultHeaders[0] = new BasicHeader("Content-Type", "application/x-ndjson");
			defaultHeaders[1] = new BasicHeader("Cache-Control", "no-cache");
			defaultHeaders[2] = new BasicHeader("Authorization", "Basic " + Base64.encodeToBase64String(user + ":" + password));
		} else {
			defaultHeaders = new Header[2];
			defaultHeaders[0] = new BasicHeader("Content-Type", "application/x-ndjson");
			defaultHeaders[1] = new BasicHeader("Cache-Control", "no-cache");
		}
		rcb.setDefaultHeaders(defaultHeaders);

		highLevelClient = new RestHighLevelClientBuilder(rcb.build())
				.setApiCompatibilityMode(true).build();
		lowLevelClient = highLevelClient.getLowLevelClient();
	}
	
	public RestHighLevelClient getRestHighLevelClient() {
		return highLevelClient;
	}
	
	public void close() {
		if (highLevelClient != null) {
			try {
				highLevelClient.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		if (isEmpty(user) == false) {
			useAuthentication = true;
			this.user = user;
		} else {
			useAuthentication = false;
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (isEmpty(password) == false) {
			this.password = password;
		}
	}
	
	public BulkResponse executeBulk(BulkRequest bulkRequest, Header ... headers) throws Exception {
		BulkResponse resp = null;
		// TODO add retry feature
		try {
			RequestOptions options = RequestOptions.DEFAULT;
			resp = highLevelClient.bulk(bulkRequest, options);
		} catch (Exception ex) {
			String message = ex.getMessage();
			if (message == null) {
				message = ex.getClass().getName();
			}
			throw new Exception("execute bulk request to servers: " + hostList + " failed: " + message, ex);
		}
		return resp;
	}
	
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		if (timeout != null && timeout.intValue() > 0) {
			this.timeout = timeout;
		}
	}

	public RestClient getLowLevelClient() {
		return lowLevelClient;
	}

	public String getPathToCertificate() {
		return pathToCertificate;
	}

	public void setPathToCertificate(String pathToCertificate) {
		if (pathToCertificate != null && pathToCertificate.trim().isEmpty() == false) {
			this.pathToCertificate = pathToCertificate.trim();
		} else {
			this.pathToCertificate = null;
		}
	}
	
	
	
}
