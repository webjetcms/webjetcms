# Nomenclature

- CSS class names and IDs written in English
- Do not use a combination of English and Slovak
- All names in lower case
- The name should be self-descriptive

## BEM methodology

- I recommend using the BEM methodology http://getbem.com/

# CSS styles editor

In the Styles selection box, the editor displays the CSS styles found in the CSS file. However, the CSS parser is very simple (it can't parse minified CSS). Practically, it is not even desirable to display all CSS styles. For this reason, searching for CSS styles in a file is supported **editor.css**. This is searched for in the same directory as the main CSS style, e.g. `/templates/INSTALL_NAME/tempname/editor.css`.

In editor.css **you don't need to define the CSS properties themselves**, just define the CSS class names (in the example the background color is set for the example, but in reality you should have the CSS properties directly in the `.scss` files).

## Basic CSS classes

The selection box will display all CSS classes starting with the newline character ., that is, general CSS classes. These can be applied to any element.

```css
.styleYellow {
	background-color: yellow !important;
}
.styleBlueViolet {
	background-color: blueviolet !important;
}
```

## CSS classes bound to a tag

Binding and setting CSS classes according to a specific tag is also supported. Just prefix the CSS class with the tag name. When such a style is selected, the editor automatically traces the tag in the parent elements and applies the CSS style to the tag. So just have the editor cursor clicked in the child tag and the CSS style will be correctly applied.

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

Often a DIV element with a certain class is used, e.g. `div.container`. To apply CSS classes directly to such a DIV, it is possible to use a notation with two CSS classes. According to the first one, the parent element with the first CSS class is retrieved and the second CSS class is applied to it.

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

## CSS class marking

The same way a CSS class is marked, it can be unmarked. If the selected CSS class is already set and you select it again, it will be removed from the element.

**Remark:** when setting/changing a CSS class, the editor needs to know what CSS class it can remove (it can't just set the class attribute, it has to add/change the selected CSS class and keep CSS classes like container, row, pb-section, etc). So when setting a value, it iterates through all the CSS classes in the Styles selection box, removes those, and then sets the selected CSS class.
