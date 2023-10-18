$(document).ready(function(){
	$('head').append('<link rel="stylesheet" type="text/css" media="screen" href="/components/gallery/prettyphoto/css/prettyPhoto.css" />');

	var twitterText = encodeURIComponent("Dummy twitter text");
	var url = encodeURIComponent(window.location.href);
	
	var social = '<a href="javascript:;" class="share">Share</a>';
	var socialBox = '<div class="pp_social_box">';
	socialBox += '<a class="share-facebook" target="_BLANK" href="https://www.facebook.com/sharer/sharer.php?u=' + url + '">Zdielať na Facebooku</a>';
	socialBox += '<a class="share-twitter" target="_BLANK" href="https://twitter.com/intent/tweet?text=' + twitterText + '&amp;url=' + url + '">Tweet</a>';
	socialBox += '<a class="share-download" download target="_BLANK" href="javascript:;">Stiahnuť originál obrázok</a>';
	socialBox += '</div>';
	
	var prettyphoto = $(".thumbs a[rel^='prettyPhoto']").prettyPhoto({
		social_tools: social,
		slideshow: 5000,
		autoplay_slideshow: false
	});
	
	$('body').on('click', '.pp_social_box a', function(){
		$('.pp_social_box').hide();
		$('.pp_social_overlay').hide();
	});
	
	$('body').on('click', 'div.pp_social_overlay', function(event){
		event.stopPropagation();
		$('.pp_social_overlay, .pp_social_box').hide();
	});
	
	$('body').on('click', 'a.share', function(){
		$.prettyPhoto.stopSlideshow();
		
		if ($('.pp_social_box').length == 0) {
			$('body').append(socialBox);
		}
		
		var left = $('a.share').offset().left;
		var top = $('a.share').offset().top;
		
		$('.pp_social_box').css({
			left: left - 163,
			top: top + 30
		});
		
		var href = $(this).parents('.pp_content').first().find('#pp_full_res img').prop('src');
		$('.share-download').prop('href', href);
		$('.fb-like').data('href', href);
		
		if ($('.pp_social_overlay').length == 0) {
			$('body').append('<div class="pp_social_overlay"></div>');
		}
		
		if ($('.pp_social_box').is(':visible')) {
			$('.pp_social_box').hide();
			$('.pp_social_overlay').hide();
		}
		else {
			$('.pp_social_box').fadeIn();
			$('.pp_social_overlay').show();
		}
	});
});