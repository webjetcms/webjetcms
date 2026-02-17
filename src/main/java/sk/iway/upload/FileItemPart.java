package sk.iway.upload;

import jakarta.servlet.http.Part;
import org.apache.commons.fileupload2.core.FileItem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class FileItemPart implements Part {

    private final FileItem fileItem;

    public FileItemPart(FileItem fileItem) {
        this.fileItem = fileItem;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return fileItem.getInputStream();
    }

    @Override
    public String getContentType() {
        return fileItem.getContentType();
    }

    @Override
    public String getName() {
        return fileItem.getFieldName();
    }

    @Override
    public String getSubmittedFileName() {
        return fileItem.getName();
    }

    @Override
    public long getSize() {
        return fileItem.getSize();
    }

    @Override
    public void write(String fileName) throws IOException {
        try {
            fileItem.write(new java.io.File(fileName).toPath());
        } catch (Exception e) {
            throw new IOException("Failed to write file", e);
        }
    }

    @Override
    public void delete() throws IOException {
        try {
            fileItem.delete();
        } catch (Exception e) {
            throw new IOException("Failed to delete file", e);
        }
    }

    @Override
    public String getHeader(String name) {
        // FileItem doesn't have headers, return null
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }
}