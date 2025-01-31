# Search

Offer your visitors the possibility of fast and accurate search directly on your site. Include a search form and search results display that allows you to set the directory, number of entries per page, and how they are arranged. Leverage the power of database search or use Lucene/Elastic Search to search with inflection as well. Text file searches are also supported `doc(x), xls(x), ppt(x), pdf, xml a txt`.

## Application settings

In the settings you can set:
- Directory - folder IDs of web pages for searching, also searches in subfolders
- Number of links per page - number of records per search page
- Duplication check - if the web page is in multiple folders, duplication checking is enabled. Increases the load on the server.
- Organize by - Priority, Title, Change Date
- Insert - form, results, total - sets the type of the inserted part, if you want to have separate search fields, e.g. in the header insert the form and the search results separately. When setting the value Form, you need to specify the ID of the search results page.

![](editor.png)

### Setting up a file search

If you also want to search in files, you need to [set up file indexing](../../files/fbrowser/folder-settings/README.md#Indexing) in the File Explorer and on the file folder and start the initial indexing.

### Setting up the use of Lucene

By default, a database server search is used. It is possible to activate the search using the library [Lucene](https://lucene.apache.org/) which is also used in `Elastic Search` as a search engine. Set conf. variable `luceneAsDefaultSearch` to the value of `true` and start the initial indexing via `/components/search/lucene_console.jsp`.

## View application

![](search.png)
