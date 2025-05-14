<%@page import="java.util.Map"%><%@ page pageEncoding="utf-8" contentType="text/javascript" %><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page import="sk.iway.iwcm.Constants" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
String forumWysiwygIcons = Constants.getString("forumWysiwygIcons");
%>

// define toolbar buttons
if (!wysiwyg_toolbarButtons) {
    var wysiwyg_toolbarButtons = new Array(
        //command, display name, value, title, prompt/function, default text, css class
        <% if (forumWysiwygIcons.indexOf("bold")!=-1) { %>["bold", "<iwcm:text key="editor.bold"/>", WYSIWYG_VALUE_NONE, null, null, null, "Bold"],<% } %>
        <% if (forumWysiwygIcons.indexOf("italic")!=-1) { %>["italic", "<iwcm:text key="editor.italic"/>", WYSIWYG_VALUE_NONE, null, null, null, "Italic"],<% } %>
        <% if (forumWysiwygIcons.indexOf("createlink")!=-1) { %>["createlink", "<iwcm:text key="editor.link"/>", WYSIWYG_VALUE_PROMPT, null, "<iwcm:text key="editor.page_enter_url"/>:", "http://", "Link"],<% } %>
        <% if (forumWysiwygIcons.indexOf("unlink")!=-1) { %>["unlink", "<iwcm:text key="editor.unlink"/>", WYSIWYG_VALUE_NONE, null, null, null, "Unlink"],<% } %>
        <% if (forumWysiwygIcons.indexOf("insertimage")!=-1) { %>["insertimage", "<iwcm:text key="editor.insert_picture"/>", WYSIWYG_VALUE_PROMPT, null, "<iwcm:text key="editor.image_enter_url"/>:", "http://", "Image"],<% } %>
        <% if (forumWysiwygIcons.indexOf("inserthorizontalrule")!=-1) { %>["inserthorizontalrule", "<iwcm:text key="editor.hline"/>", WYSIWYG_VALUE_NONE, null, null, null, "HR"],<% } %>
        ["div"], // place a toolbar divider
        //["formatblock", "Headling 1", "<H1>", "Make top level heading", null, null, "H1"],
        //["formatblock", "Headling 2", "<H2>", "Make 2nd level heading", null, null, "H2"],
        //["formatblock", "Headling 3", "<H3>", "Make 3rd level heading", null, null, "H3"],
        //["formatblock", "Paragraph", "<P>", "Make a paragraph", null, null, "P"],
        //["formatblock", "Monospace", "<PRE>", "Make paragraph monospaced text", null, null, "Pre"],
        <% if (forumWysiwygIcons.indexOf("insertunorderedlist")!=-1) { %>["insertunorderedlist", "<iwcm:text key="editor.unorder_list"/>", null, null, null, null, "UL"],<% } %>
        <% if (forumWysiwygIcons.indexOf("insertorderedlist")!=-1) { %>["insertorderedlist", "<iwcm:text key="editor.order_list"/>", null, null, null, null, "OL"]<% } else { %>["div"]<% } %>
        //,
        //["div"], // place a toolbar divider
        //["toggleview", "Source", "Compose", "Switch views", null, null, "Src"]
    );
}

// map control elements to desired elements
if (!wysiwyg_elementMap) {
    var wysiwyg_elementMap = new Array(
        //control regex, desired regex replacement
        [/<(B|b|STRONG)>(.*?)<\/\1>/gi, "<strong>$2</strong>"],
        [/<(I|i|EM)>(.*?)<\/\1>/gi, "<em>$2</em>"],
        [/<P>(.*?)<\/P>/gi, "<p>$1</p>"],
        [/<H1>(.*?)<\/H1>/gi, "<h1>$1</h1>"],
        [/<H2>(.*?)<\/H2>/gi, "<h2>$1</h2>"],
        [/<H3>(.*?)<\/H3>/gi, "<h3>$1</h3>"],
        [/<PRE>(.*?)<\/PRE>/gi, "<pre>$1</pre>"],
        [/<A (.*?)<\/A>/gi, "<a $1</a>"],
        [/<IMG (.*?)>/gi, "<img $1 alt=\"\" />"],
        [/<LI>(.*?)<\/LI>/gi, "<li>$1</li>"],
        [/<UL>(.*?)<\/UL>/gi, "<ul>$1</ul>"],
        [/<span style="font-weight: normal;">(.*?)<\/span>/gi, "$1"],
        [/<span style="font-weight: bold;">(.*?)<\/span>/gi, "<strong>$1</strong>"],
        [/<span style="font-style: italic;">(.*?)<\/span>/gi, "<em>$1</em>"],
        [/<span style="(font-weight: bold; ?|font-style: italic; ?){2}">(.*?)<\/span>/gi, "<strong><em>$2</em></strong>"],
        [/<([a-z]+) style="font-weight: normal;">(.*?)<\/\1>/gi, "<$1>$2</$1>"],
        [/<([a-z]+) style="font-weight: bold;">(.*?)<\/\1>/gi, "<$1><strong>$2</strong></$1>"],
        [/<([a-z]+) style="font-style: italic;">(.*?)<\/\1>/gi, "<$1><em>$2</em></$1>"],
        [/<([a-z]+) style="(font-weight: bold; ?|font-style: italic; ?){2}">(.*?)<\/\1>/gi, "<$1><strong><em>$3</em></strong></$1>"],
        [/<(br|BR)>/g, "<br />"],
        [/<(hr|HR)( style="width: 100%; height: 2px;")?>/g, "<hr />"]
    );
}

