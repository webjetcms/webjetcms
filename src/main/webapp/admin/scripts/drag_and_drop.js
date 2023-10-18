
$(document).ready(function(){
    var insideHover = 0;
    var appItem = $("#apps li a"),
        body =$("body");
    setTimeout(function(){

        $(".draggable").draggable({
            revert: true,
            helper: "clone",
            cursor: "move",
            scroll: true,
            refreshPositions: true,
            cursorAt: { top: 25, left: 25 },
            connectToSortable: 'div#wjInline-docdata',
            start: function() {
                var content = $(this).attr("data-rel");
                $(this).addClass("move");
                body.addClass("dragObject");
                $("div.droppable").remove();

                $('<div class="droppable ui-droppable"></div>').prependTo('div#wjInline-docdata > section');
                $('<div class="droppable ui-droppable"></div>').insertAfter("div#wjInline-docdata > .row, div#wjInline-docdata > .insertedContent, div#wjInline-docdata > section");
                $('<div class="droppable ui-droppable"></div>').appendTo("div#wjInline-docdata section div[class^=col-]");

                $('div.droppable').hide();
                var droppableLenght = $("div.droppable").length;
                if(droppableLenght <= 1){
                    $("div.droppable").show();
                }
                else{
                    $("div.droppable").hide();
                }

               $("div#wjInline-docdata > .insertedContent, div#wjInline-docdata section").addClass("hover");

                $("div#wjInline-docdata section div[class^=col-]").addClass("hover-inside");
            },
            drag: function(event) {
                insideHover = 0;
                $("div#wjInline-docdata .hover-inside").each(function(){
                    var height  = $(this).outerHeight(),
                        width   = $(this).outerWidth(),
                        offset  = $(this).offset();

                    insideHover++;
                    if(event.pageY > offset.top && event.pageX > offset.left && event.pageX < (offset.left + width) && event.pageY < (offset.top + height)) {
                    $(this).find("div.droppable").show();
                    }else{
                        $(this).find("div.droppable").hide();
                        insideHover--;
                    }
                });
        console.log("hover: "+insideHover)
                $("div#wjInline-docdata > .hover").each(function(){
                    var height  = $(this).outerHeight(),
                        width   = $(this).outerWidth(),
                        offset  = $(this).offset();

                    if(event.pageY > offset.top && event.pageX > offset.left && event.pageX < (offset.left + width)) {
                        if ((event.pageY - offset.top) > (height / 2)){
                            if(insideHover<=0) {
                                $("div.droppable").hide();
                            }
                            $(this).next().show();
                        }
                        else {
                            if(insideHover<=0) {
                                $("div.droppable").hide()
                            }
                            $(this).prev().show();
                        }
                    }
                });

                bindDropable();
            },
            stop: function() {
                $("div#wjInline-docdata > .row, div#wjInline-docdata > .insertedContent, div#wjInline-docdata > section").removeClass("hover").unbind();
                $(this).removeClass("move");
                body.removeClass("dragObject");
                $('.droppable').unbind();
                $("div.droppable").remove();
            }
        });
    },1000);
});

function bindDropable(){
    $(".droppable").droppable({
        scrollSensitivity: 100,
        addClasses: true,
        activeClass: "ui-state-highlight",
        classes: {
            "ui-droppable-hover": "ui-state-hover"
        },
        drop: function( event, ui ) {
            var content = $(".move").attr("data-rel");

            if ( $(".move").hasClass("component") ) {
                $(this).html(content).removeClass("ui-droppable").removeClass("droppable").addClass("insertedComponent insertedContent");

                componentDrop();
            }
            else {
                $(this).load(content, function(){
                    if($(this).html().indexOf("!INCLUDE")>(-1)) {
                        //ak ma blok v sebe komponentu
                        var editorInstance = CKEDITOR.instances["wjInline-docdata"];
                        editorInstance.setData(editorInstance.getData());
                    }
                }).removeClass("ui-droppable").removeClass("droppable").addClass("insertedContent");
            }
        }
    });
};

function componentDrop() {
    //refreshnem content aby sa zobrazil nahlad komponenty
    var editorInstance =  CKEDITOR.instances["wjInline-docdata"];
    editorInstance.setData(editorInstance.getData(), function () {

        window.setTimeout(function(){
            $('.insertedComponent iframe').first().attr("id","actuallyEditedComponent");
            editorInstance.execCommand("webjetcomponentsDialog");
            $('.insertedComponent').removeClass("insertedComponent");


        },1000);
    });
};