package sk.iway.iwcm.tags.support_logic;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.tagext.TagSupport;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.tags.support_logic.action.ActionMapping;

@Getter
@Setter
public class FormTag extends TagSupport {
   private static final long serialVersionUID = 1L;
   public static final String TRANSACTION_TOKEN_KEY = "org.apache.struts.action.TOKEN";
   public static final String TOKEN_KEY = "org.apache.struts.taglib.html.TOKEN";

   protected static String lineEnd = System.getProperty("line.separator");
   protected String action = null;
   private String autocomplete = null;
   private String postbackAction = null;
   //protected ModuleConfig moduleConfig = null;
   protected String enctype = null;
   protected String focus = null;
   protected String focusIndex = null;
   protected ActionMapping mapping = null;
   protected String method = null;
   protected String onreset = null;
   protected String onsubmit = null;
   protected boolean scriptLanguage = true;
   //protected ActionServlet servlet = null;
   protected String style = null;
   protected String styleClass = null;
   protected String styleId = null;
   protected String target = null;
   protected String beanName = null;
   protected String beanScope = null;
   protected String beanType = null;
   protected String acceptCharset = null;
   private boolean disabled = false;
   protected boolean readonly = false;
   private String lang = null;
   private String dir = null;
   protected String name = null;
   protected String type = null;
   protected String scope = null;

   protected String renderToken() {
      StringBuffer results = new StringBuffer();
      HttpSession session = pageContext.getSession();

      if (session != null) {
            String token =
               (String) session.getAttribute(TRANSACTION_TOKEN_KEY);

            if (token != null) {
               results.append("<div><input type=\"hidden\" name=\"");
               results.append(TOKEN_KEY);
               results.append("\" value=\"");
               results.append(ResponseUtils.filter(token));

               if (this.isXhtml()) {
                  results.append("\" />");
               } else {
                  results.append("\">");
               }

               results.append("</div>");
            }
      }

      return results.toString();
   }

   private boolean isXhtml() {
      return CustomTagUtils.getInstance().isXhtml(this.pageContext);
   }
}