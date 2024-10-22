/**
 * @class  elFinder command "paste"
 * Paste filesfrom clipboard into directory.
 * If files pasted in its parent directory - files duplicates will created
 *
 * @author Dmitry (dio) Levashov
 **/
elFinder.prototype.commands.wjfilearchive = function() {
    "use strict";
    var fm = this.fm;

    this.value          = fm.viewType;
    this.alwaysEnabled  = true;
    this.updateOnSelect = false;
    this.options = { ui : 'viewbutton'};

	this.getstate = function() {
        return 0;
	};

    this.exec = function() {
        var dfrd  = $.Deferred().fail(function(error) { error && fm.error(error); });

        WJ.openIframeModalDatatable({
			url: '/components/file_archiv/file_archiv_upload.jsp',
			width: 850,
			height: 500,
			buttonTitleKey: "button.save"
		});


        return dfrd.resolve();
    }
}