# Using AI assistants

The logic for using AI assistants is consistent across the entire CMS to provide a consistent user experience regardless of context. In this chapter, you will find an overview of the assistant's behavior in data tables, the website editor, the PageBuilder, when marking up text, and the image editor.

## Text fields

The text field assistant (single-line field and `textarea`) is used to generate new content or edit existing content. It is launched via the button <button class="btn btn-outline-secondary btn-ai" type="button"><i class="ti ti-sparkles"></i></button> at the end of the field.

![](textarea-focus.png)

Clicking on it will open a window with all available assistants for the given field. They are clearly divided into groups (e.g. **Generation**, **Improvement**). Each one has a name, icon and provider.

![](textarea-assistants.png)

Clicking on the assistant will start its execution. If it has progressive loading enabled, the result will be displayed in parts; otherwise, it will only be displayed when it is complete.

![](textarea-waiting.png)

Upon successful completion, the resulting text will be automatically inserted into the field. A confirmation window will appear with information about token consumption. If the result is not satisfactory, use the button<button class="btn btn-outline-secondary btn-ai-undo" type="button"><i class="ti ti-arrow-back"></i>Cancel the change</button> to return to the original value.

![](textarea-done.png)

### User input

If the assistant requires additional instructions, after selecting it, an input form will first be displayed instead of launching immediately.

Assistants that require user input have an icon next to their name.<i class="ti ti-blockquote has-user-prompt"></i> for easy distinction.

![](textarea-prompt.png)

A field can have placeholder text (`placeholder`) with instructions on how to correctly enter the request. If the field is empty and you double-click on it (or press the `TAB` key), this placeholder text will be transferred as editable text to the field. You can then easily edit the necessary parts and use it as the basis for your request.

After entering the instructions, press **Generate**. The next process (loading, inserting the result, the option to return) is the same as with the assistant without user input.

![](textarea-prompt-loading.png)

### Quill

Since the field of type `Quill` is also a text field, AI assistants work for it in the same way as for classic text fields. The only difference is the location of the icon, which is not at the end of the field, but in the header between the tools.

![](quill.png)

## Image fields

The image field assistant is used to generate or edit an image. It is launched via the button <button class="btn btn-outline-secondary btn-ai" type="button"><i class="ti ti-sparkles"></i></button> at the end of the field.

![](image-focus.png)

After clicking, a window will open with all available assistants for the given field. They are clearly divided into groups (e.g. **Generate**, **Edit**). Each has a name, icon, and provider.

![](image-assistants.png)

Clicking on the wizard will start its execution. If you select a wizard that is supposed to edit an existing image (e.g. **Remove Background**), but the image field does not have an image set, an error message will be returned.

![](image-error-1.png)

If the field contains an image, processing will begin and a progress indication will be displayed.

![](image-loading.png)

Once completed, a confirmation, token consumption information, and a preview of the result will be displayed. Use the button<a target="_blank" class="zoom-in"><i class="ti ti-zoom-in"></i></a> you can open the image in full size in a new tab.

The image is not saved automatically. First, fill in or edit the **Image Name** and **Location** fields. The values ​​are filled in according to the original image, but you can change them. The **Location** field allows you to select a folder via a tree structure.

Press **Save image** to save.

![](image-done.png)

If you have not changed the name or location or the combination already exists, a dialog with options will appear:

- **Overwrite file** – the original file will be replaced with the new one
- **Rename file** – the system will offer a recommended (AI generated) new name
- **Cancel Save** – go back without saving

When you select **Overwrite file** or **Rename file**, the image in the field is updated immediately.

![](image-name-select.png)

### User input

If the assistant requires additional instructions, a form will appear. Additional fields are determined by the capabilities of the specific **provider, model, and operation**, so they may vary. For example, you may see:

- **Number of images**,
- **Dimension**,
- **Quality**,
- **Aspect ratio**.

The CMS will only display the fields and values ​​that the selected model supports. If the model does not publish any additional options for a given operation, the form will only contain a user instruction.

Assistants that require user input have an icon next to their name.<i class="ti ti-blockquote has-user-prompt"></i> .

![](image-prompt.png)

If the selected model supports the **Number of images** field, it can generate multiple results in a single request. In the example below, the number was set to 3.

You can only select and save one result - you select it by clicking on it. All previews can be opened via<a target="_blank" class="zoom-in"><i class="ti ti-zoom-in"></i></a> .

![](image-select.png)

### Content issues

The provider may reject the request if the instructions contain a protected or sensitive entity (a famous person, character, licensed brand, etc.). Specific rules vary by **provider**. In such a case, an error message `PROHIBITED_CONTENT` may be returned.

![](image-error-2.png)

## Website editor

Assistants also work in the website editor and are used to generate new content or edit existing page content. They are launched via the icon<button class="btn-ai" type="button"><i class="ti ti-sparkles"></i></button> located in the web editor toolbar.

![](ckeditor.png)

Clicking on it will open a window with all available assistants for the given field. They are clearly divided into groups (e.g. **Generation**, **Copywriting**, **Improvement**). Each one has a name, icon, and provider.

![](ckeditor-assistants.png)

Clicking on the assistant will start its execution. If it has progressive loading enabled, the result will be displayed in parts; otherwise, it will be displayed only when it is complete. If the content is loaded in parts, it may give the impression that the structure of the page has been broken, but everything will return to normal when the entire content is loaded.

![](ckeditor-loading.png)

