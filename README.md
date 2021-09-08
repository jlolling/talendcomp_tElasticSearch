# Talend components to work with ElasticSearch.
## tElasticSearchIndexOutput: Feed an index with json documents.
The incoming schema expects some fixed columns (you will get them from the component schema):
key - the id of the object
json - the actual document to index
delete - if true the document with the given key will be deleted in the index (json column can be null in this case)
![Example Job](https://github.com/jlolling/talendcomp_tElasticSearch/blob/master/doc/tElasticSearchIndexOutput_demo_job_basic_settings.png)
## tElasticSearchIndexErrors: Retrieve the errors from the last index action
This component provides the error messages from the last indexing action.
![Example Job](https://github.com/jlolling/talendcomp_tElasticSearch/blob/master/doc/tElasticSearchIndexOutput_demo_job_design_with_error_return.png)

