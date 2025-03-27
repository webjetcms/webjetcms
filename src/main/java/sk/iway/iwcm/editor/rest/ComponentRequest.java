package sk.iway.iwcm.editor.rest;

import lombok.Getter;
import lombok.Setter;

/**
 * Reprezentuje JSON request pre zobrazenie datatabulky parametrov aplikacie
 */
@Getter
@Setter
public class ComponentRequest {
    private String className;
    private String parameters;
    private int docId;
    private int groupId;
    private String pageTitle;
    private String originalComponentName;
    private String originalJspFileName;
}