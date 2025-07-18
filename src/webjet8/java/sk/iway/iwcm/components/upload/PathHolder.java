package sk.iway.iwcm.components.upload;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * PathHolder is a class that holds information about a file being uploaded.
 * It includes the file name, the temporary path where the file is stored, and the timestamp of the upload.
 */
@Getter
@Setter
class PathHolder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String fileName;
    private String tempPath;
    private long timestamp;

    /**
     * Constructs a PathHolder with the specified file name, temporary path, and timestamp.
     *
     * @param fileName  the name of the file
     * @param tempPath  the temporary path where the file is stored
     * @param timestamp the timestamp when the file was uploaded
     */
    public PathHolder(String fileName, String tempPath, long timestamp) {
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.tempPath = tempPath;
    }
}
