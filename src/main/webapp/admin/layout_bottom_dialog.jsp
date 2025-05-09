<%@page import="sk.iway.iwcm.stat.BrowserDetector"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<iwcm:empty name="inIframe">

	<iwcm:present name="closeTable">
	   <table border="0" cellspacing="0" cellpadding="0" width="100%" class="closeTable">
	</iwcm:present>

	<iwcm:notPresent name="closeTable">
	      <% if (BrowserDetector.isSmartphoneOrTablet(request)==false) {%></div><% } %>
		</td>
	</tr>
	</iwcm:notPresent>

			<tr id="buttonsBottomRow" height="50">
				<td class="PopupButtons" colspan="2">
					<table cellspacing="0" cellpadding="0" border="0">
						<tr>
							<td style="text-align: left; width: 100%;" nowrap="nowrap">
								<input id="btnHelp" type="button" value="<iwcm:text key="menu.top.help"/>" onclick="m_click_help();" />&nbsp;
							</td>
							<td style="text-align: right;" align="right" nowrap="nowrap">
								<iwcm:write name="dialogBottomButtons"/>
								<input id="btnCancel" style="width: 90px;" type="button" value="<iwcm:text key="button.cancel"/>" onclick="cancelWindow();"/>
								<input id="btnOk" style="width: 90px;" type="button" value="<iwcm:text key="button.ok"/>" onclick="Ok();" />&nbsp;
							</td>
						</tr>
					</table>
				</td>
			</tr>
		</table>

	</body>

	<script type="text/javascript">
		if (document.getElementById("waitDiv")!=null)
			document.getElementById("waitDiv").style.display="none";
	</script>

	<iwcm:combine type="js" set="adminStandardJs" />

	<script type="text/javascript">

	$(document).ready(function(){

		Metronic.init(); // init metronic core components
		Layout.init(); // init current layout
		ComponentsDropdowns.init();
		ComponentsPickers.init();
		Metronic.initUniform();

        var footerHeight = $(".PopupButtons table").height();
        if ($(".tab-pane").length>0) {
            var headerHeight = $(".tab-pane").offset().top;
        } else {
            var headerHeight = $("#headerTopRow").height();
        }

        //setTimeout(function() {


            var contentWidth = $(".mainTab").outerWidth() + 50;
            var contentHeight = $("#dialogCentralRow").height() + 10;
            contentHeight = (contentHeight > 500 ? 500 : contentHeight);
            var tabMenuHeight = $(".box_tab_thin").height();


            //console.log("headerHeight="+headerHeight+" footerHeight="+footerHeight+" contentHeight="+contentHeight);
            //window.alert("headerHeight="+headerHeight+" footerHeight="+footerHeight+" contentHeight="+contentHeight);

            if ($('.tab-page').length > 1) {
                $('.tab-page').each(function(){
                    $(this).siblings("hide");
                    $(this).show();

                    var height = $("#dialogCentralRow").height() + 10;

                    if (height > contentHeight) {
                        contentHeight = height;
                    }
                });

                $('.tab-page').hide();
                $('.tab-page:eq(0)').show();
            }

            if (contentHeight < 150)
            {
                //zle zdetekovana velkost
                var padding10Height = $("#dialogCentralRow .padding10").height();
                console.log("padding10Height="+padding10Height);
                if (padding10Height > 50)
                {
                    contentHeight = padding10Height + 20;
                }
            }

            if (contentHeight < 150) contentHeight = 150;

            $('#dialogCentralRow > .padding10').css('min-height', '100%').css('box-sizing', 'border-box');

            var setPopupHeight = headerHeight + footerHeight + contentHeight;

            //console.log(contentWidth);
            //console.log(setPopupHeight);

            //window.alert("contentWidth="+contentWidth+" setPopupHeight="+setPopupHeight);

            <% if (request.getAttribute("disableAutoResize")==null && request.getAttribute("closeTable")==null) { %>
                resizeDialogNew(contentWidth, setPopupHeight);
                setTimeout(function() {
                    //console.log("after timeout");
                    //resizeDialogNew(contentWidth, setPopupHeight);
                }, 500);
            <% } %>
        //}, 500);

		var waitForFinalEvent = (function () {
		  var timers = {};
		  return function (callback, ms, uniqueId) {
		    if (!uniqueId) {
		      uniqueId = "Don't call this twice without a uniqueId";
		    }
		    if (timers[uniqueId]) {
		      clearTimeout (timers[uniqueId]);
		    }
		    timers[uniqueId] = setTimeout(callback, ms);
		  };
		})();

		<% if (request.getAttribute("disableAutoResize")==null) { %>
		$(window).resize(function () {
			var popup = $(".popupSizeFinder");

			waitForFinalEvent(function(){

					var popupHeight = popup.height();
					var changeContentHeight = popupHeight - footerHeight - headerHeight;

					if ($(".tab-pane").length>0) {
						$(".tab-pane").css("max-height", changeContentHeight + 'px');
						$("#dialogCentralRow").css("max-height", changeContentHeight + tabMenuHeight + 10 + 'px');
				      } else {
				      	$("#dialogCentralRow").css("max-height", changeContentHeight + 'px');
				      }

					//window.alert("Setting width");
					//bez tohto tam chrome nachaval medzeru napravo
					if ($(".padding10").length>0) $(".padding10").css("border-right", "1px solid #f6f6f6");

			    }, 500, "some unique string");
			});
		<% } %>

		if ($(".box_tab").length>0) {
			$("#headerTopRow").css("border-bottom-width", "0");
		}
	});

	</script>

	</html>
</iwcm:empty>

<iwcm:notEmpty name="inIframe">

	<iwcm:notEmpty name="widgetData">
		</li>
		</ul>
		</div>


	</iwcm:notEmpty>


	</body>
	<iwcm:combine type="js" set="adminStandardJs" />

</iwcm:notEmpty>