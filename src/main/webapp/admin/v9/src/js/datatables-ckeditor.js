import WJ from "./webjet";

export class DatatablesCkEditor {

	ckEditorInstance = null;
	datatable = null;
	json = null;
	options = null;
	editorHeightLatest = 0;
	//zoznam regexp moznosti pre typ pola
	regexps = null;
	editingMode = "";

	//CKEDITOR object, referenced like this because of inline editing
	ckEditorObject = null;
	//window object
	myWindow = null;

    constructor(options) {
		this.options = options;
		this.datatable = options.datatable;

		this.myWindow = options.window ? options.window : window;
		this.ckEditorObject = options.ckEditorObject ? options.ckEditorObject : this.myWindow.CKEDITOR;

		var ckEditorConfig = options.ckEditorConfig ? options.ckEditorConfig : '/admin/skins/webjet8/ckeditor/config.jsp';
		var ckEditorElementId = options.ckEditorElementId ? options.ckEditorElementId : "data";
		var ckEditorInitFunction = options.ckEditorInitFunction ? options.ckEditorInitFunction : this.ckEditorObject.replace;

		//console.log("CkEditor inicializujem, options=", options, " CKEDITOR=", CKEDITOR, "window=", this.myWindow.location.href);

		this.customizeEditor(this.options);

		//console.log("ckEditorElementId=", ckEditorElementId);

		this.initializeCkEditorImpl(ckEditorElementId, ckEditorInitFunction, ckEditorConfig, this.options)
		this.editorHeightLatest = 0;

		let that = this;

		if (typeof window.ckeditorRegexps == "undefined") {
			//ziskaj regexps a potom inicializuj
			$.ajax({
				url: "/admin/rest/forms-list/regexps",
				success: function (json) {
					window.ckeditorRegexps = json;
					//console.log("regexps, json=", json);
					that.regexps = json;
					that.afterInit();
				}
			});
		}
		else {
			setTimeout(() => {
				that.regexps = window.ckeditorRegexps;
				that.afterInit();
			}, 100);
		}

		this.myWindow.ckeditorFixIframeDialog = function(iframeElement) {
			//WJ8 funkcia volana z pluginov pre nastavenie velkosti iframe elementu
			setTimeout(function() {
				//console.log("iframeElement=", iframeElement, " tr height=", $(iframeElement).parent().parent().height());
				//chrome bug - nastavime vysku TD elementu rovnako ako TR elementu, height 100% na td to neberie
				$(iframeElement).parent().css("padding-bottom", "0px");
				$(iframeElement).closest("td.cke_dialog_contents_body").css("padding-bottom", "0px");
				/*$(iframeElement).parent().css("height", ($(iframeElement).parent().parent().height()-4)+"px");
				setTimeout(function() {
					//height po rendri musime vratit nazad na 100%, aby sa mohol zvacsit podla contentu
					$(iframeElement).parent().css("height", "100%");
				}, 5000);*/
				//toto pomohlo, okno sa totiz zvacsuje a nemoze mat nastaveny presny rozmer, min-height tiez nefungoval
				if (navigator.userAgent.indexOf("Firefox")==-1) $(iframeElement).parent().css("display", "block");
			}, 100);
		}

		this.myWindow.resizeDialogCK = function(width, height)
		{
			//console.log("resizeDialogCK, width=", width, " height=", height);
			if (typeof that.ckEditorObject != 'undefined' && that.ckEditorObject.dialog.getCurrent() != null) {
				that.ckEditorObject.dialog.getCurrent().resize(width, height);
			}
			else {
				console.warn("Var CKEDITOR is undefined or ckEditorObject.dialog.getCurrent() is null");
			}
		}
	}

	afterInit() {
		var that = this;
		this.ckEditorInstance.on('afterPaste', function(e) {
			//console.log("After paste, e=", e);
			if (typeof that.myWindow.bootstrapVersion != "undefined" && that.myWindow.bootstrapVersion.indexOf("3")!=0) {
				//najdi tabulky a wrapni ich do table-responsive
				$(that.ckEditorInstance.document.$).find("table.tabulkaStandard").each(function() {
					var $table = $(this);
						if ($table.hasClass("tabulkaStandard")) {
						//console.log("table=", this);
						$table.attr("class", "table table-sm tabulkaStandard");
						//aby to bolo rovnake ako standardne vytvorenie tabulky
						$table.attr("border", "1");

						var parent = $table.parent("div.table-responsive");
						if (parent.length == 0)	$table.wrap('<div class="table-responsive"></div>');
					}

				});
			}
		 });
		 this.ckEditorInstance.on('afterCommandExec', function(e) {
			if (typeof that.myWindow.bootstrapVersion != "undefined" && that.myWindow.bootstrapVersion.indexOf("3")!=0) {
				//console.log("afterCommandExec, e=", e.data.name);
				if ("tableDelete"==e.data.name || "deleteTable"==e.data.name) {
					//odstran prazdne table-responsive div elementy
					$(that.ckEditorInstance.document.$).find("div.table-responsive:empty").remove();
				}
			}
		 });
		this.ckEditorInstance.on('fileUploadRequest', function(e)
		{
			var fileLoader = e.data.fileLoader;
			var formData = new FormData();

			fileLoader.uploadUrl = "/admin/web-pages/upload/?__sfu=0&groupId="+e.editor.element.$.form.groupId.value+"&docId="+e.editor.element.$.form.docId.value+"&title="+encodeURIComponent($("#DTE_Field_title").val());

			fileLoader.xhr.open( 'POST', fileLoader.uploadUrl, true );
			formData.append( 'uploadFile', fileLoader.file, fileLoader.fileName );
			fileLoader.xhr.send( formData );

			e.stop();
		}, null, null, 4);

		//zachytenie CTRL+S/CMD+S
		var editor = this.ckEditorInstance;
		editor.on( 'contentDom', function(e) {
			var editable = editor.editable();
			//console.log("Som contentDown");
		   	editable.attachListener( editable, "keydown", function(evt) {
				//console.log("keydown, evt=", evt);
				var keyEvent = evt.data.$;
				if ((that.myWindow.navigator.platform.match("Mac") ? keyEvent.metaKey : keyEvent.ctrlKey)  && keyEvent.key === 's') {
					//console.log("IFRAME CTRL+S, evt=", evt);

					keyEvent.preventDefault();
					try {
						that.myWindow.top.WJ.dispatchEvent("WJ.DTE.save", {});
					} catch (ex) {}
				}
			});
		});
	}

	translate(key) {
		//that.ckEditorObject.lang[ that.myWindow.userLng ].webjetcomponents.forms.form
		//console.log("translate, key=", key);
		try {
			return this.ckEditorObject.lang[ this.myWindow.userLng ].webjetadmin[key];
		} catch (e) {
			console.log(e);
		}
		return key;
	}

