package sk.iway.iwcm.components.upload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class XhrFileUploadResponse {

    // kvoli chybe pri implementacii bolo toto vracane ako pole, preto to musim vraciat ako pole
    private List<String> name;
    // kvoli chybe pri implementacii bolo toto vracane ako pole, preto to musim vraciat ako pole
    private List<String> key;
    private Long size;
    private boolean success;
    private String error;
    private String file;

    @JsonProperty(value = "chunk-uploaded")
    private Integer chunkUploaded;

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public void putName(String name) {
        if (this.name == null) {
            this.name = new ArrayList<>();
        }
        else {
            this.key.clear();
        }
        this.name.add(name);
    }

    public List<String> getKey() {
        return key;
    }

    public void setKey(List<String> key) {
        this.key = key;
    }

    public void putKey(String key) {
        if (this.key == null) {
            this.key = new ArrayList<>();
        }
        else {
            this.key.clear();
        }
        this.key.add(key);
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Integer getChunkUploaded() {
        return chunkUploaded;
    }

    public void setChunkUploaded(Integer chunkUploaded) {
        this.chunkUploaded = chunkUploaded;
    }
}