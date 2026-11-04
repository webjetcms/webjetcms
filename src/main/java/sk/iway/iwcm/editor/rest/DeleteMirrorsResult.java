package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Direct mirrors shown before deleting selected pages or folders.
 */
@Getter
@Setter
public class DeleteMirrorsResult {
    private List<Item> items = new ArrayList<>();
    private boolean hiddenMirrors;

    @Getter
    @AllArgsConstructor
    public static class Item {
        private final int id;
        private final String fullPath;
        private final String domainName;
    }
}
