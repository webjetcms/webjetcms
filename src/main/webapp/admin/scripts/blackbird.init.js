/*
	Blackbird Init
	
	<script type="text/javascript" src="/admin/scripts/blackbird.js"></script>
    <link type="text/css" rel="stylesheet" href="/admin/skins/webjet6/css/blackbird.css" />
    <script type="text/javascript">
		log.debug( 'this is a debug message' );
		log.info( 'this is an info message' );
		log.warn( 'this is a warning message' );
		log.error( 'this is an error message' );
	</script>
*/

var LOG_TIMEOUT = 1000

log = {
		debug : function(message){window.setTimeout(function(){log.debug(message)}, LOG_TIMEOUT)},
		info : function(message){window.setTimeout(function(){log.info(message)}, LOG_TIMEOUT)},
		warn : function(message){window.setTimeout(function(){log.warn(message)}, LOG_TIMEOUT)},
		error : function(message){window.setTimeout(function(){log.error(message)}, LOG_TIMEOUT)},
		profile : function(message){window.setTimeout(function(){log.profile(message)}, LOG_TIMEOUT)}
}

