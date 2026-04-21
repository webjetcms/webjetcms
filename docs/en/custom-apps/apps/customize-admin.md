# Customizing administration

WebJET allows you to customize the administration with additional CSS styles and JavaScript files. This allows you to change, for example, the logo for your company, add text to the splash screen, and so on.

## Additional CSS style

If you need to add/edit the design of the administration, just create the file `/components/INSTALL-NAME/admin/style.css` where `INSTALL-NAME` is the name of the installation. If the file exists, it will be added to the CSS styles of the administration section.

You can add CSS styles for example for [Quill type fields](../../developer/datatables-editor/standard-fields.md#quill):

```css
div.ql-editor .klass {
    font-weight: bold;
    background-color: yellow;
}
```

## Additional JavaScript

Similarly, you can create a JavaScript file `/components/INSTALL-NAME/admin/script.js` with the desired content. We recommend running scripts using the `window.domReady.add(function () {...}, 900)` function, which, with a value of `orderId>=900`, ensures execution only after the content of the standard administration page has been executed. If you do not specify a value of `orderId`, the function will be executed before the content of the current page.

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