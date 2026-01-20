package sk.iway.iwcm.editor.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDetails;

/**
 * Test pre EditorUtils - hlavne pre nonBreakingSpaceReplacement funkcionalitu
 */
class EditorUtilsTest {

    private DocDetails docDetails;

    @BeforeEach
    void setUp() {
        docDetails = new DocDetails();
    }

    @Test
    @DisplayName("Základné nahradenie medzier za spojkami")
    void testBasicConjunctionReplacement() {
        // Test základných spojok
        docDetails.setData("Text a potom ďalší text.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Text a&nbsp;potom ďalší text.", docDetails.getData());

        docDetails.setData("Idem v lese s kamarátom.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Idem v&nbsp;lese s&nbsp;kamarátom.", docDetails.getData());

        docDetails.setData("Položka číslo 5. v zozname.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Položka číslo 5.&nbsp;v&nbsp;zozname.", docDetails.getData());

        docDetails.setData("Test a v case");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Test a&nbsp;v&nbsp;case", docDetails.getData());

        docDetails.setData("Test a&nbsp;v&nbsp;case");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Test a&nbsp;v&nbsp;case", docDetails.getData());
    }

    @Test
    @DisplayName("HTML atribúty sa nemajú meniť")
    void testHtmlAttributesNotAffected() {
        // Test že HTML atribúty sa nemenia
        docDetails.setData("<a href=\"test.html?param=a value\">Link a text</a>");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("<a href=\"test.html?param=a value\">Link a&nbsp;text</a>", docDetails.getData());

        docDetails.setData("<img src=\"image.jpg\" alt=\"Obrázok a popis\" /> Text s obrázkom.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("<img src=\"image.jpg\" alt=\"Obrázok a popis\" /> Text s&nbsp;obrázkom.", docDetails.getData());

        docDetails.setData("<div class=\"class a another\">Content s text</div>");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("<div class=\"class a another\">Content s&nbsp;text</div>", docDetails.getData());
    }

    @Test
    @DisplayName("Komplexný HTML s viacerými tagmi")
    void testComplexHtml() {
        String complexHtml = "<div class=\"container\">" +
                "<p>Prvý odstavec a <strong>tučný text</strong> v dokumente.</p>" +
                "<ul>" +
                "<li>Položka s číslom 1. v zozname</li>" +
                "<li><a href=\"link.html?test=a param\">Link a text</a></li>" +
                "</ul>" +
                "</div>";

        String expectedHtml = "<div class=\"container\">" +
                "<p>Prvý odstavec a&nbsp;<strong>tučný text</strong> v&nbsp;dokumente.</p>" +
                "<ul>" +
                "<li>Položka s&nbsp;číslom 1.&nbsp;v&nbsp;zozname</li>" +
                "<li><a href=\"link.html?test=a param\">Link a&nbsp;text</a></li>" +
                "</ul>" +
                "</div>";

        docDetails.setData(complexHtml);
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals(expectedHtml, docDetails.getData());
    }

    @Test
    @DisplayName("Už existujúce &nbsp; entity")
    void testExistingNbspEntities() {
        docDetails.setData("Text s&nbsp;existujúcou entitou a novou spojkou.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Text s&nbsp;existujúcou entitou a&nbsp;novou spojkou.", docDetails.getData());

        docDetails.setData("Text&nbsp;a &nbsp;ďalší a text.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Text&nbsp;a&nbsp;&nbsp;ďalší a&nbsp;text.", docDetails.getData());
    }

    @Test
    @DisplayName("Viacero spojok za sebou")
    void testMultipleConjunctionsInSequence() {
        docDetails.setData("Test a v texte s obsahom.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Test a&nbsp;v&nbsp;texte s&nbsp;obsahom.", docDetails.getData());
    }

    @Test
    @DisplayName("Číselné spojky s bodkou")
    void testNumericConjunctions() {
        docDetails.setData("Bod 1. v zozname a bod 25. v tom istom.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Bod 1.&nbsp;v&nbsp;zozname a&nbsp;bod 25.&nbsp;v&nbsp;tom istom.", docDetails.getData());
    }

    @Test
    @DisplayName("Prázdny alebo null obsah")
    void testEmptyOrNullContent() {
        docDetails.setData("");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("", docDetails.getData());
    }

    @Test
    @DisplayName("Žiadne konfiguračné spojky")
    void testNoConjunctionsConfigured() {
        Constants.setString("editorSingleCharNbsp", "");
        docDetails.setData("Text a obsah s rôznymi spojkami.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Text a obsah s rôznymi spojkami.", docDetails.getData());

        Constants.setString("editorSingleCharNbsp", null);
        docDetails.setData("Text a obsah s rôznymi spojkami.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Text a obsah s rôznymi spojkami.", docDetails.getData());
    }

    @Test
    @DisplayName("HTML komentáre")
    void testHtmlComments() {
        docDetails.setData("<!-- komentár a poznámka --> Text a obsah.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("<!-- komentár a poznámka --> Text a&nbsp;obsah.", docDetails.getData());
    }

    @Test
    @DisplayName("Vnorené HTML tagy")
    void testNestedHtmlTags() {
        docDetails.setData("<div><span>Text a <em>zvýraznený a text</em></span> s pokračovaním.</div>");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("<div><span>Text a&nbsp;<em>zvýraznený a&nbsp;text</em></span> s&nbsp;pokračovaním.</div>", docDetails.getData());
    }

    @Test
    @DisplayName("Špecifické znaky a diakritika")
    void testSpecialCharactersAndDiacritics() {
        docDetails.setData("Slovo s diakritikou ľščťž a pokračovanie.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Slovo s&nbsp;diakritikou ľščťž a&nbsp;pokračovanie.", docDetails.getData());
    }

    @Test
    @DisplayName("Veľké a malé písmená")
    void testCaseInsensitivity() {
        docDetails.setData("Text A veľké písmeno alebo a malé písmeno.");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("Text A&nbsp;veľké písmeno alebo a&nbsp;malé písmeno.", docDetails.getData());
    }

    @Test
    @DisplayName("Spojky na začiatku a konci textu")
    void testConjunctionsAtStartAndEnd() {
        docDetails.setData(" a začiatok textu");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals(" a&nbsp;začiatok textu", docDetails.getData());

        docDetails.setData("koniec textu a ");
        EditorUtils.nonBreakingSpaceReplacement(docDetails);
        assertEquals("koniec textu a&nbsp;", docDetails.getData());
    }
}