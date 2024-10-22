"use strict";
/**
 * @class  elFinder command "resize"
 * Open dialog to resize image
 *
 * @author Dmitry (dio) Levashov
 * @author Alexey Sukhotin
 * @author Naoki Sawada
 * @author Sergio Jovani
 **/
elFinder.prototype.commands.resize = function() {

	this.updateOnSelect = false;

	this.getstate = function() {
		var sel = this.fm.selectedFiles();
		return !this._disabled && sel.length == 1 && sel[0].read && sel[0].write && sel[0].mime.indexOf('image/') !== -1 ? 0 : -1;
	};

	this.exec = function(hashes) {
		var fm    = this.fm,
			files = this.files(hashes),
			dfrd  = $.Deferred(),

			open = function(file, id) {
				var index = file.virtualPath.lastIndexOf("/");
				var iID = -1;
				var dir = file.virtualPath.substring(0, index);
				var name = file.virtualPath.substring(index+1);

				var width = 990;
				var height = 720;
				if (screen.height > 1200)
				{
					width=1300;
					height = screen.height - 150;
				}
				else if (screen.height > 800)
				{
					width = 1200;
					height = screen.height - 150;
				}

				var myWindow = window.open("/admin/v9/apps/image-editor/?iID=" + iID + "&dir=" + dir + "&name=" + name, "tinyWindow", "toolbar=no,scrollbars=yes,resizable=yes,width=" + width + ",height=" + height + ";");
			},

			id, dialog
			;


		if (!files.length || files[0].mime.indexOf('image/') === -1) {
			return dfrd.reject();
		}

		id = 'resize-'+fm.namespace+'-'+files[0].hash;
		/*
		dialog = fm.getUI().find('#'+id);

		if (dialog.length) {
			dialog.elfinderdialog('toTop');
			return dfrd.resolve();
		}
		*/

		open(files[0], id);

		return dfrd;
	};

};
