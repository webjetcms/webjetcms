// This module MUST be imported before any UMD jQuery plugin (jquery.cookie.js, etc.)
// ES module imports are hoisted — all imports execute before any statements in the importing module.
// By placing jQuery global assignment in a separate module imported first, we ensure
// window.jQuery exists before UMD wrappers that reference bare `jQuery` are evaluated.
import $ from 'jquery';

// Save queued jQuery calls from the head.pug shim before overwriting window.$
var _jqQueue = window._jqQueue && window._jqQueue.length > 0 ? window._jqQueue.slice() : null;

window.jQuery = $;
window.$ = $;

// Replay queued $(selector).method() calls after DOM elements are created by domReady callbacks.
// Order 10 ensures this runs after WJ.headerTabs (order 2) and page domReady (order 5).
if (_jqQueue) {
    window._jqQueue = null;
    window.domReady.add(function() {
        _jqQueue.forEach(function(item) {
            var result = $.apply(null, item.args);
            item.calls.forEach(function(call) {
                var ret = result[call.prop].apply(result, call.args);
                if (ret && ret !== result) result = ret;
            });
        });
    }, 10);
}
