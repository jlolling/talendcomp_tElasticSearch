package de.jlo.talendcomp.elasticsearch;

import java.nio.charset.Charset;

import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;

public class IndexOutput {
	
	private ElasticClient elasticClient = null;
	private BulkRequest bulkRequest = null;
	private int currentRowNum = 0;
	private int batchSize = 1000;
	private XContentBuilder insertContentBuilder = null;
	private XContentBuilder updateContentBuilder = null;
	private String index = null;
	private String objectType = null;
	
	public IndexOutput(ElasticClient client) {
		if (client == null) {
			throw new IllegalArgumentException("Client cannot be null");
		}
		this.elasticClient = client;
	}
	
	public void initialize() throws Exception {
		insertContentBuilder = XContentFactory.jsonBuilder();
		updateContentBuilder = XContentFactory.jsonBuilder();
		bulkRequest = null;
	}
	
	/**
	 * adds a json document
	 * @param json the document to index
	 * @param key the key of the document
	 */
	public void addDocument(Object key, Object json) throws Exception {
		if (key == null) {
			throw new Exception("Add json failed: Key cannot be null");
		}
		String id = String.valueOf(key);
		BytesReference br = createBytesReferences(json);
		insertContentBuilder.rawValue(br, XContentType.JSON);
		IndexRequest indexRequest = new IndexRequest(index, objectType, id)
				.source(insertContentBuilder);
		updateContentBuilder.rawValue(br, XContentType.JSON);
		UpdateRequest updateRequest = new UpdateRequest(index, objectType, id)
				.doc(updateContentBuilder)
				.upsert(indexRequest);
		if (bulkRequest == null) {
			bulkRequest = new BulkRequest();
		}
		bulkRequest.add(updateRequest);
		currentRowNum++;
	}
	
	private BytesReference createBytesReferences(Object value) {
		if (value instanceof String) {
			byte[] array = ((String) value).getBytes(Charset.forName("UTF-8"));
			return new BytesArray(array);
		} else if (value != null) {
			byte[] array = value.toString().getBytes(Charset.forName("UTF-8"));
			return new BytesArray(array);
		} else {
			return null;
		}
	}
	
	public void upsert() throws Exception {
		upsert(false);
	}
	
	public void finalUpsert() throws Exception {
		upsert(true);
	}
	
	/**
	 * Adds the content and key (given by the setValue method) to the bulk request and if the 
	 * number of batches is reached, the bulk request will be executed
	 * @param finalRequest true = run the request with the last values at the end of the flow
	 * @throws Exception
	 */
	public void upsert(boolean finalRequest) throws Exception {
		if (bulkRequest != null && (finalRequest || (currentRowNum % batchSize == 0))) {
			BulkResponse bulkResponse = elasticClient.executeBulk(bulkRequest, new BasicHeader("Content-Type", "application/x-ndjson"));
			if (bulkResponse.hasFailures()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Upsert failed for:");
				BulkItemResponse[] birs = bulkResponse.getItems();
				for (BulkItemResponse br : birs) {
					if (br.getFailureMessage() != null) {
						sb.append("\nId: ");
						sb.append(br.getId());
						sb.append(" failed: ");
						sb.append(br.getFailureMessage());
					}
				}
				throw new Exception(sb.toString());
			}
			bulkRequest = null;
		}
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		if (index == null || index.trim().isEmpty()) {
			throw new IllegalArgumentException("index cannot be null or empty");
		}
		this.index = index.trim();
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		if (objectType == null || objectType.trim().isEmpty()) {
			throw new IllegalArgumentException("objectType cannot be null or empty");
		}
		this.objectType = objectType.trim();
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		if (batchSize != null) {
			if (batchSize == 0) {
				throw new IllegalArgumentException("batchSize cannot be 0");
			}
			this.batchSize = batchSize;
		}
	}
	
	public int getCountUpserts() {
		return currentRowNum;
	}
	
}
