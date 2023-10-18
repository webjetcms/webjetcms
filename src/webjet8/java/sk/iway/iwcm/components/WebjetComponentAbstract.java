package sk.iway.iwcm.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Abstraktná trieda pre Webjet komponentu
 */
public abstract class WebjetComponentAbstract implements WebjetComponentInterface {

    /**
     * String viewFolder slúži pre zadanie podadresáru, kde sa bude výsledný view hľadať
     */
    private String viewFolder;

    /**
     * Inicializačná metóda pre custom inicializáciu komponenty
     * Volá sa pri každom vložení komponenty do stránky
     */
    public void init() {

    }

    /**
     * Inicializačná metóda pre custom inicializáciu komponenty
     * Volá sa pri každom vložení komponenty do stránky
     */
    public void init(HttpServletRequest request, HttpServletResponse response) {

    }

    public String getViewFolder() {
        return viewFolder;
    }

    public void setViewFolder(String viewFolder) {
        this.viewFolder = viewFolder;
    }
}