Upon successful completion, the resulting content will be automatically inserted into the page. A confirmation window will appear with information about token consumption. If the result is not satisfactory, use the button<button class="btn btn-outline-secondary btn-ai-undo" type="button"><i class="ti ti-arrow-back"></i>Cancel the change</button> to return to the original value.

In this case, the assistant is set up so that (and we also recommend this when editing page content) it will also describe all the changes it made to the page content in the window, thus offering feedback on what needed to be edited.

![](ckeditor-done.png)

### Only part of the text

You don't have to edit the entire page content. The assistant working with `CKEditor` also supports editing only a selected part. If you select only a part of the content and then call the assistant, it will edit only that selected part.

![](ckeditor-text-selection.png)

## PageBuilder

Assistants also work in `PageBuilder` and are used to generate new content or edit existing page content inserted in `PageBuilder`. They are launched via the icon<button class="btn-ai" type="button"><i class="ti ti-sparkles"></i></button> located in the toolbar.

![](page_builder.png)

Clicking on it will open a window with all available assistants for the given field. They are clearly divided into groups (e.g. **Generation**, **Copywriting**, **Improvement**). Each one has a name, icon, and provider.

![](page_builder-assistants.png)

Clicking on the assistant will start its execution. Each block is considered a separate text/content and is processed separately. The window will gradually update the status of how many blocks out of the total number have already been processed.

![](page_builder-loading.png)

Upon successful completion, the resulting content will be automatically inserted into the page. A confirmation window will appear with information about token consumption. If the result is not satisfactory, use the button<button class="btn btn-outline-secondary btn-ai-undo" type="button"><i class="ti ti-arrow-back"></i>Cancel the change</button> to return to the original value.

In this case, the assistant is set up so that (and we also recommend this when editing page content) it will also describe all the changes it made to the page content in the window, thus offering feedback on what needed to be edited.

![](page_builder-done.png)

### Only part of the text

You don't have to edit the entire page content. The assistant working with `PageBuilder` also supports editing only a selected part. If you select only a part of the content and then call the assistant, it will edit only that selected part.

![](page_builder-selection.png)

## Squad mode

In PageBuilder, you can create a page from the materials and then edit it in the same chat. If you want to open the chat later from the history, save the new page first and reopen it in the editor. For a new, unsaved page, the chat is temporary (it will not be saved in the history). Therefore, we recommend that you save the new page first and then edit its text to preserve the history.

In the toolbar, click the AI ​​icon and select the **Web Designer (PageBuilder)** assistant. Use the plus icon **Upload file** to attach the material. It can be a PDF with a text layer, a Word document (DOCX) or a PowerPoint presentation (PPTX). In the message, write, for example, "Create a hair salon page with an introductory block, services and opening hours based on the PDF." Send the message.

![Web designer assignment with attached PDF of the salon](pb-chat-prompt.png)

After sending the message, the assistant prepares the HTML blocks. When the reply is complete, the **Preview** will automatically open in the **Content Design** tab. Use the **Desktop**, **Tablet**, and **Mobile** icons to see how the blocks will appear on different screens. Use the **Maximize Window** button to enlarge the preview to the entire chat area. Press the **Esc** key to return to the reply.

![Salon page design with device switchers and block insertion buttons](pb-chat-preview.png)

When you are satisfied with the suggestion, click **Replace content** below the answer. This will insert the suggested blocks into the page. If you want to keep the original blocks and add the new ones at the end, use **Add**. The **Minimize chat** button (minus in the top right) minimizes the chat window to a small icon in the bottom right and you can continue working directly on the page.

![Salon page created from PDF and minimized chat icon](pb-chat-success.png)

Click the AI ​​icon in the bottom right corner to restore the same chat. You don't need to send the attachment again. For example, write "Change the main title to Super Barber Bernolákovo and add a block with hair care tips. Revert the entire edited page."

![Continuing chat with page edit request](pb-chat-edit.png)

Under the new answer, click **Replace content** and check the result in PageBuilder. If you haven't edited the content manually in the meantime, you can undo the last step by clicking **Undo Paste**. Save the finished page in the editor.

![Edited page with new title after second message](pb-chat-edit-success.png)

With a saved page and OpenAI or Gemini assistant with temporary chat disabled, you can find messages and attachments via **Chat History** in the same field of the page. You can also open the conversation after closing and reopening the editor. When you no longer need it, click the trash icon next to its name and confirm deletion.

The same mode is supported by text fields and the page editor, depending on the assistant settings. Working with text selection, attachments, and history is described in the chapter [Chat in a field and in the page editor](../chat/README.md#chat-in-a-field-and-in-the-page-editor).

## Images on the page

You can also use the AI ​​assistant when inserting images into a web page. The assistant button is located in the lower right corner at the end of the image address field.

![](page-image.png)

After clicking, a window will open with all available assistants, which are worked with in the same way as described in the [Image fields](./README.md#image-fields) section.

![](page-image-assistants.png)

## Empty field

Assistants often use the contents of a given field as an input value, if the field is empty, they will not be displayed. For example, Translate to English does not make sense to display if the field is empty. It may happen that no assistant is available for an empty field, in which case an error message will be displayed. You must first enter text into the field before displaying assistants.

However, the same field is not always used for the input value. For example, in editing a web page, there is an assistant for generating the Web page name from its text. If the web page text is currently empty, this option may not be displayed.

![](no-assistants-available.png)
