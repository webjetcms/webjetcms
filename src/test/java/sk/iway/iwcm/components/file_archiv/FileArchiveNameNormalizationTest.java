package sk.iway.iwcm.components.file_archiv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import sk.iway.iwcm.system.datatable.ProcessItemAction;

/**
 * Verifies normalization at archive controller boundaries, without testing database persistence.
 */
class FileArchiveNameNormalizationTest {

    /** Cleans both names for create/edit while retaining absent optional values. */
    @ParameterizedTest
    @EnumSource(value = ProcessItemAction.class, names = {"CREATE", "EDIT"})
    void normalizesDocumentNames(ProcessItemAction action) {
        FileArchiveRestController controller = new FileArchiveRestController(mock(FileArchiveRepository.class));
        FileArchivatorBean entity = new FileArchivatorBean();
        entity.setProduct("\u00A0Produkt\r\n2026\uFEFF");
        entity.setCategory(" \u202FRočné\u00A0 správy\u2060 ");

        controller.processToEntity(entity, action);

        assertEquals("Produkt 2026", entity.getProduct());
        assertEquals("Ročné správy", entity.getCategory());

        FileArchivatorBean empty = new FileArchivatorBean();
        empty.setProduct(null);
        empty.setCategory(null);
        controller.processToEntity(empty, action);
        assertNull(empty.getProduct());
        assertNull(empty.getCategory());
    }

    /** Cleans the replacement name without changing the original database lookup value. */
    @Test
    void normalizesRenameWithoutChangingOriginalName() {
        ManagerRestController controller = new ManagerRestController(mock(FileArchiveRepository.class));
        BasicManagerBean entity = new BasicManagerBean(1L, "\u00A0Original\u00A0name");
        entity.setNewName("\uFEFFNový\u202F názov\r\n");

        controller.beforeSave(entity);

        assertEquals("Nový názov", entity.getNewName());
        assertEquals("\u00A0Original\u00A0name", entity.getName());
    }

    /** Makes an invisible-only replacement fail the existing required-name constraint. */
    @Test
    void rejectsRenameThatBecomesEmpty() {
        ManagerRestController controller = new ManagerRestController(mock(FileArchiveRepository.class));
        BasicManagerBean entity = new BasicManagerBean(1L, "Original name");
        entity.setNewName("\u00A0\u202F\uFEFF\u2060\u00AD\r\n");

        controller.beforeSave(entity);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertEquals(1, factory.getValidator().validateProperty(entity, "newName").size());
        }
    }

    /** Sends the same normalized search text to both autocomplete queries. */
    @Test
    void normalizesAutocompleteQueries() {
        FileArchiveRepository repository = mock(FileArchiveRepository.class);
        FileArchiveRestController controller = new FileArchiveRestController(repository);

        controller.getAutocompleteProduct("\u00A0Ročné\u202F správy\r\n", "/files/archiv/");
        controller.getAutocompleteCategory("\u00A0Ročné\u202F správy\r\n", "/files/archiv/");

        verify(repository).findDistinctAllByProductLikeAndFilePathLikeAndDomainId(eq("%Ročné správy%"), eq("files/archiv/%"), anyInt());
        verify(repository).findDistinctAllByCategoryLikeAndFilePathLikeAndDomainId(eq("%Ročné správy%"), eq("files/archiv/%"), anyInt());
    }
}
