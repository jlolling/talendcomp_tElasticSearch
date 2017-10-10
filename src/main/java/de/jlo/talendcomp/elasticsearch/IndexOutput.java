package de.jlo.talendcomp.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class IndexOutput {
	
	private Base base = null;
	private BulkRequestBuilder bulkRequest = null;
	private int currentRowNum = 0;
	private int batchSize = 1000;
	private List<OneValue> currentRow = new ArrayList<OneValue>();
	private XContentBuilder insertContentBuilder = null;
	private XContentBuilder updateContentBuilder = null;
	private String index = null;
	private String objectType = null;
	
	public IndexOutput(Base base) {
		if (base == null) {
			throw new IllegalArgumentException("Base client cannot be null");
		}
		this.base = base;
	}
	
	public void initialize() throws Exception {
		bulkRequest = base.getTransportClient().prepareBulk();
		insertContentBuilder = XContentFactory.jsonBuilder();
	}
	
	public void setValue(String field, Object value, boolean iskey) {
		OneValue v = new OneValue();
		v.setField(field);
		v.setValue(value);
		v.setKey(iskey);
		currentRow.add(v);
	}
	
	public void upsert() throws Exception {
		insertContentBuilder.startObject();
		String key = null;
		for (OneValue value : currentRow) {
			insertContentBuilder.field(value.getField(), value.getValue());
			if (value.isKey()) {
				key = String.valueOf(value.getValue());
			}
		}
		insertContentBuilder.endObject();
		if (key == null) {
			throw new IllegalStateException("No value has been set as key. Upsert expects one field set as key field.");
		}
		IndexRequest indexRequest = new IndexRequest(index, objectType, key)
				.source(insertContentBuilder);
		updateContentBuilder.startObject();
		for (OneValue value : currentRow) {
			updateContentBuilder.field(value.getField(), value.getValue());
		}
		updateContentBuilder.endObject();
		UpdateRequest updateRequest = new UpdateRequest(index, objectType, key)
				.doc(updateContentBuilder)
				.upsert(indexRequest);
		bulkRequest.add(updateRequest);
		currentRowNum++;
		currentRow.clear();
		if (currentRowNum % batchSize == 0) {
			bulkRequest.execute(new ActionListener<BulkResponse>() {
				
				@Override
				public void onResponse(BulkResponse response) {
					
				}
				
				@Override
				public void onFailure(Exception e) {
					// TODO Auto-generated method stub
					
				}
			});
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
	
}
