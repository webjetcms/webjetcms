# Search

Offer visitors the ability to search quickly and accurately right on your site. Insert a search form and search results display that allows you to set the directory, number of records per page, and how to organize them. Harness the power of database search or use Lucene/Elastic Search for inflected searches. Text searches of `doc(x), xls(x), ppt(x), pdf, xml a txt` files are also supported.

## Application settings

In the settings you can set:

- Directory - ID of website folders for searching, also searches in subfolders
- Number of links per page - number of entries per search page
- Check for duplicates - if a website is located in multiple folders, duplicate checking is enabled. This increases the load on the server.
- Sort by - Priority, Name, Date Modified
- Insert - form, results, together - sets the type of inserted part, if you want to have a separate search field, e.g. in the header, insert the form separately and the search results separately. When setting the value Form, you need to enter the ID of the page with the search results.

![](editor.png)

### Setting up file searches

If you want to search in files as well, you need to [set up file indexing](../../files/fbrowser/folder-settings/README.md#indexing) in the Explorer section and on the given file folder and run initial indexing.

### Setting up Lucene usage

By default, search is performed using a database server. It is possible to enable search using the [Lucene](https://lucene.apache.org/) library, which is also used in ```Elastic Search``` as a search system. Set the config variable `luceneAsDefaultSearch` to the value `true` and start the initial indexing via `/components/search/lucene_console.jsp`.

## View the application

![](search.png)
