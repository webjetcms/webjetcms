"use strict";

/**
 * @class elFinder command "wjeditswitch".
 *
 * By file type, switch between wjedit and wjresize.
 *
 * Purpose is to remove redundant icons.
 *
 * @author Sebastian Ivan
 */
elFinder.prototype.commands.wjeditswitch = function() {

    this.updateOnSelect = true;

	this.getstate = function(sel) {
		var sel = this.files(sel);

		if (sel.length == 1 && typeof sel[0].mime != "undefined" && sel[0].mime != "directory") {
			return 0;
		}

		return -1;
	};

    this.exec = function(hashes) {
		var fm    = this.fm,
			files = this.files(hashes),
			dfrd  = $.Deferred();

        if (!files.length) {
            return dfrd.reject();
        }

        if(files[0].mime.indexOf('image/') === 0) {
            //If image is from /gallery open in new tab galleryDatatable with filtered image and open editor
            var imageUrl = files[0].url;
            if(imageUrl != null && imageUrl.length > 0) {
                var imageUrlArr = imageUrl.split('/');

                //check if it's from gallery
                var isGalleryImg = false;
                imageUrlArr.forEach(function(imgUrl) {
                    if(imgUrl === 'gallery') {
                        isGalleryImg = true;

                        var dirPath = "";
                        for(var i = 0; i < imageUrlArr.length - 1; i++) {
                            dirPath += imageUrlArr[i] + '/';
                        }

                        var imageName = imageUrlArr[imageUrlArr.length - 1];
                        if(imageName.startsWith("s_")) imageName = imageName.substring(2);
                        else if(imageName.startsWith("o_")) imageName = imageName.substring(2);
                        window.open("/admin/v9/apps/gallery/?dir=" + dirPath + "#dt-open-editor=true&dt-filter-imageName=" + imageName);

                        return dfrd;
                    }
                });
            }

            if (isGalleryImg==false) $('#finder').elfinder('instance').exec('resize', files[0].hash); //Its wjresize BUT it's registre as resize
        } else {
            $('#finder').elfinder('instance').exec('wjedit', files[0].hash);
        }

        return dfrd;
    };
};