package sk.iway.iwcm.components.news;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.i18n.Prop;

public class NewsContextMenuItems
{
	
	
	public static List<NewsContextMenuItem> getVelocityProperties()
	{
		List<NewsContextMenuItem> items = new ArrayList<>();
		
		items.add(new NewsContextMenuItem("If Else", 	"Podmienka #If #Else", 	"#if($foo == $bar) it's true! #{else} it's not! #end"));
		items.add(new NewsContextMenuItem("Foreach", 	"Cyklus #Foreach", 				"<table> \n #foreach( $doc in $news ) \n <tr><td>$foreach.count</td><td>$doc.title</td></tr> \n #end \n </table>"));		
		
		return items;
	}
	
	public static List<NewsContextMenuItem> getDocDetailsProperties()
	{
		List<NewsContextMenuItem> items = new ArrayList<>();
		Map<String,String> itemsProp =  Prop.getInstance().getTextStartingWith("components.menu.contextMenuItem.doc");
		
		for (Map.Entry<String, String> entry : itemsProp.entrySet()) {			
	        String[] values = entry.getValue().split("\\|", -1);
	        if(values.length==3){
	        	items.add(new NewsContextMenuItem(values[0], values[1], values[2]));
	        }
		}
	
		return items;
	}
	
	public static List<NewsContextMenuItem> getGroupDetailsProperties()
	{
		List<NewsContextMenuItem> items = new ArrayList<>();		
		Map<String,String> itemsProp =  Prop.getInstance().getTextStartingWith("components.menu.contextMenuItem.group");
		
		for (Map.Entry<String, String> entry : itemsProp.entrySet()) {			
	        String[] values = entry.getValue().split("\\|", -1);
	        if(values.length==3){
	        	items.add(new NewsContextMenuItem(values[0], values[1], values[2]));
	        }
		}
		
		return items;
	}
	
	public static List<NewsContextMenuItem> getPagingProperties()
	{
		List<NewsContextMenuItem> items = new ArrayList<>();		
		Map<String,String> itemsProp =  Prop.getInstance().getTextStartingWith("components.menu.contextMenuItem.paging");
		
		for (Map.Entry<String, String> entry : itemsProp.entrySet()) {			
	        String[] values = entry.getValue().split("\\|", -1);
	        if(values.length==3){
	        	items.add(new NewsContextMenuItem(values[0], values[1], values[2]));
	        }
		}
		
		return items;
	}
}
