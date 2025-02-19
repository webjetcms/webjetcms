/**
Core script to handle the entire theme and core functions
**/
var Layout = function () {

    var layoutImgPath = Metronic.getAssetsPath() + 'admin/layout/img/';

    var layoutCssPath = Metronic.getAssetsPath() + 'admin/layout/css/';

    var isTablet = (navigator.userAgent.indexOf("iPad")!=-1 || navigator.userAgent.indexOf("iPhone")!=-1 || navigator.userAgent.indexOf("Android")!=-1);

    //* BEGIN:CORE HANDLERS *//
    // this function handles responsive layout on screen size resize or mobile device rotate.

    // Set proper height for sidebar and content. The content and sidebar height must be synced always.
    var handleSidebarAndContentHeight = function () {
        var content = $('.page-content');
        var sidebar = $('.page-sidebar');
        var body = $('body');
        var height;

        //console.log("Layout.fixContentHeight(); - som dnu");

        if (body.hasClass("page-footer-fixed") === true && body.hasClass("page-sidebar-fixed") === false)
        {
            var available_height = Metronic.getViewPort().height - $('.page-header').outerHeight();
            if (content.height() < available_height)
            {
            	//console.log("Setting height 1 to: "+available_height);
                content.attr('style', 'min-height:' + available_height + 'px');
            }
        }
        else
        {
            if (body.hasClass('page-sidebar-fixed'))
            {
            	//console.log("Layout.fixContentHeight(); - sidebar fixed");

                height = _calculateFixedSidebarViewportHeight();

                //console.log("Layout.fixContentHeight(); - height="+height);

                if (body.hasClass('page-footer-fixed') === false)
                {
					//console.log("Setting height 2 to: "+height);

                    height = height - parseInt(content.css("margin-top")); // - parseInt($(".page-container").css("margin-top"));

                    //console.log("Setting height 2b to: "+height);
                }
            }
            else
            {
                var headerHeight = $('.page-header').outerHeight();
                var footerHeight = 0; // $('.page-footer').outerHeight();

                if (isTablet)

                {
                    height = Metronic.getViewPort().height - headerHeight - footerHeight;
                }
                else
                {
                    height = sidebar.height() + 20;
                }

                if ((height + headerHeight + footerHeight) <= Metronic.getViewPort().height) {
                    height = Metronic.getViewPort().height - headerHeight - footerHeight;
                }

                //console.log("Layout.fixContentHeight(); - height 2="+height);
            }

            var wjHeight = ($(".page-container").height() - parseInt(content.css("margin-top")) );
            //console.log("wjHeight="+wjHeight);
            if (wjHeight > height) height = wjHeight;

            //console.log("Layout.fixContentHeight(); - setting height="+height+" pagecontainer="+$(".page-container").height()+" header="+$('.page-header').outerHeight());
            content.attr('style', 'min-height:' + height + 'px');

        }
    };

    // Handle sidebar menu links
    var handleSidebarMenuActiveLink = function(mode, el) {
        var url = location.hash.toLowerCase();

        var menu = $('.page-sidebar-menu');

        if (mode === 'click' || mode === 'set') {
            el = $(el);
        } else {
            menu.find("li > a").each(function() {
                var path = $(this).attr("href").toLowerCase();
                //alert(url.substr(1, path.length - 1) + '-' + path.substr(1));
                if (path.length > 1 && url.substr(1, path.length - 1) == path.substr(1)) {
                    el = $(this);
                    return;
                }
            });
        }

        if (el && (el.attr('href').toLowerCase() === 'javascript:;' || el.attr('href').toLowerCase() === '#')) {
            return;
        }

        if (!el || el.size() == 0) {
            return;
        }

        Metronic.scrollTop();

        // disable current active nodes
        menu.find('li.active').removeClass('active');
        menu.find('li.open').removeClass('open');
        menu.find('arrow.open').removeClass('open');
        menu.find('li > a > .selected').remove();

        el.parents('li').each(function () {
            $(this).addClass('active');
            $(this).find('> a > span.arrow').addClass('open');
            if ($(this).parent('ul').size() === 1) {
                $(this).addClass('open');
                $(this).find('> a').append('<span class="selected"></span>');
            }
        });

        if (mode === 'click') {
            if (Metronic.getViewPort().width < 992 && $('.page-sidebar').hasClass("in")) { // close the menu on mobile view while laoding a page
                $('.page-header .responsive-toggler').click();
            }
        }
    };

    // Handle sidebar menu
    var handleSidebarMenu = function () {
        // handle sidebar link click
        jQuery('.page-sidebar').on('click', 'li > a', function (e) {
            if (Metronic.getViewPort().width >= 992 && $(this).parents('.page-sidebar-menu-hover-submenu').size() === 1) { // exit of hover sidebar menu
                return;
            }

            if ($(this).next().hasClass('sub-menu') === false) {
                if (Metronic.getViewPort().width < 992 && $('.page-sidebar').hasClass("in")) { // close the menu on mobile view while laoding a page
                    $('.page-header .responsive-toggler').click();
                }
                return;
            }

            if ($(this).next().hasClass('sub-menu always-open')) {
                return;
            }

            var parent = $(this).parent().parent();
            var the = $(this);
            var menu = $('.page-sidebar-menu');
            var sub = jQuery(this).next();

            var autoScroll = menu.data("auto-scroll");
            var slideSpeed = parseInt(menu.data("slide-speed"));
            var keepExpand = menu.data("keep-expanded");

            if (keepExpand !== true) {
                parent.children('li.open').children('a').children('.arrow').removeClass('open');
                parent.children('li.open').children('.sub-menu:not(.always-open)').slideUp(slideSpeed);
                parent.children('li.open').removeClass('open');
            }

            var slideOffeset = -200;

            if (sub.is(":visible")) {
                jQuery('.arrow', jQuery(this)).removeClass("open");
                jQuery(this).parent().removeClass("open");
                sub.slideUp(slideSpeed, function () {
                    if (autoScroll === true && $('body').hasClass('page-sidebar-closed') === false) {
                        if ($('body').hasClass('page-sidebar-fixed')) {
                            menu.slimScroll({
                                'scrollTo': (the.position()).top
                            });
                        } else {
                            Metronic.scrollTo(the, slideOffeset);
                        }
                    }
                    handleSidebarAndContentHeight();
                });
            } else {
                jQuery('.arrow', jQuery(this)).addClass("open");
                jQuery(this).parent().addClass("open");
                sub.slideDown(slideSpeed, function () {
                    if (autoScroll === true && $('body').hasClass('page-sidebar-closed') === false) {
                        if ($('body').hasClass('page-sidebar-fixed')) {
                            menu.slimScroll({
                                'scrollTo': (the.position()).top
                            });
                        } else {
                            Metronic.scrollTo(the, slideOffeset);
                        }
                    }
                    handleSidebarAndContentHeight();
                });
            }

            e.preventDefault();
        });

        // handle ajax links within sidebar menu
        jQuery('.page-sidebar').on('click', ' li > a.ajaxify', function (e) {
            e.preventDefault();
            Metronic.scrollTop();

            var url = $(this).attr("href");
            var menuContainer = jQuery('.page-sidebar ul');
            var pageContent = $('.page-content');
            var pageContentBody = $('.page-content .page-content-body');

            menuContainer.children('li.active').removeClass('active');
            menuContainer.children('arrow.open').removeClass('open');

            $(this).parents('li').each(function () {
                $(this).addClass('active');
                $(this).children('a > span.arrow').addClass('open');
            });
            $(this).parents('li').addClass('active');

            if (Metronic.getViewPort().width < 992 && $('.page-sidebar').hasClass("in")) { // close the menu on mobile view while laoding a page
                $('.page-header .responsive-toggler').click();
            }

            Metronic.startPageLoading({animate: true});

            var the = $(this);

            $.ajax({
                type: "GET",
                cache: false,
                url: url,
                dataType: "html",
                success: function (res) {

                    if (the.parents('li.open').size() === 0) {
                        $('.page-sidebar-menu > li.open > a').click();
                    }

                    Metronic.stopPageLoading();
                    pageContentBody.html(res);
                    Layout.fixContentHeight(); // fix content height
                    Metronic.initAjax(); // initialize core stuff
                },
                error: function (xhr, ajaxOptions, thrownError) {
                    Metronic.stopPageLoading();
                    pageContentBody.html('<h4>Could not load the requested content.</h4>');
                }
            });
        });

        // handle ajax link within main content
        jQuery('.page-content').on('click', '.ajaxify', function (e) {
            e.preventDefault();
            Metronic.scrollTop();

            var url = $(this).attr("href");
            var pageContent = $('.page-content');
            var pageContentBody = $('.page-content .page-content-body');

            Metronic.startPageLoading({animate: true});

            if (Metronic.getViewPort().width < 992 && $('.page-sidebar').hasClass("in")) { // close the menu on mobile view while laoding a page
                $('.page-header .responsive-toggler').click();
            }

            $.ajax({
                type: "GET",
                cache: false,
                url: url,
                dataType: "html",
                success: function (res) {
                    Metronic.stopPageLoading();
                    pageContentBody.html(res);
                    Layout.fixContentHeight(); // fix content height
                    Metronic.initAjax(); // initialize core stuff
                },
                error: function (xhr, ajaxOptions, thrownError) {
                    pageContentBody.html('<h4>Could not load the requested content.</h4>');
                    Metronic.stopPageLoading();
                }
            });
        });

        // handle sidebar hover effect
        handleFixedSidebarHoverEffect();

        // handle the search bar close
        $('.page-sidebar').on('click', '.sidebar-search .remove', function (e) {
            e.preventDefault();
            $('.sidebar-search').removeClass("open");
        });

        // handle the search query submit on enter press
        $('.page-sidebar .sidebar-search').on('keypress', 'input.form-control', function (e) {
            if (e.which == 13) {
                $('.sidebar-search').submit();
                return false; //<---- Add this line
            }
        });

        // handle the search submit(for sidebar search and responsive mode of the header search)
        $('.sidebar-search .submit').on('click', function (e) {
            e.preventDefault();
            if ($('body').hasClass("page-sidebar-closed")) {
                if ($('.sidebar-search').hasClass('open') === false) {
                    if ($('.page-sidebar-fixed').size() === 1) {
                        $('.page-sidebar .sidebar-toggler').click(); //trigger sidebar toggle button
                    }
                    $('.sidebar-search').addClass("open");
                } else {
                    $('.sidebar-search').submit();
                }
            } else {
                $('.sidebar-search').submit();
            }
        });

        // handle close on body click
        if ($('.sidebar-search').size() !== 0) {
            $('.sidebar-search .input-group').on('click', function(e){
                e.stopPropagation();
            });

            $('body').on('click', function() {
                if ($('.sidebar-search').hasClass('open')) {
                    $('.sidebar-search').removeClass("open");
                }
            });
        }
    };

    // Helper function to calculate sidebar height for fixed sidebar layout.
    var _calculateFixedSidebarViewportHeight = function () {
        var sidebarHeight = Metronic.getViewPort().height - $('.page-header').outerHeight();
        if ($('body').hasClass("page-footer-fixed")) {
            sidebarHeight = sidebarHeight - $('.page-footer').outerHeight();
        }

        return sidebarHeight;
    };

    // Handles fixed sidebar
    var handleFixedSidebar = function () {
        var menu = $('.page-sidebar-menu');

        //Metronic.destroySlimScroll(menu);

        if ($('.page-sidebar-fixed').size() === 0) {
            handleSidebarAndContentHeight();
            return;
        }

        if (Metronic.getViewPort().width >= 992)
        {
            //console.log("Layout.handleFixedSidebar(); - setting height to: "+_calculateFixedSidebarViewportHeight());
            var sidebarViewport = _calculateFixedSidebarViewportHeight();
            var largeMenu = $(".md-large-menu").outerHeight();
            var domainToggler = $(".js-domain-toggler").outerHeight();

            if ($("body").hasClass("ly-submenu-active")) sidebarViewport = sidebarViewport + 47;

            //console.log("Layout.handleFixedSidebar(); - sidebarViewport="+sidebarViewport+" largeMenu="+largeMenu+" domainToggler="+domainToggler);

            menu.attr("data-height", sidebarViewport - largeMenu - domainToggler - 40); //40 because bottom scrollbar
            Metronic.initSlimScroll(menu);
            //menu.attr("data-initialized", "1");
            handleSidebarAndContentHeight();
        }
    };

    // Handles sidebar toggler to close/hide the sidebar.
    var handleFixedSidebarHoverEffect = function () {

        var body = $('body');
        if (body.hasClass('page-sidebar-fixed')) {
            $('.page-sidebar').on('mouseenter', function () {
                if (body.hasClass('page-sidebar-closed')) {
                    $(this).find('.page-sidebar-menu').removeClass('page-sidebar-menu-closed');
                }
            }).on('mouseleave', function () {
                if (body.hasClass('page-sidebar-closed')) {
                    $(this).find('.page-sidebar-menu').addClass('page-sidebar-menu-closed');
                }
            });
        }

    };

    // Hanles sidebar toggler
    var handleSidebarToggler = function () {
        var body = $('body');
        if ($.cookie && $.cookie('sidebar_closed') === '1' && Metronic.getViewPort().width >= 992) {
            $('body').addClass('page-sidebar-closed');
            $('.page-sidebar-menu').addClass('page-sidebar-menu-closed');
        }

        // handle sidebar show/hide
        $('.page-sidebar, .page-header').on('click', '.sidebar-toggler', function (e) {

            var sidebar = $('.page-sidebar');
            var sidebarMenu = $('.page-sidebar-menu');
            $(".sidebar-search", sidebar).removeClass("open");

            if (body.hasClass("page-sidebar-closed")) {
                body.removeClass("page-sidebar-closed");
                sidebarMenu.removeClass("page-sidebar-menu-closed");
                if ($.cookie) {
                    $.cookie('sidebar_closed', '0');
                }
            } else {
                body.addClass("page-sidebar-closed");
                sidebarMenu.addClass("page-sidebar-menu-closed");

                $('.sidebar-toggler').hide();
                setTimeout(function() {
                	$('.sidebar-toggler').show();
                }, 30);
                console.log('unblur');

                sidebarMenu.closest(".page-sidebar").addClass("martinko");
//                $('.sidebar-toggler').trigger('mouseleave');
//                $('.sidebar-toggler').trigger('mouseout');
//
//                $('.page-sidebar *').each(function() {
//                	$(this).trigger('mouseleave');
//                });
//
                setTimeout(function(){
	                	sidebarMenu.closest(".page-sidebar").removeClass("martinko");
                }, 700);


                if (body.hasClass("page-sidebar-fixed")) {
                    sidebarMenu.trigger("mouseleave");

                }
                if ($.cookie) {
                    $.cookie('sidebar_closed', '1');
                }
            }

            $(window).trigger('resize');
        });
    };

    // Handles the horizontal menu
    var handleHorizontalMenu = function () {
        //handle tab click
        $('.page-header').on('click', '.hor-menu a[data-toggle="tab"]', function (e) {
            e.preventDefault();
            var nav = $(".hor-menu .nav");
            var active_link = nav.find('li.current');
            $('li.active', active_link).removeClass("active");
            $('.selected', active_link).remove();
            var new_link = $(this).parents('li').last();
            new_link.addClass("current");
            new_link.find("a:first").append('<span class="selected"></span>');
        });

        // handle search box expand/collapse
        $('.page-header').on('click', '.search-form', function (e) {
            $(this).addClass("open");
            $(this).find('.form-control').focus();

            $('.page-header .search-form .form-control').on('blur', function (e) {
                $(this).closest('.search-form').removeClass("open");
                $(this).unbind("blur");
            });
        });

        // handle hor menu search form on enter press
        $('.page-header').on('keypress', '.hor-menu .search-form .form-control', function (e) {
            if (e.which == 13) {
                $(this).closest('.search-form').submit();
                return false;
            }
        });

        // handle header search button click
        $('.page-header').on('mousedown', '.search-form.open .submit', function (e) {
            e.preventDefault();
            e.stopPropagation();
            $(this).closest('.search-form').submit();
        });

        // handle hover dropdown menu for desktop devices only
        $('[data-hover="megamenu-dropdown"]').not('.hover-initialized').each(function() {
            $(this).dropdownHover();
            $(this).addClass('hover-initialized');
        });

        $(document).on('click', '.mega-menu-dropdown .dropdown-menu', function (e) {
            e.stopPropagation();
        });
    };

    // Handles Bootstrap Tabs.
    var handleTabs = function () {
        // fix content height on tab click
        $('body').on('shown.bs.tab', 'a[data-toggle="tab"]', function () {
            handleSidebarAndContentHeight();
        });
    };

    // Handles the go to top button at the footer
    var handleGoTop = function () {
        var offset = 300;
        var duration = 500;

        if (navigator.userAgent.match(/iPhone|iPad|iPod/i)) {  // ios supported
            $(window).bind("touchend touchcancel touchleave", function(e){
               if ($(this).scrollTop() > offset) {
                    $('.scroll-to-top').fadeIn(duration);
                } else {
                    $('.scroll-to-top').fadeOut(duration);
                }
            });
        } else {  // general
            $(window).scroll(function() {
                if ($(this).scrollTop() > offset) {
                    $('.scroll-to-top').fadeIn(duration);
                } else {
                    $('.scroll-to-top').fadeOut(duration);
                }
            });
        }

        $('.scroll-to-top').click(function(e) {
            e.preventDefault();
            $('html, body').animate({scrollTop: 0}, duration);
            return false;
        });
    };

    // Hanlde 100% height elements(block, portlet, etc)
    var handle100HeightContent = function () {

        var target = $('.full-height-content');
        var height;

        //console.log("handle100HeightContent a: "+target);

        height = Metronic.getViewPort().height -
            $('.page-header').outerHeight(true) -
            //$('.page-footer').outerHeight(true) -
            $('.page-title').outerHeight(true) -
            //$('.page-bar').outerHeight(true) -
            $('.full-height-content-ignore-block').outerHeight(true);

        if (target.hasClass('portlet')) {
            var portletBody = target.find('.portlet-body');

            if (isTablet) {
                Metronic.destroySlimScroll(portletBody.find('.full-height-content-body')); // destroy slimscroll
                return;
            }

            height = height -
                target.find('.portlet-title').outerHeight(true) -
                parseInt(target.find('.portlet-body').css('padding-top')) -
                parseInt(target.find('.portlet-body').css('padding-bottom')) - 2;

            if (target.hasClass("full-height-content-scrollable")) {
                height = height - 35;
                portletBody.find('.full-height-content-body').css('height', height);
                Metronic.initSlimScroll(portletBody.find('.full-height-content-body'));
            } else {
                portletBody.css('min-height', height);
            }
        } else {
            if (isTablet) {
                //console.log("isTablet");
                Metronic.destroySlimScroll(target.find('.full-height-content-body')); // destroy slimscroll
                return;
            }

            if (target.hasClass("full-height-content-scrollable")) {
            	//WEBJET ZMENA z 35 na 13
                //uz netreba, hlavicka je nizsia ako bola height = height - 8;
                //console.log("resising slimscroll to "+height);
                target.find('.full-height-content-body').css('min-height', height);
                target.find('.full-height-content-body').css('height', height);
                target.find('.full-height-content-body').data('height', height);
                //console.log(target.find('.full-height-content-body'));
                setTimeout( function() { Metronic.initSlimScroll(target.find('.full-height-content-body')); }, 10);
            } else {
                target.css('min-height', height);
            }
        }
    };
    //* END:CORE HANDLERS *//

    return {
        // Main init methods to initialize the layout
        //IMPORTANT!!!: Do not modify the core handlers call order.

        initHeader: function() {
            handleHorizontalMenu(); // handles horizontal menu
        },

        initSidebar: function() {
            //layout handlers
            handleFixedSidebar(); // handles fixed sidebar menu
            handleSidebarMenu(); // handles main menu
            handleSidebarToggler(); // handles sidebar hide/show

            Metronic.addResizeHandler(handleFixedSidebar); // reinitialize fixed sidebar on window resize
        },

        initContent: function() {
            handle100HeightContent(); // handles 100% height elements(block, portlet, etc)
            handleTabs(); // handle bootstrah tabs

            Metronic.addResizeHandler(handleSidebarAndContentHeight); // recalculate sidebar & content height on window resize
            Metronic.addResizeHandler(handle100HeightContent); // reinitialize content height on window resize
        },

        initFooter: function() {
            handleGoTop(); //handles scroll to top functionality in the footer
        },

        init: function () {
            this.initHeader();
            this.initSidebar();
            this.initContent();
            this.initFooter();
        },

        //public function to fix the sidebar and content height accordingly
        fixContentHeight: function ()
        {
        	//console.log("Layout.fixContentHeight();");
            handleSidebarAndContentHeight();
        },

        initFixedSidebarHoverEffect: function() {
            handleFixedSidebarHoverEffect();
        },

        initFixedSidebar: function() {
            handleFixedSidebar();
        },

        getLayoutImgPath: function () {
            return layoutImgPath;
        },

        getLayoutCssPath: function () {
            return layoutCssPath;
        }
    };

}();