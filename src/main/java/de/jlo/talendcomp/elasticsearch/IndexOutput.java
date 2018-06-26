package de.jlo.talendcomp.elasticsearch;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;

public class IndexOutput {
	
	private static final Logger LOG = Logger.getLogger(IndexOutput.class);
	private ElasticClient elasticClient = null;
	private BulkRequest bulkRequest = null;
	private int currentRowNum = 0;
	private int countUpserted = 0;
	private int countDeleted = 0;
	private int batchSize = 1000;
	private XContentBuilder insertContentBuilder = null;
	private XContentBuilder updateContentBuilder = null;
	private String index = null;
	private String objectType = null;
	private List<IndexError> listErrors = new ArrayList<IndexError>();
	
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
		listErrors = new ArrayList<IndexError>();
	}
	
	public void addDocumentForDelete(Object key) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("addDocumentForDelete key: " + key);
		}
		if (key == null) {
			throw new Exception("Add document for delete failed: Key cannot be null");
		}
		String id = String.valueOf(key);
		DeleteRequest deleteRequest = new DeleteRequest(index, objectType, id);
		if (bulkRequest == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Setup new bulk request");
			}
			bulkRequest = new BulkRequest();
		}
		bulkRequest.add(deleteRequest);
		currentRowNum++;
		countDeleted++;
	}
	
	/**
	 * adds a json document
	 * @param json the document to index
	 * @param key the key of the document
	 */
	public void addDocumentForUpsert(Object key, Object json) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("addDocumentForUpsert key: " + key + " json: " + json);
		}
		if (key == null) {
			throw new Exception("Add document for upsert failed: Key cannot be null");
		}
		if (json == null) {
			throw new Exception("Add document for upsert failed: Json content cannot be null");
		}
		insertContentBuilder = XContentFactory.jsonBuilder();
		updateContentBuilder = XContentFactory.jsonBuilder();
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
			if (LOG.isDebugEnabled()) {
				LOG.debug("Setup new bulk request");
			}
			bulkRequest = new BulkRequest();
		}
		bulkRequest.add(updateRequest);
		currentRowNum++;
		countUpserted++;
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
	
	public void executeBulk() throws Exception {
		executeBulk(false);
	}
	
	public void executeBulkFinal() throws Exception {
		executeBulk(true);
	}
	
	public int getCountErrors() {
		return listErrors.size();
	}
	
	public IndexError getIndexError(int i) {
		return listErrors.get(i);
	}
	
	public void clearIndexErrors() {
		listErrors.clear();
	}
	
	/**
	 * Adds the content and key (given by the setValue method) to the bulk request and if the 
	 * number of batches is reached, the bulk request will be executed
	 * @param finalRequest true = run the request with the last values at the end of the flow
	 * @throws Exception
	 */
	public void executeBulk(boolean finalRequest) throws Exception {
		if (bulkRequest != null && (finalRequest || (currentRowNum % batchSize == 0))) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Send bulk " + (finalRequest ? "final " : "") + "request at current row num: " + currentRowNum + ", number actions: " + bulkRequest.numberOfActions());
			}
			BulkResponse bulkResponse = elasticClient.executeBulk(bulkRequest);
			if (bulkResponse.hasFailures()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Bulk request failed:\n");
				BulkItemResponse[] birs = bulkResponse.getItems();
				for (BulkItemResponse br : birs) {
					if (br.isFailed()) {
						IndexError ie = new IndexError();
						ie.setOperation(br.getOpType().getLowercase());
						ie.setId(br.getId());
						ie.setMessage(br.getFailureMessage());
						sb.append(ie.getOperation());
						sb.append("> Id: ");
						sb.append(ie.getId());
						sb.append(" failed: ");
						sb.append(ie.getMessage());
						sb.append("\n");
						listErrors.add(ie);
					}
				}
				String message = "Bulk request for index: " + index + " type: " + objectType + " failed: " + sb.toString();
				LOG.error(message);
				throw new Exception(message);
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
	
	public int getCountProcessed() {
		return currentRowNum;
	}

	public int getCountIndexed() {
		return countUpserted;
	}

	public int getCountDeleted() {
		return countDeleted;
	}
	
}
