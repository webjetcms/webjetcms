<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" contentType="text/javascript" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
$(document).ready(function(){

	$('head').append('<link rel="stylesheet" type="text/css" media="screen" href="/components/gallery/photoswipe/css/photoswipe.css" />');
	$('head').append('<link rel="stylesheet" type="text/css" media="screen" href="/components/gallery/photoswipe/default-skin/default-skin.css" />');

	insertPSWP();

	var $pswp = $('.pswp')[0];

	$('.thumbs').on('click', 'a.thumb', function(event) {
	    event.preventDefault();

	    var $index = $(this).parent('li').index();

		 var items = [];

		 var $selector = $(this).parents('.thumbs').find('a.thumb');
		 //ak nic nenaslo pouzi vsetky mozne v stranke, asi to nie je standardna foto galeria
		 if ($selector.length < 1) $selector = $('.thumbs a.thumb');

	    $selector.each(function(){
			var el = $(this);
			//console.log(el);
			var dimensions = el.data('dimensions').split('x');
			var titles = el.data('title');

			var title = "";

			if (titles != undefined) {

				if (typeof titles === "string")
					{
						titles = titles.replace(/&quot;/gi, "\"");
						titles = titles.replace(/&amp;/gi, "&");
						titles = titles.replace(/&#39;/gi, "\\");
						titles = titles.replace(/&lt;/gi, "<");
						titles = titles.replace(/&gt;/gi, ">");
						titles = titles.replace(/'/gi, "\"");
						titles = JSON.parse(titles);
					}

					if (titles.shortDescription != '') {
						title = '<span class="photoswipeShortDesc">' + titles.shortDescription + '</span>';
					}

					if (titles.longDescription != '') {
						if (title != '') {
							title += "<span class='photoswipeDelimiter'> - </span>";
						}
						title += ' <span class="photoswipeLongDesc">';
						title += titles.longDescription;
						title += '</span>';
					}

					if (titles.author != '') {
						if (title != '') {
							title += '<br />';
						}
						title += '<small><iwcm:text key="components.gallery.photoswipe.author"/>' + titles.author + '</small>';
					}
			}

			var item = {
				src: el.prop('href'),
				w: dimensions[0],
				h: dimensions[1],
				title: title
			}

			items.push(item);
		});

		 //console.log("index=", $index, "items=", items);

		 var photoSwipe;

	    var options = {
	        index: $index,
	        showHideOpacity: true,
	        shareButtons: [
			    {id:'facebook', label:"<iwcm:text key="components.gallery.photoswipe.share_on_facebook"/>", url:'https://www.facebook.com/sharer/sharer.php?u={{url}}'},
			    {id:'twitter', label:"<iwcm:text key="components.gallery.photoswipe.tweet"/>", url:'https://twitter.com/intent/tweet?text={{text}}&url={{url}}'},
			    {id:'download', label:"<iwcm:text key="components.gallery.photoswipe.download"/>", url:'{{raw_image_url}}', download:true}
			  ],
			  getImageURLForShare: function( shareButtonData ) {
					var imgUrl = photoSwipe.currItem.src || '';
					//console.log(imgUrl);
					var i = imgUrl.lastIndexOf("/");
					if (i > 0) {
						imgUrl = imgUrl.substring(0, i+1) + "o_" + imgUrl.substring(i+1);
					}
					//console.log(imgUrl);
					return imgUrl;
				}
	    }

	    // Initialize PhotoSwipe
	    photoSwipe = new PhotoSwipe($pswp, PhotoSwipeUI_Default, items, options);
	    photoSwipe.init();

	    //console.log(photoSwipe);
	});

	<% // JCH add - for use with directly inserted gallery images - AJAX load of image data (dimmensions and description) in JSON format %>
	$("a[rel='wjimageviewer']").on('click', function(event) {
	    event.preventDefault();

	    var imageWidth = 800;
	    var imageHeight = 600;
	    var imageTitle = "";
		var imagePath = $(this).attr("href");
	    $.getJSON(
	    		"/components/gallery/ajax/getimagedata.jsp",
	    		{
	    			path: $(this).attr("href")
	    		}
	    )
	    	.done(function( data ) {
		    	imageWidth = data.imageWidth;
		    	imageHeight = data.imageHeight;
		    	imageTitle = data.imageTitle;
				var singleItem = [];

				var item = {
					src: imagePath,
					w: imageWidth,
					h: imageHeight,
					title: imageTitle
				}

				singleItem.push(item);

			    var options = {
			        index: 1,
			        showHideOpacity: true,
			        shareButtons: [
					    {id:'facebook', label:"<iwcm:text key="components.gallery.photoswipe.share_on_facebook"/>", url:'https://www.facebook.com/sharer/sharer.php?u={{url}}'},
					    {id:'twitter', label:"<iwcm:text key="components.gallery.photoswipe.tweet"/>", url:'https://twitter.com/intent/tweet?text={{text}}&url={{url}}'},
					    {id:'download', label:"<iwcm:text key="components.gallery.photoswipe.download"/>", url:'{{raw_image_url}}', download:true}
					  ]
			    }

			    // Initialize PhotoSwipe
			    var photoSwipe = new PhotoSwipe($pswp, PhotoSwipeUI_Default, singleItem, options);
			    photoSwipe.init();
	   	    });

	});
	<%// JCH - add end%>
});

function insertPSWP()
{
	if ($('.pswp').length > 0) {
		return;
	}

	var html = '<div class="pswp" tabindex="-1" role="dialog" aria-hidden="true">' +
	    			'<div class="pswp__bg"></div>' +
	    			'<div class="pswp__scroll-wrap">' +
	        			'<div class="pswp__container">' +
				            '<div class="pswp__item"></div>' +
				            '<div class="pswp__item"></div>' +
				            '<div class="pswp__item"></div>' +
				        '</div>' +
				        '<div class="pswp__ui pswp__ui--hidden">' +
				            '<div class="pswp__top-bar">' +
				                '<div class="pswp__counter"></div>' +
				                '<button class="pswp__button pswp__button--close" title="<iwcm:text key="components.gallery.photoswipe.close"/>"></button>' +
				                '<button class="pswp__button pswp__button--share" title="<iwcm:text key="components.gallery.photoswipe.share"/>"></button>' +
				                '<button class="pswp__button pswp__button--fs" title="<iwcm:text key="components.gallery.photoswipe.fulscreen"/>"></button>' +
				                '<button class="pswp__button pswp__button--zoom" title="<iwcm:text key="components.gallery.photoswipe.zoom"/>"></button>' +
				                '<div class="pswp__preloader">' +
				                    '<div class="pswp__preloader__icn">' +
				                      '<div class="pswp__preloader__cut">' +
				                        '<div class="pswp__preloader__donut"></div>' +
				                      '</div>' +
				                    '</div>' +
				                '</div>' +
				            '</div>' +
				            '<div class="pswp__share-modal pswp__share-modal--hidden pswp__single-tap">' +
				                '<div class="pswp__share-tooltip"></div>' +
				            '</div>' +
				            '<button class="pswp__button pswp__button--arrow--left" title="<iwcm:text key="components.gallery.photoswipe.previous"/>"></button>' +
				            '<button class="pswp__button pswp__button--arrow--right" title="<iwcm:text key="components.gallery.photoswipe.next"/>"></button>' +
				            '<div class="pswp__caption">' +
				                '<div class="pswp__caption__center"></div>' +
				            '</div>' +
						'</div>' +
					'</div>' +
				'</div>';

	$('body').append(html);
}