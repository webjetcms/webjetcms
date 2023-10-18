/**
 * jQuery jqGalViewII Plugin
 * Examples and documentation at: http://benjaminsterling.com/jquery-jqgalviewii-photo-gallery/
 *
 * @author: Benjamin Sterling
 * @version: 1.0
 * @copyright (c) 2007 Benjamin Sterling, KenzoMedia
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *   
 * @requires jQuery v1.2.1 or later
 * 
 * 
 * @name jqGalViewII
 * @example $('ul').jqGalViewII();
 * 
 * @Semantic requirements:
 * 				The structure fairly simple and should be unobtrusive, you
 * 				basically only need a parent container with a list of imgs
 * 
 * 	<ul>
 *		<li><img src="common/img/dsc_0003.thumbnail.JPG"/></li>
 *		<li><img src="common/img/dsc_0012.thumbnail.JPG"/></li>
 *	</ul>
 *
 *  -: or :-
 * 
 * <div>
 * 		<img src="common/img/dsc_0003.thumbnail.JPG"/>
 * 		<img src="common/img/dsc_0012.thumbnail.JPG"/>
 * </div>
 * 
 * @param Integer getUrlBy
 * 					By default, it is set to 0 (zero) and the plugin will
 * 					get the url of the full size img from the images 
 * 					parent A tag, or you can set it to 1 will and provide
 * 					the fullSizePath param with the path to the full size
 * 					images.  Finally, you can set it to 2 and provide text
 * 					to prefix param and have that prefix removed from the
 * 					src tag of the thumbnail to create the path to the
 * 					full sized image
 * 
 * @example $('#gallery').jqGalViewII({getUrlBy:1,fullSizePath:'fullPath/to/fullsize/folder'});
 * 
 * @example $('#gallery').jqGalViewII({getUrlBy:2, prefix:'.tn'});
 * 					".tn" gets removed from the src attribute of your image
 * 
 * @param String fullSizePath
 * 					Set to null by default, but if you are going to set
 * 					getUrlBy param to 1, you need to provide the full path
 * 					to the full size image.
 * 
 * @example $('#gallery').jqGalViewII({getUrlBy:1,fullSizePath:'fullPath/to/fullsize/folder'});
 * 
 * @param String prefix
 * 					Set to null by default, but if you are going to set
 * 					getUrlBy param to 2, you need to provide text you
 * 					want to remove from the src attribute of the thumbnail
 * 					to get create the full size image name
 * 
 * @example $('ul').jqGalViewII({getUrlBy:2, prefix:'.tn'});
 * 					".tn" gets removed from the src attribute of your image
 * 
 * @styleClasses
 * 		gvIIContainer:  overall holder of thumbnails and gvIIHolder div, the
 * 						gvIILoader div and the gvIIImgContainer div
 * 		gvIIHolder: contains the thumbnails divs
 *		gvIIItem: contains the thumbnail img, the gvLoaderMini div and the gvOpen div
 *		gvIILoaderMini :empty but styled with a loader images as background image.
 * 		gvIIImgContainer: the full size image container and the gvDescText div
 * 		gvIILoader: empty but styled with a loader images as background image.
 * 
 * 
 * changes:
 *
 */
