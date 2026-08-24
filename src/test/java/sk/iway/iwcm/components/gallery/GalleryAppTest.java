package sk.iway.iwcm.components.gallery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.io.IwcmFile;

class GalleryAppTest {

    @Test
    void getStyleNamesFiltersNonJspFiles() {
        IwcmFile styleDirectory = directory(
            file("gallery-bento.css"),
            file("gallery-bento.js"),
            file("gallery-bento.jsp"),
            file("gallery-justified.jsp"),
            file("gallery-.jsp"),
            file("other.jsp"),
            directoryFile("gallery-directory.jsp")
        );

        assertEquals(List.of("bento", "justified"), GalleryApp.getStyleNames(styleDirectory));
    }

    @Test
    void getStyleNamesMergesDirectoriesAndRemovesDuplicates() {
        IwcmFile installationDirectory = directory(
            file("gallery-bento.jsp"),
            file("gallery-justified.jsp")
        );
        IwcmFile commonDirectory = directory(
            file("gallery-bento.jsp"),
            file("gallery-fullscreen.jsp")
        );

        assertEquals(
            List.of("bento", "justified", "fullscreen"),
            GalleryApp.getStyleNames(installationDirectory, commonDirectory)
        );
    }

    @Test
    void getStyleNamesIgnoresMissingDirectories() {
        IwcmFile missingDirectory = mock(IwcmFile.class);
        when(missingDirectory.listFiles()).thenReturn(null);
        IwcmFile commonDirectory = directory(file("gallery-fullscreen.jsp"));

        assertEquals(
            List.of("fullscreen"),
            GalleryApp.getStyleNames(null, missingDirectory, commonDirectory)
        );
    }

    private IwcmFile directory(IwcmFile... files) {
        IwcmFile directory = mock(IwcmFile.class);
        when(directory.listFiles()).thenReturn(files);
        return directory;
    }

    private IwcmFile file(String name) {
        IwcmFile file = mock(IwcmFile.class);
        when(file.getName()).thenReturn(name);
        when(file.isFile()).thenReturn(true);
        when(file.length()).thenReturn(100L);
        return file;
    }

    private IwcmFile directoryFile(String name) {
        IwcmFile file = mock(IwcmFile.class);
        when(file.getName()).thenReturn(name);
        when(file.isFile()).thenReturn(false);
        return file;
    }
}
