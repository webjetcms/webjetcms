
$(function() {
	setTimeout(function(){
		if(renderer) {
            renderer.setAfterRenderCallback(function(self) {
                // remove item
                $("#editorWrapper .removeItem").on('click', function(e) {
                    e.preventDefault();
                    $(this).closest('.item').remove();
                    self.editorItemsList.setDataFromDom($(self.selector + " .item"));
                });

                // choose image
                $('#editorWrapper .imageDiv').on('click', function(e) {
                    e.preventDefault();
                    console.log($(this).closest('.item').attr('id'));
                    openImageDialogWindow($(this).closest('.item').attr('id'), "image", null);

                    //perexImageBlur(document.getElementById($(this).closest('.item').find('input[name=image]').attr('id')));
                });

                $('#editorWrapper input[name=image]').on('change', function() {
                    console.log('tu som blur!');
                    console.log(this);
                    //perexImageBlur(document.getElementById($(this).attr('id')));
                    $(this).closest('.item').find('.imageDiv img').attr('src', "/thumb"+$(this).val()+"?w=100&h=100&ip=5");
                });

                // show redirect url
                $('#editorWrapper .editorItemCheckbox').on('click', function() {
                    var itemVal = $(this).closest('.propertyWrapper').find('.editorItemValue').val();
                    if(itemVal != undefined && itemVal != "") {
                        $(this).closest('.propertyWrapper').find('.editorItemValue').val("");
                    }
                    $(this).closest('.propertyWrapper').find('.editorItemValue').toggle();
                });

                // choose redirect url
                $('#editorWrapper input[name=redirectUrl]').on('click', function(e) {
                    e.preventDefault();
                    openLinkDialogWindow($(this).closest('.item').attr('id'), "redirectUrl", null, null);
                });

                // show/hide each redirect url
                $('#editorWrapper .editorItemCheckbox').each(function() {
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
                $('#editorWrapper .imageDiv').each(function() {
                    if($(this).closest('.item').find('input[name=image]').val() != "" && $(this).closest('.item').find('input[name=image]').val() != undefined) {
                        //perexImageBlur(document.getElementById($(this).closest('.item').find('input[name=image]').attr('id')));
                    }
                });



                if($('.colorpicker-rgba').length && jQuery().minicolors){
                    $('.colorpicker-rgba').minicolors({
                        "format":"rgb",
                        "opacity":true,
                        show: function() {

                            // disabling dragable wrapper of item
                            $('.editorWrapper').sortable( "disable" );
                        },
                        hide: function() {
                            $('.editorWrapper').sortable( "enable" );
                        }
                    });
                }
                if($('.rozsireneLink').length){
                    addClickEvent();
                    $('.rozsireneLink').each(function(){
                        var dataParentField = $(this).data('parentfield');
                        var dataId = $(this).data('id');
                        $('#'+dataParentField+dataId).addClass('inputWithIcon').after($(this));
                    });
                }
            });



		 //collapse menu
	    $('.editorWrapper.collapsable').each(function() { // pridam collapse menu
	        var collapseText = $('.editorWrapper.collapsable').data('collapse');
	        var expandText = $('.editorWrapper.collapsable').data('expand');
	        if(collapseText == undefined){collapseText = "";}
	        if(expandText == undefined){expandText = "";}

	        var insertHtml = '<div class="collapse_menu"> <a class="button_collapse"><span class="collapseSpan">';
	        insertHtml+=collapseText;
	        insertHtml+='</span><span class="expandSpan" style="display: none;">';
	        insertHtml+=expandText;
	        insertHtml+='</span> <img src="/components/json_editor/icon_arrow.png" /></a></div>';
	        $(this).before(insertHtml);
	    });

	    $('a.button_collapse').click(function(event) {
	    	var spanCollapse = $(this).find('span.collapseSpan');
	         var spanExpand = $(this).find('span.expandSpan');

	    	if($(this).hasClass('collapsed')){
	    		$('.editorWrapper .item').removeClass('collapsed');
	    		$(this).removeClass('collapsed');
	    		spanExpand.fadeOut(function(){
	    			spanCollapse.fadeIn();
	    		});

	    }else{
	    	 $('.editorWrapper .item').addClass('collapsed');
	    	 $(this).addClass('collapsed');
	    	 spanCollapse.fadeOut(function(){
	    		 spanExpand.fadeIn();
	    	 });
	    }

	    });


	    $('#addItem').on('click', function(e) {
	        e.preventDefault();
	        editorItemsList.setDataFromDom($("#editorWrapper .item"));
	        editorItemsList.addNewItem();
	        renderer.render();
	         if($('a.button_collapse').hasClass('collapsed')){
				$("#editorWrapper .item").addClass('collapsed');
	         }

	    });
	    renderer.render();
        }
	}, 500);
});