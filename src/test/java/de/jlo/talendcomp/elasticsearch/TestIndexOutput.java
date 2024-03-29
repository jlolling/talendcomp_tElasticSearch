package de.jlo.talendcomp.elasticsearch;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestIndexOutput {

	private Map<String, Object> globalMap = new HashMap<String, Object>();
	
	@Test
	public void testUpsert() throws Exception {
		int batchSize = 10;
		int maxRecords = 100;
		de.jlo.talendcomp.elasticsearch.ElasticClient client_tElasticSearchIndexOutput_1 = new de.jlo.talendcomp.elasticsearch.ElasticClient();
		try {
			client_tElasticSearchIndexOutput_1.setNodes("localhost:9200", null);
			client_tElasticSearchIndexOutput_1.setupClient();
		} catch (Exception e) {
			String message = "Setup ElasticSearch client failed: "
					+ e.getMessage();
			globalMap.put("tElasticSearchIndexOutput_1_ERROR_MESSAGE",
					message);
			throw e;
		}
		de.jlo.talendcomp.elasticsearch.IndexOutput tElasticSearchIndexOutput_1 = new de.jlo.talendcomp.elasticsearch.IndexOutput(
				client_tElasticSearchIndexOutput_1);
		globalMap.put("tElasticSearchIndexOutput_1",
				tElasticSearchIndexOutput_1);
		try {
			tElasticSearchIndexOutput_1.setIndex("test");
			tElasticSearchIndexOutput_1.setObjectType("test_type");
			tElasticSearchIndexOutput_1.setBatchSize(batchSize);
			tElasticSearchIndexOutput_1.initialize();
		} catch (Exception e) {
			String message = "Initialize Requests failed: "
					+ e.getMessage();
			globalMap.put("tElasticSearchIndexOutput_1_ERROR_MESSAGE",
					message);
			throw e;
		}

		for (int i = 0; i < maxRecords; i++) {
			Integer key = i;
			Object json = "{\"number\":" + key + ",\"name\":\"Jan" + i + "\"}"; 

			try {
				tElasticSearchIndexOutput_1.addDocumentForUpsert(
						key, json);
			} catch (Exception tElasticSearchIndexOutput_1_addex) {
				String message = "Add document failed: "
						+ tElasticSearchIndexOutput_1_addex
								.getMessage();
				globalMap
						.put("tElasticSearchIndexOutput_1_ERROR_MESSAGE",
								message);
				throw tElasticSearchIndexOutput_1_addex;
			}
			try {
				tElasticSearchIndexOutput_1.executeBulk();
			} catch (Exception tElasticSearchIndexOutput_1_upex) {
				String message = "Upsert request failed: "
						+ tElasticSearchIndexOutput_1_upex
								.getMessage();
				globalMap
						.put("tElasticSearchIndexOutput_1_ERROR_MESSAGE",
								message);
				throw tElasticSearchIndexOutput_1_upex;
			}


		}


		try {
			tElasticSearchIndexOutput_1.executeBulkFinal();
		} catch (Exception tElasticSearchIndexOutput_1_upex) {
			String message = "Final upsert request failed: "
					+ tElasticSearchIndexOutput_1_upex.getMessage();
			globalMap.put("tElasticSearchIndexOutput_1_ERROR_MESSAGE",
					message);
			throw tElasticSearchIndexOutput_1_upex;
		}
		client_tElasticSearchIndexOutput_1.close();
		int actual = tElasticSearchIndexOutput_1.getCountIndexed();
		int expected = maxRecords;
		globalMap.put("tElasticSearchIndexOutput_1_NB_LINE",
				tElasticSearchIndexOutput_1.getCountIndexed());
		assertEquals(expected, actual);
		
	}

}
