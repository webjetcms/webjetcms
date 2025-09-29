package sk.iway.iwcm.components.upload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PartialUploadHolder implements Serializable {
    private static final long serialVersionUID = 1L;
    private int chunks;
    private String name;
    private String key;
    private List<String> partPaths;

    public PartialUploadHolder(int chunks, String name, String key)
    {
        this.chunks = chunks;
        this.name = name;
        this.key = key;
        partPaths = new ArrayList<>(chunks);
    }

    public int getChunks()
    {
        return chunks;
    }

    public void setChunks(int chunks)
    {
        this.chunks = chunks;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public List<String> getPartPaths()
    {
        return partPaths;
    }

    public void setPartPaths(List<String> partPaths)
    {
        this.partPaths = partPaths;
    }
}
