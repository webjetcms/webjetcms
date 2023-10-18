"use strict";
if (!Date.prototype.toISOString) {
    Date.prototype.toISOString = function () {
        function pad(n) { return n < 10 ? '0' + n : n; }
        function ms(n) { return n < 10 ? '00'+ n : n < 100 ? '0' + n : n }
        return this.getFullYear() + '-' +
            pad(this.getMonth() + 1) + '-' +
            pad(this.getDate()) + 'T' +
            pad(this.getHours()) + ':' +
            pad(this.getMinutes()) + ':' +
            pad(this.getSeconds()) + '.' +
            ms(this.getMilliseconds()) + 'Z';
    }
}

function createHAR(address, title, startTime, resources)
{
    var entries = [];

    resources.forEach(function (resource) {
        var request = resource.request,
            startReply = resource.startReply,
            endReply = resource.endReply;

        if (!request || !startReply || !endReply) {
            return;
        }

        // Exclude Data URI from HAR file because
        // they aren't included in specification
        if (request.url.match(/(^data:image\/.*)/i)) {
            return;
	}

        entries.push({
            startedDateTime: request.time.toISOString(),
            time: endReply.time - request.time,
            request: {
                method: request.method,
                url: request.url,
                httpVersion: "HTTP/1.1",
                cookies: [],
                headers: request.headers,
                queryString: [],
                headersSize: -1,
                bodySize: -1
            },
            response: {
                status: endReply.status,
                statusText: endReply.statusText,
                httpVersion: "HTTP/1.1",
                cookies: [],
                headers: endReply.headers,
                redirectURL: "",
                headersSize: -1,
                bodySize: startReply.bodySize,
                content: {
                    size: startReply.bodySize,
                    mimeType: endReply.contentType
                }
            },
            cache: {},
            timings: {
                blocked: 0,
                dns: -1,
                connect: -1,
                send: 0,
                wait: startReply.time - request.time,
                receive: endReply.time - startReply.time,
                ssl: -1
            },
            pageref: address
        });
    });

    return {
        log: {
            version: '1.2',
            creator: {
                name: "PhantomJS",
                version: phantom.version.major + '.' + phantom.version.minor +
                    '.' + phantom.version.patch
            },
            pages: [{
                startedDateTime: startTime.toISOString(),
                id: address,
                title: title,
                pageTimings: {
                    onLoad: page.endTime - page.startTime
                }
            }],
            entries: entries
        }
    };
}

var page = require('webpage').create(),system = require('system'),address,fileName;
address = system.args[1];
fileName = system.args[2];
//console.log("*"+system.args.length+"*");

if(system.args.length >= 9)
{
	phantom.addCookie ({
	   'name'     : 'html_data',      /* mandatory property */
	   'value'    : system.args[7],         /* mandatory property */
	   'domain'   : system.args[8],    /* mandatory property */
	   'path'     : '/',
	   'httponly' : true,
	   'secure'   : false,
	   'expires'  : (new Date()).getTime() + (5000 * 60 * 60)
	});
}
var MAX_EXECUTION_TIME   = 60000;

phantom.onError = function(msg, trace)
{
	  console.log("onError !\n" + msg);
	  phantom.exit(3);
};

setTimeout(function() {
    console.log("Max execution time " + Math.round(MAX_EXECUTION_TIME) + " seconds exceeded");
    phantom.exit(3);
}, MAX_EXECUTION_TIME);

page.settings.resourceTimeout = 5000;
if(system.args.length >= 7)
{
page.settings.resourceTimeout = system.args[6];
}
page.onResourceTimeout = function(e)
{
	  console.log(e.errorCode);   // it'll probably be 408
	  console.log(e.errorString); // it'll probably be 'Network timeout on resource'
	  console.log(e.url);         // the url whose request timed out
	  console.log('resourceTimeout is over!');
	  phantom.exit(2);
};

var image_width = 2048;
var image_height = 3028;
var zoom = 2;
if(system.args.length >= 5)
{
	var image_width = system.args[3];
	var image_height = system.args[4];
}
if(system.args.length >= 6)
{
	var zoom = system.args[5];
}

page.viewportSize = {width: image_width, height: image_height};

/**/
page.resources = [];
page.onResourceRequested = function(req) {
  //console.log('Request ' + JSON.stringify(req, undefined, 4));
	page.resources[req.id] = {
		request: req,
		startReply: null,
		endReply: null
	};
};
page.onResourceReceived = function(res) {
  //console.log('Receive ' + JSON.stringify(res, undefined, 4));
    if (res.stage === 'start') {
        page.resources[res.id].startReply = res;
    }
    if (res.stage === 'end') {
        page.resources[res.id].endReply = res;
    }
};

page.onLoadStarted = function () {
	page.startTime = new Date();
};


try
{
	console.log('\nTrying to load address '+address+'\n');
	page.open(address, function (status)
	{
		var cookies = page.cookies;
		//cookie nepotrebujeme zobrazovat
		//console.log('cookies: '+JSON.stringify(cookies));
		var har;
		if(status != 'success')
		{
	        console.log('Unable to load the address! '+status);
	        phantom.exit(1);
		}
		else
		{
			window.setTimeout(function () {

                page.evaluate(function() {
                    document.body.bgColor = 'white';
                });

                var autoHeight = -1;
                try
                {
                        autoHeight = page.evaluate(function() {
                        return document.getElementById("phantomAutoHeight").offsetHeight;
                    });
                }
                catch(e) {
                    autoHeight = -1;
                }

                console.log('autoHeight: '+autoHeight+', image_height: '+image_height+', image_width: '+image_width);

                if(system.args[9] == 'true' && autoHeight > 0)
                {
                    image_height = autoHeight;
                }

				try
				{
				  var scrollOffsets = page.evaluate(function() {
				    return {
				      x: window.pageXOffset,
				      y: window.pageYOffset
				    };
				  });
				  page.clipRect = {
				    top: scrollOffsets.y,
				    left: scrollOffsets.x,
				    height: image_height,
				    width: image_width
                  };
                  console.log("clipRect top:"+page.clipRect.top+" left="+page.clipRect.left+" height="+page.clipRect.height+" page.clipRect.width="+page.clipRect.width+" zoom="+zoom+" fileName="+fileName);
				  page.zoomFactor = zoom;
				  //http://phantomjs.org/api/webpage/method/render.html
				  //Supported formats: PDF, PNG, JPEG, BMP, PPM, GIF support depends on the build of Qt used
				  //quality from 1 to 100. 100 is best
					page.render(fileName,{format: 'JPEG', quality: '85'});
					page.endTime = new Date();
					page.title = page.evaluate(function () {
						return document.title;
					});
		            //har = createHAR(page.address, page.title, page.startTime, page.resources);
		            //console.log(JSON.stringify(har, undefined, 4));
					console.log('OK');
				}
				catch(e)
				{
					console.log(e.message);
					phantom.exit(4);
				}
				phantom.exit();
			}, 1000);
		}
	});
}
catch (e)
{
	phantom.exit(3);
}