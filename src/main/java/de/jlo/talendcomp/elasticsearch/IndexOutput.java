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
	private XContentBuilder builderIndex = null;
	private XContentBuilder builderUpdate = null;
	private String index = null;
	private String type = null;
	
	public void initialize() throws Exception {
		bulkRequest = base.getTransportClient().prepareBulk();
		builderIndex = XContentFactory.jsonBuilder();
	}
	
	public void setValue(String field, Object value, boolean iskey) {
		OneValue v = new OneValue();
		v.setField(field);
		v.setValue(value);
		v.setKey(iskey);
		currentRow.add(v);
	}
	
	public void upsert() throws Exception {
		builderIndex.startObject();
		String key = null;
		for (OneValue value : currentRow) {
			builderIndex.field(value.getField(), value.getValue());
			if (value.isKey()) {
				key = String.valueOf(value.getValue());
			}
		}
		builderIndex.endObject();
		if (key == null) {
			throw new IllegalStateException("No value has been set as key. Upsert expects one field set as key field.");
		}
		IndexRequest indexRequest = new IndexRequest(index, type, key)
				.source(builderIndex);
		builderUpdate.startObject();
		for (OneValue value : currentRow) {
			builderUpdate.field(value.getField(), value.getValue());
		}
		builderUpdate.endObject();
		UpdateRequest updateRequest = new UpdateRequest(index, type, key)
				.doc(builderUpdate)
				.upsert(indexRequest);
		bulkRequest.add(updateRequest);
		currentRowNum++;
		currentRow.clear();
		if (currentRowNum % batchSize == 0) {
			bulkRequest.execute(new ActionListener<BulkResponse>() {
				
				@Override
				public void onResponse(BulkResponse response) {
					// TODO Auto-generated method stub

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if (type == null || type.trim().isEmpty()) {
			throw new IllegalArgumentException("type cannot be null or empty");
		}
		this.type = type.trim();
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
	
}
