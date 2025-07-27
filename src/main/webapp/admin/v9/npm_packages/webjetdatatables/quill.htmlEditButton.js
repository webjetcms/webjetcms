//based on: https://github.com/benwinding/quill-html-edit-button/blob/v1.1.2/src/demo.js
//turned off formating because it was breaking the whitespaces around inline tags

function $create(elName) {
  return document.createElement(elName);
}
function $setAttr(el, key, value) {
  return el.setAttribute(key, value);
}

let debug = false;
const Logger = {
  prefixString() {
    return `</> quill-html-edit-button: `;
  },
  get log() {
    if (!debug) {
      return (...any) => {};
    }
    const boundLogFn = console.log.bind(console, this.prefixString());
    return boundLogFn;
  }
};

class htmlEditButton {
  constructor(quill, options) {
    debug = options && options.debug;
    Logger.log("logging enabled");
    // Add button to all quill toolbar instances
    const toolbarModule = quill.getModule('toolbar');
    if (!toolbarModule) {
      throw new Error(
        'quill.htmlEditButton requires the "toolbar" module to be included too'
      );
    }
    this.registerDivModule();
    let toolbarEl = toolbarModule.container;
    const buttonContainer = $create("span");
    $setAttr(buttonContainer, "class", "ql-formats");
    const button = $create("button");
    button.innerHTML = options.buttonHTML || "&lt;&gt;";
    button.title = options.buttonTitle || "Show HTML source";
    button.onclick = function(e) {
      e.preventDefault();
      launchPopupEditor(quill, options);
    };
    buttonContainer.appendChild(button);
    toolbarEl.appendChild(buttonContainer);
  }

  registerDivModule() {
    // To allow divs to be inserted into html editor
    // obtained from issue: https://github.com/quilljs/quill/issues/2040
    var Block = Quill.import('blots/block');
    class Div extends Block {}
    Div.tagName = "div";
    Div.blotName = "div";
    Div.allowedChildren = Block.allowedChildren;
    Div.allowedChildren.push(Block)
    Quill.register(Div);
  }
}

function launchPopupEditor(quill, options) {
  let htmlFromEditor = quill.container.querySelector(".ql-editor").innerHTML;

  htmlFromEditor = window.quillToHtmlFormat(htmlFromEditor);

  const popupContainer = $create("div");
  const overlayContainer = $create("div");
  const msg = options.msg || 'Edit HTML here, when you click "OK" the quill editor\'s contents will be replaced';
  const cancelText = options.cancelText || "Cancel";
  const okText = options.okText || "Ok";

  $setAttr(overlayContainer, "class", "ql-html-overlayContainer");
  $setAttr(popupContainer, "class", "ql-html-popupContainer");
  const popupTitle = $create("h5");
  $setAttr(popupTitle, "class", "modal-title ql-html-popupTitle");
  popupTitle.innerText = msg
  const textContainer = $create("div");
  textContainer.appendChild(popupTitle);
  $setAttr(textContainer, "class", "ql-html-textContainer");
  const textArea = $create("textarea");
  $setAttr(textArea, "class", "ql-html-textArea");
  textArea.value = formatHTML(htmlFromEditor);
  const buttonCancel = $create("button");
  buttonCancel.innerHTML = cancelText;
  $setAttr(buttonCancel, "class", "ql-html-buttonCancel");
  const buttonOk = $create("button");
  buttonOk.innerHTML = okText;
  $setAttr(buttonOk, "class", "ql-html-buttonOk");
  const buttonGroup = $create("div");
  $setAttr(buttonGroup, "class", "ql-html-buttonGroup");

  buttonGroup.appendChild(buttonCancel);
  buttonGroup.appendChild(buttonOk);
  textContainer.appendChild(textArea);
  textContainer.appendChild(buttonGroup);
  popupContainer.appendChild(textContainer);
  overlayContainer.appendChild(popupContainer);
  document.body.appendChild(overlayContainer);

  buttonCancel.onclick = function() {
    document.body.removeChild(overlayContainer);
  };
  overlayContainer.onclick = buttonCancel.onclick;
  popupContainer.onclick = function(e) {
    e.preventDefault();
    e.stopPropagation();
  };
  buttonOk.onclick = function() {
    const output = textArea.value.split(/\r?\n/g).map(el => el.trim());
    const noNewlines = output.join("");
    quill.container.querySelector(".ql-editor").innerHTML =  window.quillFromHtmlFormat(noNewlines);
    document.body.removeChild(overlayContainer);
  };
}

