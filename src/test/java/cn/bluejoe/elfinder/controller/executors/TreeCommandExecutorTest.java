package cn.bluejoe.elfinder.controller.executors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;

class TreeCommandExecutorTest {
    @ParameterizedTest @CsvSource({"OBRAZ, contains, true", "zlte, startwith, true", "obrazky, endwith, true",
            "zlte obrazky, equals, true", "obrazky, equals, false", "secret, contains, false"})
    void searchesReadableFoldersWithAncestorsAndSkipsCycles(String query, String mode, boolean matches) throws Exception {
        FsItemEx parent = folder("Parent", folder("Žlté obrázky"));
        FsItemEx denied = folder("Secret"), file = mock(FsItemEx.class);
        FsItemEx root = folder("Root", parent, denied, file);
        when(root.listChildren()).thenReturn(List.of(parent, denied, file, root));
        when(denied.isReadable(denied)).thenReturn(false);
        var result = TreeCommandExecutor.searchFolders(root, query, mode);
        assertEquals(matches ? Set.of("Žlté obrázky") : Set.of(), result.matches());
        assertEquals(matches ? Set.of("Root", "Parent", "Žlté obrázky") : Set.of(), result.folders().keySet());
        verify(denied, never()).listChildren();
        verify(file, never()).listChildren();
        verify(root, times(1)).listChildren();
    }
    private FsItemEx folder(String name, FsItemEx... children) throws Exception {
        FsItemEx folder = mock(FsItemEx.class);
        when(folder.getHash()).thenReturn(name);
        when(folder.getPath()).thenReturn("/" + name);
        when(folder.getName()).thenReturn(name);
        when(folder.isFolder()).thenReturn(true);
        when(folder.isReadable(folder)).thenReturn(true);
        when(folder.listChildren()).thenReturn(List.of(children));
        return folder;
    }
}
