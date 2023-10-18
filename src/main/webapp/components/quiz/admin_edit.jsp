<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.tags.*" %>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@ page import="sk.iway.iwcm.components.quiz.QuizType" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_quiz"/>
<iwcm:menu notName="cmp_quiz">
	<iwcm:text key="components.permsDenied"/>
	<% if (1==1) return; %>
</iwcm:menu>
<%@ include file="/admin/layout_top_dialog.jsp" %>

<%-- JSON Editor Script --%>
<%=Tools.insertJQueryUI(pageContext, "sortable") %>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	request.setAttribute("cmpName", "quiz");
	request.setAttribute("titleKey", "components.quiz.title");
	request.setAttribute("descKey", "components.quiz.desc");
	request.setAttribute("iconLink", "/components/quiz/editoricon.png");
	String id = request.getParameter("id") == null ? "0" : request.getParameter("id");
%>
<link type="text/css" rel="stylesheet" media="screen" href="/components/json_editor/editor_style.css" />
<script type="text/javascript" src="/components/json_editor/editor.js"></script>
<script type="text/javascript" src="/components/json_editor/editor_functions.js"></script>
<%-- END of JSON Editor Script --%>
<script type="text/javascript">

	// Editor Configuration For questions
    // For fields configuration
    var editorItemFields = {
        id: {
            title: "ID",
            type: "hidden",
            dataType: "int"
        },
		question: {
            title: "Otázka",
            type: "text",
            dataType: "string",
            classes: ""
        },
        image: {
            title: "Obrazok",
            type: "image",
            dataType: "string",
            classes: ""
        },
        option1: {
            title: "Odpoveď 1",
            type: "text",
            dataType: "string",
            classes: "",
        },
        option2: {
            title: "Odpoveď 2",
            type: "text",
            dataType: "string",
            classes: "",
        },
        option3: {
            title: "Odpoveď 3",
            type: "text",
            dataType: "string",
            classes: "",
        },
        option4: {
            title: "Odpoveď 4",
            type: "text",
            dataType: "string",
            classes: "",
        },
        option5: {
            title: "Odpoveď 5",
            type: "text",
            dataType: "string",
            classes: "",
        },
        option6: {
            title: "Odpoveď 6",
            type: "text",
            dataType: "string",
            classes: "",
        },
        rate1: {
            title: "Body 1",
            type: "text",
            dataType: "int",
            classes: "",
        },
        rate2: {
            title: "Body 2",
            type: "text",
            dataType: "int",
            classes: "",
        },
        rate3: {
            title: "Body 3",
            type: "text",
            dataType: "int",
            classes: "",
        },
        rate4: {
            title: "Body 4",
            type: "text",
            dataType: "int",
            classes: "",
        },
        rate5: {
            title: "Body 5",
            type: "text",
            dataType: "int",
            classes: "",
        },
        rate6: {
            title: "Body 6",
            type: "text",
            dataType: "int",
            classes: "",
        },
        rightAnswer: {
        	title: "Správna odpoveď",
        	type: "select",
        	dataType: "int",
        	options: [
        		{
        			title: 'Odpoveď 1',
        			value: 1
        		},
        		{
        			title: 'Odpoveď 2',
        			value: 2
        		},
        		{
        			title: 'Odpoveď 3',
        			value: 3
        		},
        		{
        			title: 'Odpoveď 4',
        			value: 4
        		},
        		{
        			title: 'Odpoveď 5',
        			value: 5
        		},
        		{
        			title: 'Odpoveď 6',
        			value: 6
        		}
        	]
        }
    };

    // Form fields to edit
    var editorItemsToUse = [
        "id",
        "question",
		<% if(Constants.getBoolean("quizAdminShowImageUrl")) { %>
        "image",
		<% } %>
        "option1",
        "rate1",
        "option2",
        "rate2",
        "option3",
        "rate3",
        "option4",
        "rate4",
        "option5",
        "rate5",
        "option6",
        "rate6",
        "rightAnswer"
    ];

    // Editor configuration for results
    var editorItemFieldsResults = {
        id: {
            title: "ID",
            type: "hidden",
            dataType: "int"
        },
        from: {
            title: "Počet bodov od",
            type: "text",
            dataType: "int",
            classes: ""
        },
        to: {
            title: "Počet bodov do",
            type: "text",
            dataType: "int",
            classes: "",
        },
        description: {
            title: "Text Odpovede",
            type: "textarea",
            dataType: "string",
            classes: "",
        }
    };

    // Form fields to edit
    var editorItemsToUseResults = [
        "id",
        "from",
        "to",
        "description"
    ];

    var inputData = null;
    var editorItemsList = null;
    var editorItemsListResults = null;
    var renderer = null;
    var rendererResults = null;
    var quizData = {};

    $.ajax({
        url: "/admin/rest/quiz/<%=id %>",
		error: function ( jqXHR, textStatus, errorThrown ) {
            window.location.href = "/components/quiz/admin_edit.jsp";
		},
        success: function (data) {
        	console.log('data');
        	console.log(data);
        	quizData = data;
        	inputData = data.quizQuestions;
        	editorItemsList = new EditorItemsList(inputData, editorItemsToUse);
        	renderer = new EditorRenderer("#editorWrapper", editorItemsList, editorItemFields, editorItemsToUse, "EditorItemFieldRenderer");

            editorItemsListResults = new EditorItemsList(data.quizResults, editorItemsToUseResults);
        	rendererResults = new EditorRenderer("#editorWrapperResults", editorItemsListResults, editorItemFieldsResults, editorItemsToUseResults, "EditorItemFieldRenderer");
        },
        dataType: 'json',
        async: false
    });

    function prepareData(data, editorItemFields) {
        for(var i = 0; i < data.length; i++) {
            var item = data[i];
            for(key in item) {
                if(editorItemFields[key] && editorItemFields[key].dataType == 'int') {
                    item[key] = parseInt(item[key]);
                    if(item[key] == null || isNaN(item[key]) || item[key] == undefined) {
                        delete item[key];
                    }
                } else if(editorItemFields[key] && editorItemFields[key].dataType == 'number') {
                    item[key] = parseFloat(item[key]);
                    if(item[key] == null || isNaN(item[key]) || item[key] == undefined) {
                        delete item[key];
                    }
                } else {
                    if(item[key] == null || item[key] == undefined || item[key] == "") {
                        delete item[key];
                    }
                }
            }
        }
        return data;
	}

    function saveQuiz(quiz) {
    	editorItemsList.setDataFromDom($("#editorWrapper .item"));
    	editorItemsListResults.setDataFromDom($("#editorWrapperResults .item"));

    	quiz.name = $('#quizName').val();
    	quiz.quizType = $('#quizType').val();
    	if(parseInt($('#quizId').val()) > 0) {
    		quiz.id = parseInt($('#quizId').val());
    	} else {
    		delete quiz['id'];
    	}
    	quiz.quizQuestions = editorItemsList.getData();
        quiz.quizQuestions = prepareData(quiz.quizQuestions, editorItemFields);

        quiz.quizResults = editorItemsListResults.getData();
        quiz.quizResults = prepareData(quiz.quizResults, editorItemFieldsResults);

    	console.log(quiz);

		$.ajax({
			url:'/admin/rest/quiz/save',
			type:"POST",
			data:JSON.stringify(quiz),
			contentType:"application/json; charset=utf-8",
			dataType:"json",
			success: function(result) {
		 		if(JSON.parse(result) === true) {
					window.close();
					window.opener.location.reload();
				} else {
					alert('there is some problem!');
				}
			}
		});
    }

    function Ok()
	{
    	saveQuiz(quizData);
	}

    $(function() {
    	$('#quizName').val(quizData.name);
        $("#quizType option:selected").removeAttr("selected");
        $("#quizType option[value=" + quizData.quizType + "]").attr("selected", "selected");
    	$('#quizId').val(parseInt(quizData.id));
    });

