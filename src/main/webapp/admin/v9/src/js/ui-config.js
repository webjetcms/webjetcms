import $ from 'jquery';

// set back-compat before loading any jquery-ui widgets, elfinder needs it
$.uiBackCompat = true;

export default $; // optional, to ensure the bundler doesn't discard the file