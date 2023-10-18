"use strict";
/**
 * @class  elFinder command "paste"
 * Paste filesfrom clipboard into directory.
 * If files pasted in its parent directory - files duplicates will created
 *
 * @author Dmitry (dio) Levashov
 **/
elFinder.prototype.commands.wjfilearchive = function() {

    var fm = this.fm;
    this.value          = fm.viewType;
    this.alwaysEnabled  = true;
    this.updateOnSelect = false;
    this.options = { ui : 'viewbutton'};

	this.getstate = function() {
        return 0;
	};

    this.exec = function() {
        window.popup('/components/file_archiv/file_archiv_upload.jsp');
    }
}
