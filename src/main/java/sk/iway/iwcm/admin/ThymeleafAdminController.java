package sk.iway.iwcm.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import sk.iway.iwcm.*;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * ThymeleafAdminController.java
 *
 * Zakladna trieda pre zobrazenie stranok administracie cez Thymeleaf sablony
 *
 * Title webjet9 Company Interway a.s. (www.interway.sk) Copyright Interway a.s.
 * (c) 2001-2020
 *
 * @author $Author: jeeff $
 */
@Controller
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class ThymeleafAdminController {

   /**
    *
    * @param page String
    * @param subpage - String - nesmie obsahovat znak ., cize nemoze to byt subor
    * @param allParams MultiValueMap&lt;String, String&gt;
    * @param model ModelMap
    * @param request HttpServletRequest
    * @return String
    */
   @GetMapping({ "/admin/v9/", "/admin/v9/{page}/", "/admin/v9/{page}/{subpage:[a-zA-Z0-9_-]*}/" })
   public ModelAndView defaultHandler(@PathVariable(required = false) String page,
         @PathVariable(required = false) String subpage,
         @RequestParam(required = false) final MultiValueMap<String, String> allParams,
         final ModelMap model,
         final RedirectAttributes redirectAttributes,
         final HttpServletRequest request) {

      // /admin/v9/ == zobraz dashboard
      if (Tools.isEmpty(page)) {
         page = "dashboard";
         subpage = "overview";
      }

      if (Tools.isEmpty(subpage))
         subpage = "index";

      // @TODO: nepotrebujeme sem nejaku kontrolu URL parametrov?
      String forward = "admin/v9/dist/views/" + page + "/" + subpage;

      Logger.debug(ThymeleafAdminController.class, "Thymeleaf forward=" + forward);

      removePermissionFromCurrentUser(request);
      fireEvent(page, subpage, model, redirectAttributes, request);
      setLayout(model, request);

      if (model.containsKey("redirect")) {
         return new ModelAndView("redirect:" + model.get("redirect"));
      }

      forward = checkPerms(forward, forward, request);
      return new ModelAndView(forward);
   }

   @PostMapping(path = { "/admin/v9/{page}/", "/admin/v9/{page}/{subpage:[a-zA-Z0-9_-]*}/" }, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
   public ModelAndView defaultHandlerPost(@PathVariable(required = false) String page,
         @PathVariable(required = false) String subpage,
         @RequestParam(required = false) final MultiValueMap<String, String> allParams,
         final ModelMap model,
         final RedirectAttributes redirectAttributes,
         final HttpServletRequest request) {

         Logger.debug(ThymeleafAdminController.class, "post loaded admin: {}", page);

         return defaultHandler(page, subpage, allParams, model, redirectAttributes, request);
   }

   /**
    * Vykona forward pre admin cast aplikacii, tie sa nachadzaju v /app/MENO_APP/admin/ adresari
    * Technicky vykona forward na app/default.pug kde sa do vnutra stranky dynamicky includne
    * HTML subor z app adresara (cize v nom nie je potrebne riesit okoli stranky ako hlavicka a menu)
    * @param app String
    * @param subpage String
    * @param model ModelMap
    * @param request HttpServletRequest
    * @return String
    */
   @GetMapping({ "/apps/{app}/admin/", "/apps/{app}/admin/index.html", "/apps/{app}/admin/{subpage:[a-zA-Z0-9_-]*}/" })
   public ModelAndView appHandler(
           @PathVariable String app,
           @PathVariable(required = false) String subpage,
           final ModelMap model,
           final RedirectAttributes redirectAttributes,
           final HttpServletRequest request)
   {
      String originalPath = "/apps/"+app+"/admin/";
      if (Tools.isNotEmpty(subpage) && "index.html".equals(subpage)==false) originalPath += subpage+"/";

      //spring zrazu prazdne subpage vracia ako index.html
      if (Tools.isEmpty(subpage) || "index.html".equals(subpage))
         subpage = "index";

      String forward = "admin/v9/dist/views/apps/default";

      final String appIncludePath = "apps/"+app+"/admin/"+subpage+".html";
      //kontrola, ci zadana cesta existuje
      if (FileTools.isFile("/"+appIncludePath) == false) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      model.addAttribute("appIncludePath", appIncludePath);

      Logger.debug(ThymeleafAdminController.class, "Thymeleaf APP forward=" + forward + " appIncludePath=" + appIncludePath);

      //check if there is index.js file to include as script element
      String jsFilePath = "/apps/"+app+"/admin/"+app+".js";
      if (FileTools.isFile(jsFilePath)) {
         model.addAttribute("appIncludePathJs", jsFilePath);
      }

      removePermissionFromCurrentUser(request);
      fireEvent(app, subpage, model, redirectAttributes, request);
      setLayout(model, request);

      if (model.containsKey("redirect")) {
         return new ModelAndView("redirect:" + model.get("redirect"));
      }

      forward = checkPerms(forward, originalPath, request);
      return new ModelAndView(forward);
   }

   @PostMapping(path = { "/apps/{app}/admin/", "/apps/{app}/admin/index.html", "/apps/{app}/admin/{subpage:[a-zA-Z0-9_-]*}/" }, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
   public ModelAndView appHandlerPost(
           @PathVariable String app,
           @PathVariable(required = false) String subpage,
           final ModelMap model,
           final RedirectAttributes redirectAttributes,
           final HttpServletRequest request) {
      Logger.debug(ThymeleafAdminController.class, "post loaded: {}", app);

      return appHandler(app, subpage, model, redirectAttributes, request);
   }

   /**
    * Vyvola event, na ktory sa da pocuvat a doplnit do modelu dalsie data
    * @param page String
    * @param subpage String
    * @param model ModelMap
    * @param request HttpServletRequest
    */
   private void fireEvent(String page, String subpage, ModelMap model, RedirectAttributes redirectAttributes, HttpServletRequest request) {
      //event vyvolame pred layoutService, keby event nieco v layoute menil (napr. domenu)
      if ("apps".equals(page)) {
         //pre napr. /admin/v9/apps/gallery nastavime podobne ako pre /apps/admin/gallery/
         page = subpage;
         subpage = "index";
      }
      ThymeleafEvent event = new ThymeleafEvent(page, subpage, model, redirectAttributes, request);
      (new WebjetEvent<>(event, WebjetEventType.ON_START)).publishEvent();
   }

   /**
    * Nastavi LayoutService pre generovanie hlavicky/menu
    * @param model ModelMap
    * @param request HttpServletRequest
    */
   private void setLayout(ModelMap model, HttpServletRequest request) {
      final LayoutService ls = new LayoutService(request);
      model.addAttribute("layout", ls.getLayoutBean());
      model.addAttribute("layoutService", ls);

      if ("true".equals(request.getParameter("userlngr"))) {
         String lng = Prop.getLng(request, false);
         Prop.getInstance(Constants.getServletContext(), lng, true);
      }
   }

   private void removePermissionFromCurrentUser(HttpServletRequest request) {
      String permission = request.getParameter("removePerm");
      if (Tools.isNotEmpty(permission)) removePermissionFromCurrentUser(permission, request);
   }

   private void removePermissionFromCurrentUser(String permissionString, HttpServletRequest request) {
      Identity user = UsersDB.getCurrentUser(request);
      //allow remove perms for e2e tests
      if (user != null && user.getLogin().startsWith("tester")) {

         String[] permsArr = Tools.getTokens(permissionString, ",");
         for(String permission : permsArr) {
            user.addDisabledItem(permission);
         }
         LogonTools.setUserToSession(request.getSession(), user);

         //setUserToSession nepomohlo, musime hacknut
         try
         {
               //prihlasenie pre SPRING / REST
               //RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
               if (Constants.getServletContext().getAttribute("springContext")!=null)
               {
                  Authentication authentication = WebjetAuthentificationProvider.authenticate(user);
                  List<GrantedAuthority> grantedAuths = new ArrayList<>();

                  for (GrantedAuthority ga : authentication.getAuthorities()) {
                     if (ga instanceof SimpleGrantedAuthority) {
                        SimpleGrantedAuthority sig = (SimpleGrantedAuthority)ga;

                        boolean remove = false;
                        for(String permission : permsArr) {
                           String itemKey = WebjetSecurityService.normalizeUserGroupName(permission);
                           String name = "ROLE_Permission_" + itemKey;
                           if (sig.getAuthority().equals(name)) {
                              Logger.debug(ThymeleafAdminController.class, "Removing SPRING perm "+name);
                              remove = true;
                           }
                        }

                        if (remove == false) {
                           grantedAuths.add(ga);
                        }
                     }
                  }

                  Authentication auth = new UsernamePasswordAuthenticationToken(
                     user.getLogin(),
                     "password",
                     grantedAuths);
                  SecurityContextHolder.getContext().setAuthentication(auth);
               }
         }
         catch (Exception ex)
         {
            Logger.error(ThymeleafAdminController.class, ex);
         }

         //este raz, lebo to zle nastavovalo
         user = UsersDB.getCurrentUser(request);
         for(String permission : permsArr) {
            user.addDisabledItem(permission);
         }
      }
   }

   /**
    * Overi pravo na zobrazenie originalUrl, ak je povolene, vrati forward, inak /admin/403.jsp
    * @param forward - adresa stranky pre zobrazenie (thymeleaf sablona)
    * @param originalUrl - povodna URL adresa v prehliadaci
    * @param request
    * @return
    */
   private String checkPerms(String forward, String originalUrl, HttpServletRequest request) {
      String perms = MenuService.getPerms(originalUrl);
      Identity user = UsersDB.getCurrentUser(request);

      if (Tools.isNotEmpty(perms) && user.isDisabledItem(perms)) {
         Logger.debug(ThymeleafAdminController.class, "====> PERMS DENIED: perms="+perms+" originalUrl="+originalUrl+" forward="+forward);
         return "redirect:/admin/403.jsp";
      }

      return forward;
   }
}