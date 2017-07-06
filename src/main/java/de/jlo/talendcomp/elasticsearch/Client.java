package de.jlo.talendcomp.elasticsearch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

public class Client {
	
	private TransportClient client = null;
	private Boolean paramClientTransportSniff = null;
	private Integer paramClientTransportPingTimeout = null;
	private Integer paramClientTransportNodeSamplerInterval = null;
	private String clusterName = null;
	private List<String> hostList = new ArrayList<String>();
	private static final int DEFAULT_PORT = 9300;
	private String user = null;
	private String password = null;
	private boolean useSSL = false;
	
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
	
	/**
	 * Set the hosts in form of semicolon delimited list of hosts
	 * @param hosts host1:port1;host2;host3:port3
	 */
	public void setHosts(String hosts) {
		hostList = new ArrayList<String>();
		if (isEmpty(hosts) == false) {
			StringTokenizer st = new StringTokenizer(hosts, ";,|");
			while (st.hasMoreTokens()) {
				String host = st.nextToken().trim();
				if (isEmpty(host) == false) {
					hostList.add(host);
				}
			}
		}
	}
	
	private InetSocketTransportAddress getAddress(String hostPort) throws UnknownHostException {
		int pos = hostPort.indexOf(':');
		String host = null;
		int port = DEFAULT_PORT;
		if (pos != -1) {
			host = hostPort.substring(0, pos);
			String portStr = hostPort.substring(pos + 1);
			if (isEmpty(portStr)) {
				throw new IllegalArgumentException(hostPort + " is not a valid address like host:port");
			}
			port = Integer.parseInt(portStr);
		} else {
			host = hostPort;
		}
		return new InetSocketTransportAddress(InetAddress.getByName(host), port);
	}
	
	private void configureAddresses() throws UnknownHostException {
		if (hostList.isEmpty()) {
			throw new IllegalStateException("No hosts set!");
		} else {
			for (String host : hostList) {
				client.addTransportAddress(getAddress(host));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void initialize() throws Exception {
		org.elasticsearch.common.settings.Settings.Builder builder = Settings.builder();
		if (paramClientTransportSniff != null) {
			builder.put("client.transport.sniff", paramClientTransportSniff);
		}
		if (paramClientTransportPingTimeout != null) {
			builder.put("client.transport.ping_timeout", paramClientTransportPingTimeout);
		}
		if (paramClientTransportNodeSamplerInterval != null) {
			builder.put("client.transport.nodes_sampler_interval", paramClientTransportNodeSamplerInterval);
		}
		if (isEmpty(clusterName) == false) {
			builder.put("cluster.name", clusterName);
		}
		if (useSSL) {
			builder.put("xpack.security.enabled", "true");
			builder.put("xpack.security.transport.ssl.enabled", "true");
		}
		if (isEmpty(user) == false && isEmpty(password) == false) {
			builder.put("xpack.security.user", user + ":" + password);
		}
		Settings settings = builder.build();
		client = new PreBuiltXPackTransportClient(settings);
		configureAddresses();
	}

	public Boolean getParamClientTransportSniff() {
		return paramClientTransportSniff;
	}

	public void setParamClientTransportSniff(Boolean paramClientTransportSniff) {
		this.paramClientTransportSniff = paramClientTransportSniff;
	}

	public Integer getParamClientTransportPingTimeout() {
		return paramClientTransportPingTimeout;
	}

	public void setParamClientTransportPingTimeout(Integer paramClientTransportPingTimeout) {
		this.paramClientTransportPingTimeout = paramClientTransportPingTimeout;
	}

	public Integer getParamClientTransportNodeSamplerInterval() {
		return paramClientTransportNodeSamplerInterval;
	}

	public void setParamClientTransportNodeSamplerInterval(Integer paramClientTransportNodeSamplerInterval) {
		this.paramClientTransportNodeSamplerInterval = paramClientTransportNodeSamplerInterval;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		if (isEmpty(clusterName) == false) {
			this.clusterName = clusterName.trim();
		}
	}

	public TransportClient getClient() {
		return client;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

}
