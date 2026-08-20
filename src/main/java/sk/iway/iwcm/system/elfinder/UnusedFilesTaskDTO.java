package sk.iway.iwcm.system.elfinder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnusedFilesTaskDTO {

    private String taskId;
    private String state;
    private String directory;
    private Boolean includeSubfolders;
    private Long startedAt;
    private Long finishedAt;
    private Integer totalFiles;
    private String error;
}
