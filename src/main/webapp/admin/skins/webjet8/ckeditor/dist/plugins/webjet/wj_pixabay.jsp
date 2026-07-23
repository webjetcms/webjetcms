<%@page import="sk.iway.iwcm.i18n.Prop"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.editor.*,java.util.*"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<iwcm:checkLogon admin="true" perms='<%=Constants.getString("webpagesFunctionsPerms")%>'/>
<%

String userLanguage = Prop.getLng(request, true);
if (Tools.isEmpty(userLanguage)) {
	userLanguage = "sk";
}

Prop prop = Prop.getInstance(request);
String[] imageTypeOptions = Tools.getTokens(prop.getText("editor.pixabay.imageType"), ",", true);
String[] categoryOptions = Tools.getTokens(prop.getText("editor.pixabay.category"), ",", true);

request.setAttribute("apiKey", Constants.getString("pixabayApiKey"));
request.setAttribute("defaultWidth", Constants.getString("pixabayDefaultWidth"));
request.setAttribute("userLanguage", userLanguage);
%>
<%@ include file="/admin/layout_top_popup.jsp" %>

<style type="text/css">
div.results { background-color: white; padding: 5px; padding-top: 10px; }
div.pixabayBox { margin: 0px; }
.pixabayBox .imageSearch { padding: 10px; padding-top: 10px; }
.pixabayBox .paging { padding-top: 10px; }
.pixabayBox #imageModal .imageExtension { background-color: transparent; border-right: var(--bs-border-width) solid var(--bs-border-color); justify-content: center; min-width: 48px; }
.pixabayBox #imageModal .imageExtension .ti { font-size: 24px; }
.pixabayBox #imageModal #imageName.is-invalid + .imageExtension { border-color: var(--bs-form-invalid-border-color, #dc3545); }
div.no_results { color: red; font-weight: bold; text-align: center; }
</style>

<script type="text/javascript" src="js/fck_dialog_common.jsp"></script>
<script type="text/javascript">

