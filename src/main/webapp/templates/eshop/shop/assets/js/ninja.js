$(document).ready(function () {

    Ninja.init({
        debug: true
    });

    $("[data-toggle='tooltip']").tooltip();

    $("select[multiple]").selectpicker({
        styleBase: "form-control",
        style: "btn btn-outline-secondary",
    });

    var body = $("body");
    var menuToggler = $(".md-navigation .menu-toggler");

    menuToggler.click(function () {
        body.toggleClass("side-menu-open");
    });

    var isMobile = false;

    var mobileMenuHolder = document.createElement("div");
    mobileMenuHolder = $(mobileMenuHolder).addClass("md-mobile-menu");

    body.append(mobileMenuHolder);

    var menu = $("#menu");
    var menuOrigin = menu.parent();

    checkMenu();

    $(window).resize(function(){
        checkMenu();
    });

    function checkMenu(){
        isMobile = menuToggler.is(":visible");
        if(isMobile){
            mobileMenuHolder.append(menu);
        } else{
            menuOrigin.append(menu);
        }
    }



    var mainSlider = $(".md-product-detail .main-slider.slider");
    var subSlider = $(".md-product-detail .sub-slider.slider");

    mainSlider.slick({
        infinite: true,
        slidesToShow: 1,
        prevArrow: "<button type='button' class='slick-prev'><i class='fas fa-angle-left'></i></button>",
        nextArrow: "<button type='button' class='slick-next'><i class='fas fa-angle-right'></i></button>",
        mobileFirst: true,
        swipeToSlide: true
    });

    subSlider.slick({
        infinite: true,
        slidesToShow: 4,
        prevArrow: "<button type='button' class='slick-prev'><i class='fas fa-angle-left'></i></button>",
        nextArrow: "<button type='button' class='slick-next'><i class='fas fa-angle-right'></i></button>",
        mobileFirst: true,
        swipeToSlide: true,
        asNavFor: mainSlider,
        focusOnSelect: true
    });

    subSlider.on('click', 'img', function(){
        var index = $(this).attr("data-slick-index");
        $(".md-product-detail .main-slider.slider").slick("slickGoTo", parseInt(index));
    });

    $(".md-product-list.list-type-slider .slider").slick({
        infinite: true,
        slidesToShow: 3,
        prevArrow: "<button type='button' class='slick-prev'>Prev</button>",
        nextArrow: "<button type='button' class='slick-next'>Next</button>",
        mobileFirst: true,
        swipeToSlide: true,
        responsive: [
            {
                breakpoint: 992,
                settings: {
                    slidesToShow: 3
                }
            },
            {
                breakpoint: 576,
                settings: {
                    slidesToShow: 2
                }
            },
            {
                breakpoint: 0,
                settings: {
                    slidesToShow: 1
                }
            }
        ]
    });

    $(".buy .controls button").click(function () {
        var input = $(".qty-input");
        if (input.is(":disabled")) return;
        if ($(this).hasClass("add")) {
            input.val(parseInt(input.val()) + 1);
            console.log("add");
        }
        if ($(this).hasClass("remove")) {
            input.val(parseInt(input.val()) - 1);
            console.log("remove");
        }

        if (parseInt(input.val()) < 1) {
            input.val(1);
        }
    });

});