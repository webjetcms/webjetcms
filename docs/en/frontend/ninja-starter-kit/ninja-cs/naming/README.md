# Nomenclature

- Write CSS class names and IDs in English
- Do not use combinations of English and Slovak languages
- All names should be written in lowercase.
- The title should be self-descriptive

## BEM methodology

- I recommend using the BEM methodology http://getbem.com/


# CSS editor styles

The editor in the Styles selection box displays the CSS styles found in the CSS file. However, the CSS parser is very simple (it cannot parse minified CSS). In practice, it is not even desirable to display all CSS styles. For this reason, searching for CSS styles in the **editor.css** file is supported. This is searched in the same directory as the main CSS style, e.g. ```/templates/INSTALL_NAME/tempname/editor.css```.

In editor.css **you don't need to define the CSS properties themselves**, just define the CSS class names (in the example, the background color is set for demonstration, but in reality you should have CSS properties directly in the ```.scss``` files).

## Basic CSS classes

The selection box displays all CSS classes starting on a new line with the character ., i.e. general CSS classes. These can be applied to any element.

```css
.styleYellow {
   background-color: yellow !important;
}
.styleBlueViolet {
   background-color: blueviolet !important;
}
```

## CSS classes attached to a tag

Binding and setting a CSS class to a specific tag is also supported. Just prefix the CSS class with the tag name. When you select such a style, the editor automatically searches for the given tag in the parent elements and applies the CSS style to this tag. So, just have the cursor in the editor clicked on the child and the CSS style is applied correctly.

```css
section.aqua {
   background-color: aqua !important;
}
section.azure {
   background-color: azure !important;
}
div.burlywood {
   background-color: burlywood !important;
}
div.chocolate {
   background-color: chocolate !important;
}
```

## CSS classes bound to a tag and a CSS class

A DIV element with a specific class is often used, e.g. ```div.container```. To apply CSS classes directly to such a DIV, it is possible to use a notation with two CSS classes. According to the first, a parent element with the first CSS class is found and the second CSS class is applied to it.

```css
//aplikuje sa iba na div.container
div.container.red {
   background-color: red !important;
}
div.container.orange {
   background-color: orange !important;
}

//aplikuje sa iba na div.row
div.row.darkorange {
   background-color: darkorange;
}
div.row.orange {
   background-color: orange;
}

//aplikuje sa iba na div.col-12
div.col-12.chartreuse {
   background-color: chartreuse;
}
div.col-12.darkgreen {
   background-color: darkgreen;
}
```
## Unchecking CSS class

Just as a CSS class is marked, it can be unmarked. If the selected CSS class is already set and you select it again, it will be removed from the element.

**Note:** when setting/changing a CSS class, the editor needs to know which CSS class it can remove (it can't just set the class attribute, it has to add/replace the selected CSS class and keep CSS classes like container, row, pb-section, etc.). So when setting a value, it iterates over all CSS classes in the Styles selection box, removes them, and then sets the selected CSS class.





