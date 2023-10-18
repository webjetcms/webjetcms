autoCompleters = new Array();

function AutoCompleter(target)
{
	this.target = target;
	this.url = '';
	this.params = null;
	this.onOptionSelect = null;
	this.hasQueryString = false;
	this.maxLengthOfAutocomplete = 30;
	this.minLengthBeforeSearch = 3;
}

AutoCompleter.prototype.setUrl = function(url){
	this.url = url;
	this.hasQueryString = url.indexOf('?') != -1;
	return this;
};

AutoCompleter.prototype.setParams = function(paramString){
	fields = paramString.replace(/\s+/, '').split(',');
	this.params = fields;
	return this;
};

AutoCompleter.prototype.setOnOptionSelect = function(functionName){
	this.onOptionSelect = functionName;
	return this;
};

AutoCompleter.prototype.setMaxRows = function(max) {
	this.maxLengthOfAutocomplete = max;
	return this;
};

AutoCompleter.prototype.setMinLength = function(minLength) {
	this.minLengthBeforeSearch = minLength;
	return this;
};

AutoCompleter.prototype.transform = function() {
	autoCompleters.push(this);
	$(document).ready(function(){
		while(autoCompleters.length > 0)
			addAutoCompleteTo(autoCompleters.pop());
	});
};

function addAutoCompleteTo(autoCompleter)
{
	try
	{
		$(autoCompleter.target).autocomplete({
			source : function(actual, options){
				url = autoCompleter.url;
				if (autoCompleter.hasQueryString)
					url += "&"+$(autoCompleter.target).attr("name")+"="+encodeURIComponent(actual.term);
				else
					url += "?"+$(autoCompleter.target).attr("name")+"="+encodeURIComponent(actual.term);
				
				for (param in autoCompleter.params)
				{
					param = autoCompleter.params[param];
					url += "&"+$(param).attr("name")+"="+encodeURIComponent($(param).val());
				}
				$.ajax({
					url: url,
					cache: false,
					success: function(response){
						allOptions = eval(response);
						if (allOptions)
						{
							while (allOptions.length > autoCompleter.maxLengthOfAutocomplete)
								allOptions.pop();
							options(allOptions);
						}
					},
					async: false
				});
			},
			select: eval(autoCompleter.onOptionSelect),
			delay: 800,
			minLength: autoCompleter.minLengthBeforeSearch
		});
	} catch (e) { console.log("error in addAutoCompleteTo:"); console.log(e); }
}