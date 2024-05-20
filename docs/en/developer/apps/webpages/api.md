# Website - API

Storing and editing web pages is a complex process. Classes are in the package `sk.iway.iwcm.editor` divided as follows:
- [facade.EditorFacade](../../../../javadoc/sk/iway/iwcm/editor/facade/EditorFacade.html) - main class covering the complexity of loading and saving a web page. By default, there is no reason to use a class other than this one to do the job.
- [rest.WebpagesRestController](../../../../javadoc/sk/iway/iwcm/editor/rest/WebpagesRestController.html) - rest interface for loading the web page table and editing it.
- [service.ApproveService](../../../../javadoc/sk/iway/iwcm/editor/service/ApproveService.html) - website approval services.
- [service.EditorService](../../../../javadoc/sk/iway/iwcm/editor/service/EditorService.html) - service for storing web pages (table `documents` a `documents_history`).
- [service.MediaService](../../../../javadoc/sk/iway/iwcm/editor/service/MediaService.html) - a service for storing the media of a newly created page. Since the page does not yet have an associated `doc_id` the value is stored in the database with the value `-user_id`.
- [service.MultigroupService](../../../../javadoc/sk/iway/iwcm/editor/service/MultigroupService.html) - service for web pages stored in multiple directories.
- [service.WebpagesService](../../../../javadoc/sk/iway/iwcm/editor/service/WebpagesService.html) - service for the list of pages in the datatable.
- [util.EditorUtils](../../../../javadoc/sk/iway/iwcm/editor/util/EditorUtils.html) - additional methods for the editor.
If you need to edit a page in your code, use the class exclusively `EditorFacade` for the acquisition of an existing/new `DocDetails` of the object by calling `getDocForEditor(int docId, int historyId, int groupId)`, or to save changes by calling `save(DocDetails entity)`.

To quickly create a new page, you can use the method `EditorFacade.createEmptyWebPage(GroupDetails group, String title, boolean available)`. If the value is `title` empty the directory name is used as the page name. Attribute `available` determines whether or not the page should be immediately available for viewing after it is created.
