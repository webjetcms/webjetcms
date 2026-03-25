// This module MUST be imported before any UMD jQuery plugin (jquery.cookie.js, etc.)
// ES module imports are hoisted — all imports execute before any statements in the importing module.
// By placing jQuery global assignment in a separate module imported first, we ensure
// window.jQuery exists before UMD wrappers that reference bare `jQuery` are evaluated.
import $ from 'jquery';
window.jQuery = $;
window.$ = $;
