"use strict";
/**
 * @class  elFinder command "paste"
 * Paste filesfrom clipboard into directory.
 * If files pasted in its parent directory - files duplicates will created
 *
 * @author Dmitry (dio) Levashov
 **/
elFinder.prototype.commands.dirprops = function() {
	this.getstate = function(sel) {
		var sel = this.files(sel);

		if (sel.length == 1 && typeof sel[0].mime != "undefined" && sel[0].mime == "directory") {
			return 0;
		}

		return -1;
	};

	this.exec = function(hashes) {
		var file = this.files(hashes)[0];

		var modal = $('#elfinder-modal');
		modal.find('#modalLabel').text(this.title);
		modal.find('.modal-body').html('<iframe id="elfinderIframe" src="/admin/fbrowser/dirprop/?dir=' + file.virtualPath + '" class="iframe" />');
		modal.find('.action').show();

		if (typeof modal.modal == 'function') {
			modal.modal('show');
		}
		else {
			modal.modal({});
		}
	}
}
