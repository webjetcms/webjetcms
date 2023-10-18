<script type="text/javascript">

jQuery(document).ready(function() {
	$(".table-wj thead tr").attr("role","row").attr("class","heading" );
	Metronic.init(); // init metronic core components
	Layout.init(); // init current layout
	//UITree.init();
	//TableLon.init();
	ComponentsDropdowns.init();
	ComponentsPickers.init();
	//Index.initMiniCharts();
	
	$('.portlet a.reload[data-load="true"]').click();
	//componentsEditors.init();
	
	if (document.getElementById("waitDiv")!=null) document.getElementById("waitDiv").style.display="none";
});

</script>
</body>
</html>
