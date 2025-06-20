<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,org.apache.commons.codec.binary.Base64,sk.iway.iwcm.i18n.*,java.util.*"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<iwcm:checkLogon admin="true" perms="cmp_app-social_icon"/>

	<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
	Prop prop = Prop.getInstance(request);
	request.setAttribute("cmpName", "app-social_icon");
	request.setAttribute("titleKey",
			"components.app-social_icon.title");
	request.setAttribute("descKey",
			"components.app-social_icon.desc");
	request.setAttribute("iconLink",
			"/components/app-social_icon/editoricon.png");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams)) {
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	//out.println("pageParams: " + pageParams.getPageParams());
	String blog_url = pageParams.getValue("blog_url", "");
	String facebook_url = pageParams.getValue("facebook_url", "");
	String flickr_url = pageParams.getValue("flickr_url", "");
	String instagram_url = pageParams.getValue("instagram_url", "");
	String linkedin_url = pageParams.getValue("linkedin_url", "");
	String mail_url = pageParams.getValue("mail_url", "");
	String rss_url = pageParams.getValue("rss_url", "");
	String twitter_url = pageParams.getValue("twitter_url", "");
	String youtube_url = pageParams.getValue("youtube_url", "");
	Base64 b64 = new Base64();
	if (Tools.isNotEmpty(blog_url)) {
		blog_url = new String(b64.decode(blog_url.getBytes()));
	}
	if (Tools.isNotEmpty(facebook_url)) {
		facebook_url = new String(b64.decode(facebook_url.getBytes()));
	}
	if (Tools.isNotEmpty(flickr_url)) {
		flickr_url = new String(b64.decode(flickr_url.getBytes()));
	}
	if (Tools.isNotEmpty(instagram_url)) {
		instagram_url = new String(b64.decode(instagram_url.getBytes()));
	}
	if (Tools.isNotEmpty(linkedin_url)) {
		linkedin_url = new String(b64.decode(linkedin_url.getBytes()));
	}
	if (Tools.isNotEmpty(mail_url)) {
		mail_url = new String(b64.decode(mail_url.getBytes()));
	}
	if (Tools.isNotEmpty(rss_url)) {
		rss_url = new String(b64.decode(rss_url.getBytes()));
	}
	if (Tools.isNotEmpty(twitter_url)) {
		twitter_url = new String(b64.decode(twitter_url.getBytes()));
	}
	if (Tools.isNotEmpty(youtube_url)) {
		youtube_url = new String(b64.decode(youtube_url.getBytes()));
	}

	String style = pageParams.getValue("style", "01");

%>

<jsp:include page="/components/top.jsp" />

<script type="text/javascript">
	//<![CDATA[
	function getAppSocialIcon() {
		var blog_url_coded = $("#blog_url").val();
		var facebook_url_coded = $("#facebook_url").val();
		var flickr_url_coded = $("#flickr_url").val();
		var instagram_url_coded = $("#instagram_url").val();
		var linkedin_url_coded = $("#linkedin_url").val();
		var mail_url_coded = $("#mail_url").val();
		var rss_url_coded = $("#rss_url").val();
		var twitter_url_coded = $("#twitter_url").val();
		var youtube_url_coded = $("#youtube_url").val();
		var blog_url = window.btoa(blog_url_coded);
		var facebook_url = window.btoa(facebook_url_coded);
		var flickr_url = window.btoa(flickr_url_coded);
		var instagram_url = window.btoa(instagram_url_coded);
		var linkedin_url = window.btoa(linkedin_url_coded);
		var mail_url = window.btoa(mail_url_coded);
		var rss_url = window.btoa(rss_url_coded);
		var twitter_url = window.btoa(twitter_url_coded);
		var youtube_url = window.btoa(youtube_url_coded);
		var typ = $(".styleBox input:checked").val();
		return "!INCLUDE(/components/app-social_icon/social_icon.jsp, blog_url="
				+ blog_url
				+ ", style="+typ+", facebook_url="
				+ facebook_url
				+ ", flickr_url="
				+ flickr_url
				+ ", instagram_url="
				+ instagram_url
				+ ", linkedin_url="
				+ linkedin_url
				+ ", mail_url="
				+ mail_url
				+ ", rss_url="
				+ rss_url
				+ ", twitter_url="
				+ twitter_url
				+ ", socialIconAlign="
				+ $('input[name="customTextAlign"]:checked').val()
				+ ", youtube_url="
				+ youtube_url + ")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getAppSocialIcon());
		return true;
	}
	//--------------------------- align ------------------
	  function changeCustomStyle(){


		    $('label[for="textAlignRight"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right.png)");
		    $('label[for="textAlignLeft"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left.png)");
		    $('label[for="textAlignCenter"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center.png)");
		    $('label[for="textAlignJustify"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify.png)");


		    switch($('input[name="customTextAlign"]:checked').val()){
		    	case "left":
		    		$('label[for="textAlignLeft"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left_selected.png)");
		    		break;

		    case "right":
		    		$('label[for="textAlignRight"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right_selected.png)");
		    		break;

		    case "center":
		    		$('label[for="textAlignCenter"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center_selected.png)");
		    		break;

		    case "justify":
		    		$('label[for="textAlignJustify"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify_selected.png)");
		    		break;
		    }

		    }
	  $(document).ready(function(){

		   switch($('input[name="customTextAlign"]:checked').val()){
		   	case "left":
		   		$('label[for="textAlignLeft"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left_selected.png)");
		   		break;

		   case "right":
		   		$('label[for="textAlignRight"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right_selected.png)");
		   		break;

		   case "center":
		   		$('label[for="textAlignCenter"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center_selected.png)");
		   		break;

		   case "justify":
		   		$('label[for="textAlignJustify"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify_selected.png)");
		   		break;
		   }
	  });
	  // -------------------------END align	----------------------

	//]]>
</script>
<style type="text/css">

	.styleBox {display: block; position: relative; width: 342px; height: 160px; background: #fff; margin: 3px; padding: 10px; border: 1px solid #bcbcbc; border-radius: 4px;}
	* HTML BODY .styleBox {width: 402px; height: 180px;}

	.boxes .styleBox {height: 110px;}
	* HTML BODY .boxes .styleBox {height: 130px;}

	.styleBox .radioSelect { position: absolute; left: 0; top: 0; text-align: left; width: 100%; height: 100%;}
	.styleBox .radioSelect input {position: absolute; left: 10px; top: 80px; border: 0px none;}
	.boxes .styleBox .radioSelect input  {top: 55px;}
	.styleBox img  {position: absolute; top: 10px; left: 10px;}

	div.colBox {display: block; float: left; margin: 10px 10px 0 0; padding: 0;  width: 408px; overflow: auto;}

	div.clearer {width: 100%; clear: both; height: 0; line-height: 0; font-size: 0; display: block; visibility: hidden;}

	select { width: 300px; }
	input { padding-left: 4px; }
	input[type=text] { width:100%; }
</style>

<div class="tab-pane toggle_content tab-pane-fullheight">



				<div class="col-xs-6">
					<strong><iwcm:text key="components.news.visualStyle"/>:</strong>

					<div id="styleSelectArea" style="height: 480px; width: 430px; overflow: auto;">

						<%
						int checkedInputPosition = 0;
						IwcmFile stylesDir = new IwcmFile(Tools.getRealPath("/components/app-social_icon/admin-styles/"));
						if (stylesDir.exists() && stylesDir.canRead())
						{
							IwcmFile styleFiles[] = stylesDir.listFiles();
							styleFiles = FileTools.sortFilesByName(styleFiles);
							int counter = 0;
							for (IwcmFile file : styleFiles)
							{
								if (file.getName().endsWith(".png")==false) continue;

								String styleValue = Tools.escapeHtml(file.getName().substring(0, file.getName().lastIndexOf(".")));
								if (styleValue.equals(style)) checkedInputPosition = counter;
								%>

									<div class="styleBox">
										<label class="image" for="style-<%=styleValue%>">
											<img src="<%=Tools.escapeHtml(file.getVirtualPath()) %>" alt="<%=styleValue%>" />
											<div class="radioSelect">
			  									<input type="radio" name="style" id="style-<%=styleValue%>" value="<%=styleValue%>" <%= styleValue.equals(style) ? " checked=\"checked\"" : "" %> onclick="changeTyp(this.value)"/>
			  									<% if ("iwcm.interway.sk".equals(request.getServerName())) out.print(styleValue); %>
			  								</div>
										</label>
									</div>
								<%
								counter++;
							}
						}
						%>
					</div>
				</div>

				<div class="col-xs-6">
					<form name=textForm>


	<div class="form-group clearfix">
	<div class="col-xs-12" colspan="2"><iwcm:text key="components.app-social_icon.editor_components.align"/></div>
	</div>


		<div class="form-group clearfix">

<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_left.png)" for="textAlignLeft"></label>
<input style="display:none" type="radio" onclick="changeCustomStyle()" id="textAlignLeft" name="customTextAlign" value="left" <% if(pageParams.getValue("socialIconAlign", "left").equals("left")){%>checked="checked"<%}%>>
<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_center.png)" for="textAlignCenter"></label>
<input style="display:none" type="radio" onclick="changeCustomStyle()" id="textAlignCenter" name="customTextAlign" value="center" <% if(pageParams.getValue("socialIconAlign", "left").equals("center")){%>checked="checked"<%}%> >
<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_right.png)" for="textAlignRight"></label>
<input style="display:none" type="radio" onclick="changeCustomStyle()" id="textAlignRight" name="customTextAlign" value="right" <% if(pageParams.getValue("socialIconAlign", "left").equals("right")){%>checked="checked"<%}%>>
			</div>


	<div class="form-group clearfix">
	<div class="col-xs-12"><iwcm:text key="components.app-social_icon.editor_components.info"/></div>

	</div>
		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.facebook")%>">
				<img src="/components/app-social_icon/facebook.png">
			</div>
			<div class="col-xs-10"><input type="text" name="facebook_url"
				id="facebook_url" value="<%=facebook_url%>" size="70"
				maxlength="250" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.instagram")%>">
				<img src="/components/app-social_icon/instagram.png">
			</div>
			<div class="col-xs-10"><input type="text" name="instagram_url"
				id="instagram_url" value="<%=instagram_url%>" size="70"
				maxlength="250" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.linkedin")%>">
				<img src="/components/app-social_icon/linkedin.png">
			</div>
			<div class="col-xs-10"><input type="text" name="linkedin_url"
				id="linkedin_url" value="<%=linkedin_url%>" size="70"
				maxlength="250" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.youtube")%>">
				<img src="/components/app-social_icon/youtube.png">
			</div>
			<div class="col-xs-10"><input type="text" name="youtube_url"
				id="youtube_url" value="<%=youtube_url%>" size="70" maxlength="250" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.twitter")%>">
				<img src="/components/app-social_icon/twitter.png">
			</div>
			<div class="col-xs-10"><input type="text" name="twitter_url"
				id="twitter_url" value="<%=twitter_url%>" size="70" maxlength="250" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.mail")%>">
				<img src="/components/app-social_icon/mail.png">
			</div>
			<div class="col-xs-10"><input type="text" name="mail_url"
				id="mail_url" value="<%=mail_url%>" size="70" maxlength="250" /></div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.blog")%>">
				<img src="/components/app-social_icon/blog.png">
			</div>
			<div class="col-xs-10"><input type="text" name="blog_url"
				id="blog_url" value="<%=blog_url%>" size="70" maxlength="250" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.flickr")%>">
				<img src="/components/app-social_icon/flickr.png">
			</div>
			<div class="col-xs-10"><input type="text" name="flickr_url"
				id="flickr_url" value="<%=flickr_url%>" size="70" maxlength="250" /></div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-2"
				title="<%=prop.getText("components.app-social_icon.editor_components.rss")%>">
				<img src="/components/app-social_icon/rss.png">
			</div>
			<div class="col-xs-10"><input type="text" name="rss_url" id="rss_url"
				value="<%=rss_url%>" size="70" maxlength="250" /></div>
		</div>


	</form>
</div>


<jsp:include page="/components/bottom.jsp" />
