/*
**  jquery.extlink.js -- jQuery plugin for external link annotation
**  Copyright (c) 2007-2008 Ralf S. Engelschall <rse@engelschall.com> 
**  Licensed under GPL <http://www.gnu.org/licenses/gpl.txt>
**
**  USAGE:
**  $(document).ready(function () {
**                $(this).extlink("grey");
**            });
**
**  $LastChangedDate$
**  $LastChangedRevision$
*/

(function($) {
    $.fn.extend({
        extlink: function (color) {
            if (typeof color === "undefined")
                color = "grey";
            var site = String(document.location)
                .replace(/^(https?:\/\/[^:\/]+).*$/, "$1")
                .replace(/^((site)?(file:\/\/.+\/))[^\/]+$/, "$3")
                .replace(/(\\.)/g, "\\$1");
            $("a", this).filter(function (i) {
                var href = $(this).attr("href");
                if (href == null)
                    return false;
                return (
                       href.match(RegExp("^("+site+"|(https?:)?/[^/])")) == null
                    && href.match(RegExp("^(https?|ftp)://.+")) != null
                );
            }).each(function () {
                $(this)
                    .css("backgroundImage",    "url('jquery.extlink.d/extlink-" + color + ".gif')")
                    .css("backgroundRepeat",   "no-repeat")
                    .css("backgroundPosition", "right center")
                    .css("padding-right",      "11px");
            });
        }
    });
})(jQuery);