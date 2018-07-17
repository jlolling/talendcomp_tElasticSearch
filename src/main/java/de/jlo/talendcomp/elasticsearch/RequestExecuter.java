package de.jlo.talendcomp.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestExecuter {

	private RestClient restClient = null;
	private int httpStatusCode = 0;
	private String errorMessage = null;
	protected final static ObjectMapper objectMapper = new ObjectMapper();
	
	public RequestExecuter(ElasticClient elasticClient) {
		if (elasticClient == null) {
			throw new IllegalArgumentException("ElasticClient cannot be null!");
		}
		this.restClient = elasticClient.getLowLevelClient();
	}
	
	public String performRequest(String method, String path, Map<String, String> queryParams, Object payload, Map<String, String> headerMap) throws Exception {
		HttpEntity entity = buildEntity(payload);
		Header[] headerArray = null;
		if (headerMap == null) {
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
				if (entry.getValue() != null && entry.getValue().isEmpty() == false) {
					throw new IllegalStateException("Header " +  entry.getKey() + " cannot have null or empty string as value!");
				}
				headerArray[i++] = new BasicHeader(entry.getKey(), entry.getValue());
			}
		}
		Response response = restClient.performRequest(method, path, queryParams, entity, headerArray);
		StatusLine sl = response.getStatusLine();
		httpStatusCode = sl.getStatusCode();
		if (httpStatusCode == 200) {
			return EntityUtils.toString(response.getEntity(), "UTF-8");
		} else {
			errorMessage = sl.getReasonPhrase() + ": " + EntityUtils.toString(response.getEntity(), "UTF-8");
			return errorMessage;
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

}
