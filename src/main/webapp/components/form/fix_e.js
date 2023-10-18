// fix for Internet Explorer event model
function fixE(e) {
	if (!e && window.event) e = window.event;
	if (!e.target) e.target = e.srcElement;
	return e;
}