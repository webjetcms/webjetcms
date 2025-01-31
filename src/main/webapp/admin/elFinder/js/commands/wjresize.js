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

			open = function(file) {
				var index = file.virtualPath.lastIndexOf("/");
				var dir = file.virtualPath.substring(0, index);
				var name = file.virtualPath.substring(index + 1);

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

				let openedWindow = WJ.openPopupDialog('/admin/v9/apps/image-editor?id=-1&dir=' + dir + '&name=' + name + '&showOnlyEditor=true', width, height);
				let closeBtn;
				let saveBtn;

				openedWindow.addEventListener("WJ.DTE.opened", function(e) {
					//Remove close button action from table
					openedWindow.$("#galleryTable_modal").off();

					//Get buttons instances
					closeBtn = openedWindow.$("div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn-close-editor");
					saveBtn = openedWindow.$("div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn-primary");

					//Prepare new close button action
					closeBtn.on("click", function() {
						openedWindow.close();
					});

					//
					saveBtn.on("click", function() {
						$('#finder').elfinder('instance').exec('reload');
					});
				});

				openedWindow.addEventListener("WJ.imageEditor.upload.success", function(e) {
					//Call relaod after picture is uploaded successfully
					$('#finder').elfinder('instance').exec('reload');
				});
			};

		if (!files.length || files[0].mime.indexOf('image/') === -1) {
			return dfrd.reject();
		}

		open(files[0]);

		return dfrd;
	};

};
