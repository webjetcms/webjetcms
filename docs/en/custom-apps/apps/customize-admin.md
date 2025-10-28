# Customisation of administration

WebJET allows you to customize the administration with additional CSS styles and JavaScript files. You can change, for example, your logo for your company logo, add text to the home screen and so on.

## Additional CSS style

If you need to add/edit the administration design just create a file `/components/INSTALL-NAME/admin/style.css` While `INSTALL-NAME` is the name of the installation. If the file exists it will be added to the CSS stylesheet in the administration section.

So you can add CSS styles, for example, for [Quill field](../../developer/datatables-editor/standard-fields.md#quill):

```css
div.ql-editor .klass {
    font-weight: bold;
    background-color: yellow;
}
```

## Additional JavaScript

Similarly, you can create a JavaScript file `/components/INSTALL-NAME/admin/script.js` with the required content. We recommend to run the scripts using the `window.domReady.add(function () {...}, 900)`, which at the value of `orderId>=900` ensures execution only after the content of the standard administration page has been executed. If the value `orderId` you do not specify, the function is executed before the content of the current page.

```javascript
if ("/admin/v9/"===window.location.pathname) {
    //domReady orderId >= 900 puts the script last in the queue
    window.domReady.add(function () {
        console.log("Admin script loaded for path");
        //because of the custom container we need to call WJ.notify("warning") instead of WJ.notifyWarning
        WJ.notify("warning", "Production environment", "You are in a production environment, please be careful.", 0, [], false, "toast-container-overview");
    }, 900);
}
```
