package sk.iway.iwcm.filebrowser;

public class UnusedFile
{
	private String name;
	private String virtualParent;
	long lastModified;
	long length;
	
	public UnusedFile(String name, String virtualParent, long lastModified, long length)
	{
		this.name = name;
		this.virtualParent = virtualParent;
		this.lastModified = lastModified;
		this.length = length;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getVirtualParent()
	{
		return virtualParent;
	}

	public void setVirtualParent(String virtualParent)
	{
		this.virtualParent = virtualParent;
	}

	public long getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(long lastModified)
	{
		this.lastModified = lastModified;
	}

	public long getLength()
	{
		return length;
	}

	public void setLength(long length)
	{
		this.length = length;
	}
}
