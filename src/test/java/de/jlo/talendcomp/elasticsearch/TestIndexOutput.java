package de.jlo.talendcomp.elasticsearch;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class TestIndexOutput {

	private Map<String, Object> globalMap = new HashMap<String, Object>();
	private final java.util.Map<String, Long> start_Hash = new java.util.HashMap<String, Long>();
	private final java.util.Map<String, Long> end_Hash = new java.util.HashMap<String, Long>();
	private final java.util.Map<String, Boolean> ok_Hash = new java.util.HashMap<String, Boolean>();
	private String currentComponent = null;
	
	@Before
	public void setupClient() throws Exception {
		Logger root = Logger.getRootLogger();
		root.setLevel(Level.DEBUG);
		BasicConfigurator.configure();
	}

	@Test
	public void testUpsert() throws Exception {
		int batchSize = 10;
		int maxRecords = 33;
		int tos_count_tElasticSearchIndexOutput_1 = 0;
		de.jlo.talendcomp.elasticsearch.ElasticClient client_tElasticSearchIndexOutput_1 = new de.jlo.talendcomp.elasticsearch.ElasticClient();
		try {
			client_tElasticSearchIndexOutput_1.setNodes(
					"searchdev01.gvl.local:9200", null);
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
			Object json = "{\"number\":" + key + ",\"name\":\"Hain" + i + "\"}"; 
			currentComponent = "tElasticSearchIndexOutput_1";

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

			tos_count_tElasticSearchIndexOutput_1++;
		}

		currentComponent = "tElasticSearchIndexOutput_1";

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

		ok_Hash.put("tElasticSearchIndexOutput_1", true);
		end_Hash.put("tElasticSearchIndexOutput_1",
				System.currentTimeMillis());
		assertEquals(expected, actual);
		
	}

}