;( function( $, window, document, undefined ) {
	"use strict";

		// Create the defaults once
		var pluginName = "pixabay",
			defaults = {
				apiKey: 'no-api-key',
				imageUrl: 'https://pixabay.com/api/',
				videoUrl: 'https://pixabay.com/api/videos/',
				itemsPerPage: 8,
				itemsPerRow: 4,
				page: 1,
				pagesCount: 0,
				search: '',
				imageType: 'all',
				category: 'all'
			};

		var cache = {};
		var imageDimenions = {
			tiny: '_180',
			small: '_340',
			normal: '_640',
			big: '_960'
		};
		var allowedLanguages = ['cs', 'da', 'de', 'en', 'es', 'fr', 'id', 'it', 'hu', 'nl', 'no', 'pl', 'pt', 'ro', 'sk', 'fi', 'sv', 'tr', 'vi', 'th', 'bg', 'ru', 'el', 'ja', 'ko', 'zh'];
		var userLanguage = '${userLanguage}';

		var Metronic = window.parent.Metronic;

		// The actual plugin constructor
		function Plugin ( element, options ) {
			this.element = $(element);

			this.elements = {
				form: $(".pixabayBox .imageSearch"),
				results: $(".pixabayBox .results"),
				noResults: $(".pixabayBox .no_results"),
				paging: $(".pixabayBox .paging")
			}

			this.lang = $.inArray(userLanguage, allowedLanguages) != -1 ? userLanguage : 'en';

			this.settings = $.extend( {}, defaults, options );

			if (this.settings.apiKey == "no-api-key") {
				console.log('Pixabay plugin - no api key');
				return;
			}

			this.init();
		}

		$.extend( Plugin.prototype, {
			init: function() {
				var self = this;
				self.ensureFilterOptions();
				self.elements.form.submit(function(e){
					var q = $('#search').val();
					var imageType = $('#imageType').val();
					var category = $('#category').val();

					self.getJSON(q, 1, imageType, category);
					e.preventDefault();
				});

				self.elements.paging.on('click', '.page', function(e){
					var page = parseInt($(this).prop('id').replace('page', ''));

					if (page > 0) {
						var q = $('#search').val();
						var imageType = $('#imageType').val();
						var category = $('#category').val();
						self.getJSON(q, page, imageType, category);
					}

					e.preventDefault();
				});

				self.elements.paging.on('click', '.next, .prev', function(e){
					var page = self.settings.page;

					if ($(this).hasClass('prev')) {
						page -= 1;
					}
					else {
						page += 1;
					}

					if (page > 0 && page <= self.settings.pagesCount) {
						var q = $('#search').val();
						var imageType = $('#imageType').val();
						var category = $('#category').val();
						self.getJSON(q, page, imageType, category);
					}

					e.preventDefault();
				});

				self.elements.results.on('click', 'a', function(e){
					var src = $(this).find('img').first().prop('src');
					var img = $(this).prop('href');
					var extension = self.getImageExtension(img);

					var width = $(this).data('width');
					var height = $(this).data('height');

					$('#imageModal').find('img').first().prop('src', src);
					$('#imageModal').find('img').first().data('img', img);
					$('#imageModal').find('img').first().data('width', width);
					$('#imageModal').find('img').first().data('height', height);
					$('#imageModal #imageName').val(self.sanitizeFileName(self.settings.search));
					$('#imageModal').data('file-extension', extension);
					self.clearFileNameError();
					self.setImageExtension(extension);
					self.toggleDimensions(extension);

					$('#imageModal').modal('show');
					$('#imageModal #imageWidth').val('${defaultWidth}').keyup();


					e.preventDefault();
				});

				$('#imageModal #imageHeight, #imageModal #imageWidth').on('keyup blur', function(){
					var el = $(this);
					var id = el.prop('id');
					var otherEl = $('#imageModal #imageHeight, #imageModal #imageWidth').not('#' + id);

					var val = el.val();
					var img = $('#imageModal img');
					var height = img.data('height');
					var width = img.data('width');


					if (val == "" || isNaN(parseInt(val))) {
						otherEl.val('');

						el.prop('disabled', false);
						otherEl.prop('disabled', false);

						self.checkInputs();

						return;
					}

					var scale = 1;

					if ($(this).prop('id') == "imageHeight") {
						scale = height / width;
					}
					else {
						scale = width / height;
					}

					var result = Math.ceil(val / scale);
					otherEl.val(result);

					el.prop('disabled', false);
					otherEl.prop('disabled', true);

					self.checkInputs();
				});

				$('#imageModal #imageName').on('input', function(){
					self.clearFileNameError();
				}).on('blur', function(){
					$(this).val(self.sanitizeFileName($(this).val()));
					self.checkInputs();
				});

				$('#category').on('change', function(){
					self.settings.category = $(this).val();
					$('#imageType').prop('disabled', self.isVideoSearch(self.settings.category));
				});

				$('#imageType').on('change', function(){
					self.settings.imageType = $(this).val();
				});

				$('#imageType').prop('disabled', self.isVideoSearch($('#category').val()));

				$('#imageModal .saveImage').click(function(){

					var url = '/components/gallery/admin_save_image_ajax_utf-8.jsp';
					var data = {
						'saveImage': 'true'
					};

					data.img = $('#imageModal').find('img').first().data('img');
					data.height = $('#imageModal #imageHeight').val();
					data.width = $('#imageModal #imageWidth').val();
					data.fileName = self.sanitizeFileName($('#imageModal #imageName').val());
					$('#imageModal #imageName').val(data.fileName);

					var errors = [];

					if (!self.checkInputs()) {
						return;
					}

					var doc = window.parent.$('#wjImageIframeElement').contents()[0];
					var elfinder;
					if (doc === undefined)
					{
						//sme v dialogu pre perex obrazok
						elfinder = window.parent.elFinderInstance;
					}
					else
					{
						//sme v editore vlozenie obrazku
						elfinder = doc.defaultView.elFinderInstance;
					}

					var cwd = elfinder.cwd();

					data.virtualPath = cwd.virtualPath;

					$.ajax({
						url: url,
						data: data,
						method: 'post',
						success: function(data){
							if (data.result) {

								$('#imageModal .errors').hide();
								$('#imageModal').modal('hide');
								$('#imageModal #imageHeight, #imageModal #imageWidth, #imageModal #imageName').val('');
								self.setImageExtension('');

								setTimeout(function()
								{
									if (window.parent.CKEDITOR === undefined)
									{
										//sme v dialogu pre perex obrazok
										var inputElement = window.parent.$(".row:not(.template) input.elfinder-url-input");
										//console.log("FILE: ", inputElement);
										inputElement.val(data.virtualPath + "?v=" + new Date().getTime());
										window.parent.$(".md-tabs ul.nav li:first a").trigger("click");
										window.parent.$(".md-tabs ul.nav li.nav-item a.active").removeClass("active");
										window.parent.$(".md-tabs ul.nav li:first a").addClass("active");
									}
									else
									{
										//sme v editore vlozenie obrazku
										window.parent.CKEDITOR.dialog.getCurrent().selectPage('wjImage');
										window.parent.CKEDITOR.dialog.getCurrent().getContentElement("info", "txtUrl").setValue(data.virtualPath + "?v=" + new Date().getTime());
										window.parent.$('#wjImageIframeElement').contents().find('#txtUrl').val(data.virtualPath + "?v=" + new Date().getTime());
										window.parent.$('#wjImageIframeElement').contents().find('#txtUrl').trigger('change');
									}

									setTimeout(function(){
										elfinder.exec('reload');
									}, 500);
								}, 500);
							}
							else {
								var duplicateError = $('#imageNameDuplicateError').data('server-message');
								if ($.inArray(duplicateError, data.errors) != -1) {
									$('#imageModal .errors').hide();
									self.showFileNameError('duplicate');
									return;
								}

								$('#imageModal .errors').empty();
								var html = '<ul>';

								$.each(data.errors, function(i, v){
									html += '<li>' + v + '</li>';
								});

								html += '</ul>';
								$('#imageModal .errors').html(html).show();
							}
						}
					});
				});

				/*
				zakomentovane jeeff - nefungovalo vo Firefoxe, modal prekryl plochu a nedalo sa pisat do inputu
				$('#imageModal').modal({
					keyboard: false
				}).modal("hide");
				*/
			},
			ensureFilterOptions: function() {
				if ($('#imageType option').length === 0) {
					$('#imageType').append('<option value="all">All</option><option value="photo">Photo</option><option value="illustration">Illustration</option><option value="vector">Vector</option>');
				}

				if ($('#category option').length === 0) {
					$('#category').append('<option value="all">All categories</option><option value="backgrounds">Backgrounds</option><option value="fashion">Fashion</option><option value="nature">Nature</option><option value="science">Science</option><option value="education">Education</option><option value="feelings">Feelings</option><option value="health">Health</option><option value="people">People</option><option value="religion">Religion</option><option value="places">Places</option><option value="animals">Animals</option><option value="industry">Industry</option><option value="computer">Computer</option><option value="food">Food</option><option value="sports">Sports</option><option value="transportation">Transportation</option><option value="travel">Travel</option><option value="buildings">Buildings</option><option value="business">Business</option><option value="music">Music</option><option value="video:all">Video - all</option><option value="video:film">Video - film</option><option value="video:animation">Video - animation</option>');
				}
			},
			getJSON: function( search, page, imageType, category ) {
				var self = this;
				self.settings.search = search;
				self.settings.imageType = imageType || 'all';
				self.settings.category = category || 'all';

				//Metronic.blockUI({target: $('.results'), iconOnly: true});

				var self = this;
				var search = encodeURIComponent(search);

				if (search.length == 0) {
					self.elements.paging.hide();
					self.elements.results.hide();
				    self.elements.noResults.show();

				    //Metronic.unblockUI({target: $('.results')});
					return;
				}

				if (typeof page == "undefined") {
					page = 1;
				}

				self.settings.page = page;

				var isVideo = self.isVideoSearch(self.settings.category);
				var cacheKey = [search, page, self.settings.itemsPerPage, self.settings.imageType, self.settings.category, isVideo ? 'video' : 'image'].join("-");

				if (cache[cacheKey] != null) {
					self.renderItems(cache[cacheKey]);

					//Metronic.unblockUI({target: $('.results')});
					return;
				}

				$.ajax({
					dataType: "json",
					method: "get",
					url: self.buildRequestUrl(search, page),
					cache: true,
					success: function(data){
						self.renderItems(data);
						cache[cacheKey] = data;
						//Metronic.unblockUI({target: $('.results')});
					}
				});
			},
			buildRequestUrl: function(search, page) {
				var isVideo = this.isVideoSearch(this.settings.category);
				var url = isVideo ? this.settings.videoUrl : this.settings.imageUrl;
				var params = [
					'key=' + encodeURIComponent(this.settings.apiKey),
					'q=' + search,
					'page=' + page,
					'lang=' + this.lang,
					'per_page=' + this.settings.itemsPerPage
				];

				if (isVideo) {
					params.push('video_type=' + encodeURIComponent(this.getVideoType(this.settings.category)));
				}
				else {
					params.push('image_type=' + encodeURIComponent(this.settings.imageType || 'all'));
					if (this.settings.category && this.settings.category !== 'all') {
						params.push('category=' + encodeURIComponent(this.settings.category));
					}
				}

				return url + '?' + params.join('&');
			},
			isVideoSearch: function(category) {
				return typeof category == 'string' && category.indexOf('video:') === 0;
			},
			getVideoType: function(category) {
				if (!this.isVideoSearch(category)) return 'all';
				var parts = category.split(':');
				return parts.length > 1 ? parts[1] : 'all';
			},
			getVideoData: function(item) {
				if (!item.videos) return null;

				var candidates = ['medium', 'small', 'tiny', 'large'];
				for (var i = 0; i < candidates.length; i++) {
					var variant = item.videos[candidates[i]];
					if (variant && variant.url) return variant;
				}

				return null;
			},
			renderItems: function( items ) {
				var self = this;
				var isVideo = self.isVideoSearch(self.settings.category);
				var totalHits = parseInt(items.totalHits);
			    if (totalHits > 0) {
			    	var html = '';
			    	var index = 0;
			    	var itemsPerRow = self.settings.itemsPerRow;
			    	var rows = Math.ceil(items.hits.length / itemsPerRow);

			    	for (var i=0; i < rows; i++) {
			    		html += '<div class="row">';

			    		for (var j=0; j < itemsPerRow; j++) {
			    			if (typeof items.hits[index] == "undefined") {
			    				continue;
			    			}

			    			var item = items.hits[index];
			    			var preview = item.previewURL;
			    			var image = '';
							var webformatWidth = 0;
							var webformatHeight = 0;

							if (isVideo) {
								var videoData = self.getVideoData(item);
								if (videoData == null) {
									index++;
									continue;
								}

								image = videoData.url;
								preview = videoData.thumbnail || '';
								webformatWidth = videoData.width || 0;
								webformatHeight = videoData.height || 0;
							}
							else {
								var webFormatImage = item.largeImageURL;
								image = self.getImage(webFormatImage, imageDimenions.big);
								webformatWidth = item.webformatWidth;
								webformatHeight = item.webformatHeight;
							}

			    			html += '<div class="col-xs-3">';
							html += '<a data-height="' + webformatHeight + '" data-width="' + webformatWidth + '" href="' + image + '" target="_blank"><img class="img-responsive" src="' + preview + '" alt="" /></a>';
			    			html += '</div>';

			    			index++;
			    		}

			    		html += '</div>';
			    	}

			        self.elements.results.html(html).show();
		        	self.setPaging(totalHits);
		        	self.elements.paging.show();
		        	self.elements.results.show();
		        	self.elements.noResults.hide();
			    }
			    else {
			    	self.setPaging(totalHits);
			    	self.elements.paging.hide();
			    	self.elements.results.hide();
			    	self.elements.noResults.show();

			        console.log('No hits');
			    }
			},
			setPaging: function( totalHits ) {
				var itemsPerPage = this.settings.itemsPerPage

				if (totalHits <= itemsPerPage) {
					return;
				}

				var html = '';
				this.settings.pagesCount = Math.ceil(totalHits / itemsPerPage);

				for (var i=0; i<this.settings.pagesCount; i++) {
					var actual = i+1;
					var classes = ['page'];

					if (actual == this.settings.page) {
						classes.push('active');
					}

					html += '<a href="javascript:;" class="' + classes.join(' ') + '" id="page' + actual  + '">' + actual + '</a>';
				}

				this.elements.paging.find('.pages').html(html);

				if (this.settings.page < 2) {
					this.elements.paging.find('.prev').css({visibility: 'hidden'});
				}
				else {
					this.elements.paging.find('.prev').css({visibility: 'visible'});
				}

				if (this.settings.page >= this.settings.pagesCount) {
					this.elements.paging.find('.next').css({visibility: 'hidden'});
				}
				else {
					this.elements.paging.find('.next').css({visibility: 'visible'});
				}
			},
			getImage: function( image, size ) {
				image = image.replace('_640', size);
				return image;
			},
			getImageExtension: function( image ) {
				var path = new URL(image, window.location.href).pathname;
				var filename = path.substring(path.lastIndexOf('/') + 1);
				var extension = filename.indexOf('.') == -1 ? '' : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
				return extension == 'jpeg' ? 'jpg' : extension;
			},
			setImageExtension: function( extension ) {
				var element = $('#imageModal .imageExtension');
				element.empty().removeAttr('title role aria-label');

				if (extension == '') return;

				var fileTypeIcons = ['jpg', 'png', 'svg', 'bmp'];
				var icon = fileTypeIcons.indexOf(extension) == -1 ? 'ti-file' : 'ti-' + extension;
				$('<i>', { 'class': 'ti ' + icon, 'aria-hidden': 'true' }).appendTo(element);
				element.attr({
					title: extension.toUpperCase(),
					role: 'img',
					'aria-label': extension.toUpperCase()
				});
			},
			clearFileNameError: function() {
				var fileName = $('#imageModal #imageName');
				fileName.removeClass('is-invalid').removeAttr('aria-invalid').removeData('error-type');
				$('#imageModal .imageNameError').hide();
			},
			showFileNameError: function( errorType ) {
				var fileName = $('#imageModal #imageName');
				this.clearFileNameError();
				fileName.addClass('is-invalid').attr('aria-invalid', 'true').data('error-type', errorType);
				$('#imageName' + (errorType == 'duplicate' ? 'Duplicate' : 'Required') + 'Error').show();
			},
			toggleDimensions: function(extension) {
				var dimensionsRows = $('#imageModal .imageDimensions');
				if (extension == 'mp4') {
					dimensionsRows.hide();
				}
				else {
					dimensionsRows.show();
				}
			},
			sanitizeFileName: function( fileName ) {
				fileName = WJ.fixFileName(fileName).replace(/\./g, '-');
				return fileName.replace(/^[-_]+|[-_]+$/g, '');
			},
			checkInputs: function() {
				var result = true;
				var extension = $('#imageModal').data('file-extension');

				if (extension != 'mp4') {
					$('#imageModal #imageHeight, #imageModal #imageWidth').each(function(i,v){
						var el = $(this);
						var val = el.val();
						var formGroup = el.closest('.row');
						var tooltip = el.closest('.input-icon').find('.tooltips');

						if (val == "" || isNaN(parseInt(val))) {
							result = false;
							formGroup.addClass('has-error');
							tooltip.show();
						}
						else {
							formGroup.removeClass('has-error');
							tooltip.hide();
						}
					});
				}
				else {
					$('#imageModal #imageHeight, #imageModal #imageWidth').closest('.row').removeClass('has-error');
					$('#imageModal #imageHeight, #imageModal #imageWidth').closest('.input-icon').find('.tooltips').hide();
				}

				var fileName = $('#imageModal #imageName');
				if (fileName.val() == '') {
					result = false;
					this.showFileNameError('required');
				}
				else if (fileName.data('error-type') == 'duplicate') {
					result = false;
				}
				else {
					this.clearFileNameError();
				}

				return result;
			}
		});

		$.fn[ pluginName ] = function( options ) {
			return this.each( function() {
				if ( !$.data( this, "plugin_" + pluginName ) ) {
					$.data( this, "plugin_" +
						pluginName, new Plugin( this, options ) );
				}
			});
		};
} )( jQuery, window, document );