// Adapted FROM jsfiddle here: https://jsfiddle.net/buksy/rxucg1gd/
function formatHTML(code) {
  "use strict";

  //====================== webjet fix =======================
    //add new line to end p and div tag
    let blockTags = ["p", "div", "ul", "li", "ol", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "table", "tr", "td", "th"];
    blockTags.forEach(tag => {
      code = code.replace(new RegExp(`</${tag}>`, "g"), `</${tag}>\n`);
    });
    //also opening tags
    let openingTags = ["ul", "li", "ol", "table"];
    openingTags.forEach(tag => {
      code = code.replace(new RegExp(`<${tag}`, "g"), `\n<${tag}`);
    });
    //add tab to li, tr, td, th tags
    let tabTags = ["li", "tr", "td", "th"];
    tabTags.forEach(tag => {
      code = code.replace(new RegExp(`<${tag}`, "g"), `\t<${tag}`);
    });
    //replace double new lines with single
    code = code.replace(/\n\n/g, "\n");

    //remove OL helper classes
    code = code.replace(/<span class="ql-ui" contenteditable="false"><\/span>/gi, '');

    //DO NOT CONTINUE WITH THE OLD CODE BELOW
    if (1==1) return code;
  //===================== end webjet fix =====================

  let stripWhiteSpaces = true;
  let stripEmptyLines = true;
  const whitespace = " ".repeat(2); // Default indenting 4 whitespaces
  let currentIndent = 0;
  const newlineChar = "\n";
  let prevChar = null;
  let char = null;
  let nextChar = null;

  let result = "";
  for (let pos = 0; pos <= code.length; pos++) {
    prevChar = char;
    char = code.substr(pos, 1);
    nextChar = code.substr(pos + 1, 1);

    const isBrTag = code.substr(pos, 4) === "<br>";
    const isOpeningTag = char === "<" && nextChar !== "/" && !isBrTag;
    const isClosingTag = char === "<" && nextChar === "/" && !isBrTag;
    const isTagEnd = prevChar === ">" && char !== "<" && char !== " " && currentIndent > 0;
    if (isBrTag) {
      // If opening tag, add newline character and indention
      result += newlineChar;
      currentIndent--;
      pos += 4;
    }
    if (isOpeningTag) {
      // If opening tag, add newline character and indention
      result += newlineChar + whitespace.repeat(currentIndent);
      currentIndent++;
    }
    // if Closing tag, add newline and indention
    else if (isClosingTag) {
      // If there're more closing tags than opening
      if (--currentIndent < 0) currentIndent = 0;
      result += newlineChar + whitespace.repeat(currentIndent);
    }
    if(isTagEnd) {
      result += newlineChar + whitespace.repeat(currentIndent);
    }

    // remove multiple whitespaces
    else if (stripWhiteSpaces === true && char === " " && nextChar === " ")
      char = "";
    // remove empty lines
    else if (stripEmptyLines === true && char === newlineChar) {
      //debugger;
      if (code.substr(pos, code.substr(pos).indexOf("<")).trim() === "")
        char = "";
    }

    result += char;
  }
  Logger.log("formatHTML", {
    before: code,
    after: result
  });
  return result;
}

window.htmlEditButton = htmlEditButton;
export default htmlEditButton;
export { htmlEditButton };