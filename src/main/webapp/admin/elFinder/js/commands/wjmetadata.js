"use strict";
/**
 * @class  elFinder command "wjmetadata"
 * Show dialog for metadata when file is uploaded.
 *
 * @author Lukas (Paso) Pasek
 **/
elFinder.prototype.commands.wjmetadata = function() {

	// riadi zobrazenie polozky v kontext menu (0 zobrazi, -1 nezobrazi)
	this.getstate = function(sel) {
        var result = -1;

        if (!this.isExecAllowed()) {
            return result;
        }

        var files = this.files(sel);
        if (this.isMetadataAllowed() && this.isRootFiles(files)) {
			result = 0;
    	}

    	if (this.isFileArchiveEnabled() && this.isRootFileArchive()) {
        	result = 0;
		}

		if (result > -1) {
            this.setTitle();
		}

		return result;
	};

	// vykonanie prikazu wjmetadata
	this.exec = function(hashes) {

		if (!this.isExecAllowed()) {
			return;
		}

		var self = this;
		self.setTitle();

		var files = self.getFiles(hashes),
			files = self.filterFiles(files),
			virtualPaths = self.getVirtualPaths(files);

		var src = self.isRootFileArchive() ? self.getCustomData('wjmetadata.fileArchive.iframe', '') : self.getCustomData('wjmetadata.metadata.iframe', '');
		src += '?files=' + virtualPaths.join("&files=");

		var modal = $('#elfinder-modal');
		modal.find('#modalLabel').text(this.title);
		modal.find('.modal-body').html('<iframe id="elfinderIframe" src="' + src + '" class="iframe" />');
		modal.find('.action').hide();

		if (typeof modal.modal == 'function') {
			modal.modal('show');
		}
		else {
			modal.modal({});
		}
	};

	// kontrola na pouzitie prikazu
	this.isExecAllowed = function() {
		if (this.isRootFileArchive() && this.isFileArchiveEnabled()) {
        	return true;
		}

		if (!this.isRootFileArchive() && this.isMetadataAllowed()) {
        	return true;
		}

		return false;
	}

	// nastavi title dialogovemu oknu
	this.setTitle = function() {
        if (this.isRootFileArchive()) {
            this.title = this.isFileArchiveEnabled() ? this.getCustomData('wjmetadata.fileArchive.title', this.title) : this.title;
        }
        else {
            this.title = this.isMetadataAllowed() ? this.getCustomData('wjmetadata.metadata.title', this.title) : this.title;
		}
	}

	// Vrati data pre command, nastavuju sa pri inicializacii elFindru
	this.getCustomData = function(key, defaultValue) {
        var result = defaultValue;
		if (typeof this.fm.options == 'undefined' || this.fm.options.customData == 'undefined') {
			return result;
		}

		result = this.getObjectByKey(this.fm.options.customData, key, defaultValue);
        return result;
	}

	// ziska hodnotu z objektu pomocou kluca oddelenho bodkami, napr. getObjectByKey(object, 'foo.bar', defaultValue);
	this.getObjectByKey = function(object, key, defaultValue) {
        key = key.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties
        key = key.replace(/^\./, '');           // strip a leading dot
        var a = key.split('.');
        for (var i = 0, n = a.length; i < n; ++i) {
            var k = a[i];
            if (k in object) {
                object = object[k];
            } else {
                return defaultValue;
            }
        }
        return object;
	}

	// kontrola ci su metadata povolene
	this.isMetadataAllowed = function() {
		return this.getCustomData('wjmetadata.metadata.enabled', false);
	}

    // kontrola ci je file archiv povoleny
    this.isFileArchiveEnabled = function() {
        return this.getCustomData('wjmetadata.fileArchive.enabled', false);
    }

    // ziska suborove objekty pomocou hashov
	this.getFiles = function(hashes) {
		if (hashes == null | hashes.length == 0) {
			return [];
		}

        var self = this,
			result = [];
		
		$.each(hashes, function(i, file) {
            if (self.isString(file)) {
				file = self.fm.file(file);
            }

            result.push(file);
		});

		return result;
	}

	// kontrola na string
	this.isString = function(hash) {
		return typeof hash == 'string';
	}

	// vyfiltruje len subory v adresari /files/
	this.filterFiles = function(files) {
        return $.grep(files, function(v){
            return v.virtualPath.indexOf("/files/") == 0;
        });
	};

	// ziska virtualne cesty pomocou suborov
	this.getVirtualPaths = function(files) {
		return $.map(files, function(v, i){
            return v.virtualPath;
        })
	};

	// kontrola, ci sme v adresari file archive
	this.isRootFileArchive = function() {
        var fm = this.fm,
            root = fm.root(),
            file = fm.file(root),
			filesRoot = this.getCustomData('wjmetadata.fileArchive.root', '/files/archiv');

        if (typeof file === 'undefined' || file.mime !== 'directory') {
        	return false;
		}

        // remove trailing slash
		var fileUrl = file.url;
        fileUrl = fileUrl.replace(/\/$/, "");
        filesRoot = filesRoot.replace(/\/$/, "");
		
        return fileUrl === filesRoot;
    };

    // kontrola, ci su subory vo files ale nie vo file archive
    this.isRootFiles = function(files) {
		var result = true;

        if (files == null || files.length == 0 || this.isRootFileArchive()) {
        	return false;
		}

        $.each(files, function (i, file) {
            if (file.mime == 'directory' || file.mime == "undefined" || file.url.indexOf('/files') != 0) {
                result = false
                return false;
            }
        });

        return result;
    }
}
