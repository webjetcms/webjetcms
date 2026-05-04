package sk.iway.iwcm.forum;

/**
 *  ForumSortBy.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.2.2013 16:15:45
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public enum ForumSortBy {
	
	LastPost("stat_last_post"),
	QuestionDate("question_date");
	
	private final String columnName;
	
	public String getColumnName(){
		return columnName;
	}
	
	ForumSortBy(String columnName){
		this.columnName = columnName;
	}
	
	public static ForumSortBy fromString(String text) {
	    if (text != null) {
	      for (ForumSortBy sort : ForumSortBy.values()) {
	        if (text.equalsIgnoreCase(sort.columnName)) {
	          return sort;
	        }
	      }
	    }
	    return null;
	  }
	
}
