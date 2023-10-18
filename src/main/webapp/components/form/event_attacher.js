// Scott Andrew's event attacher
function addEvent(obj, evType, fn)
{    
    if (navigator.userAgent.indexOf("Opera")!=-1)
    {
    	//opera je tupa, cez addEventListener to nefunguje
    	eval("obj.on"+evType+"=fn");
    	return;
    }
    
	if (obj.addEventListener)
	{
		obj.addEventListener(evType, fn, true);
		return true;
	}
	else if (obj.attachEvent)
	{
		var r = obj.attachEvent("on"+evType, fn);
		return r;
	}
	else
	{
		return false;
	}
}