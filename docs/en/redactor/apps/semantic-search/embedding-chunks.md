# Semantic index

The semantic index converts page content into vector representations (`embedding`) using the OpenAI API and stores them in a vector database. It is used for semantic search, hybrid search, and for generating RAG answers in search.

For more accurate results, the content is divided into smaller parts - **chunks**. Each chunk is indexed separately, which allows the system to match queries to specific parts of the text rather than the entire page at once.

You can find vector management in the **Settings → Semantic Index** section.

Currently, **website** indexing is supported. Additional types may be added in the future.

!>**Warning:** Indexing **does not happen immediately**. Each request (add, modify, delete) is placed in a **queue** and processed at regular intervals using a cron job.

To view the list of indexed objects, you must have the Semantic Index right.

## Website indexing

The plain text of the page without HTML tags is indexed. Only web pages that are enabled for search are included in the indexing. The content is divided into chunks, which are shown in the table below.

Each chunk contains the following columns:

- **Entity ID** - Website ID.
- **Chapter index** - the order of the chunk within the page (0, 1, 2, ...).
- **Part text** - the text for which the embedding was generated. The embedding itself is not displayed in the table.
- **Model** - OpenAI model used, e.g. `text-embedding-3-small`.
- **Dimensions** - number of dimensions of the vector, e.g. `1536`.
- **Language** - language version of the page.
- **Status** - processing status:
  - **COMPLETED** - successfully processed.
  - **ERROR** - an error occurred.
  - **PENDING** - waiting for processing.
- **Error message** - error description if processing failed.
- **Creation Date** - processing time, not queue addition time.

Additionally, the database stores `group_id` and the columns `root_group_l1`, `root_group_l2`, `root_group_l3`. These values ​​are used to quickly limit semantic and hybrid searches to folders selected in the **Search** application.

![](datatable.png)

## Splitting text into chunks

The chunk size is set by configuration variables:

- `ragEmbeddingChunkSize` - ​​maximum size of one part of the text in characters, default `1000`.
- `ragEmbeddingChunkOverlap` - ​​number of characters of overlap between adjacent parts, default `200`.

When splitting text, the system tries to preserve the natural context. The end of the chunk is selected in the following order:

1. end of paragraph,
2. end of line,
3. end of sentence or similar punctuation,
4. space between words,
5. hard division by maximum size.

Overlay is used to preserve context between adjacent chunks. In a RAG response, adjacent chunks of a single page can be re-merged, removing duplicate text created by the overlay.

!>**Warning:** The older configuration variables `ragChunkSize` and `ragChunkOverlap` are no longer used. Please re-run indexing after changing chunk sizes or migrating from older settings.

## Filtering

The following filters are available in the table header:

- **Select folder** - displays chunks only for pages from a given folder within the current domain.
- **Show also from subfolders** - includes pages from subfolders in the results.

!>**Warning:** If you select **Root Folder** without enabling **Show also from subfolders**, you will not get any results. The root folder is virtual and does not contain pages directly.

## Redirect from Websites

In the **Websites** section, you can click the button next to the selected folder. <button class="btn btn-sm buttons-selected btn-outline-secondary"><span><i class="ti ti-database-search"></i></span></button> in the folder header. This will open the **Semantic Index** section with the filter automatically set for that folder.

### Automatic indexing

The system automatically queues a page when:

- **created or edited** - the page is indexed or updated without manual intervention,
- **deleted or moved to the trash** - all related chunks are removed from the database,
- **restore from trash** - the page is being re-indexed.

### Manual indexing

Click the button <button class="btn btn-sm btn-success" type="button"><span><i class="ti ti-database-plus"></i></span></button> to open the indexing dialog.

The dialog will display an overview of the pages in the selected folder - total number, number already indexed, and number in queue. The folder will be set according to the active filter. After confirmation, all searchable pages from the selected folder will be queued. If the chunk text has not changed, the system will try to use the existing embedding based on its hash value.

You start the action with the button <button class="btn btn-primary"><i class="ti ti-check"></i><span>Start the action</span></button> .

![](index-dialog.png)

### Manually remove indexing

Click the button <button class="btn btn-sm btn-danger" type="button"><span><i class="ti ti-database-minus"></i></span></button> to open the delete indexes dialog.

The dialog will display the same overview as for indexing. After confirmation, the pages will be queued for removal of all chunks for the pages of the selected folder.

You start the action with the button <button class="btn btn-primary"><i class="ti ti-check"></i><span>Start the action</span></button> .

![](remove-index-dialog.png)

## Indexing errors

If an error occurs while indexing a page, the system saves a record with the status **ERROR** and a shortened error message. The error is also written to the administrator log in the category **RAG**. If processing of an item fails at the queue level, the item remains in the queue and the system attempts to process it the next time the cron job runs.

## Implementation details

A technical description of the indexing process can be found in the [developer documentation](../../../custom-apps/apps/rag/semantic-search/README.md).
