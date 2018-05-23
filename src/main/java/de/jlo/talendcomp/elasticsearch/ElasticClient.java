package de.jlo.talendcomp.elasticsearch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticClient {
	
	private static final Logger LOG = Logger.getLogger(ElasticClient.class); 
	private RestHighLevelClient client = null;
	private List<HttpHost> hostList = new ArrayList<HttpHost>();
	private static final String DEFAULT_PORT = "9200";
	private String user = null;
	private String password = null;

	/**
	 * Set the hosts in form of semicolon delimited list of hosts
	 * @param nodes host1:port1;host2;host3:port3
	 * @param protocol http (default) or https protocol
	 */
	public void setNodes(String nodes, String protocol) throws Exception {
		if (isEmpty(protocol)) {
			protocol = "http";
		}
		hostList = new ArrayList<HttpHost>();
		if (isEmpty(nodes) == false) {
			StringTokenizer st = new StringTokenizer(nodes, ";,|");
			while (st.hasMoreTokens()) {
				String hostAndPort = st.nextToken().trim();
				if (isEmpty(hostAndPort) == false) {
					try {
						int pos = hostAndPort.indexOf(':');
						String host = hostAndPort.substring(0, pos);
						String port = hostAndPort.substring(pos + 1);
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
		client = new RestHighLevelClient(RestClient.builder(httpHosts));
	}
	
	public RestHighLevelClient getRestHighLevelClient() {
		return client;
	}
	
	public void close() {
		if (client != null) {
			try {
				client.close();
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
			this.user = user;
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
		if (LOG.isDebugEnabled()) {
			LOG.debug(">> Execute bulk request: " + bulkRequest.getDescription());
		}
		BulkResponse resp = null;
		// TODO add retry feature
		try {
			resp = client.bulk(bulkRequest, headers);
			if (LOG.isDebugEnabled()) {
				LOG.debug("<< has failures?: " + resp.hasFailures() + " status: " + resp.status());
			}
		} catch (Exception ex) {
			String message = ex.getMessage();
			if (message == null) {
				message = ex.getClass().getName();
			}
			throw new Exception("executeBulk failed: " + ex.getMessage(), ex);
		}
		return resp;
	}
	
	public void setDebug(boolean debug) {
		if (debug) {
			Logger.getRootLogger().setLevel(Level.DEBUG);
		} else {
			Logger.getRootLogger().setLevel(Level.INFO);
		}
	}
	
}
