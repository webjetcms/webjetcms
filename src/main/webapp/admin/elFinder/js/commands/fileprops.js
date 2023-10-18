"use strict";
/**
 * @class  elFinder command "paste"
 * Paste filesfrom clipboard into directory.
 * If files pasted in its parent directory - files duplicates will created
 *
 * @author Dmitry (dio) Levashov
 **/
elFinder.prototype.commands.fileprops = function() {

	this.getstate = function(sel) {
		var sel = this.files(sel);

		if (sel.length == 1 && typeof sel[0].mime != "undefined" && sel[0].mime != "directory") {
			return 0;
		}

		return -1;
	};

	this.exec = function(hashes) {
		var fileObj = this.files(hashes)[0];
		var virtualPath = fileObj.virtualPath;

		var dir = virtualPath.substring(0, virtualPath.lastIndexOf('/') + 1);
		var file = virtualPath.substring(virtualPath.lastIndexOf('/') + 1);

		var modal = $('#elfinder-modal');
		modal.find('#modalLabel').text(this.title);
		modal.find('.modal-body').html('<iframe id="elfinderIframe" src="/admin/fbrowser/fileprop/?prop=yes&dir=' + dir + '&file=' + file + '" class="iframe" />');
		modal.find('.action').show();

		if (typeof modal.modal == 'function') {
			modal.modal('show');
		}
		else {
			modal.modal({});
		}

	}

}
