# Customising applications

If you need to modify an existing application (e.g. a banner), never do it directly in the original JSP file, as your changes may be overwritten when you update.

To modify a file `/components/search/search.jsp`, first make a copy of the file named `/components/INSTALL_NAME/search/search.jsp`.

Value `INSTALL_NAME` is the name of the installation, in the configuration in the item named `installName`. You can make modifications to the file without worrying about overwriting the file when updating. In the page can remain `!INCLUDE(...)!` of the original application, WebJET performs an automatic path replacement:
- `/components/INSTALL_NAME/search/search.jsp`
- `/components/search/search.jsp`

The search for the correct file will be done automatically. The first one found according to the order given will be used.

Similarly for Spring applications in the folder `/apps/INSTALL_NAME/`, e.g. `/apps/file-archive/mvc/file-archive.html` copy to `/apps/INSTALL_NAME/file-archive/mvc/file-archive.html` and adjust to your needs.
