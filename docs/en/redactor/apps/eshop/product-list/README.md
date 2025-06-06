# List of products

The Product List application provides an overview and management of available products for e-commerce.

![](datatable.png)

The displayed products are filtered by the selected **product categories**. Such a product category is represented by a folder. In the upper left corner of the application there is a category filter ![](select.png ":no-zoom") which actually un-filters the data for that folder but also all sub-folders.

The available folders representing the categories are arranged in a so-called tree, where the sub-folders are always after the corresponding parent folder.

![](select-options.png)

The first option in the list is always the main section, which represents all categories (all products will be displayed). When a category (folder) is selected, the data of that folder and all sub-folders will be displayed.

## Adding a new product category

Adding a new category will create a sub-folder that will be placed under the currently selected folder (category).

Example. If we have just selected a folder ![](select-phones.png ":no-zoom") and create a new one called **Android**, a new folder will be created at ![](select-phones-android.png ":no-zoom")

Add a new folder by clicking <button class="btn btn-sm btn-outline-secondary" type="button"><span><i class="ti ti-folder-plus" ></i></span></button>. When you press it, the window for adding a folder appears.

![](toaster-new-folder.png)

The window also contains information about under which folder this new one will be created. After (un)filling in the fields in the window and confirming with the button <button class="btn btn-primary" type="button">Confirm</button> four situations can occur:
- if a new category name is not entered, the creation will fail and a message will be displayed

![](toaster-new-folder-A.png)

- if the name of the new category is not unique (unique for the folder), the creation will fail and a message will be displayed

![](toaster-new-folder-B.png)

- if any other error occurs, the message will be displayed

![](toaster-new-folder-A.png)

- if everything goes well

![](toaster-new-folder-C.png)

## Product management

Products are represented by pages that you can add under specific categories. The parent folder is automatically pre-set in the page according to the currently selected category (but can be changed). You can do all operations over the products (pages) like create/edit/clone/import ...

![](new-product.png)

## Important settings

### Card **Perex**

In the charts **Perex** is an important setting:
- values **Image**. This image will be displayed in the e-shop as a product preview.

![](new-product-image.png)

- values **Tags**. These tags make it easy to filter products in the e-shop.

![](new-product-perex.png)

### Attributes tab

In the attributes tab, we use the phone group selection on the item specifications. As can be seen in the figure below, for the group `iPhone X` it is possible to set the colour and memory variant of the device. These selection fields are displayed in the e-shop, next to the product details.

![](new-product-attr.png)

Read more about attributes in [Page attributes](../../../webpages/doc-attributes/README.md).
