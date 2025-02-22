var Ninja = (function(){

    var  debugInfo = {
        allow: false
    };

    function createDebugInfo() {

        if($(".md-debug-info").length) {

            var setDebugInfo = {
                allow: $(".md-debug-info").length > 0,
                blockDeviceSize: $(".md-debug-info").find(".device-size"),
                blockClasses: $(".html-class-list"),
                blockNewSize: $(".device-resize-new-size"),
                blockWidthInfo: $(".device-resize-w-info"),
                blockHeightInfo: $(".device-resize-h-info"),
                blockDeviceType: $(".device-type-from-css"),
                classesList: $('html').attr('class').split(/\s+/),
                windowInfo: $(window).width() + " x " + $(window).height()
            }

            debugInfo = $.extend({}, debugInfo, setDebugInfo);

            setTimeout(function () {
                var classes = "";
                $.each(debugInfo.classesList, function (index, item) {
                    classes += item + "<br>";
                });
                debugInfo.blockClasses.html("").append(classes);
                debugInfo.blockDeviceSize.html(debugInfo.windowInfo);
            }, 1000);
        }
    }

    var defaults = {
        fireTime:200,
        debug:false,

        onDinosaur: function() {},
        onDinosaurBlink: function() {},
        onDinosaurUp: function() {},
        onDinosaurUpBlink: function() {},

        onMobile: function() {},
        onMobileBlink: function() {},
        onMobileUp: function() {},
        onMobileUpBlink: function() {},
        onMobileDown: function() {},
        onMobileDownBlink: function() {},

        onTablet: function() {},
        onTabletBlink: function() {},
        onTabletUp: function() {},
        onTabletUpBlink: function() {},
        onTabletDown: function() {},
        onTabletDownBlink: function() {},

        onLaptop: function() {},
        onLaptopBlink: function() {},
        onLaptopUp: function() {},
        onLaptopUpBlink: function() {},
        onLaptopDown: function() {},
        onLaptopDownBlink: function() {},

        onDesktop: function() {},
        onDesktopBlink: function() {},
        onDesktopDown: function() {},
        onDesktopDownBlink: function() {},

        onResize: function() {},
        onResizeWidth: function() {},
        onResizeHeight: function() {}
    }

    function bindEvents() {

        var isNew,
        lastWidth = $(window).width(),
        lastHeight = $(window).height();

        $(window).on("orientationchange resize", function() {

            function isNewWidth(){
                isNew = false;
                if($(window).width()!=lastWidth){
                    lastWidth = $(window).width();
                    isNew = true;
                }
                return isNew;
            }

            function isNewHeight(){
                isNew = false;
                if($(window).height()!=lastHeight){
                    lastHeight = $(window).height();
                    isNew = true;
                }
                return isNew;
            }

            waitForFinalEvent(function(){
                defaults.onResize();
                if(debugInfo.allow){
                    debugInfo.blockNewSize.html(" / " + $(window).width() + " x " + $(window).height());
                }
                if(isNewWidth()) {
                    defaults.onResizeWidth();
                    bp.storeLastValue();
                    callFuncByScreenSize();
                    if(debugInfo.allow){
                        debugInfo.blockWidthInfo.html("true");
                    }
                }
                if(isNewHeight()) {
                    defaults.onResizeHeight();
                    if(debugInfo.allow){
                        debugInfo.blockHeightInfo.html("true");
                    }
                }
            }, defaults.fireTime);

        });
    }
    
    var bp = {
        deviceType: [
            "onChange_dinosaur",
            "onChange_mobile",
            "onChange_tablet",
            "onChange_laptop",
            "onChange_desktop",
            "onChangeUp_dinosaur",
            "onChangeUp_mobile",
            "onChangeUp_tablet",
            "onChangeUp_laptop",
            "onChangeDown_mobile",
            "onChangeDown_tablet",
            "onChangeDown_laptop",
            "onChangeDown_desktop"
        ],
        deviceValue: [],
        deviceLastValue: [],
        deviceState: [],
        deviceFunctions: {
            "onChange_dinosaur": function() { defaults.onDinosaur(); if(debugInfo.allow){ debugInfo.blockDeviceType.html("onDinosaur"); } },
            "onChange_mobile": function() { defaults.onMobile(); if(debugInfo.allow){ debugInfo.blockDeviceType.html("onMobile"); } },
            "onChange_tablet": function() { defaults.onTablet(); if(debugInfo.allow){ debugInfo.blockDeviceType.html("onTablet"); } },
            "onChange_laptop": function() { defaults.onLaptop(); if(debugInfo.allow){ debugInfo.blockDeviceType.html("onLaptop"); } },
            "onChange_desktop": function() { defaults.onDesktop(); if(debugInfo.allow){ debugInfo.blockDeviceType.html("onDesktop"); } },
            "onChangeUp_dinosaur": function() { defaults.onDinosaurUp(); },
            "onChangeUp_mobile": function() { defaults.onMobileUp(); },
            "onChangeUp_tablet": function() { defaults.onTabletUp(); },
            "onChangeUp_laptop": function() { defaults.onLaptopUp(); },
            "onChangeDown_mobile": function() { defaults.onMobileDown(); },
            "onChangeDown_tablet": function() { defaults.onTabletDown(); },
            "onChangeDown_laptop": function() { defaults.onLaptopDown(); },
            "onChangeDown_desktop": function() { defaults.onDesktopDown(); }
        },
        deviceFunctionsBlink: {
            "onChange_dinosaur": function() { defaults.onDinosaurBlink(); },
            "onChange_mobile": function() { defaults.onMobileBlink(); },
            "onChange_tablet": function() { defaults.onTabletBlink(); },
            "onChange_laptop": function() { defaults.onLaptopBlink(); },
            "onChange_desktop": function() { defaults.onDesktopBlink(); },
            "onChangeUp_dinosaur": function() { defaults.onDinosaurUpBlink(); },
            "onChangeUp_mobile": function() { defaults.onMobileUpBlink(); },
            "onChangeUp_tablet": function() { defaults.onTabletUpBlink(); },
            "onChangeUp_laptop": function() { defaults.onLaptopUpBlink(); },
            "onChangeDown_mobile": function() { defaults.onMobileDownBlink(); },
            "onChangeDown_tablet": function() { defaults.onTabletDownBlink(); },
            "onChangeDown_laptop": function() { defaults.onLaptopDownBlink(); },
            "onChangeDown_desktop": function() { defaults.onDesktopDownBlink(); }
        },
        refreshValue: function() {
            var self = this;
            $.each(self.deviceType, function (index, item) {
                self.deviceValue[item] = self.getElementValue('.'+item+'');
            });
            this.setBreakpointState();
        },
        storeLastValue: function() {
            var self = this;
            $.each(self.deviceType, function (index, item) {
                self.deviceLastValue[item] = self.deviceValue[item];
            });
        },
        setBreakpointState: function() {
            var self = this;
            $.each(self.deviceType, function (index, item) {
                self.deviceState[item] = self.isStateTrue(item);
            });
        },
        isStateTrue: function(item) {
            if(this.deviceValue[item] == "true") {
                return true;
            } else {
                return false;
            }
        },
        getElementValue: function(selector) {
            var value = window.getComputedStyle(
                document.querySelector(selector), ':before'
            ).getPropertyValue('content').replace(/\"/g, '');
            return value;
        }
    };

    function createQueryHolders() {
        var device = "";
        $.each(bp.deviceType, function (index, item) {
            device += '<span class="'+item+'"></span>';
        });
        $('body').append('<div class="queryHolders">'+device+'</div>');
    }

    function callFuncByScreenSize() {
        bp.refreshValue();
        $.each(bp.deviceType, function (index, item) {
            if(bp.deviceLastValue[item] != bp.deviceValue[item]) {
                if (bp.deviceState[item]) { bp.deviceFunctions[item](); }
            } else {
                if (bp.deviceState[item]) { bp.deviceFunctionsBlink[item]();  }
            }
        });
        bp.storeLastValue();
    }

    var waitForFinalEvent = (function() {
        var timers = {};
        return function(callback, ms, uniqueId) {
            if (!uniqueId) {
                uniqueId = "Don't call this twice without a uniqueId";
            }
            if (timers[uniqueId]) {
                clearTimeout(timers[uniqueId]);
            }
            timers[uniqueId] = setTimeout(callback, ms);
        };
    })();

    function NinjaLog(param) {
        if(defaults.debug || debugInfo.allow) {
            console.warn(param);
        }
    }

    function NinjaTouchDevice() {
        var isTouchDevice = false,
            supportsTouch = false;
        if ( ('ontouchstart' in window) || (window.navigator.msPointerEnabled) || ('ontouchstart' in document.documentElement) ) {
            supportsTouch = true;
        }
        isTouchDevice = Modernizr.touchevents || supportsTouch;
        return isTouchDevice;
    }

    return {
        init: function(config) {
            if ($('html').hasClass('ninja-initialized')) {
                return;
            }
            $('html').addClass('ninja-initialized');
            defaults = $.extend({}, defaults, config);
            createDebugInfo();
            createQueryHolders();
            bindEvents();
            callFuncByScreenSize();
        },
        log: function(param) {
            NinjaLog(param);
        },
        isTouchDevice: function () {
            return NinjaTouchDevice();
        }
    }
}());