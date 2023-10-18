package sk.iway.iwcm.search;

/**
 * navratovy objekt pre hladanie
 */
public class SearchResult
{
	private String label;
	private String link;
	private String text;
	private String type;
	private String location;
	private String date;
	private int docId;

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getLink()
	{
		return link;
	}

	public void setLink(String link)
	{
		this.link = link;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getLocation()
	{
		return location;
	}

	public void setLocation(String location)
	{
		this.location = location;
	}

	public String getDate()
	{
		return date;
	}

	public void setDate(String date)
	{
		this.date = date;
	}

	public int getDocId()
	{
		return docId;
	}

	public void setDocId(int docId)
	{
		this.docId = docId;
	}
}
