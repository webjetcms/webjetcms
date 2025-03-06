var ComponentsDropdowns = function () {

    var handleSelect2 = function () {

    }

    var handleSelect2Modal = function () {

    }

    var handleBootstrapSelect = function() {
        $('.bs-select').selectpicker({
            iconBase: 'fa',
            tickIcon: 'fa-check'
        });
    }

    var handleMultiSelect = function () {
        $('#inquiryGroups').multiSelect();
        $('#my_multi_select2').multiSelect({
            selectableOptgroup: true
        });
    }

    return {
        //main function to initiate the module
        init: function () {
            handleSelect2();
            handleSelect2Modal();
            handleMultiSelect();
            handleBootstrapSelect();
        }
    };

}();