</script>

<style type="text/css">
	.editorWrapper form.item	{padding: 10px 0 0 35px; margin: 10px 0 0 0;}
	form.item div.propertyWrapper {display: none;}
	form.item div.propertyWrapper:nth-child(2),
	form.item.show div.propertyWrapper	{display: block;}
	form.item:hover {cursor: pointer; border-color: #35aa47; box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);}
	.editorWrapper span.removeItem {right: -35px; top: 10px;}
	.editorWrapper span.moveItem {bottom: 50%; right: 100%; transform: translate(25px,50%); width: 14px; height: 40px; background-size: 14px 40px;}
	.editorWrapper form.item input[type=text] {max-width: 600px;}

	form.item div.propertyWrapper:nth-child(2) label:after {content:""; display: block; width: 30px; height: 30px; background: red; position: absolute; top: 10px; right: 12px; opacity: 0.5;
															background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAMCAYAAABiDJ37AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyJpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMy1jMDExIDY2LjE0NTY2MSwgMjAxMi8wMi8wNi0xNDo1NjoyNyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENTNiAoV2luZG93cykiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6MkE5RDE0Q0JCMjY0MTFFNzhGMzFENTI4ODM0RTdDRkIiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6MkE5RDE0Q0NCMjY0MTFFNzhGMzFENTI4ODM0RTdDRkIiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDoyQTlEMTRDOUIyNjQxMUU3OEYzMUQ1Mjg4MzRFN0NGQiIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDoyQTlEMTRDQUIyNjQxMUU3OEYzMUQ1Mjg4MzRFN0NGQiIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/PppQYuYAAADTSURBVHjaYmBgYPAE4uVAzMJAPmAG4iVA7A3i7ATi/0A8H4iZyDCMEYhnQ83YBRLgB+JzUIF+qAJSDOuG6j0HNQsMxID4JlSijgQDq6B6bkLNQAFyQPwIqiCXCMOyoGofQfViBepA/AqqMBaPYTFQNa+gevACQyD+CMR/gNgfi7wfVA6kxojYsLEF4m9A/AOInZDEHaFi36FqSAKg9PkbiD8DsRkQm0LZIDEvchNsOBD/A+K3UAxiRzBQCNKhEfAfysYLiMluM4FYAJqIZxJSDBBgAPmpMRGj7k2sAAAAAElFTkSuQmCC') center no-repeat;}
	form.item div.propertyWrapper:nth-child(2) label:hover:after {opacity: 1;}
	form.item.show div.propertyWrapper:nth-child(2) label:after {transform: rotate(180deg);}

	div.col-sm-4 label {display: block; padding: 7px 10px 7px 0; margin: 0; text-align: right;}
	div.col-sm-4,
	div.col-sm-8	{float: left; width: auto; padding: 0;}
	div.col-sm-4 {width: 80px; position: static;}
	div.col-sm-8 {width: 450px;}

</style>

<div class="padding10">
	<div style="margin-left:0px; padding-left:0px; width:620px; min-height:800px;">
		<label style="width:113px; text-align:right;padding-right:7px;"><iwcm:text key="components.quiz.name"/></label>
		<input type="text" id="quizName" size="" style="width:450px;">
		<br><br>
		<label style="width:113px; text-align:right;padding-right:7px;"><iwcm:text key="components.quiz.type"/></label>
		<select id="quizType" size="" style="width:450px;">
			<% for(QuizType qt : QuizType.values()) { %>
				<option value="<%=qt.name() %>"><%=qt %></option>
			<% } %>
		</select>

		<input type="hidden" id="quizId">
		<div id="editorWrapper" class="editorWrapper"></div>
		<input type="button" id="addItem" class="button50 button50grey" style="margin-top: 8px; margin-bottom: 8px;" value="Nová otázka">

		<div id="editorWrapperResults" class="editorWrapper"></div>
		<input type="button" id="addItemResults" class="button50 button50grey" style="margin-top: 8px; margin-bottom: 8px;" value="Nová odpoveď">
	</div>
</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>

<script type="text/javascript">
	$(document).ready(function() {

		$("#editorWrapper, #editorWrapperResults").on("click", "form.item label", function (){
			$(this).closest("form.item").toggleClass("show");
		});

        $(function() {
            setTimeout(function(){
                if(rendererResults) {
                    rendererResults.setAfterRenderCallback(function(self) {
                        // remove item
                        $("#editorWrapperResults .removeItem").on('click', function(e) {
                            e.preventDefault();
                            $(this).closest('.item').remove();
                            self.editorItemsList.setDataFromDom($(self.selector + " .item"));
                        });

                        // choose image
                        $('#editorWrapperResults .imageDiv').on('click', function(e) {
                            e.preventDefault();
                            console.log($(this).closest('.item').attr('id'));
                            openImageDialogWindow($(this).closest('.item').attr('id'), "image", null);

                            //perexImageBlur(document.getElementById($(this).closest('.item').find('input[name=image]').attr('id')));
                        });

                        $('#editorWrapperResults input[name=image]').on('change', function() {
                            console.log('tu som blur!');
                            console.log(this);
                            perexImageBlur(document.getElementById($(this).attr('id')));
                        });

                        // show redirect url
                        $('#editorWrapperResults .editorItemCheckbox').on('click', function() {
                            var itemVal = $(this).closest('.propertyWrapper').find('.editorItemValue').val();
                            if(itemVal != undefined && itemVal != "") {
                                $(this).closest('.propertyWrapper').find('.editorItemValue').val("");
                            }
                            $(this).closest('.propertyWrapper').find('.editorItemValue').toggle();
                        });

                        // choose redirect url
                        $('#editorWrapperResults input[name=redirectUrl]').on('click', function(e) {
                            e.preventDefault();
                            openLinkDialogWindow($(this).closest('.item').attr('id'), "redirectUrl", null, null);
                        });

                        // show/hide each redirect url
                        $('#editorWrapperResults .editorItemCheckbox').each(function() {
                            var itemVal = $(this).closest('.propertyWrapper').find('.editorItemValue').val()
                            if(itemVal != undefined && itemVal != "") {
                                $(this).prop('checked', true);
                                $(this).closest('.propertyWrapper').find('.editorItemValue').show();
                            } else {
                                $(this).prop('checked', false);
                                $(this).closest('.propertyWrapper').find('.editorItemValue').hide();
                            }
                        });

                        // show choosen images
                        $('#editorWrapperResults .imageDiv').each(function() {
                            if($(this).closest('.item').find('input[name=image]').val() != "" && $(this).closest('.item').find('input[name=image]').val() != undefined) {
                                perexImageBlur(document.getElementById($(this).closest('.item').find('input[name=image]').attr('id')));
                            }
                        });
                    });
                    rendererResults.render();

                    $('#addItemResults').on('click', function(e) {
                        e.preventDefault();
                        editorItemsListResults.setDataFromDom($("#editorWrapperResults .item"));
                        editorItemsListResults.addNewItem();
                        rendererResults.render();
                        if($('a.button_collapse').hasClass('collapsed')){
                            $("#editorWrapperResults .item").addClass('collapsed');
                        }
                    });
				}
            });
		});



	});
</script>