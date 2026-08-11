# Database cleaning

The **Database Cleanup** application allows you to remove unnecessary data from your database, which can improve server performance and free up disk space.
You can find this tool in the **Settings** section under **Database Cleanup**.

## Cache objects

Displays a list of objects stored in the application cache and allows them to be deleted individually, which can reduce memory consumption or refresh data in the server cache. By clicking on the name, it is possible to view the content of the record for selected data types. The [Cache](../../../../src/main/java/sk/iway/iwcm/Cache.java) object is used for work

![](cache-objects.png)

## Database records

Deleting data from selected database tables, deletion is possible from the following groups:

- **Statistics**: Removes statistical data. Deleting older data can significantly improve server performance, but you will lose information about website traffic for the selected period.
- **Emails**: Allows you to delete emails sent from the Bulk Email application and emails sent with a time delay (or emails sent within a multi-node cluster).
- **Page History**: Deletes recorded historical versions of web pages, which are saved each time a web page is published. They are displayed in the History tab when editing a web page. Deleting does not affect the currently displayed pages, historical versions are deleted.
- **Server Monitoring**: Deletes recorded server monitoring data, such as performance metrics and logs.
- **Audit**: Deletes audit logs that monitor user activities and system events, only selected log types can be deleted.
- **Website Recycle Bin**: Allows you to permanently delete Websites in the Recycle Bin and Website Folders in the Recycle Bin. The deletion is permanent and the deleted items can no longer be restored from the Recycle Bin. Automatic deletion can also be set in the [GDPR / Data Deletion] application (../../redactor/apps/gdpr/data-deleting.md).
  - The **Websites in Trash** item deletes pages in the trash that were created in the selected period.
  - The **Website Folders in Trash** item deletes:
    - old folders in the trash that were created in the selected period, including subfolders and pages within them
    - folders in the trash that are empty, or those that remain empty after deleting old pages and folders in them

With each deletion, an optimization of the given database table is also performed to physically free up disk space and optimize the order of records in the database table.

![](database-delete.png)

## Persistent cache objects

Management and deletion of objects stored in a persistent cache that retains data even after a server restart (data is stored in the database). The [PersistentCacheDB](../../../../src/main/java/sk/iway/iwcm/system/cache/PersistentCacheDB.java) object is used for work. Only text data can be stored in this cache, typically the `downloadUrl(String url, int cacheInMinutes)` method is used, which downloads data from the specified URL in the background and updates it at a set time. The application uses this method and immediately retrieves data from the cache.

![](persistent-cache-objects.png)
