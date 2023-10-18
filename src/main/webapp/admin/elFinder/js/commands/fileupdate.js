"use strict";
/**
 * @class  elFinder command "paste"
 * Paste filesfrom clipboard into directory.
 * If files pasted in its parent directory - files duplicates will created
 *
 * @author Dmitry (dio) Levashov
 **/
elFinder.prototype.commands.fileupdate = function() {

	this.getstate = function(sel) {
		var sel = this.files(sel);

		if (sel.length == 1 && typeof sel[0].mime != "undefined" && sel[0].mime != "directory") {
			return 0;
		}

		return -1;
	};

	this.exec = function(hashes) {
        var fileObj = this.files(hashes)[0];
        var data = [fileObj.hash];

		if ($('#finder').length > 0) {
            $('#finder').elfinder('instance').exec('upload', data);
    	}
    	else {
			console.warn("Cannot find elfinder element");
		}
	}

}
