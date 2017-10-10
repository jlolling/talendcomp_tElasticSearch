package de.jlo.talendcomp.elasticsearch;

import org.elasticsearch.client.transport.TransportClient;
import org.junit.Before;
import org.junit.Test;

public class TestIndexOutput {

	private TransportClient transportClient = null;
	
	@Before
	public void setupTransportClient() throws Exception {
		Base client = new Base();
		client.setNodes("camundadev02.gvl.local:9200");
		client.initialize();
	}

	@Test
	public void testUpsert() {
		
	}

}
