# Scheduled publishing with protected files

You can also prepare a future version of the page with images and documents that must not be available to visitors until the publication date. The live content and the regular working version remain separate. The feature must be enabled by the administrator with the configuration variable `plannedPublishingEnabled=true` ; it is disabled by default.

<div class="video-container">
    <iframe width="790" height="444" src="https://www.youtube.com/embed/z3QGUdDG2pk" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Site preparation

1. Open an existing page or create a new one and fill in its content.
2. In the **Perex** tab, set the **Start date** and enable **Schedule page change after this date**. For a new page, confirm **Save and continue**. WebJET saves the in-progress content with the **View** option disabled, closes the editor, and reopens it in the Perex tab with scheduling enabled and the date preserved. In the event of a save error, the original editor remains open. WebJET copies the media of the saved page to the scheduled set once.
3. Upload new materials in the file selection to **Media of this page → Images - planned** or **Files - planned**. These folders are also available when selecting a perex image and links in the **Media** tab.
4. Edit the content and scheduled media. Click **Schedule**.
5. For further editing, use **Open Scheduled Version** in the Perex tab or open the relevant version via History. Reopening no longer copies live media.

![](working-in-editor/image_dialog.png)

The first time you save a new page, it does not confirm the publishing date. If you close the editor without clicking **Schedule**, the page will remain saved as unpublished. Canceling the initial confirmation will not save the page. With `plannedPublishingEnabled` disabled, the original procedure remains: you can schedule a new page directly, but its files and images are not saved in the scheduled folders.

One schedule is supported per page. Changing the date will replace the original schedule. The scheduled version will replace the content and the entire media set, including their order and groups, when published. Therefore, you must manually transfer newer revisions to the live page to the staging version. If you remove all scheduled media, the live set will be empty after publishing.

Page attributes and changes to its assignment to other folders (multi-categories) are not scheduled. As with the original schedule, they are saved immediately when the scheduled version is saved.

Media changes are saved continuously. Closing the editor via **Cancel** does not revert them. Unchecking the timing does not delete the prepared data. The **Cancel scheduled changes** button cancels the scheduling, removes the scheduled media, and deletes all images and files from the scheduled folders, including subfolders. It does not change the HTML of the live page or history records. Therefore, links to deleted documents in the canceled history will no longer work.

When saving a page or media without scheduled publishing, you cannot reference files or images from scheduled changes. If you see this warning, turn on scheduled publishing for the page or select the file or image from a common folder. Scheduled files are checked for availability, permissions on their destination folders, and conflicts with other schedules when the schedule is saved and before publishing, not every time the media is edited.

The plan cannot be saved if the content or media references a plan folder on another page or uses a plan path notation that cannot be overwritten when publishing. Select the file or image again from the plan folders on this page.

## Files and links

Example for a page with ID `42`:

```text
/files/protected/planned-changes/42/images/apple/foto.webp
→ /images/apple/foto.webp

/files/protected/planned-changes/42/files/apple/parametre.pdf
→ /files/apple/parametre.pdf
```

The destination is stored in the file path, so renaming the page later will not change it. **All files in the Images - Scheduled and Files - Scheduled folders, including subfolders, are published**, even if the page or media does not reference them. After the page, media, and history are successfully saved, these working folders are cleaned up. This means that the next plan starts with empty folders.

Content, annotation, perex image, `htmlHead`, optional fields and links, previews or media descriptions are supported. When publishing, each occurrence of the prefix `/files/protected/planned-changes/<docId>/` on the given page is replaced with `/`. The replacement also works in absolute URLs, `/thumb/`, CSS and `!INCLUDE(...)!` parameters without distinguishing domain or HTML attributes. Existing parameters including `v` and fragments are preserved unchanged; new parameters are not added. The same transcription is also saved in the specific published history record. Other history records are not changed; their original links to the cleaned scheduled folders may stop working.

The **Image Properties** dialog, when selecting an image, sets the parameter `v` to the time the file was last modified. When replacing an image, select it again in the dialog to get the new value into the HTML. When `elfinderUseFastLoading` is enabled, the modification time is not read and the value is `0` ; in that case, use the new file name.

For perex images, use **new file name** when replacing. Their URLs usually do not contain the `v` parameter, so if the name is kept the same, the browser or CDN may display the old image from the cache even after publishing. The same restriction applies to other links without the changed `v` parameter. When overwriting a file, WebJET will clear its own file and thumbnail cache, but it will not clear the browser or CDN cache.

Unrecognized use of a protected URL will be rejected on save or publish. External CSS/JS, iframes and their dependencies are not traversed. Product browser and e-shop need their own publish process.

Overwriting an existing file will affect all pages using the same path. When saving and publishing, a check is made to see if another plan is already using the same target file. This check does not lock the database, so it does not guarantee that a collision will be detected when saving two plans at the same time. A file that has already been publicly uploaded cannot be unhidden using this procedure. Unpublishing a page does not automatically hide previously published files either.

## Deployment and operation

- Before enabling the feature for the first time, check existing timed versions in the page history. Leave no more than one pending scheduled version for each page; cancel the other schedules. Enabling the feature itself will not cancel older schedules. If more than one remains, the older version may be published after the newer one and overwrite its contents.
- Set the configuration variable `plannedPublishingEnabled` to `true` when you deploy the feature and do not change the value while it is in use. Disabling and re-enabling the feature with existing plans is not a supported operating procedure.

## Publishing errors

Before the first exchange, the files found in the planned folders, their targets, and the author's current write permissions to the target folders are checked. The author's right to edit the page is not re-verified during automatic publishing. The list to copy is created solely from the contents of the folders; the existence of the files listed in the links is not checked. Individual files are replaced atomically, so the visitor does not receive a partially written file. The original files are stored in a protected subfolder `<docId>/backup/<historyId>/` ; the administrator can restore them via the file system. The existing file history is also used, if enabled.

Both scheduled and live versions are published using a common process in `DocPublishService`. The scheduled files are published first and their paths are overwritten, then the page content, media, multicategories, and comments are saved. The history is marked as published only after successful writes. Writes are not done in a single, shared transaction. Therefore, if an error occurs, the files or content may already be new, and the live media set may be temporarily empty or incomplete.

If a multicategory or editor notes save fails, the history remains pending and the working files are not purged. The next publishing check will retry the save. The page content may already be published at that time.

The scheduled media, including the original protected links, are retained until all live copies are saved. If the swap fails, the next attempt deletes the incomplete live set and recreates it from the complete plan. After the entire set is saved, `planned_media_initialized` is reset, the scheduled media records are removed, and only then is the history marked as published. If this final step fails, the next attempt leaves the completed live set and completes the cleanup and history. The working files are deleted only upon successful completion; backups of the original public files in `backup/<historyId>/` remain on disk. If the folder cleanup itself fails, the publishing remains complete and an error is logged. The remaining working files can be deleted using the **Cancel Scheduled Changes** button.

You can also repeat the initialization: if the flag is not already set, any incomplete scheduled copies are first removed and the live media is copied again. When changing the schedule, the original scheduling is only canceled after the new version is saved to the history. The publishing error is logged along with the history ID.

Before production use, perform an acceptance test on the upcoming Apple WebJET site, including a production proxy/CDN and limited editor permissions.
