export function typeElfinder() {

    function setValue(conf, val) {
        //console.log("setValue=", conf);
        if (val == null) val = "";
        conf._input.val( val );
        conf._input.trigger("change");

        if (conf._prepend != null) {
            if (val.indexOf(".jpg")!=-1 || val.indexOf(".jpeg")!=-1 || val.indexOf(".gif")!=-1 || val.indexOf(".png")!=-1) {
                conf._prepend.addClass("has-image");
                conf._prepend.css("background-image", "url("+val+")");
            } else {
                conf._prepend.removeClass("has-image");
                conf._prepend.css("background-image", "none");
            }
        }
    }

    return {
        create: function ( conf ) {
            var that = this;
            //console.log("conf=", conf);

            var prependHtmlCode = "";
            var isImage = false;
            if (conf.className != null && conf.className.indexOf("image")!=-1) {
                isImage = true;
                prependHtmlCode = '<span class="input-group-text"><i class="far fa-image"></i></span>';
                //console.log("is image class=", conf.className);
            }

            var htmlCode = $('<div class="input-group">'+prependHtmlCode+'<input type="text" class="form-control"><button class="btn btn-outline-secondary" type="button" data-toggle="tooltip" title="'+WJ.translate('button.select')+'"><i class="far fa-crosshairs"></i></button></div>');

            conf._input = htmlCode.find("input.form-control");
            if (typeof conf.attr != undefined && conf.attr != null) {
                $.each(conf.attr, function( key, value ) {
                    //console.log("Setting attr: key=", key, " value=", value);
                    if (key.indexOf("data-dt-field-dt-")!=-1) return;
                    $(conf._input).attr(key, value);
                });
            }

            var button = htmlCode.find("button");
            button.on("click", function() {
                //console.log("CLICK");
                var input = conf._input[0];
                input.form.name = "form_"+that.TABLE.id;
                input.name = conf.data;
                WJ.openElFinder({
                    link: conf._input.val(),
                    title: conf.label,
                    okclick: function(link) {
                        //console.log("OK click");
                        setValue(conf, link);
                    }
                });
            });

            //odkaz na prepend div
            conf._prepend = null;
            if (isImage) conf._prepend = htmlCode.find(".input-group-text");

            return htmlCode;
        },

        get: function ( conf ) {
            return conf._input.val();
        },

        set: function ( conf, val ) {
            //console.log("SET value=", val);
            setValue(conf, val);
        },

        enable: function ( conf ) {},

        disable: function ( conf ) {}
    }
};