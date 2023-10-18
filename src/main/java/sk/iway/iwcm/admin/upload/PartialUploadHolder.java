package sk.iway.iwcm.admin.upload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PartialUploadHolder implements Serializable {
    private static final long serialVersionUID = 1L;
    private int chunks;
    private String name;
    
    private List<String> partPaths;
    
    public PartialUploadHolder(int chunks, String name)
    {
        this.chunks = chunks;
        this.name = name;
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

    public List<String> getPartPaths()
    {
        return partPaths;
    }

    public void setPartPaths(List<String> partPaths)
    {
        this.partPaths = partPaths;
    }
}