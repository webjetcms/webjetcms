package sk.iway.iwcm.admin.jstree;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       lpasek $
 * @created      2020/07/13 12:50
 *
 *  Abstraktny univerzalny RestController na pracu s JsTree
 *
 */
public abstract class JsTreeRestController<T> {

    private Prop prop;

    @Autowired
    private HttpServletRequest request;

    /**
     * Endpoint pre presun polozky v stromovej strukture
     * @param item  - {@link JsTreeMoveItem} presunuta polozka
     * @return
     */
    @PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
    @PostMapping("/tree")
    public ResponseEntity<Map<String, Object>> tree(@RequestBody JsTreeMoveItem item) {
        Map<String, Object> result = new HashMap<>();

        item.setSkipFoldersConst( Tools.getStringValue(request.getParameter("skipFolders"), null) );
        item.setRootFolder( Tools.getStringValue(request.getParameter("rootFolder"), null) );
        item.setHideRootParents( Tools.getBooleanValue(request.getParameter("hideRootParents"), false) );

        tree(result, item);

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint pre presun polozky v stromovej strukture
     * @param item  - {@link JsTreeMoveItem} presunuta polozka
     * @return
     */
    @PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> moveItem(@RequestBody JsTreeMoveItem item) {
        Map<String, Object> result = new HashMap<>();

        move(result, item);

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint pre ulozenie polozky
     * @param item  - {@link JsTreeMoveItem} presunuta polozka
     * @return
     */
    @PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveItem(@RequestBody T item) {
        Map<String, Object> result = new HashMap<>();

        save(result, item);

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint pre zmazanie polozky
     * @param item  - {@link JsTreeMoveItem} presunuta polozka
     * @return
     */
    @PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteItem(@RequestBody T item) {
        Map<String, Object> result = new HashMap<>();

        delete(result, item);

        return ResponseEntity.ok(result);
    }

    /**
     * @param result - mapa s vysledkom, ocakava sa kluc result (boolean) a moze obsahovat error (String), ktory sa na FE zobrazi v toastr.error
     * @param item - {@link JsTreeMoveItem} presunuta polozka
     */
    protected abstract void tree(Map<String, Object> result, JsTreeMoveItem item);

    /**
     * @param result - mapa s vysledkom, ocakava sa kluc result (boolean) a moze obsahovat error (String), ktory sa na FE zobrazi v toastr.error
     * @param item - {@link JsTreeMoveItem} presunuta polozka
     */
    protected abstract void move(Map<String, Object> result, JsTreeMoveItem item);

    /**
     * @param result - mapa s vysledkom, ocakava sa kluc result (boolean) a moze obsahovat error (String), ktory sa na FE zobrazi v toastr.error
     * @param item - {@link T} polozka na ulozenie
     */
    protected abstract void save(Map<String, Object> result, T item);

    /**
     * @param result - mapa s vysledkom, ocakava sa kluc result (boolean) a moze obsahovat error (String), ktory sa na FE zobrazi v toastr.error
     * @param item - {@link T} polozka na zmazanie
     */
    protected abstract void delete(Map<String, Object> result, T item);

    /**
     * Metoda pre kontrolu pristupu pouzivatela pomocou requestu
     * @param request
     * @return
     */
    public abstract boolean checkAccessAllowed(HttpServletRequest request);

    protected Prop getProp() {
        if (prop == null) {
            prop = Prop.getInstance(getRequest());
        }
        return prop;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public Identity getUser(){
		return UsersDB.getCurrentUser(getRequest());
	}
}