<!--
$(document).ready(function(){
	$(".results").pixabay({apiKey: '${apiKey}'});
});
//-->
</script>

<div class="pixabayBox">
	<form class="form-inline imageSearch">
		<div class="row g-2 align-items-end">
			<div class="input-group mb-3">
				<input type="text" name="search" id="search" class="form-control" style="width: 200px" aria-label="<iwcm:text key="editor.search.find_what" />" placeholder="<iwcm:text key="editor.search.find_what" />" />
				<select id="imageType" class="form-select" aria-label="<iwcm:text key="editor.pixabay.imageTypeLabel" />">
					<%
						for (String item : imageTypeOptions) {
							String[] pair = item.split(":", 2);
							if (pair.length != 2) continue;
					%>
						<option value="<%=pair[1]%>"><%=pair[0]%></option>
					<%
						}
					%>
				</select>
				<select id="category" class="form-select" aria-label="<iwcm:text key="editor.pixabay.categoryLabel" />">
					<%
						for (String item : categoryOptions) {
							String[] pair = item.split(":", 2);
							if (pair.length != 2) continue;
					%>
						<option value="<%=pair[1]%>"><%=pair[0]%></option>
					<%
						}
					%>
				</select>
				<button type="submit" class="btn btn-outline-secondary"><iwcm:text key="searchall.search" /></button>
			</div>
		</div>
	</form>

	<div class="results">
	</div>

	<div class="no_results">
		<iwcm:text key="components.monitoring.no_results"/>
	</div>

	<div class="paging">
		<a href="javascript:;" class="prev btn btn-primary" style="vertical-align: top; margin-top: 10px;"><span class="ti ti-player-track-prev"></span></a>

		<div class="pages">

		</div>

		<a href="javascript:;" class="next btn btn-primary" style="vertical-align: top; margin-top: 10px;"><span class="ti ti-player-track-next"></span></a>

		<a style="position: absolute; right: 20px; color: black; font-weight: normal; text-decoration: none !important;" href="https://pixabay.com" target="_blank">powered by<br/><img src="/admin/skins/webjet8/ckeditor/dist/plugins/webjet/images/pixabaylogo.png" width="80"/></a>
	</div>

	<div class="modal fade" id="imageModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-bs-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
					<h4 class="modal-title" id="exampleModalLabel"><iwcm:text key="pixabay.modal.title" /></h4>
				</div>
				<div class="modal-body">
					<div class="row">
						<div class="col-sm-4">
							<img class="img-responsive" src="" alt="" style="width: 100%" />
						</div>
						<div class="col-sm-8">
							<form class="form-horizontal" role="form">
                                <div class="form-body">
									<div class="row">
										<div class="col-xs-3">
											<label for="imageName" class="control-label" style="white-space: nowrap;"><iwcm:text key="fbrowse.file_name"/></label>
										</div>
										<div class="col-xs-9">
											<div class="input-group">
												<input type="text" class="form-control" id="imageName" aria-describedby="imageNameRequiredError imageNameDuplicateError" />
												<span class="input-group-text imageExtension"></span>
											</div>
											<div id="imageNameRequiredError" class="form-text text-danger small imageNameError" style="display: none;"><iwcm:text key="editor.upload_iframe.enterFileName"/></div>
											<div id="imageNameDuplicateError" class="form-text text-danger small imageNameError" data-server-message="<iwcm:text key="multiple_files_upload.file_exist"/>" style="display: none;"><iwcm:text key="pixabay.modal.imageNameDuplicateError"/></div>
										</div>
									</div>
									<div class="row imageDimensions">
										<div class="col-xs-3">
                                        	<label for="imageWidth" class="control-label"><iwcm:text key="editor.table.width"/></label>
										</div>
                                        <div class="col-xs-9">
                                        	<div class="input-icon right">
					                        	<i class="ti ti-exclamation-mark tooltips" data-original-title="Please write a valid width" data-container="body"></i>
					                        	<input type="text" class="form-control" id="imageWidth" maxlength="4" />
					                        </div>
                                        </div>
                                    </div>
									<div class="row imageDimensions">
										<div class="col-xs-3">
                                        	<label for="imageHeight" class="control-label"><iwcm:text key="editor.table.height"/></label>
										</div>
                                        <div class="col-xs-9">
                                        	<div class="input-icon right">
					                        	<i class="ti ti-exclamation-mark tooltips" data-original-title="Please write a valid height" data-container="body"></i>
					                        	<input type="text" class="form-control" id="imageHeight" maxlength="4" />
					                        </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="errors">
                                </div>
                            </form>
						</div>
					</div>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-default" data-bs-dismiss="modal"><iwcm:text key="webpages.modal.close" /></button>
					<button type="button" class="btn btn-primary saveImage"><iwcm:text key="pixabay.modal.save" /></button>
				</div>
			</div>
		</div>
	</div>
</div>

<%@ include file="/admin/layout_bottom_popup.jsp" %>