// attach to window onload event
if (document.getElementById && document.designMode && !<%=Constants.getBoolean("disableWysiwyg")%>) {

    $(function(){
        wysiwyg();
    });

    <%--
    if (window.addEventListener){
        window.addEventListener("load", wysiwyg, false);
    } else if (window.attachEvent){
        window.attachEvent("onload", wysiwyg);
    } else {
        alert("Could not attach event to window.");
    }
    --%>
}

// init wysiwyg
function wysiwyg() {    

    setTimeout(createWysiwygControls, 100);

    // turn textareas into wysiwyg controls
    function createWysiwygControls() {
        //window.alert("10 sekund");
    	//var textareas = document.getElementsByTagName("textarea");

        $('textarea.wysiwyg').each(function(){
            textarea = $(this).get(0);

            var wysiwyg = document.createElement("div");
            var control = document.createElement("iframe");

            //var textarea = textareas[foo];
            wysiwyg.className = "wysiwyg";
            wysiwyg.id = "wysiwyg";
            var src = textarea.className.match(/[a-zA-Z0-9_.-]+\.html/);
            if (src == null)
            {
               src = "/components/_common/wysiwyg/empty.jsp";
            }
            control.src = src;
            control.className = "";
            wysiwyg.appendChild(control);
            wysiwyg.control = control;
            textarea.style.display = "none";

            $(this).removeClass("wysiwyg");
            //textarea.className = "";
            textarea.parentNode.replaceChild(wysiwyg, textarea);
            wysiwyg.appendChild(textarea);
            wysiwyg.textarea = textarea;
            createToolbar(wysiwyg);

            //WebJET - add submit event - lepsie tu ako neskor
            addEventWysiwyg(textarea.form, "submit", wysiwygSubmit);

        });
        setTimeout(initWysiwygControls, 300); // do this last and after a slight delay cos otherwise it can get turned off in Gecko
    }
    
    // get controls from DOM
    function getWysiwygControls() {
        var divs = document.getElementsByTagName("div");
        var wysiwygs = new Array();
        for (var foo = 0, bar = 0; foo < divs.length; foo++) {
            if (divs[foo].className == "wysiwyg") {
                wysiwygs[bar] = divs[foo];
                bar++;
            }
        }
        return wysiwygs;
    }
    
    // initiate wysiwyg controls
    function initWysiwygControls() {
        //window.alert("initializing");
        var wysiwygs = getWysiwygControls();
        if (!wysiwygs[0]) return; // no wysiwygs needed
        if (!wysiwygs[0].control.contentWindow) { // if not loaded yet, wait and try again
            setTimeout(initWysiwygControls, 100);
            return;
        }
        for (var foo = 0; foo < wysiwygs.length; foo++) 
        {
        		//window.alert("attaching");
            // turn on design mode for wysiwyg controls
            wysiwygs[foo].control.contentWindow.document.designMode = "on";
            // attach submit method
            var element = wysiwygs[foo].control;
            //window.alert("elem1a="+element.tagName);
            while (element.tagName && element.tagName.toLowerCase() != "form") 
            {
            	//window.alert("elem2="+element.tagName);
                if (!element.parentNode) break;
                element = element.parentNode;
            }
            //window.alert("elem="+element.tagName);
            if (element.tagName && element.tagName.toLowerCase() == "form" && !element.wysiAttached) {
                /*
                if (element.onsubmit) {
                    element.onsubmit = function() {
                        element.onsubmit();
                        wysiwygSubmit();
                    }
                } else {
                    element.wysiAttached = true;
                    element.onsubmit = wysiwygSubmit;
                }
                */
                //Opera fix
                //window.alert("pridavam submit");
                //presunute vyssie kvoli IE8
                //addEventWysiwyg(element, "submit", wysiwygSubmit);
            }
        }
        // schedule init of content (we do this due to IE)
        setTimeout(initContent, 800);
    }

    // set initial content    
    function initContent() {
        var wysiwygs = getWysiwygControls();
        for (var foo = 0; foo < wysiwygs.length; foo++) {
            wysiwygUpdate(wysiwygs[foo]);
        }
    }

    // create a toolbar for the control
    function createToolbar(wysiwyg) {
        var toolbar = document.createElement("div");
        var bar = 0;
        toolbar.className = "toolbar toolbar" + bar;
        for (var foo = 0; foo < wysiwyg_toolbarButtons.length; foo++) {
            if (wysiwyg_toolbarButtons[foo][0] == "toggleview") {
                var button = createButton(wysiwyg, foo);
                button.onclick = toggleView;
                button.htmlTitle = wysiwyg_toolbarButtons[foo][1];
                button.composeTitle = wysiwyg_toolbarButtons[foo][2];
                toolbar.appendChild(button);
            } else if (wysiwyg_toolbarButtons[foo].length >= 3) {
                var button = createButton(wysiwyg, foo);
                button.onclick = execCommand;
                toolbar.appendChild(button);
            } else if (wysiwyg_toolbarButtons[foo][0] == "div") {
                var divider = document.createElement("div");
                divider.className = "divider";
                toolbar.appendChild(divider);
            } else {
                bar++;
                wysiwyg.insertBefore(toolbar, wysiwyg.control);
                var toolbar = document.createElement("div");
                toolbar.className = "toolbar toolbar" + bar;
            }
        }
        wysiwyg.insertBefore(toolbar, wysiwyg.control);
    }
    
    // create a button for the toolbar
    function createButton(wysiwyg, number) {
        if (WYSIWYG_BUTTONS_AS_FORM_ELEMENTS) {
            var button = document.createElement("input");
            button.type = "button";
            button.value = wysiwyg_toolbarButtons[number][1];
        } else {
            if (document.all) { // IE needs the buttons to be anchors
                var button = document.createElement("a");
                button.href = "";
            } else {
                var button = document.createElement("span");
            }
            button.appendChild(document.createTextNode(wysiwyg_toolbarButtons[number][1]));
        }
        button.number = number;
        button.className = "toolbarButton toolbarButton" + wysiwyg_toolbarButtons[number][6];
        button.command = wysiwyg_toolbarButtons[number][0];
        if (wysiwyg_toolbarButtons[number][2]) button.commandValue = wysiwyg_toolbarButtons[number][2];
        if (wysiwyg_toolbarButtons[number][3])
        {
           button.title = wysiwyg_toolbarButtons[number][3];
        }
        else
        {
           button.title = wysiwyg_toolbarButtons[number][1];
        }
        
        button.wysiwyg = wysiwyg;
        return button;
    }
   
    // execute a toolbar command
    function execCommand() {
        var value = null;
        switch(this.commandValue) {
        case WYSIWYG_VALUE_NONE:
            value = null;
            break;
        case WYSIWYG_VALUE_PROMPT:
            if (wysiwyg_toolbarButtons[this.number][4]) var promptText = wysiwyg_toolbarButtons[this.number][4]; else var promptText = "";
            if (wysiwyg_toolbarButtons[this.number][5]) var defaultText = wysiwyg_toolbarButtons[this.number][5]; else var defaultText = "";
            var value = prompt(promptText, defaultText);
            if (!value) return false;
            break;
        case WYSIWYG_VALUE_FUNCTION:
        
        default:
            value = this.commandValue;
        }
        if (this.command instanceof Array) { // if command is array, execute all commands
            for (var foo = 0; foo < this.command.length; foo++) {
                if (this.command[foo] == "insertcontent") {
                    insertContent(this.wysiwyg, value);
                } else {
                    this.wysiwyg.control.contentWindow.document.execCommand(this.command[foo], false, value);
                }
            }
        } else {
            if (this.command == "insertcontent") {
                insertContent(this.wysiwyg, value);
            } else {
                this.wysiwyg.control.contentWindow.document.execCommand(this.command, false, value);
            }
        }
        textareaUpdate(this.wysiwyg);
        this.wysiwyg.control.contentWindow.focus();
        return false;
    }
    
    // insert HTML content into control
    function insertContent(wysiwyg, content) {
        var textarea = wysiwyg.textarea;
        var control = wysiwyg.control;
        if (document.selection) { // IE
            control.focus();
            sel = document.selection.createRange();
            sel.text = content;
        } else { // Mozilla
            var sel = control.contentWindow.getSelection();
            var range = sel.getRangeAt(0);
            sel.removeAllRanges();
            range.deleteContents();
            var oldContent = control.contentWindow.document.body.innerHTML;
            var inTag = false;
            var insertPos = 0;
            for (var foo = 0, pos = 0; foo < oldContent.length; foo++) {
                var aChar = oldContent.substr(foo, 1);
                if (aChar == "<") {
                    inTag = true;
                }
                if (!inTag) {
                    pos++;
                    if (pos == range.startOffset) {
                        insertPos = foo + 1;
                    }
                }
                if (aChar == ">") {
                    inTag = false;
                }
            }
            control.contentWindow.document.body.innerHTML = oldContent.substr(0, insertPos) + content + oldContent.substr(insertPos, oldContent.length);
        }
        textareaUpdate(wysiwyg);
    }
    
    // show textarea view
    function toggleView() {
        var control = this.wysiwyg.control;
        var textarea = this.wysiwyg.textarea;
        var toolbars = this.wysiwyg.getElementsByTagName("div");
        if (textarea.style.display == "none") {
            textareaUpdate(this.wysiwyg);
            control.style.display = "none";
            textarea.style.display = "block";
            for (var foo = 0; foo < toolbars.length; foo++) {
                for (var bar = 0; bar < toolbars[foo].childNodes.length; bar++) {
                    var button = toolbars[foo].childNodes[bar];
                    if (button.command != "toggleview") {
                        if (button.tagName != "DIV") button.disabled = true;
                        button.oldClick = button.onclick;
                        button.onclick = null;
                        button.oldClassName = button.className;
                        button.className += " disabled";
                    }
                }
            }
        } else {
            wysiwygUpdate(this.wysiwyg);
            textarea.style.display = "none";
            control.style.display = "block";
            control.contentWindow.document.designMode = "on";
            for (var foo = 0; foo < toolbars.length; foo++) {
                for (var bar = 0; bar < toolbars[foo].childNodes.length; bar++) {
                    var button = toolbars[foo].childNodes[bar];
                    if (button.command != "toggleview") {
                        if (button.tagName != "DIV") button.disabled = false;
                        if (button.oldClick) button.onclick = button.oldClick;
                        if (button.oldClassName) button.className = button.oldClassName;
                    }
                }
            }
        }
        return false;
    }
   
    
    // update the wysiwyg to contain the source for the textarea control
    function wysiwygUpdate(wysiwyg) 
    {
        //window.alert("1");        
        var value = wysiwyg.textarea.value;
        if (""==value)
        {
           value = "<p>&nbsp;</p>";
        }
        //window.alert(value);
        //console.log(value);
        if (wysiwyg && wysiwyg.control && wysiwyg.control.contentWindow && wysiwyg.control.contentWindow.document) {
            wysiwyg.control.contentWindow.document.body.innerHTML = value;
        }
    }
    
    // update for upon submit
    function wysiwygSubmit() 
    {
        var divs = null;
        try
        {
        		if (this.document) divs = this.document.getElementsByTagName("div");
            divs = this.getElementsByTagName("div");
        }
        catch (e) {}
        for (var foo = 0; foo < divs.length; foo++) 
        {
            if (divs[foo].className == "wysiwyg") 
            {
                textareaUpdate(divs[foo]);
            }
        }
    }

}

