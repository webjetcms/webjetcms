window.lastSelect = null;
window.setParentGroupId = function(returnValue)
{
    //console.log("setParentGroupId, returnValue=", returnValue, "lastSelect=", lastSelect);
    if (returnValue.length > 15)
    {
        var groupid = returnValue.substr(0,15);
        var groupname = returnValue.substr(15);
        groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");

        var optionName = new Option(groupname, groupid, true, true)
        lastSelect.options[lastSelect.length] = optionName;
        $(lastSelect).selectpicker("refresh");
        $(lastSelect).trigger("change");
    }
    else
    {
        lastSelect.selectedIndex = 0;
    }
}