package de.jlo.talendcomp.elasticsearch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticClient {
	
	private RestHighLevelClient highLevelClient = null;
	private RestClient lowLevelClient = null;
	private List<HttpHost> hostList = new ArrayList<HttpHost>();
	private static final String DEFAULT_PORT = "9200";
	private String user = null;
	private String password = null;
	private boolean useAuthentication = false;
	private int timeout = 10000;

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
	
	public void setupClient() {
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
		        		.setContentCompressionEnabled(true);
		    }
		    
		});
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
		highLevelClient = new RestHighLevelClient(rcb);
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
			resp = highLevelClient.bulk(bulkRequest, headers);
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
	
	
	
}