(function($){
	$.fn.jqGalViewII = function(options){
		return this.each(function(i){
			var el = this;
			el.jqthis = $(this);
			el.jqchildren = el.jqthis.find('img');
			el.opts = $.extend({}, jqGalViewII.defaults, options);
			el.index = i;
			el.totalChildren = el.jqchildren.size();
			el.jqjqviewii = jqGalViewII.swapOut(el);
			
			el.container = $('<div class="gvIIContainer">').appendTo(el.jqjqviewii);

			el.mainImgContainer = $('<div class="gvIIImgContainer">').appendTo(el.container);
			el.image = $('<img style="display:none;"/>').appendTo(el.mainImgContainer);
			el.loader = $('<div class="gvIILoader"/>').appendTo(el.mainImgContainer);
			el.altTextBox = $('<div class="gvIIAltText"/>').appendTo(el.mainImgContainer);
			el.holder = $('<div class="gvIIHolder"/>').appendTo(el.container);
			
			if (el.opts.originalLink==true)
			{
				el.image.click(function() 
					{
						try
						{
							//vygeneruj odkaz na original obrazok
							var data = this.src.split("/");
							data[data.length-1] = "o_"+data[data.length-1];
							var novaCesta = "";
							for (c=0; c<data.length; c++)
							{
								if (c>0) novaCesta += "/";
								novaCesta += data[c];
							}
							window.open(novaCesta);
						} catch (e) {}
					});
			}
			el.jqthis.after(el.jqjqviewii).remove();

			el.imgCw = el.mainImgContainer.width();
			el.imgCh = el.mainImgContainer.height();

			el.jqchildren.each(function(j){
				var jqimage = $(this);
				var tmpimage = this;
				
				tmpimage.index = j;

				var jqdiv = $('<div id="gvIIID'+j+'" class="gvIIItem">')
				.appendTo(el.holder)
				.append('<div class="gvIILoaderMini">');// end : $div
				
				if(el.opts.getUrlBy == 0){
					tmpimage.altImg = jqimage.parent().attr('href');
				}
				else if(el.opts.getUrlBy == 1){
					tmpimage.altImg = el.opts.fullSizePath + tmpimage.src.split('/').pop();
				}
				else if(el.opts.getUrlBy == 2){
					tmpimage.altImg = tmpimage.src.replace(el.opts.prefix,'');
				};
				
				this.altTxt = jqimage.attr('alt');
				
				var image = new Image();
				image.onload = function(){
					image.onload = null;
					jqdiv.empty().append(jqimage);
					
					var margins = jqGalViewII.center({"w":jqdiv.width(),"h":jqdiv.height()},{"w":image.width,"h":image.height});

					//WebJET
					//jqimage.css({marginLeft:margins.l,marginTop:margins.t});
					var largeImage = new Image();
					largeImage.onload = function(){
						largeImage.onload = null;

						$('<div class="gvIIFlash">').appendTo(jqdiv).css({opacity:".01"})
						.mouseover(
							function(){
								var $f = $(this);
								$f.css({opacity:".75"}).stop().animate({opacity:".01"},500);
							}
						)						
						.click(function(){							
								jqimage.trigger('click');
						}).trigger('mouseover');						

						jqimage.click(function(){
							jqGalViewII.view(this,el);		   
						})
						//WebJET
						var ml = margins.l+"px";
						var mt = margins.t+"px";
						jqimage.css({marginLeft:ml,marginTop:mt});

						if( tmpimage.index  == 0 ){
							jqimage.trigger('click');
							jqimage.siblings().trigger('mouseover');
						};
					};  // end : largeImage.onload 
					largeImage.src = tmpimage.altImg;					
				};// end : image.onload 
				image.src = tmpimage.src;
			});
		});
	};

	jqGalViewII = {
		//pDem parent deminsions
		//iDem img deminsions
		resize : function(pDem,iDem){
			if (iDem.w > pDem.w) {
				iDem.h = iDem.h * (pDem.w / iDem.w); 
				iDem.w = pDem.w; 
				if (iDem.h > pDem.w) { 
					iDem.w = iDem.w * (pDem.h / iDem.h); 
					iDem.h = pDem.h; 
				};
			} else if (iDem.h > pDem.h) { 
				iDem.w = iDem.w * (pDem.h / iDem.h); 
				iDem.h = pDem.h; 
				if (iDem.w > pDem.w) { 
					iDem.h = iDem.h * (pDem.w /iDem.w); 
					iDem.w = pDem.w;
				};
			};
			
			return iDem;
		},
		center : function(pDem,iDem){
			return { "l":(pDem.w-iDem.w)*.5, "t": (pDem.h-iDem.h)*.5 };
		},
		swapOut : function(el){
			return $('<div id="jqgvii'+el.index+'">');
		},
		view : function(img, el){
			if(typeof img.altImg == 'undefined') return false;
			var url = img.altImg;
			if(/picasa/.test(url)){
				url = /\?/.test(img.altImg) ? '&imgmax=800' : '?imgmax=800';
			};
			
			el.loader.show();		
			
			image = new Image();
			image.onload = function(){
				image.onload = null;
				dem = {};
				dem.w = $wOrg = image.width;
				dem.h = $hOrg = image.height;
				dem = jqGalViewII.resize({"w":el.imgCw,"h":el.imgCh},{"w":dem.w,"h":dem.h});

				var margins = jqGalViewII.center({"w":el.imgCw,"h":el.imgCh},{"w":dem.w,"h":dem.h});
	
				el.image.css({width:dem.w,height:dem.h, marginLeft:margins.l,marginTop:margins.t});
				el.loader.fadeOut('fast');
				el.altTextBox.fadeTo('fast', 0.1);
				el.image.fadeOut('fast',function(){
					el.image.attr('src',url).fadeIn();
					if(typeof img.altTxt != 'undefined'){
						el.altTextBox.fadeTo("fast",el.opts.titleOpacity).text(img.altTxt);
					};
				});
				
				/*
				el.image.click(function(){
					var src = img.altImg;
					window.alert(img.src);
					// thickbox execution code
					
				});
				*/				
			};
			image.src = url;
						
		},
		defaults : {
			getUrlBy : 0, // 0 == from parent A tag | 1 == the full size resides in another folder
			fullSizePath : null,
			originalLink : false,
			prefix: 'thumbnail.',
			titleOpacity : .60
		}
	};
})(jQuery);