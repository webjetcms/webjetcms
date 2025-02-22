$(document).ready(function() {

    Ninja.init({
        debug:true
    });

    try {
        AOS.init({
            once: true
        });
    } catch (e) {}

    $("a.showMenu").click(function() {
		$("#menuBox").toggleClass("active");
    });

    $("div.radio a").click(function(){
        $(this).parent("div.radio").find(".btn-primary").removeClass("btn-primary");
        $(this).addClass("btn-primary");
    })

});