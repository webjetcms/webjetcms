(function( $ ) {
 
    $.fn.pagination = function(options) {
    	
    	var elements = this;
 		var settings = $.extend({
 			items: 0,
	        itemsOnPage: 10,
	        paginationElement: '.pagination',
	        cssStyle: 'light-theme',
	        onPageClick: function(pageNumber, event){	        	
	        	var start = settings.itemsOnPage * (pageNumber - 1);
	        	var end = start + settings.itemsOnPage;
	        	
	        	elements.hide().slice(start, end).fadeIn().css("display", "");
	        	window.location.hash = settings.clickId;
	        },
	        prevText: "&laquo;",
	        nextText: "&raquo;"
        }, options );

 		if (settings.items == 0) {
 			settings.items = this.length;
 		}

 		if (settings.items == 0) {
 			console.warn('Gallery has 0 elements');
 			return this;
 		}
 		
		//$('head').append('<link rel="stylesheet" type="text/css" media="screen" href="/components/gallery/pagination/simplePagination.css" />');

        $(settings.paginationElement).simplePagination({
	        items: settings.items,
	        itemsOnPage: settings.itemsOnPage,
	        cssStyle: settings.cssStyle,
	        listStyle: 'pagination',
	        onPageClick: settings.onPageClick,
	        prevText: settings.prevText,
	        nextText: settings.nextText
	    });
        
        $('#thumbs'+options.galleryCounter+' li').hide().slice(0, settings.itemsOnPage).fadeIn().css("display", "");

        return this.each(function() {
        	
        }); 
    };
 
}( jQuery ));

/* Thanks to CSS Tricks for pointing out this bit of jQuery
http://css-tricks.com/equal-height-blocks-in-rows/
It's been modified into a function called at page load and then each time the page is resized. One large modification was to remove the set height before each new calculation. */

equalheight = function(container){

	var currentTallest = 0,
	     currentRowStart = 0,
	     rowDivs = new Array(),
	     $el,
	     topPosition = 0;
	 $(container).each(function() {
	
	   $el = $(this);
	   $($el).height('auto')
	   topPostion = $el.position().top;
	
	   if (currentRowStart != topPostion) {
	     for (currentDiv = 0 ; currentDiv < rowDivs.length ; currentDiv++) {
	       rowDivs[currentDiv].height(currentTallest);
	     }
	     rowDivs.length = 0; // empty the array
	     currentRowStart = topPostion;
	     currentTallest = $el.height();
	     rowDivs.push($el);
	   } else {
	     rowDivs.push($el);
	     currentTallest = (currentTallest < $el.height()) ? ($el.height()) : (currentTallest);
	  }
	   for (currentDiv = 0 ; currentDiv < rowDivs.length ; currentDiv++) {
	     rowDivs[currentDiv].height(currentTallest);
	   }
	 });
}
$(window).on('load', function(){
  equalheight('#thumbs li');
});


$(window).resize(function(){
  equalheight('#thumbs li');
});

