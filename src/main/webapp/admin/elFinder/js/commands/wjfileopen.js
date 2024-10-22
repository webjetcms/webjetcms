/**
 * @class  elFinder command "paste"
 * Paste filesfrom clipboard into directory.
 * If files pasted in its parent directory - files duplicates will created
 **/
elFinder.prototype.commands.wjfileopen = function() {
	"use strict";
	var fm = this.fm;

	this.getstate = function(sel) {
		var sel = this.files(sel);

		if (sel.length == 1 && typeof sel[0].mime != "undefined" && sel[0].mime != "directory") {
			return 0;
		}

		return -1;
	};

	this.exec = function(hashes) {
		var dfrd  = $.Deferred().fail(function(error) { error && fm.error(error); });

		if(hashes === null || hashes === undefined || hashes.length < 1) { 
			return dfrd.reject("Hashes are not valid.");
		}

		var fileVirtualPath;
		try {
			var file = this.files(hashes)[0];
			fileVirtualPath = file.virtualPath;
		} catch(e) {
			return dfrd.reject("File not found.");
		}

		window.open(fileVirtualPath);
		return dfrd.resolve();
	}
}
