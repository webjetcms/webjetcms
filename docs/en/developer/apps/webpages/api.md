# Websites - API

Saving and editing web pages is a complex process. The classes in the ```sk.iway.iwcm.editor``` package are divided as follows:

- [facade.EditorFacade](../../../../javadoc/sk/iway/iwcm/editor/facade/EditorFacade.html) - the main class covering the complexity of loading and saving a web page. By default, there is no reason to use anything other than this class for work.
- [rest.WebpagesRestController](../../../../javadoc/sk/iway/iwcm/editor/rest/WebpagesRestController.html) - rest interface for loading the web pages table and editing it.
- [service.ApproveService](../../../../javadoc/sk/iway/iwcm/editor/service/ApproveService.html) - services related to website approval.
- [service.EditorService](../../../../javadoc/sk/iway/iwcm/editor/service/EditorService.html) - service for storing web pages (table ```documents``` and ```documents_history```).
- [service.MediaService](../../../../javadoc/sk/iway/iwcm/editor/service/MediaService.html) - service for storing media of a newly created page. Since the page does not yet have an assigned ```doc_id``` value, the media is stored in the database with the value ```-user_id```.
- [service.MultigroupService](../../../../javadoc/sk/iway/iwcm/editor/service/MultigroupService.html) - service for web pages stored in multiple directories.
- [service.WebpagesService](../../../../javadoc/sk/iway/iwcm/editor/service/WebpagesService.html) - service for the list of pages in the data table.
- [util.EditorUtils](../../../../javadoc/sk/iway/iwcm/editor/util/EditorUtils.html) - additional methods for the editor.

If you need to edit the page in your code, use only the ```EditorFacade``` class to get an existing/new ```DocDetails``` object by calling ```getDocForEditor(int docId, int historyId, int groupId)```, or to save the changes by calling ```save(DocDetails entity)```.

To quickly create a new page, you can use the ```EditorFacade.createEmptyWebPage(GroupDetails group, String title, boolean available)``` method. If the ```title``` value is empty, the directory name will be used as the page name. The ```available``` attribute determines whether the page should be immediately available for viewing after creation or not.