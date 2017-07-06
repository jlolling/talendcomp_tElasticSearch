package de.jlo.talendcomp.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.util.Date;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestBulk extends Client {

  @Before
  public void init() throws Exception {
    setHosts("localhost");
    initialize();
  }

  @Test
  public void testBulkImport() throws Exception {

    BulkRequestBuilder bulkRequest = getClient().prepareBulk();

    // either use client#prepare, or use Requests# to directly build index/delete requests
    bulkRequest.add(
        getClient().prepareIndex("twitter", "tweet", "1").setSource(jsonBuilder().startObject().field("user", "kimchy")
            .field("postDate", new Date()).field("message", "trying out Elasticsearch").endObject()));

    bulkRequest.add(getClient().prepareIndex("twitter", "tweet", "2").setSource(jsonBuilder().startObject()
        .field("user", "kimchy").field("postDate", new Date()).field("message", "another post").endObject()));

    BulkResponse bulkResponse = bulkRequest.get();
    if (bulkResponse.hasFailures()) {
      // process failures by iterating through each bulk response item
      Assert.fail("Failures in response.");
    }
  }

  @Test
  public void testUpsert() throws Exception {

    IndexRequest indexRequest = new IndexRequest("index", "type", "1")
        .source(jsonBuilder().startObject().field("name", "Joe Smith").field("gender", "male").endObject());
    UpdateRequest updateRequest = new UpdateRequest("index", "type", "1")
        .doc(jsonBuilder().startObject().field("gender", "male").endObject()).upsert(indexRequest);
    UpdateResponse updateResponse = getClient().update(updateRequest).get();
    Assert.assertNotNull(updateResponse);
  }

  @Test
  public void testUpsertBulk() throws Exception {

    BulkRequestBuilder bulkRequest = getClient().prepareBulk();

    IndexRequest indexRequest = new IndexRequest("index", "type", "1")
        .source(jsonBuilder().startObject().field("name", "Joe Smith").field("gender", "male").endObject());
    UpdateRequest updateRequest = new UpdateRequest("index", "type", "1")
        .doc(jsonBuilder().startObject().field("gender", "male").endObject()).upsert(indexRequest);

    bulkRequest.add(updateRequest);

    BulkResponse bulkResponse = bulkRequest.execute().actionGet(10000l);
    if (bulkResponse.hasFailures()) {
      Assert.fail("Failures in response.");
    }
  }
}