function wysiwygUpdateFromTextarea()
{
	var divs = document.getElementsByTagName("div");
   var wysiwygs = new Array();
   for (var foo = 0, bar = 0; foo < divs.length; foo++) {
       if (divs[foo].className == "wysiwyg") {
           wysiwygs[bar] = divs[foo];
           bar++;
       }
   }
   for (var foo = 0; foo < wysiwygs.length; foo++) 
   {
       var wysiwyg = wysiwygs[foo];
       var value = wysiwyg.textarea.value;
        if (""==value)
        {
           value = "<p>&nbsp;</p>";
        }
        //window.alert(value);
        if (wysiwyg && wysiwyg.control && wysiwyg.control.contentWindow && wysiwyg.control.contentWindow.document) wysiwyg.control.contentWindow.document.body.innerHTML = value;
   }
}

function addEventWysiwyg(obj, evType, fn)
{    
    if (navigator.userAgent.indexOf("Opera")!=-1)
    {
    	//opera je tupa, cez addEventListener to nefunguje
    	eval("obj.on"+evType+"=fn");
    	return;
    }
    
	if (obj.addEventListener)
	{
		obj.addEventListener(evType, fn, true);
		return true;
	}
	else if (obj.attachEvent)
	{
		var r = obj.attachEvent("on"+evType, fn);
		return r;
	}
	else
	{
		return false;
	}
}

// update the textarea to contain the source for the wysiwyg control
function textareaUpdate(wysiwyg)
{
    //console.log(wysiwyg);
    var html = wysiwyg.control.contentWindow.document.body.innerHTML;
    //console.log(html);
    for (var foo = 0; foo < wysiwyg_elementMap.length; foo++) {
        html = html.replace(wysiwyg_elementMap[foo][0], wysiwyg_elementMap[foo][1]);
    }
    wysiwyg.textarea.value = html;
}