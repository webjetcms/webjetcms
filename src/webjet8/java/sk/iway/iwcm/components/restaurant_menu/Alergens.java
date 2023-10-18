package sk.iway.iwcm.components.restaurant_menu;

public class Alergens{
	
    int number;
    String name;
    
    public Alergens(int n, String s)
    {
    	number=n;
    	name=s;
    }
        
    public int getNumber()
    {
    	return number;
    }
    
    public String getName()
    {
    	return name;
    }
}