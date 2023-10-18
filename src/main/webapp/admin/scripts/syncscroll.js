// This is a function that returns a function that is used
// in the event listener
function getOnScrollFunction(oElement) {
	return function () {
	    //window.alert(oElement.scrollTop);
		if (oElement._scrollSyncDirection == "horizontal" || oElement._scrollSyncDirection == "both")
			oElement.scrollLeft = event.srcElement.scrollLeft;
		if (oElement._scrollSyncDirection == "vertical" || oElement._scrollSyncDirection == "both")
			oElement.scrollTop = event.srcElement.scrollTop;
	};

}
// This function adds scroll syncronization for the fromElement to the toElement
// this means that the fromElement will be updated when the toElement is scrolled
function addScrollSynchronization(fromElement, toElement, direction) {
	removeScrollSynchronization(fromElement);
	
	fromElement._syncScroll = getOnScrollFunction(fromElement);
	fromElement._scrollSyncDirection = direction;
	fromElement._syncTo = toElement;
	
	//window.alert("to="+toElement+" from="+fromElement);
	
	//if (toElement.addEventListener) toElement.addEventListener("onscroll", fromElement._syncScroll);
	if (toElement.attachEvent) toElement.attachEvent("onscroll", fromElement._syncScroll);
	else toElement.onscroll = fromElement._syncScroll;
}

// removes the scroll synchronization for an element
function removeScrollSynchronization(fromElement) {
	if (fromElement._syncTo != null)
		fromElement._syncTo.detachEvent("onscroll", fromElement._syncScroll);

	fromElement._syncTo = null;;
	fromElement._syncScroll = null;
	fromElement._scrollSyncDirection = null;
}
