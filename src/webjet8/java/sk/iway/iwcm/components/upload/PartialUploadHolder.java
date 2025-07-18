package sk.iway.iwcm.components.upload;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PartialUploadHolder is a class that holds information about a file being uploaded in chunks.
 * It includes the number of chunks, the name of the file, and a list of paths for each part of the upload.
 */
@Getter
@Setter
class PartialUploadHolder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int chunks;
    private String name;

    private List<String> partPaths;

    /**
     * Constructs a PartialUploadHolder with the specified number of chunks and file name.
     *
     * @param chunks the number of chunks for the upload
     * @param name   the name of the file being uploaded
     */
    public PartialUploadHolder(int chunks, String name) {
        this.chunks = chunks;
        this.name = name;
        partPaths = new ArrayList<>(chunks);
    }
}
