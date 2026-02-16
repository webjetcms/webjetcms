package sk.iway.iwcm.components.seo;

public class Density
{
	private String keyWord;
	private int h1Count;
	private int h2Count;
	private int h3Count;
	private int strong;
	private int italics;
	private int link;
	private int alltogether;

	public Density(String keyWord)
	{
		super();
		this.keyWord = keyWord;
		this.h1Count = 0;
		this.h2Count = 0;
		this.h3Count = 0;
		this.strong = 0;
		this.italics = 0;
		this.link = 0;
		this.alltogether = 0;
	}

	public String getKeyWord()
	{
		return keyWord;
	}

	public int getH1Count()
	{
		return h1Count;
	}

	public int getH2Count()
	{
		return h2Count;
	}

	public int getH3Count()
	{
		return h3Count;
	}
	
	public int getStrong()
	{
		return strong;
	}
	
	public int getItalics()
	{
		return italics;
	}
	
	public int getLink()
	{
		return link;
	}
	
	public int getAllTogether()
	{
		return alltogether;
	}
	
	public void incrementH1(int i){
		h1Count += i;
	}
	
	public void incrementH2(int i){
		h2Count += i;
	}
	
	public void incrementH3(int i){
		this.h3Count += i;
	}
	
	public void incrementStrong(int i){
		this.strong += i;
	}
	
	public void incrementItalics(int i){
		this.italics += i;
	}
	
	public void incrementLink(int i){
		this.link += i;
	}
	
	public void incrementAlltogether(int i){
		this.alltogether += i;
	}
}
