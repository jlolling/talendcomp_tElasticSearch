package de.jlo.talendcomp.elasticsearch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestExecuter {

	private RestClient restClient = null;
	private int httpStatusCode = 0;
	private String errorMessage = null;
	protected final static ObjectMapper objectMapper = new ObjectMapper();
	private Map<String, String> queryParams = new HashMap<>();
	private String path = null;
	private String method = null;
	private Map<String, String> headerMap = new HashMap<>();
	private Object payload = null;
	
	public RequestExecuter(ElasticClient elasticClient) {
		if (elasticClient == null) {
			throw new IllegalArgumentException("ElasticClient cannot be null!");
		}
		this.restClient = elasticClient.getLowLevelClient();
	}
	
	public String performRequest() throws Exception {
		if (isEmpty(path)) {
			throw new IllegalArgumentException("Path cannot be null or empty");
		}
		if (isEmpty(method)) {
			throw new IllegalArgumentException("Method cannot be null or empty");
		}
		HttpEntity entity = buildEntity(payload);
		Header[] headerArray = null;
		if (headerMap == null || headerMap.isEmpty()) {
			headerMap = new HashMap<>();
			headerMap.put("Content-Type", "application/json");
			headerMap.put("Accept", "application/json");
		} else {
			if (headerMap.containsKey("Content-Type") == false) {
				headerMap.put("Content-Type", "application/json");
			}
			if (headerMap.containsKey("Accept") == false) {
				headerMap.put("Accept", "application/json");
			}
		}
		if (headerMap.size() > 0) {
			headerArray = new Header[headerMap.size()];
			int i = 0;
			for (Map.Entry<String, String> entry : headerMap.entrySet()) {
				if (entry.getValue() == null || entry.getValue().isEmpty()) {
					throw new IllegalStateException("Header " +  entry.getKey() + " cannot have null or empty string as value!");
				}
				headerArray[i++] = new BasicHeader(entry.getKey(), entry.getValue());
			}
		}
		Response response = null;
		try {
			response = restClient.performRequest(method, path, queryParams, entity, headerArray);
			StatusLine sl = response.getStatusLine();
			httpStatusCode = sl.getStatusCode();
			if (httpStatusCode == 200) {
				return EntityUtils.toString(response.getEntity(), "UTF-8");
			} else {
				errorMessage = sl.getReasonPhrase() + ": " + EntityUtils.toString(response.getEntity(), "UTF-8");
				return errorMessage;
			}
		} catch (ResponseException cpe) {
			Response errorResponse = cpe.getResponse();
			errorMessage = EntityUtils.toString(errorResponse.getEntity(), "UTF-8");
			httpStatusCode = errorResponse.getStatusLine().getStatusCode();
			return errorMessage;
		} catch (ClientProtocolException cpe) {
			throw new Exception("Request http-protocol error: " + cpe.getMessage(), cpe);
		} catch (IOException ioe) {
			throw new Exception("Request io-error: " + ioe.getMessage(), ioe);
		}
	}
	
	private HttpEntity buildEntity(Object content) throws UnsupportedEncodingException {
		if (content instanceof JsonNode) {
			JsonNode node = (JsonNode) content;
			if (node != null && node.isNull() == false && node.isMissingNode() == false) {
				HttpEntity entity = new StringEntity(node.toString(), "UTF-8");
				return entity;
			}
		} else if (content instanceof String) {
			HttpEntity entity = new StringEntity((String) content, "UTF-8");
			return entity;
		} else if (content != null) {
			HttpEntity entity = new StringEntity(String.valueOf(content), "UTF-8");
			return entity;
		}
		return null;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}
	
	public static boolean isEmpty(String s) {
		return (s != null && s.trim().isEmpty() == false && "null".equals(s) == false) == false;
	}
	
	public void setQueryParameter(String key, String value) {
		if (isEmpty(key) == false && isEmpty(value) == false) {
			queryParams.put(key, value);	
		}
	}
	
	public void setHeaderParameter(String key, String value) {
		if (isEmpty(key) == false && isEmpty(value) == false) {
			headerMap.put(key, value);
		}
	}

	public void setPath(String path) {
		if (isEmpty(path)) {
			throw new IllegalArgumentException("Path cannot be null or empty");
		}
		this.path = path;
	}
	
	public void setMethod(String method) {
		if (isEmpty(method)) {
			throw new IllegalArgumentException("Method cannot be null or empty");
		}
		this.method = method;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	public void close() {
		if (restClient != null) {
			try {
				restClient.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

}