	customizeEditor(options) {
		var that = this;

		that.ckEditorObject.config.justifyClasses = [ 'text-left', 'text-center', 'text-right', 'text-justify' ];
        var imageAlignClasses = ['pull-left image-left', 'pull-right image-right'];

		if (typeof that.ckEditorObject.ckEditorAtLeastOneInitialized == "undefined")
		{
			//console.log("customizeEditor, initializing=", that.ckEditorObject.ckEditorAtLeastOneInitialized, "CKEDITOR=", that.ckEditorObject);

			that.ckEditorObject.aaa = "aaa";
			that.ckEditorObject.on( 'dialogDefinition', function( ev )
			{
				var dialogName = ev.data.name;
				var dialogDefinition = ev.data.definition;

				/*
				console.log("On dialog definition, name="+dialogName);
				console.log("THIS:");
				console.log(this);
				console.log("EV:");
				console.log(ev);
				*/

				dialogDefinition.dialog.on( 'show', function() {
					//::before contain tabler icon, remove X text
					$(".cke_dialog_body > a.cke_dialog_close_button > span.cke_label").text("");
				});

				//console.log("dialogName: "+dialogName);

				if ( dialogName == 'tableProperties' || dialogName == 'table')
				{
					//console.log("tableProperties");

					var infoTab = dialogDefinition.getContents('info');
					infoTab.get('selHeaders')['default'] = 'row';
					infoTab.get('txtWidth')['default'] = '100%';
					var advancedTab = dialogDefinition.getContents('advanced');
					if (typeof that.myWindow.bootstrapVersion == "undefined" || that.myWindow.bootstrapVersion.indexOf("3")==0) {
						advancedTab.get('advCSSClasses')['default'] = 'tabulkaStandard';
					} else {
						advancedTab.get('advCSSClasses')['default'] = 'table table-sm tabulkaStandard';
					}
					dialogDefinition.dialog.on( 'show', function()
					{
						//this.getContentElement("info", "txtSummary");
						//this.getContentElement("info", "txtSummary").getElement().hide();
					});
					dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
						return function()
						{
							var wrapTable = false;
							if (typeof that.myWindow.bootstrapVersion != "undefined" && that.myWindow.bootstrapVersion.indexOf("3")!=0) wrapTable = true;

							var id = null;
							var originalId = null;
							if (wrapTable) {
								//nastav ID na nasu hodnotu
								var advField = this.getContentElement("advanced", "advId");
								id = "table"+(new Date()).getTime();
								originalId = advField.getValue();
								advField.setValue(id);
							}

							original.call(this);

							if (wrapTable) {
								var table = window.getCkEditorInstance().document.$.getElementById(id);
								var $table = $(table);
								var parent = $table.parent("div.table-responsive");

								//console.log("table=", table, "$table=", $table);
								//console.log("this=", this, "parent=", parent);

								if (parent.length == 0)	$table.wrap('<div class="table-responsive"></div>');

								if (originalId != "") $table.attr("id", originalId);
								else $table.removeAttr("id");
							}

							return;
						}
					});
				}

				if ( dialogName == 'image')
				{

					dialogDefinition.minWidth = 800;
					dialogDefinition.minHeight = 460;

					dialogDefinition.addContents(
					{
						id: 'wjImage',
						label: that.translate("images"),
						elements: [
						{
							type: 'html',
							id: 'wjImageIframe',
							html: '<div><iframe id="wjImageIframeElement" style="width: 800px; height: 463px;" src="/admin/v9/files/wj_image/?stop_resizing=true" border="0"/></div>'
						}
						]
					}, 'info');

					var linkTab = dialogDefinition.getContents('Link');
					linkTab.add({
						id: 'txtRel',
						type: 'text',
						requiredContent: 'a[rel]',
						label: that.translate("rel"),
						'default': '',
						setup: function( type, element ) {
							if ( type == 2 )
								this.setValue( element.getAttribute( 'rel' ) || '' );
						},
						commit: function( type, element ) {
							if ( type == 2 ) {
								if ( this.getValue() || this.isChanged() )
									element.setAttribute( 'rel', this.getValue() );
							}
						}
					});

					//boolean pixabay = Constants.getBoolean("pixabayEnabled");
					//console.log("pixabay=", options.constants.pixabayEnabled);

					if (true == options.constants.pixabayEnabled) {
						dialogDefinition.addContents(
						{
							id: 'wjImagePixabay',
							label: that.translate("photobank"),
							elements: [
							{
								type: 'html',
								id: 'wjImagePixabayIframe',
								html: '<div><iframe id="wjImagePixabayIframeElement" style="width: 800px; height: 455px;" src="/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_pixabay.jsp" border="0"/></div>'
							}
							]
						}, 'info');
					}

					//console.log("dialogDefinition=", dialogDefinition);

					dialogDefinition.dialog.on( 'show', function()
					{
						//Need little adjustments
						var cke_dialog_ui_html = this.getContentElement("wjImage", "wjImageIframe").getElement();
						cke_dialog_ui_html.getParent().setStyle("padding", "0px");
						cke_dialog_ui_html.getParent().setStyle("display", "flex");
						cke_dialog_ui_html.getParent().getParent().getParent().getParent().getParent().getParent().setStyle("padding", "0px");

						this.getContentElement("info", "basic").getElement().getParent().setStyle("width", "100%");
						this.getContentElement("info", "txtWidth").getElement().getParent().getParent().getParent().getParent().getParent().getParent().setStyle("padding-right", "0px");
						this.getContentElement("info", "txtWidth").getElement().getParent().getParent().getParent().getParent().getParent().getParent().setStyle("width", "100%");

						this.getContentElement("info", "txtUrl").getElement().hide();
						this.getContentElement("info", "txtAlt").getElement().hide();
						//this.getContentElement("info", "dataName").getElement().hide();
						//this.getContentElement("info", "txtWidth").getElement().hide();
						//this.getContentElement("info", "txtHeight").getElement().hide();
						this.getContentElement("info", "ratioLock").getElement().hide();
						this.getContentElement("info", "txtBorder").getElement().hide();
						this.getContentElement("info", "cmbAlign").getElement().hide();
						//TODO: this.getContentElement("Link").getElement().hide();

						// image alignment classes
						var dialog = this;
						this.getContentElement("info", "cmbAlign").on( 'change', function( changeEvent ) {
							var align = $.trim(this.getValue());
							var classes = dialog.getContentElement("advanced", "txtGenClass").getValue();
							classes = classes.split(" ");

							var imageAlignClassesGrep = imageAlignClasses.join(" ").split(" ");
							//console.log("imageAlignClassesGrep"); console.log(imageAlignClassesGrep);

							classes = jQuery.grep(classes, function( v ) {
								return $.inArray(v, imageAlignClassesGrep) == -1;
							});

							if (align != "") {
								classes.push("image-" + align);
								classes.push("pull-" + align);
							}

							dialog.getContentElement("advanced", "txtGenClass").setValue($.trim(classes.join(" ")));
						});

						this.getContentElement("info", "txtWidth").getElement().on( 'keydown', function( changeEvent ) {
							//ked napise cislo do sirky tak zrusme img-responsive, inak sa hodnota nepouzije
							var classes = dialog.getContentElement("advanced", "txtGenClass").getValue();
							classes = classes.replace("img-responsive", "");
							classes = classes.replace("fixedSize", "");
							classes = classes.replace("img-fluid", "");
							classes = classes.replace("w-100", "");
							dialog.getContentElement("advanced", "txtGenClass").setValue(classes);
						});

						this.getContentElement("info", "txtHeight").getElement().on( 'keydown', function( changeEvent ) {
							//ked napise cislo do sirky tak zrusme img-responsive, inak sa hodnota nepouzije
							var classes = dialog.getContentElement("advanced", "txtGenClass").getValue();
							classes = classes.replace("img-responsive", "");
							classes = classes.replace("fixedSize", "");
							classes = classes.replace("img-fluid", "");
							classes = classes.replace("w-100", "");
							dialog.getContentElement("advanced", "txtGenClass").setValue(classes);
						});

						setTimeout( function()
						{
							//for next open update data
							try
							{
								var wjImageIframe = cke_dialog_ui_html.find("iframe").$[0]; //that.ckEditorObject.document.getById( 'wjImageIframeElement' );
								//console.log("wjImageIframe=", wjImageIframe);
								window.wjImageIframe = wjImageIframe;
								if (wjImageIframe)
								{
									//console.log("INITIALIZED: "+wjImageIframe.$.contentWindow.elFinderInitialized);

									wjImageIframe.contentWindow.refreshValuesFromCk();

									if (wjImageIframe.contentWindow.elFinderInitialized == true) wjImageIframe.contentWindow.updateElfinderCustomData();
								}
							}
							catch (e)
							{
								//toto moze nastat pri prvom nacitani, vtedy sa ale refresh zavola priamo v iframe kode
								//console.log("Error image dialog show, e=", e);
							}
						}, 200);

					});

					dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
						return function()
						{
							var video = ['.mp3', '.mp4', '.flv', 'youtube.com', 'youtu.be', 'vimeo.com', 'facebook.com/facebook/videos/'];
							var txtUrl = this.getContentElement("info", "txtUrl").getValue();

							var videoFile = "";
							$.each(video, function(i,v)
							{
								if (txtUrl.indexOf(v) != -1) {
									videoFile = txtUrl;
									return false;
								}
							});

							if (videoFile != "") {
								this._.editor.wjInsertUpdateComponent("!INCLUDE(/components/video/video_player.jsp, file="+videoFile+")!");
								return;
							}

							//vybral maly obrazok z galerie, treba nastavit odkaz
							if (txtUrl.indexOf("/images")!=-1 && txtUrl.indexOf("/s_")!=-1)
							{
								this.getContentElement("Link", "txtUrl").setValue(txtUrl.replace("/s_", "/"));
								this.getContentElement("Link", "txtRel").setValue("wjimageviewer");
							}

							//pre fixedSize nesmieme nastavit width a height
							var classNames = this.getContentElement("advanced", "txtGenClass").getValue();
							if (classNames != undefined && classNames != null)
							{
								//img-fluid a w-100, w-75 atd su z Bootstrap 4
								if (classNames.indexOf("fixedSize")!=-1 || classNames.indexOf("img-responsive")!=-1 || classNames.indexOf("img-fluid")!=-1 || classNames.indexOf("w-")!=-1 || classNames.indexOf("card-img-top")!=-1)
								{
									if (txtUrl.indexOf(".svg")>0 && classNames.indexOf("fixedSize")!=-1)
									{
										var fixedSizeRegex = /fixedSize-(\d+)-(\d+)(?:-(\d+))?/gi;
										var matched = fixedSizeRegex.exec(classNames);
										//console.log(matched);
										if (matched != null && matched.length == 4)
										{
											var actualWidth = matched[1];
											var actualHeight = matched[2];

											var ip = 0;
											if (matched[3]!=undefined) ip = matched[3];
											//console.log("ip=", ip);
											if ("1"==ip || 1==ip) actualHeight = "";
											else if ("2"==ip || 2==ip) actualWidth = "";

											this.getContentElement("info", "txtWidth").setValue(actualWidth);
											if ("0"==actualWidth || 0==actualWidth) this.getContentElement("info", "txtWidth").setValue("");
											this.getContentElement("info", "txtHeight").setValue(actualHeight);
											if ("0"==actualHeight || 0==actualHeight) this.getContentElement("info", "txtHeight").setValue("");
										}
									}
									else
									{
										this.getContentElement("info", "txtWidth").setValue("");
										this.getContentElement("info", "txtHeight").setValue("");
									}
								}
							}

							if(true == options.constants.editorImageAutoTitle)
							{
								var txtGenTitle = this.getContentElement("advanced", "txtGenTitle").getValue();
								if((txtUrl!=undefined && txtUrl != null) && (txtGenTitle===null || txtGenTitle==""))
								{
									//odpazime cestu k suobru, potom jeho priponu a potom nahradime podciarniky a pomlcky za medzery
									var imageName = txtUrl.replace(/^.*[\\\/]/, '').replace(/\.[^/.]+$/, "").replace(/_|-/g, " ");
									//TODO: toto by malo ist asi zo skupiny sablon
									var projectName = options.lang.projectName;
									if(projectName.length>0)
										projectName = " | " + projectName;
									this.getContentElement("advanced", "txtGenTitle").setValue(imageName + projectName);
								}
							}

							return original.call(this);
						};
					});

				}
				else if ( dialogName == 'link')
				{
					dialogDefinition.minWidth = 800;
					dialogDefinition.minHeight = 445;

					//console.log("dialogDefinition link=", dialogDefinition);

					dialogDefinition.dialog.on( 'show', function()
					{
						var dialog = this;

						this.getContentElement("info", "url").getElement().hide();
						this.getContentElement("info", "protocol").getElement().hide();
						this.getContentElement("info", "linkType").getElement().hide();

						//console.log("ON SHOW 1");
						var urlElement = this.getContentElement("info", "url").getElement();

						var iframeInsertElement = urlElement.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent();
						//console.log("iframeInsertElement=", iframeInsertElement.getParent().$.innerHTML);
						var iframeElement = dialog.iframeElement;
						//console.log("iframeElement=", iframeElement);
						if (typeof iframeElement == "undefined")
						{
							//console.log("Creating iframe");
							var iframeElement = new that.ckEditorObject.dom.element("IFRAME");
							iframeElement.setAttribute("src", "/admin/v9/files/wj_link/?stop_resizing=true");
							iframeElement.setAttribute("id", "wjLinkIframe");
							//iframeElement.setAttribute("width", 580);
							iframeElement.setStyle("width", 800+"px");
							//iframeElement.setAttribute("height", 445);
							iframeElement.setStyle("height", 455+"px");
							iframeElement.setStyle("margin-left", "-12px");
							iframeElement.setStyle("margin-right", "-12px");

							dialog.iframeElement = iframeElement;

							//
							var tabpanel = this.getContentElement("info", "linkDisplayText").getElement().getParent().getParent().getParent().getParent().getParent();
							tabpanel.setStyle("min-height", "auto");
							tabpanel.setStyle("padding-right", "0px");
							tabpanel.setStyle("display", "inline");
							tabpanel.getParent().setStyle("padding", "0px");

							//urlElement.$.insertBefore(iframeElement, .$);
							iframeInsertElement.insertBeforeMe(iframeElement);
							//schovaj celu tabulku lebo ma paddingy a pod URL fieldom sa zobrazuje sedy pas
							iframeInsertElement.hide();
						}

						//console.log("SHOW EVENT: "+urlElement);
						setTimeout( function()
						{
							try
							{
								//console.log("ON SHOW");

								//var wjLinkIframe = that.ckEditorObject.document.getById( 'wjLinkIframe' );
								var wjLinkIframe = dialog.iframeElement;
								if (wjLinkIframe && wjLinkIframe.$)
								{
									//console.log("Idem updatnut, wjLinkIframe=", wjLinkIframe);

									var phone = dialog.getContentElement("info", "telNumber").getValue();
									//console.log("phone=", phone);
									if (phone != "") {
										dialog.getContentElement("info", "url").setValue("tel:"+phone);
									}

									wjLinkIframe.$.contentWindow.refreshValuesFromCk();

									if (wjLinkIframe.$.contentWindow.elFinderInitialized == true) wjLinkIframe.$.contentWindow.updateElfinderCustomData();

									//console.log("Updatnute");
								}
							}
							catch (e)
							{
								//toto moze nastat pri prvom nacitani, vtedy sa ale refresh zavola priamo v iframe kode
								//console.log(e);
							}
						}, 200);

					});

					dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
						return function()
						{
							//prepneme na URL typ inak nam to nesetne dobre z kotvy
							this.getContentElement("info", "linkType").setValue("url");
							var url = this.getContentElement("info", "url").getValue();
							//kontrolujeme / lebo tiktok ma @ v URL, cize www.tiktok.com/@username
							if (url.indexOf("@")!=-1 && url.indexOf("mailto:")==-1 && url.indexOf("/")==-1)
							{
								url = "mailto:"+url;
								this.getContentElement("info", "url").setValue(url);
							}

							//console.log("url=", url);

							if (url.indexOf("tel:")==0 || (url.indexOf("://")!=-1 && url.indexOf("http")==-1)) {
								//console.log("Setting protocol to none");
								this.getContentElement("info", "protocol").setValue("");
							}

							//ak ako URL je zadane #nazov vytvori sa kotva
							if (url.indexOf("#")==0)
							{
								var editor = ckEditorInstance;
								var sel = editor.getSelection(),
								range = sel && sel.getRanges()[ 0 ];

								var name = url.substring(1);
								var attributes = {
									id: name,
									name: name,
									'data-cke-saved-name': name
								};

								// Empty anchor
								if ( range.collapsed )
								{
									var anchor = editor.createFakeElement( editor.document.createElement( 'a', {
										attributes: attributes
									} ), 'cke_anchor', 'anchor' );
									range.insertNode( anchor );
									return;
								}
							}

							return original.call(this);
						}
					});

				}
				else if ( dialogName == 'form')
				{
					dialogDefinition.minWidth = 600;
					dialogDefinition.minHeight = 445;

					dialogDefinition.addContents(
					{
						id: 'wjFormAttributes',
						label: ev.editor.lang.webjetcomponents.forms.formAttributes,
						elements: [
							{
							type: 'html',
							id: 'wjFormAttributesIframe',
							//html: '<div><iframe id="wjFormAttributesIframeElement" style="width: 600px; height: 455px;" src="/components/form/admin_form_attributes.jsp" border="0"/></div>'
							html: '<div><div id="attributesContent" style="height: 425px; overflow: auto;">'+that.translate("waitPlease")+'</div></div>'
						}
						]
					});

					dialogDefinition.addContents(
					{
						id: 'wjFormRestrictions',
						label: ev.editor.lang.webjetcomponents.forms.fileLimits,
						elements: [
							{
							type: 'html',
							id: 'wjFormRestrictionsIframe',
							//html: '<div><iframe id="wjFormAttributesIframeElement" style="width: 600px; height: 455px;" src="/components/form/admin_form_attributes.jsp" border="0"/></div>'
							html: '<div><div id="restrictionsContent" style="height: 150px; width: 670px; padding-top: 16px;">'+that.translate("waitPlease")+'</div></div>'
						}
						]
					});

					var infoTab = dialogDefinition.getContents( 'info' );
					infoTab.add( {
						type: 'text',
						label: ev.editor.lang.common.name,
						id: 'wjFormName'
					}, "txtName");

					dialogDefinition.dialog.on( 'show', function()
					{
						//console.log("ON SHOW 2");
						//console.log(this.getContentElement("info", "action").getElement());
						//console.log(this.getContentElement("info", "action").getValue());

						this.getContentElement("info", "txtName").getElement().hide();
						this.getContentElement("info", "action").getElement().hide();

						var path = this.getParentEditor().elementPath();
						var form = path.contains( 'form', 1 );

						//console.log(path);
						//console.log(form);

						if (this.getContentElement("info", "action").getValue() == "")
						{
							this.getContentElement("info", "action").setValue("/formmail.do");
						}
						if (this.getContentElement("info", "txtName").getValue() == "")
						{
							this.getContentElement("info", "txtName").setValue("formMailForm");
						}
						if (this.getContentElement("info", "method").getValue() == "")
						{
							this.getContentElement("info", "method").setValue("post");
						}

						//console.log("form=", ev.editor.element.$.form);
						//console.log("title=", $("#"+that.datatable.DATA.id+"_modal #DTE_Field_title"));
						var wjFormName = $("#"+that.datatable.DATA.id+"_modal #DTE_Field_title").val();

						if (form != null)
						{
							var formAction = form.getAttribute("action");
							var formMethod = form.getAttribute("method");

							if (formAction.indexOf("formmail.do")!=-1)
						{
								var index = formAction.indexOf("?");
								if (index>0)
								{
									var params = formAction.substring(index+1);
									var paramsArray = params.split("&");
									var i;
									for (i=0; i < paramsArray.length; i++)
									{
										index = paramsArray[i].indexOf("=");
										if (index!=-1)
										{
										var name = paramsArray[i].substring(0, index);
										var value = paramsArray[i].substring(index+1);
										value = WJ.unencodeValue(value);

										if (name=="savedb")
										{
											wjFormName = value;
										}
										}
									}
								}
						}
						}

						//odstran diakritiku a medzery z wjformname
						wjFormName = WJ.removeChars(WJ.internationalToEnglish(wjFormName));

						this.getContentElement("info", "wjFormName").setValue( wjFormName );

						try
						{

							var input = {hasUserApproved:'true',formName:WJ.encodeValue(wjFormName)}
							$.ajax({
								type: "POST",
								url: "/components/form/admin_gdpr_check-ajax.jsp",
								data: input,
								success: async function(data){
									data = $.trim(data);
									if(data.indexOf('true') == 0)
									{
										$("#attributesContent").load("/components/form/admin_form_attributes.jsp?formname="+ WJ.encodeValue(wjFormName));
										$("#restrictionsContent").load("/components/form/admin_file_restriction_form.jsp?formname="+WJ.encodeValue(wjFormName));
									}
									else
									{
										var message = await WJ.translate("components.forms.alert.gdpr");
										if(confirm(message))//pratest
										{
											$.post( "/components/form/admin_gdpr_check-ajax.jsp?addUserApprove=true&formName="+WJ.encodeValue(wjFormName), function( data ) {
												data = $.trim(data);
												if(data.indexOf('true') == 0)
												{
													//alert('ok');
												}
											});
											$("#attributesContent").load("/components/form/admin_form_attributes.jsp?formname="+ WJ.encodeValue(wjFormName));
											$("#restrictionsContent").load("/components/form/admin_file_restriction_form.jsp?formname="+WJ.encodeValue(wjFormName));

										}
										else
										{
											setTimeout(function(){
											dialogDefinition.dialog.hide()
											}, 1);
										}
									}
								}
							});
						}
						catch (e)
						{
							//toto moze nastat pri prvom nacitani, vtedy sa ale refresh zavola priamo v iframe kode
							console.log(e);
						}

						//this.getContentElement("info", "txtName").getElement().hide();
					});

					dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
						return function()
						{
							var formName = this.getContentElement("info", "wjFormName").getValue();
							formName = WJ.removeChars(WJ.internationalToEnglish(formName));
							this.getContentElement("info", "wjFormName").setValue(formName);

							if (this.getContentElement("info", "method").getValue() == "")
							{
								this.getContentElement("info", "method").setValue("post");
							}
							var attribues = this;

							var input = {hasUserApproved:'true',formName:formName}

							var thisDialog = this;

							$.ajax({
								type: "POST",
								url: "/components/form/admin_gdpr_check-ajax.jsp",
								data: input,
								success: async function(data){
									data = $.trim(data);
									if(data.indexOf('true') == 0)
									{
										//return true;
										saveAttributes(attribues);//this

										var formAction = thisDialog.getContentElement("info", "action").getValue();
										if (formAction.indexOf("/formmail.do")!=-1)
										{
											formAction = "/formmail.do?savedb=" + formName;
											thisDialog.getContentElement("info", "action").setValue(formAction);

											original.call(thisDialog);

											dialogDefinition.dialog.hide()
										}
									}
									else
									{
										var message = await WJ.translate("components.forms.alert.gdpr");
										if(confirm(message))//pratest
										{
											$.post( "/components/form/admin_gdpr_check-ajax.jsp?addUserApprove=true&formName="+formName, function( data ) {
												data = $.trim(data);
												if(data.indexOf('true') == 0)
												{
													//alert('ok');
													saveAttributes(attribues);//this

													var formAction = thisDialog.getContentElement("info", "action").getValue();
													if (formAction.indexOf("/formmail.do")!=-1)
													{
														formAction = "/formmail.do?savedb=" + formName;
														thisDialog.getContentElement("info", "action").setValue(formAction);
													}

													original.call(thisDialog);

													dialogDefinition.dialog.hide()
												}
											});
										}
										else
										{
											setTimeout(function(){
												dialogDefinition.dialog.hide()
											}, 1);
										}
									}
								}
							});

							return false;
						};
					});
				}
				else if ( dialogName == 'textfield' || dialogName == 'textarea' || dialogName == 'radio' || dialogName == 'checkbox' || dialogName == 'select')
				{
					var infoTab = dialogDefinition.getContents( 'info' );

					if (infoTab != null)
					{
						var infoTab = dialogDefinition.getContents( 'info' );
						infoTab.add( {
							type: 'text',
							label: that.translate("title"),
							id: 'data-name'
						}, '_cke_saved_name');

						/**/
						infoTab.add({
							type: 'text',
							id: 'id'
						});
						/**/

						infoTab.remove( 'required' );

						infoTab.add( {
							id: 'wjrequired',
							type: 'checkbox',
							label: ev.editor.lang.webjetcomponents.forms.required,
							'default': '',
							accessKey: 'Q',
							value: 'required',
							setup: function( element )
							{
								if (element.getAttribute && element.getAttribute( 'class' ) != null && element.getAttribute( 'class' ).indexOf("required")!=-1 ) this.setValue("required");
								else this.setValue("");
							},
							commit: function( data )
							{
								//console.log("COMMIT na checkboxe");

								var element = data.element;
								//toto plati pre textareu
								if (typeof element === 'undefined')
								{
									element = data.$;
								}

								var dialog = that.ckEditorObject.dialog.getCurrent();
								var required = this.getValue();
								var requiredType = "";
								try
								{
									requiredType = dialog.getValueOf("info", "requiredType");
								} catch (e) {}

								var classValue = requiredType;
								if (required == true)
								{
									if (classValue != "") classValue += " ";
									classValue += "required";
								}

								//console.log("commit required, classValue="+classValue+" element="+element+" current:"+element.getAttribute("class"));
								var currentClasses = element.getAttribute("class");
								if (currentClasses!=null && currentClasses!="")
								{
									var currentArray = currentClasses.split(" ");
									//console.log(currentArray);

									var checkFormNames = new Array();
									checkFormNames.push("required");
									let regexps = that.regexps;
									for (let i=0; i<regexps.length; i++) {
										checkFormNames.push(regexps[i].value);
									}

									//console.log("checkFormNames:", checkFormNames);

									var isCheckFormClass = false;
									for (var i=0; i<currentArray.length; i++)
									{
										//odstranime len CSS triedy checkformu, ostatne ponechame
										isCheckFormClass = false;
										for (var j=0; j<checkFormNames.length; j++) {
											//console.log(currentArray[i]+":"+checkFormNames[j]+";");
											if (currentArray[i] == checkFormNames[j]) {
												//console.log("================== isCheckFormClass --------")
												isCheckFormClass = true;
												break;
											}
										}
										if (isCheckFormClass == false)
										{
											//console.log("Pridavam classValue "+currentArray[i]);
											classValue = classValue + " " + currentArray[i];
										}
									}
								}
								//console.log("SETTING classValue:"+classValue);
								if (element) element.setAttribute("class", classValue);
							}
						});

						//vyber typu povinneho pola je len pre text polia
						//console.log("dialogName="+dialogName);
						if ( dialogName == 'textfield' || dialogName == 'textarea')
						{
							let items = [[ '',	'' ]];
							let regexps = that.regexps;
							for (let i=0; i<regexps.length; i++) {
								items.push([regexps[i].value+" - "+regexps[i].label, regexps[i].value]);
							}

							infoTab.add( {
								id: 'requiredType',
								type: 'select',
								label: ev.editor.lang.webjetcomponents.forms.requiredType,
								'default': 'text',
								accessKey: 'M',
								items: items,
								setup: function( element )
								{
									//this.setValue( element.getAttribute( 'type' ) );
									if (element.getAttribute && element.getAttribute( 'class' ) != null)
									{
										//console.log(this.items);
										var elClass = element.getAttribute( 'class' ).split(" ");
										var i = 0;
										for (i=0; i< elClass.length; i++)
										{
											if (elClass[i]=="required") continue;
											for (var j=0; j<this.items.length; j++)
											{
												//console.log("comparing "+elClass[i]+":"+this.items[j][1]+";");
												if (elClass[i]==this.items[j][1]) this.setValue(elClass[i]);
											}

										}
									}
								}
							});
						}

						dialogDefinition.dialog.on('show', function(event)
						{
							var savedNameElement = this.getContentElement("info", "_cke_saved_name");
							var idElement = this.getContentElement("info", "id");

							if (typeof savedNameElement != "undefined") {
									$(savedNameElement.getElement().$).parent('td').hide()
							}

							if (typeof idElement != "undefined") {
								$(idElement.getElement().$).parent('td').hide()
							}

							var $this = this;

							// hodnotu z '_cke_saved_name' sa nedarilo nacitat bez timeoutu
							setTimeout(function(){
								setName($this);
							}, 1);

							function setName($this)
							{
								var value = $this.getContentElement("info", "data-name").getValue();

								if (value == "")
								{
									//skus najst element podla ID a z neho data element, lebo CK nam pre select vracia clear namiesto elementu
									var nameCleared = $this.getContentElement("info", "_cke_saved_name");
									if (nameCleared != "")
									{
										var editor = $this._.editor;
										//toto je element na ktory sa doubleclicklo (je selectnuty)
										var element = editor.getSelection().getStartElement();

										//console.log("MAM ELEMENT: ")
										//console.log(element);
										if (element)
										{
											value = element.data("name");

											//lebo select sa zle renderuje
											if (element.hasClass("required"))
											{
												$this.getContentElement("info", "wjrequired").setValue("true");
											}
										}
									}
								}

								//console.log("setName, value 1="+value);

								var savedNameElement = $this.getContentElement("info", "_cke_saved_name");

								//console.log(savedNameElement);

								if ((value == null || value == "") && typeof savedNameElement != "undefined") {
									value = savedNameElement.getValue();
								}

								//console.log("setName, value="+value);

								$this.getContentElement("info", "data-name").setValue(value);
							}
						});
					}

					dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
						return function()
						{
							var savedNameElement = this.getContentElement("info", "_cke_saved_name");
							var oldElement = this.getParentEditor().getSelection().getSelectedElement();
							//console.log("oldElement="+oldElement);

							var prevName = "";
							if (oldElement != null)
							{
							prevName = $(oldElement.$).prop("id");
							//console.log("prevName 1="+prevName);
							}

							if (typeof savedNameElement == "undefined")
							{
								var result = original.call(this);
								return result;
							}

							var name = this.getContentElement("info", "data-name").getValue();

							//console.log(name);

							if (name == "" && typeof this.getContentElement("info", "name") != "undefined") {
								name = this.getContentElement("info", "name").getValue();
							}

							var nameCleared = WJ.internationalToEnglish(name).toLowerCase();
							var idElement = this.getContentElement("info", "id");

							if (typeof savedNameElement != "undefined") {
								savedNameElement.setValue(nameCleared);
							}

							if (typeof idElement != "undefined") {
								idElement.setValue(nameCleared);
							}

							//----- zavolajme vytvorenie input pola ------
							var result = original.call(this);

							var editor = this._.editor;
							var startElement = editor.getSelection().getStartElement();

							//console.log(startElement);
							//console.log("prevName="+prevName);

							var tagName = "input";
							if (dialogName == 'select') tagName = "select";
							if (dialogName == 'textarea') tagName = "textarea";
							var element = $(startElement.$).find(tagName+'[name="' + nameCleared + '"]');
							if (element.length == 0) element = $(startElement.$).find(tagName+'[id="' + nameCleared + '"]');
							if (element.length == 0) element = $(startElement.$).find(tagName+'[data-cke-saved-name="' + nameCleared + '"]');

							//console.log("element.length="+element.length);
							//console.log(element);

							if (element.length == 0) {
								element = $(startElement.$);
							}

							//console.log("ID="+element.attr("id"));
							element.attr("id", nameCleared);
							//console.log("Setting data-name to: "+name);
							element.attr("data-name", name);

							if (dialogName == 'select')
							{
								//selectu sa zle nastavuje required
								var required = this.getContentElement("info", "wjrequired").getValue();
								//console.log("required="+required);
								var classValue = "";
								if (required == true)
								{
									if (classValue != "") classValue += " ";
									classValue += "required";
								}
								//console.log("commit required, classValue="+classValue+" element="+element);
								element.attr("class", classValue);
							}


							if (element.is(':radio, :checkbox')) {
								name = element.val();
							}

							if (oldElement==null) bootstrapWrapField(element, name);
							insertLabel(element, prevName, name);

							return result;
						};
					});

					function bootstrapWrapField(element, text) {
						if (typeof that.myWindow.bootstrapVersion == "undefined" || that.myWindow.bootstrapVersion.indexOf("3")==0) return;

						//console.log("bootstrapWrapField, element=", element);
						that.myWindow.bsElement = element;
						var $el = $(element);
						var id = element.prop('id');

						//ak je parent TD, tak neries bs wrapping
						var parents = $el.closest("TD,TH,FORM");
						//console.log("parents=", parents);
						if (parents.length>0 && (parents[0].tagName=="TD" || parents[0].tagName=="TH")) return;

						var wrapperClassName = "form-group";
						var elementClassName = "form-control";
						var inputType = $el.prop("type");
						var label = null;
						if (typeof inputType!="undefined") {
							inputType = inputType.toLowerCase();
							if ("radio"==inputType || "checkbox"==inputType) {
								wrapperClassName = "form-check";
								elementClassName = "form-check-input";

								//priprav label za element
								label = $('<label>' + text + '</label>');
								label.prop("for", id);
								label.addClass("form-check-label");
							}
						}
						var wrapperHtml = '<div class="'+wrapperClassName+'"></div>';
						if ("FORM" == $el.parent().prop("tagName")) {
							//console.log("wrapping element");
							$el.wrap(wrapperHtml);
						} else if ("P" == $el.parent().prop("tagName")) {
							//console.log("changing from P to DIV");
							$el.unwrap().wrap(wrapperHtml);
						}
						$el.addClass(elementClassName);
						if (label != null) label.insertAfter(element);
					}

					function getCurrentLabel(element, prevName, id)
					{
						//console.log("Searching for label id=", id, " closest=", element.closest('form').html(), "find=", element.closest('form').find("label[for='" + id + "']"));
						var label = null;
						element.closest('form').find("label").each(function() {
							var forId = this.getAttribute("for");
							//console.log("Comparing label forId=", forId, "prevName=", prevName, "id=", id);
							if (typeof forId != "undefined" && forId != null && forId!="" && (id==forId || prevName==forId)) {
								//console.log("Exists, returning");
								label = $(this);
								return false;
							}
						});
						return label;
					}

					function insertLabel(element, prevName, text)
					{
						if (element.length == 0) {
							console.warn("Element can not be empty");
						}

						//console.log(element.attr("placeholder"));

						//ak mame placeholder, tak setni ten, nevytvaraj label
						var placeholder = element.attr("placeholder");
						if (placeholder)
						{
							element.attr("placeholder", text);
							return;
						}

						var id = element.prop('id');
						var label = getCurrentLabel(element, prevName, id);
						//console.log("Current label: "+label);
						var hasPrevTd = element.parent('td').length > 0 && element.parent('td').prev('td').length > 0;
						var hasNextTd = element.parent('td').length > 0 && element.parent('td').next('td').length > 0;

						//console.log("label="+label+" id="+id);

						if (label == null) {
							label = $('<label>' + text + '</label>', {'for': id});

							if (hasPrevTd) {
								label.prependTo(element.parent('td').prev('td'));
							}
							else if (hasNextTd) {
								label.insertBefore(element);
								element.detach().appendTo(label.parent('td').next('td'));
								element.prev("br").remove();
							}
							else {
								//console.log("Inserting label before element", element);
								label.insertBefore(element);
							}
						}

						label.prop("for", id);
						//console.log("Setting label text to: "+text);
						label.text(text);
					}

					function CreateElement(tag, atrs)
					{
						var element = that.ckEditorObject.document.createElement(tag);

						$.each(atrs, function(k,v){
							if (k == "value") {
								element.setHtml(v);
							}
							else {
								element.setAttribute(k,v);
							}
						});

						return element;
					}
				}
				else if (dialogName == 'button')
				{
					var label = that.translate("btnSendByAjax");

					var infoTab = dialogDefinition.getContents( 'info' );
					infoTab.add( {
						type: 'checkbox',
						label: label,
						id: 'sendAjax'
					});

					var typeSelectBox = infoTab.get('type');
					if (typeSelectBox.items.length==3)
					{
						typeSelectBox.items[3] = [that.translate("printPage"), "button"];
					}

					var onclickAjax = "return invokeWJAjax('formMailForm', 'ajaxFormResultContainer', 'bSubmit', '/FormMailAjax.action');";
					var onclickPrint = "window.print();"
					dialogDefinition.dialog.on('show', function(event)
					{
						var element = this.getSelectedElement();
						if (element != null && element.getAttribute('data-cke-pa-onclick') != null) {
							var hasOnclick = element.getAttribute('data-cke-pa-onclick').indexOf(onclickAjax) != -1;
							this.getContentElement("info", "sendAjax").setValue(hasOnclick);

							if (element.getAttribute('data-cke-pa-onclick').indexOf(onclickPrint) != -1)
							{
								var infoType = document.getElementById(this.getContentElement("info", "type")._.inputId);
								setTimeout(function() { infoType.selectedIndex = 3; }, 300);
							}
						}

						var dialog = dialogDefinition.dialog,
						nameElement = dialog.getContentElement('info', 'name'),
				typeElement = dialog.getContentElement('info', 'type'),
						textElement = dialog.getContentElement('info', 'value');

					if (textElement.getValue() == '') {
							textElement.setValue(that.translate("btnSubmit"));
							typeElement.setValue("submit");
							nameElement.setValue("bSubmit");
					}
					});

					dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
						return function()
						{
							var value = this.getContentElement("info", "sendAjax").getValue();
							var element = this.getSelectedElement();

							var onclick = onclickAjax;
							var typeSelectedIndex = document.getElementById(this.getContentElement("info", "type")._.inputId).selectedIndex;
							if (typeSelectedIndex==3)
							{
								onclick = onclickPrint;
								value = true;
								if (this.getContentElement('info', 'name').getValue()=="bSubmit") this.getContentElement('info', 'name').setValue("bPrint");
								if (this.getContentElement('info', 'value').getValue()==that.translate("btnSubmit")) this.getContentElement('info', 'value').setValue(that.translate("printPage"));
							}

							if (value && element != null)
							{
								element.setAttribute('data-cke-pa-onclick', onclick);
							}
							else
							{
								if (element != null)
								{
									var onclickValue = element.getAttribute('data-cke-pa-onclick');

									if (onclickValue != null) {
										value = $.trim(onclickValue.replace(onclick, ''));

										if (value != "") {
											element.setAttribute('data-cke-pa-onclick', value);
										}
										else {
											element.removeAttribute('data-cke-pa-onclick');
										}
									}
								}
							}

							var editor = this.getParentEditor();
							element = this.button;
							var isInsertMode = !element;

							var fake = element ? that.ckEditorObject.htmlParser.fragment.fromHtml( element.getOuterHtml() ).children[ 0 ] : new that.ckEditorObject.htmlParser.element( 'input' );
							this.commitContent( fake );

							var writer = new that.ckEditorObject.htmlParser.basicWriter();
							fake.writeHtml( writer );
							var newElement = that.ckEditorObject.dom.element.createFromHtml( writer.getHtml(), editor.document );

							if ( isInsertMode )
								editor.insertElement( newElement );
							else {
								newElement.replace( element );
								editor.getSelection().selectElement( newElement );
							}

							if (value && newElement != null)
							{
								newElement.setAttribute('data-cke-pa-onclick', onclick);
							}

							newElement.setAttribute("class", "btn btn-primary")
						};
					});
				}
				else if ( dialogName == 'bulletedListStyle')
				{
					dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
						return function()
						{
							//console.log("List style DONE");

							var editor = this.getParentEditor();

							var element = null; // this.getListElement( editor, 'ul' );
							var range;
							try {
								range = editor.getSelection().getRanges()[ 0 ];

								range.shrink( that.ckEditorObject.SHRINK_TEXT );
								element = editor.elementPath( range.getCommonAncestor() ).contains( 'ul', 1 );

								element && this.commitContent( element );

								//console.log(element);
							} catch ( e ) {

							}

							if (element != null)
							{
								//nastav class podla zvoleneho typu, aby sa dalo lepsie stylovat
								element.removeClass("circle");
								element.removeClass("disc");
								element.removeClass("square");
								var type = this.getContentElement("info", "type").getValue();
								if (type != "") element.addClass(type);
							}

							return original.call(this);
						};
					});
				}

				/*
					//toto uz netreba, bolo potrebne len pre iframe verziu
					dialogDefinition.dialog.on('show', function(event) {
						expandIframeOnShow();
					});

					dialogDefinition.dialog.on('hide', function(event) {
						compressIframeOnHide();
					});

					dialogDefinition.dialog.on('close', function(event) {
						compressIframeOnHide();
					});

					if (typeof dialogDefinition.onOk != "undefined") {
						dialogDefinition.onOk = that.ckEditorObject.tools.override(dialogDefinition.onOk, function(original) {
							return function(){
								//console.log('onOk');
								compressIframeOnHide();
								return original.call(this);
							}
						});
					}

					if (typeof dialogDefinition.onHide != "undefined") {
						dialogDefinition.onHide = that.ckEditorObject.tools.override(dialogDefinition.onHide, function(original) {
							return function(){
								//console.log('onHide');
								compressIframeOnHide();
								return original.call(this);
							}
						});
					}

					if (typeof dialogDefinition.onCancel != "undefined") {
						dialogDefinition.onCancel = that.ckEditorObject.tools.override(dialogDefinition.onCancel, function(original) {
							return function(){
								//console.log('onCancel');
								compressIframeOnHide();
								return original.call(this);
							}
						});
					}
				*/
			});
		}
		that.ckEditorObject.ckEditorAtLeastOneInitialized = true;
	}

    initializeCkEditorImpl(ckEditorElementId, ckEditorInitFunction, configLink, options)
    {
		var that = this;
		//setneme len contents.css aby nam neloadlo defaultne /css/page.css,
		//az ked sa loadne stranka sa natiahnu potrebne CSS podla sablony, pre init to nepotrebujeme
		that.myWindow.webjetContentsCss = [
		   '/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/samples/contents.css'
		];

	    //var editorElem = document.getElementById("trEditor");

        that.ckEditorObject.dtd.$removeEmpty['i'] = false;
        that.ckEditorObject.dtd.$removeEmpty['span'] = false;
        that.ckEditorObject.dtd.$removeEmpty['div'] = false;
        that.ckEditorObject.dtd.$removeEmpty['section'] = false;
        that.ckEditorObject.dtd.a.div = 1;
        that.ckEditorObject.dtd.a.h1 = 1;
        that.ckEditorObject.dtd.a.h2 = 1;
        that.ckEditorObject.dtd.a.h3 = 1;
        that.ckEditorObject.dtd.a.h4 = 1;
        that.ckEditorObject.dtd.a.h5 = 1;
        that.ckEditorObject.dtd.a.h6 = 1;
        that.ckEditorObject.dtd.a.ul = 1;

        //sem nam to nastavi editor.jsp pre popup okno
        var webjetContentsCss = that.myWindow.webjetContentsCss;
        if (webjetContentsCss == undefined)
        {
            webjetContentsCss = ['/css/page.css', '/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/samples/contents.css']
        }

        this.ckEditorInstance = ckEditorInitFunction(ckEditorElementId, {

			uploadUrl: '/admin/web-pages/upload/?__sfu=0',

			contentsCss: webjetContentsCss,

			allowedContent: true,

			floatSpacePinnedOffsetY: 50,

			customConfig: configLink,

			language: that.myWindow.userLng,

			on:
			{
				// maximize the editor on startup
				'instanceReady' : function( evt )
				{
					//console.log("instanceReady");
					//FCKeditor_OnComplete();
					if (options.hasOwnProperty("onReady")) {
						//console.log("instanceReady callback");
						options.onReady(that.ckEditorInstance);
					}

				//console.log("Setting resize interval, that=", that);
				that.myWindow.setTimeout(function() { that.resizeEditor(that); }, 100);
				that.myWindow.setInterval(function() { that.resizeEditor(that); }, 3000);
				},

				'getData' : function(e)
				{
					//console.log("getData, e=", e);
					var data = e.editor.getData(true);
					data = data.replace(/<article>/gi, '');
					data = data.replace(/<\/article>/gi, '');
					data = data.replace(/&lt;article&gt;/gi, '');
					data = data.replace(/&lt;\/article&gt;/gi, '');
					e.data.dataValue = data;
					//console.log("Vysledne data=", data);
				},

				'setData' : function(e)
				{
					//console.log("setData, e=", e);
					if (typeof e.data.dataValue == "undefined") {
						//ked je to standalone nemame dataValue, nastavujeme podla aktualnej hodnoty HTML kodu editora
						e.data.dataValue = e.editor.getData(true);
					}
					var data = e.data.dataValue;
					if (data == null) data = "";
					data = data.replace(/(!INCLUDE\((.*?)\)!)/gi, '<article>$1</article>');
					data = data.replace(/<article><article>/gi, '<article>');
					data = data.replace(/<\/article><\/article>/gi, '</article>');
					e.data.dataValue = data;
					//console.log("Vysledne data=", data);
				},

				afterPasteFromWord: function( evt ) {
					//POZOR: pri zmene uprav aj kod vo webpages-common.js.jsp, ktory sa pouziva v PB

					var filter = new that.ckEditorObject.filter({
						$1: {
							// Use the ability to specify elements as an object.
							elements: that.ckEditorObject.dtd,
							attributes: true,
							styles: false,
							classes: true
						}
					}),
					fragment = that.ckEditorObject.htmlParser.fragment.fromHtml( evt.data.dataValue ),
					writer = new that.ckEditorObject.htmlParser.basicWriter();

					//console.log("html1=", evt.data.dataValue);
					//console.log("fragment 2: ", fragment);

					//filter.allow("*{*}");

					try {
						//disable colgroup on tables
						filter.allowedContent[0].elements.table.colgroup = 0;
					} catch (e) {}

					filter.disallow( 'table[width]' );
					filter.disallow( 'table[height]' );
					filter.disallow( 'table[border]' );
					filter.disallow( 'td(*)' ); //all class on TD
					filter.disallow( 'span' );
					filter.disallow( 'col[width]' );
					filter.disabled = false;

					filter.addTransformations([
						[
							{
								element: 'table',
								left: function( el ) {
									return true;
								},
								right: function( el, tools ) {
									el.classes = ["table", "table-sm", "tabulkaStandard"];
								}
							}
						]
					]);

					//console.log("filter=", filter);

					filter.applyTo( fragment );
					fragment.writeHtml( writer );

					var html = writer.getHtml();
					html = html.replace(/<table/gi, "<div class=\"table-responsive\"><table style=\"width: 100%\" ");
					html = html.replace(/<\/table>/gi, "</table></div>");

					html = html.replace(/<b>/gi, "<strong>");
					html = html.replace(/<\/b>/gi, "</strong>");

					html = html.replace(/<i>/gi, "<em>");
					html = html.replace(/<\/i>/gi, "</em>");

					evt.data.dataValue = html;

					//console.log("html2=", evt.data.dataValue);
				}
	      }
		});

		var instance = this.ckEditorInstance;
		that.myWindow.getCkEditorInstance = function() {
			//console.log("getCkEditorInstance, instance=", instance, "win instance=", that.myWindow.ckEditorInstance, "this.ckEditorInstance=", that.ckEditorInstance);
			//return last instance for pagebuilder mode
			return that.myWindow.ckEditorInstance;
		}
		that.myWindow.ckEditorInstance = instance;
	}

	setJson(json) {
		this.json = json;
		//undefined je json ked sa nacita zoznam a da sa zmazat stranka
		if (typeof json != "undefined") {
			this.setCssStyle();
			this.setFormData();
			this.setData(json.data);
			this.showEditorNote();
			this.setStyleComboList(this.json.editorFields.styleComboList);

			//toto musi byt posledne, inak sa zle nacitaval obsah stranky
			this.setEditingMode(json);
		}
		setTimeout(() => {
			this.resizeEditor(this);
		}, 1000);
	}

	/**
	 * Aktualizuje zoznam CSS suborov v editore podla sablony, ta je priamo v JSON objekte
	 */
	setCssStyle() {
		var newCssStyles = new Array();
		//zachovaj /admin CSS styly z pluginov a pridaj tie zo sablony
		for (var style of this.ckEditorInstance.config.contentsCss) {
			//console.log("current style=", style);
			if (style.indexOf("/admin")==0 && style.indexOf("standalone.css")==-1) newCssStyles.push(style);
		}
		//console.log("newCssStyles=", newCssStyles);

		//toto nastane pri standalone verzii
		if(typeof this.datatable.DATA.json.options.tempId == "undefined") {
			newCssStyles.push("/admin/skins/webjet8/ckeditor/standalone.css");
			this.ckEditorInstance.config.contentsCss = newCssStyles;
			return;
		}

		//console.log("this.json.options=", this.datatable.DATA.json);
		for (var template of this.datatable.DATA.json.options.tempId) {
			//console.log("Testing temp: ", template.label, "id=", template.original.tempId, " json.tempId=", this.json.tempId);
			//console.log("template=", template, "json=", this.json);
			if (template.original.tempId == this.json.tempId) {
				//console.log("CSS=", template.original.baseCssPath, " current=", this.ckEditorInstance.config.contentsCss);

				var templateCssStyles = (template.original.baseCssPath + "," + template.original.css).split(/[,\n]/);
				for (var style of templateCssStyles) {
					//console.log("temp style=", style);
					if (style != "") {
						if (style.indexOf("/")!=0) style = this.json.editorFields.baseCssLink + "/" + style;
						newCssStyles.push(style);
					}
				}

				//console.log("newCssStyles=", newCssStyles);

				this.ckEditorInstance.config.contentsCss = newCssStyles;
				break;
			}
		}
	}

	/**
	 * Nastavi polia formularu editorForm podla JSON objektu, primarne sa jedna o nastavenie docId, groupId a podobne
	 */
	setFormData() {
		for (var element of this.ckEditorInstance.element.$.form.elements) {
			if (element.name == "data") continue;
			element.value = this.json[element.name];
		}
	}

	setData(data) {
		//console.log("Set data, instance=", this.ckEditorInstance, "data=", data);
		//WARNING: this property is not YET set, do not count on it: if ("pageBuilder"===this.editingMode) {
		this.ckEditorInstance.setData(data);
	}

	getData() {
		//console.log("getData, data=", data, "this=", this);
		let htmlCode = this.ckEditorInstance.getData();
		if ("pageBuilder"===this.editingMode) {
			//ziskaj HTML kod z iframe elementu
			let fieldId = this.options.fieldid;
			let pageBuilderIframe = $("#"+fieldId+"-pageBuilderIframe");
			try {
				let saveData = pageBuilderIframe[0].contentWindow.getSaveData();
				//console.log("saveData=", saveData);
				//ziskaj data pre element doc_data
				for (let i=0; i<saveData.editable.length; i++) {
					let inlineData = saveData.editable[i];
					if ("doc_data"===inlineData.wjAppField) {
						htmlCode = inlineData.data;
					}
				}
			} catch (error) {
				console.error("Error getting data from pageBuilderIframe:", error);
			}
		}
		//console.log("getData, htmlCode=", htmlCode);
		return htmlCode;
	}

	pbInsertContent(html, mode=null, final=false) {
		//console.log("html=", html, mode+" to PageBuilder editors", "markPbElements=", markPbElements);

		//if we are appending content we must wait for final version, otherwise we would append content multiple times
		if ("append" === mode && final===false) return;

		if (html.indexOf("<section")==-1)
        {
            //console.log("HTML kod neobsahuje ziadnu section, pridavam, html=", html);
            if ("<p>&nbsp;</p>"==html) html = "<p>Text</p>";
            html = "<section><div class=\"container\"><div class=\"row\"><div class=\"col-md-12\">"+html+"</div></div></div></section>";
        }

		//let options = self.EDITOR.field(aiCol.to).s.opts;
		let fieldId = this.options.fieldid;
		let pbIframe = $("#"+fieldId+"-pageBuilderIframe")[0].contentWindow;
		pbIframe.$("[data-wjapp='pageBuilder']").each(function(index) {
			if ("doc_data" != $(this).data("wjappfield")) return;

			const $container = $(this);

			if ("replace" === mode || "edit" === mode) {
				//remove all section elements, in edit mode we expect to send all data and return whole new HTML code
				$container.children('section').remove();
				$container.children('style').remove();
			}
			const $lastSection = $container.children('section').last();
			if ($lastSection.length > 0) {
				$lastSection.after(html);
			} else {
				// if there are no sections yet, just prepend to container
				$container.prepend(html);
			}
			//scroll window to bottom
			pbIframe.scrollTo(0, pbIframe.document.body.scrollHeight+200);
		});
		//reinitialize pb blocks
		if (final===true) {
			this.markPbElements();
		}
	}

	markPbElements() {
		if ("pageBuilder"===this.editingMode) {
			//get ARRAY of content for all editors
			let fieldId = this.options.fieldid;
			let pageBuilderIframe = $("#"+fieldId+"-pageBuilderIframe");

			pageBuilderIframe[0].contentWindow.markPbElements("doc_data");
		}
	}

	getDataArray() {
		if ("pageBuilder"===this.editingMode) {
			//get ARRAY of content for all editors
			let fieldId = this.options.fieldid;
			let pageBuilderIframe = $("#"+fieldId+"-pageBuilderIframe");

			let editorsContent = pageBuilderIframe[0].contentWindow.getEditorsContent("doc_data");
			console.log("getData, editorsContent=", editorsContent);
			return editorsContent;
		}
		return [].push(this.getData());
	}

	getWysiwygEditors() {
		if ("pageBuilder"===this.editingMode) {
			let fieldId = this.options.fieldid;
			let pageBuilderIframe = $("#"+fieldId+"-pageBuilderIframe");
			return pageBuilderIframe[0].contentWindow.getWysiwygEditors("doc_data")
		}
		return [];
	}

	setEditingMode(json) {
		var that = this;
		//console.log("setEditingMode, json=", json);
		this.editingMode = json?.editorFields?.editingMode;

		let fieldId = this.options.fieldid;
		//console.log("setEditingMode, this=", this, "mode=", this.editingMode, "json=", json);

		let pageBuilderIframe = $("#"+fieldId+"-pageBuilderIframe");
		let editorTypeSelector = $("#"+fieldId+"-editorTypeSelector");
		var isPageBuilder = false;

		if ("pageBuilder"===this.editingMode) {
			isPageBuilder = true;
			pageBuilderIframe.attr("src", json.editorFields.editingModeLink);

			var editorTypeForced = WJ.getAdminSetting("editorTypeForced");
			if (typeof editorTypeForced != "undefined") that.myWindow.top.editorTypeForced = editorTypeForced;
			if (typeof that.myWindow.top.editorTypeForced != "undefined") {
				//uz si user raz zmenil editor type, tak ho zachovajme
				this.editingMode = that.myWindow.top.editorTypeForced;
			}

			editorTypeSelector.find("select").val(this.editingMode);
			editorTypeSelector.find("select").selectpicker('refresh');

			//console.log("SomPB init, mode=", this.editingMode);

		} else if ("html"===this.editingMode) {
			pageBuilderIframe.attr("src", "about:blank");
		} else {
			pageBuilderIframe.attr("src", "about:blank");
		}

		//console.log("editingMode=", this.editingMode, "isPageBuilder=", isPageBuilder);
		var pbOption = editorTypeSelector.find("select option[value='pageBuilder']");
		if (isPageBuilder) {
			if (typeof pbOption.attr("disabled")!="undefined") {
				pbOption.removeAttr("disabled");
				editorTypeSelector.find("select").selectpicker('refresh');
			}
		} else {
			if (typeof pbOption.attr("disabled")=="undefined") {
				pbOption.attr("disabled", "disabled");
				editorTypeSelector.find("select").selectpicker('refresh');
			}
		}

		this.switchEditingMode(this.editingMode);
	}

	/**
	 * Switch mode
	 * @param {String} newEditingMode
	 * @param {Boolean} userChange - true if this is user change and we would like to preserve HTML code between modes
	 */
	switchEditingMode(newEditingMode, userChange=false) {
		//console.log("switchEditingMode to ", newEditingMode, " userChange=", userChange);
		let fieldId = this.options.fieldid;
		let ckEditorElement = $("#trEditor div.wysiwyg_textarea");
		let pageBuilderElement = $("#"+fieldId+"-trPageBuilder");
		let editorTypeSelector = $("#"+fieldId+"-editorTypeSelector");
		let oldEditingMode = this.editingMode;

		var data = null;
		if (userChange === true) {
			data = this.getData();
		}

		this.editingMode = newEditingMode;
		if ("pageBuilder"===this.editingMode) {
			//console.log("ckEditorElement=", ckEditorElement, "pageBuilderElement=", pageBuilderElement);
			ckEditorElement.hide();
			pageBuilderElement.show();
			//prevencia pred zbytocnym loadingom HTML objektov
			this.ckEditorInstance.setMode('source');

			//nastav select na korektnu hodnotu
			if (pageBuilderElement.find("iframe").length>0 && pageBuilderElement.find("iframe")[0].contentWindow && pageBuilderElement.find("iframe")[0].contentWindow.$) {
				pageBuilderElement.find("iframe")[0].contentWindow.$("div.exit-inline-editor select").val('pageBuilder');
			}

			if (data != null) {
				this.pbInsertContent(data, "replace", true);
			}
		} else if ("html"===this.editingMode) {
			ckEditorElement.show();
			pageBuilderElement.hide();
			if (data == null) data = this.ckEditorInstance.getData();
			var ck = this.ckEditorInstance;
			if (data != null && "pageBuilder"===oldEditingMode) {
				ck.setMode('wysiwyg');
				ck.setData(data);
			}
			setTimeout(()=>{
				//this fix problems with codemirror line gutter
				if (ck.mode!=="source") ck.setMode('source');
				ck.setData(data);
			}, 500);

			//nastav select na korektnu hodnotu
			editorTypeSelector.find("select").selectpicker("val", "html");
		} else {
			ckEditorElement.show();
			pageBuilderElement.hide();
			this.ckEditorInstance.setMode('wysiwyg');

			//nastav select na korektnu hodnotu
			editorTypeSelector.find("select").selectpicker("val", "");

			if (data != null && "pageBuilder"===oldEditingMode) {
				var ck = this.ckEditorInstance;
				setTimeout(()=>{
					ck.setData(data);
				}, 500);
			}
		}

		this.editorHeightLatest = 0;
		this.resizeEditor(this);
	}

	setStyleComboList(sessionCssParsed) {

		//console.log("setStyleComboList, sessionCssParsed=", sessionCssParsed, "instance=", instance);
		//nastav vyberove pole stylov
		try
		{
			var instance = this.ckEditorInstance;
			//console.log("instance=", instance);

			// elementy podla dokumentacia ckeditora: 'address', 'div', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'pre', 'a', 'embed', 'hr', 'img', 'li', 'object', 'ol', 'table', 'td', 'tr', 'ul'
			var allElements = ['a', 'b', 'i', 'u', 'blockquote', 'dd', 'div', 'dl', 'dt', 'hr', 'img', 'input', 'select', 'textarea', 'label', 'ul', 'ol', 'li', 'table', 'th', 'td', 'p', 'span', 'strong', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'];
			var cssData = [
				{name: WJ.translate("editor.paragraph"), element: 'p' },
				{name: WJ.translate("editor.h1"), element: 'h1'},
				{name: WJ.translate("editor.h2"), element: 'h2'},
				{name: WJ.translate("editor.h3"), element: 'h3'},
				{name: WJ.translate("editor.h4"), element: 'h4'},
				{name: WJ.translate("editor.h5"), element: 'h5'},
				{name: WJ.translate("editor.h6"), element: 'h6'}
			];

			//console.log("setStylesDef, adding styles=", sessionCssParsed);

			$.each(sessionCssParsed, function(i,v){
				var result = {};
				var name = "";
				var nameClass = v.class;
				if (v.tag != "*") {
					name += "["+v.tag;
					var space = nameClass.indexOf(" ");
					//zmen format z [div] container div01 na [div.container] div01
					if (space > 0) {
						name += "."+nameClass.substring(0, space);
						nameClass = nameClass.substring(space+1);
					}
					name += "] ";
				}
				name += nameClass;

				result.name = name;
				result.element = v.tag == "*" ? allElements : v.tag;
				result.attributes = { 'class': v.class };

				cssData.push(result);
			});

			//console.log(JSON.stringify(instance.config.stylesSet) + " vs " + JSON.stringify(cssData));
			//console.log(cssData);

			if (JSON.stringify(instance.config.stylesSet) != JSON.stringify(cssData)) {
				instance.ui.get("Styles").reset();
				instance.config.stylesCombo_stylesSet = cssData;
				instance.config.stylesSet = cssData;
				instance._.stylesDefinitions = cssData;
				instance.fire('stylesSet', {styles: cssData});

				//console.log("instance.config.stylesSet=", instance.config.stylesSet);

				try
				{
					instance.ui.get("Styles").__proto__.showAll()
					instance.ui.get("Styles")._.panel = null;
				} catch (e) {}
			}

		} catch (e) { console.log(e); }
	}

	/**
	 * Pocita vysku editora v dialogovom okne datatabuliek, predpoklada plne vyuzitie plochy karty
	 * Z vysky okna odpocita margin z hora a dola a vysku hlavicky a paticky a zvysok vyuzije pre editor
	 */
	resizeEditor(datatablesCkEditor) {
		var that = this;

		//console.log("this=", this, "datatablesCkEditor=", datatablesCkEditor);
		if (typeof datatablesCkEditor == "undefined" || typeof datatablesCkEditor.datatable == "undefined" || datatablesCkEditor.datatable == null) return;

		var windowInnerHeight = $(that.myWindow).height(); //on phone height was not correct: that.myWindow.innerHeight;
		var dialogMarginTop = parseInt($("#"+datatablesCkEditor.datatable.DATA.id+"_modal > div.modal-dialog").css("margin-top"));
		var dialogMarginBottom = parseInt($("#"+datatablesCkEditor.datatable.DATA.id+"_modal > div.modal-dialog").css("margin-bottom"));
		var headerHeight = parseInt($("#"+datatablesCkEditor.datatable.DATA.id+"_modal div.DTE_Header").css("height"));
		var headerMarginTop = parseInt($("#"+datatablesCkEditor.datatable.DATA.id+"_modal div.DTE_Header").css("margin-top"));
		var footerHeight = parseInt($("#"+datatablesCkEditor.datatable.DATA.id+"_modal div.DTE_Footer").css("height"));

		//console.log("id=", datatablesCkEditor.datatable.DATA.id, "windowInnerHeight=", windowInnerHeight, "dialogMarginTop=", dialogMarginTop, "dialogMarginBottom=", dialogMarginBottom, "headerHeight=", headerHeight, "footerHeight=", footerHeight);
		//console.log("modal=", $("#"+datatablesCkEditor.datatable.DATA.id+"_modal"));

		var editorHeight = windowInnerHeight - dialogMarginTop - dialogMarginBottom - headerHeight - headerMarginTop - footerHeight - 4; //4 je safe konstanta
		if (editorHeight < 300) editorHeight = 300;

		//console.log("Resizing editor, latest=", this.editorHeightLatest, " new=", editorHeight);

		if (editorHeight != datatablesCkEditor.editorHeightLatest) {
			//console.log("this=", this, "datatablesCkEditor=", datatablesCkEditor);
			//console.log("RESIZING, editorHeight=", editorHeight);
			this.ckEditorInstance.resize("99%", editorHeight);
			datatablesCkEditor.editorHeightLatest = editorHeight;

			var pageBuilderElement = $(`#${datatablesCkEditor.options.fieldid}-pageBuilderIframe`);
			//console.log("pageBuilderElement=", pageBuilderElement, "id=", `#${datatablesCkEditor.options.fieldid}-pageBuilderIframe`);
			that.myWindow.pageBuilderElement = pageBuilderElement;
			pageBuilderElement.css("height", (editorHeight)+"px");
		}
	}

	/**
	 * zobrazi poznamku redaktora ako notifikaciu (pre lepsie zobrazenie)
	 */
	showEditorNote() {
		try {
			let note = this.json.editorFields.redactorNote;
			if (note != null && note != "") {
				note = note.replace(/\n/gi, "\n<br/>");
				WJ.notifyWarning($("label[for=DTE_Field_editorFields-redactorNote]").text(), note, 15000);
			}
		} catch (e) {console.log(e);}
	}
}