# Customizing apps

If you need to modify an existing application (e.g. a banner), never do it directly in the original JSP file, as your changes may be overwritten when updated.

To modify the file `/components/search/search.jsp`, first make a copy of the file named `/components/INSTALL_NAME/search/search.jsp`.

The value `INSTALL_NAME` is the installation name, in the configuration in the item named `installName`. You can make modifications in the file created in this way without worrying about overwriting the file during the update. The page can remain `!INCLUDE(...)!` of the original application, WebJET will perform automatic path replacement:

- `/components/INSTALL_NAME/search/search.jsp`
- `/components/search/search.jsp`

The search for the correct file will be done automatically. The first one found in the specified order will be used.

It works similarly for Spring applications in the `/apps/INSTALL_NAME/` folder, e.g. copy `/apps/file-archive/mvc/file-archive.html` to `/apps/INSTALL_NAME/file-archive/mvc/file-archive.html` and modify according to your needs.