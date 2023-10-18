var selectedForm
var selectedTextField
var selectedTextArea
var selectedHidden
var selectedbutton
var selectedCheckbox
var selectedRadio
var selectedSelect

function isCursorInForm()
{
	if (document.selection.type != "Control")
	{
		var elem = document.selection.createRange().parentElement()
		while (elem.tagName != "FORM")
		{
		  elem = elem.parentElement
		  if (elem == null) break
		}
		if (elem)
		{
			selectedForm = elem
			return true
		}
	}
}

function insertForm()
{
	if (isAllowed)
	{
		var leftPos = (screen.availWidth-400) / 2
		var topPos = (screen.availHeight-215) / 2
		insertFormWin = window.open("/components/form/editor_form.jsp",'','width=400,height=215,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
	}
}

function modifyForm()
{
	if (isAllowed)
	{
		if (isCursorInForm())
		{
			var leftPos = (screen.availWidth-400) / 2
			var topPos = (screen.availHeight-215) / 2
			modifyFormWin = window.open("../components/form/editor_form.jsp?modify=true",'','width=400,height=215,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}
	}
}

function doTextField()
{
	if (isAllowed())
	{

		var leftPos = (screen.availWidth-400) / 2
		var topPos = (screen.availHeight-195) / 2

		if (ObjEditoriwcm.selection.type == "Control")
		{
			var oControlRange = ObjEditoriwcm.selection.createRange();
			if (oControlRange(0).tagName.toUpperCase() == "INPUT")
			{
				if ((oControlRange(0).type.toUpperCase() == "TEXT") || (oControlRange(0).type.toUpperCase() == "PASSWORD"))
				{
					selectedTextField = ObjEditoriwcm.selection.createRange()(0);
					textFieldWin = window.open('../components/form/editor_form_tf.jsp?modify=true','','width=400,height=210,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
				}
				return true;
			}
		}
		else
		{
			textFieldWin = window.open('../components/form/editor_form_tf.jsp','','width=400,height=210,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}

	}
}

function doTextArea()
{
	if (isAllowed())
	{

		var leftPos = (screen.availWidth-430) / 2
		var topPos = (screen.availHeight-205) / 2

		if (ObjEditoriwcm.selection.type == "Control")
		{
			var oControlRange = ObjEditoriwcm.selection.createRange();
			if (oControlRange(0).tagName.toUpperCase() == "TEXTAREA")
			{
				selectedTextArea = ObjEditoriwcm.selection.createRange()(0);
				textFieldWin = window.open('../components/form/editor_form_ta.jsp?modify=true','','width=430,height=205,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
				return true;
			}
		}
		else
		{
			textFieldWin = window.open('../components/form/editor_form_ta.jsp','','width=430,height=205,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}

	}
}

function doHidden()
{
	if (isAllowed())
	{

		var leftPos = (screen.availWidth-400) / 2
		var topPos = (screen.availHeight-80) / 2

		if (ObjEditoriwcm.selection.type == "Control")
		{
			var oControlRange = ObjEditoriwcm.selection.createRange();
			if (oControlRange(0).tagName.toUpperCase() == "INPUT") {
				if (oControlRange(0).type.toUpperCase() == "HIDDEN") {
					selectedHidden = ObjEditoriwcm.selection.createRange()(0);
					hiddenWin = window.open('../components/form/editor_form_hf.jsp?modify=true','','width=420,height=80,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
				}
				return true;
			}
		}
		else
		{
			hiddenWin = window.open('../components/form/editor_form_hf.jsp','','width=420,height=80,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}

	}
}

function doButton()
{
	if (isAllowed())
	{

		var leftPos = (screen.availWidth-400) / 2
		var topPos = (screen.availHeight-105) / 2

		if (ObjEditoriwcm.selection.type == "Control")
		{
			var oControlRange = ObjEditoriwcm.selection.createRange();
			if (oControlRange(0).tagName.toUpperCase() == "INPUT") {
				if ((oControlRange(0).type.toUpperCase() == "RESET") || (oControlRange(0).type.toUpperCase() == "SUBMIT") || (oControlRange(0).type.toUpperCase() == "BUTTON")) {
					selectedButton = ObjEditoriwcm.selection.createRange()(0);
					buttonWin = window.open('../components/form/editor_form_btn.jsp?modify=true','','width=400,height=125,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
				}
				return true;
			}
		}
		else
		{
			buttonWin = window.open('../components/form/editor_form_btn.jsp','','width=400,height=125,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}

	}
}

function doCheckbox()
{
	if (isAllowed())
	{

		var leftPos = (screen.availWidth-400) / 2
		var topPos = (screen.availHeight-130) / 2

		if (ObjEditoriwcm.selection.type == "Control")
		{
			var oControlRange = ObjEditoriwcm.selection.createRange();
			if (oControlRange(0).tagName.toUpperCase() == "INPUT") {
				if (oControlRange(0).type.toUpperCase() == "CHECKBOX") {
					selectedCheckbox = ObjEditoriwcm.selection.createRange()(0);
					checkboxWin = window.open('../components/form/editor_form_cb.jsp?modify=true','','width=400,height=130,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
				}
				return true;
			}
		}
		else
		{
			checkboxWin = window.open('../components/form/editor_form_cb.jsp','','width=400,height=130,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}

	}
}

function doSelect()
{
	if (isAllowed())
	{

		var leftPos = (screen.availWidth-400) / 2
		var topPos = (screen.availHeight-130) / 2

		if (ObjEditoriwcm.selection.type == "Control")
		{
			var oControlRange = ObjEditoriwcm.selection.createRange();
			if (oControlRange(0).tagName.toUpperCase() == "SELECT") {
				selectedSelect = ObjEditoriwcm.selection.createRange()(0);
				selectWin = window.open('../components/form/editor_form_sl.jsp?modify=true','','width=400,height=130,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
				return true;
			}
		}
		else
		{
			selectedSelect = null;
			selectWin = window.open('../components/form/editor_form_sl.jsp','','width=400,height=130,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}

	}
}

function createSelect(name, multiple, size, options)
{
	var sel = ObjEditoriwcm.selection;
	if (sel!=null)
	{
		var rng = sel.createRange();
		if (rng!=null)
		{
			id = "";
			if (sel.type == "Control" && rng(0).tagName.toUpperCase() == "SELECT")
			{
				oSelect = ObjEditoriwcm.selection.createRange()(0);
				oSelect.name = name;
				oSelect.multiple = multiple;
				oSelect.size = size;
			}
			else
			{
				id = "selectElementPasted"+name;
				HTMLTextField = "<select name='"+name+"'" + multiple + size +" id='"+id+"'>";
				HTMLTextField += "<option value='xxx'>xxx</option></select>";
				rng.pasteHTML(HTMLTextField);
				oSelect = ObjEditoriwcm.getElementById(id);
			}

			if (oSelect)
			{
				oSelect.selectedIndex = 0;
				for (var i=0; i<options.length; i++)
				{
					var option = new Option();
					option.text = options[i].text;
					option.value = options[i].value;
					option.selected = options[i].defaultselected;
					option.defaultSelected = options[i].defaultselected;
					oSelect.options[i] = option;
				}
				oSelect.length = options.length;

				if (id!="") oSelect.removeAttribute("id");
			}
		}
	}
}

function doRadio()
{
	if (isAllowed())
	{

		var leftPos = (screen.availWidth-500) / 2
		var topPos = (screen.availHeight-400) / 2

		if (ObjEditoriwcm.selection.type == "Control")
		{
			var oControlRange = ObjEditoriwcm.selection.createRange();
			if (oControlRange(0).tagName.toUpperCase() == "INPUT") {
				if (oControlRange(0).type.toUpperCase() == "RADIO") {
					selectedRadio = ObjEditoriwcm.selection.createRange()(0);
					radioWin = window.open('../components/form/editor_form_rb.jsp?modify=true','','width=400,height=130,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
				}
				return true;
			}
		}
		else
		{
			radioWin = window.open('../components/form/editor_form_rb.jsp','','width=400,height=130,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
		}

	}
}