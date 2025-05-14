package sk.iway.iwcm.tags.support_logic;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
// import org.apache.struts.action.ActionForm;
// import org.apache.struts.action.ActionMapping;
// import org.apache.struts.action.ActionServlet;
// import org.apache.struts.config.ActionConfig;
// import org.apache.struts.config.FormBeanConfig;
// import org.apache.struts.config.ModuleConfig;
// import org.apache.struts.taglib.TagUtils;
// import org.apache.struts.util.MessageResources;
// import org.apache.struts.util.RequestUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormTag extends TagSupport {
   private static final long serialVersionUID = 1L;
   protected static String lineEnd = System.getProperty("line.separator");
   //protected static MessageResources messages = MessageResources.getMessageResources("org.apache.struts.taglib.html.LocalStrings");
   protected String action = null;
   private String autocomplete = null;
   private String postbackAction = null;
   //protected ModuleConfig moduleConfig = null;
   protected String enctype = null;
   protected String focus = null;
   protected String focusIndex = null;
   //protected ActionMapping mapping = null;
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

}