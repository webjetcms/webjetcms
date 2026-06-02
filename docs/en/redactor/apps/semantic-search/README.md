# Semantic Search (RAG)

Semantic search allows visitors to find relevant pages based on the **meaning of the query**, not just keyword matches. It uses technology built on the [pgvector](https://github.com/pgvector/pgvector) vector database and vectors generated via the OpenAI API.

## Setting up semantic search

To run a semantic search, you need:

- Enable semantic search by setting the configuration variable `ragSemanticSearchEnabled` to the value `true`.
- Set the configuration variable `searchType` to the value `semantic` to select this type of search.
- Verify that the configuration variable `luceneAsDefaultSearch` is set to the value `false`. If it is set to `true`, Lucene will be used instead regardless of the value of the variable `searchType`, as it has higher priority.
- Run indexing via the administrator interface to create vectors and populate the vector database.

## Semantic index

To use semantic search, you need to have your content indexed using semantic indexing, which is available in the admin interface. For more information, see [Semantic Index](./embedding-chunks.md).

## Implementation and setup details

A technical description of the indexing process and how to set it up initially can be found in the [developer documentation](../../../custom-apps/apps/rag/semantic-search/README.md).