package sk.iway.iwcm.components.news.criteria;

import java.util.LinkedList;
import java.util.List;

import sk.iway.iwcm.doc.DocDetails;

public abstract class ResultCriteria implements Criteria
{
	public abstract boolean filter(DocDetails doc);
	
	/**
	 * retazenie podmienok s OR - musi vyhovovat aspon jednemu kriteriu
	 * @param criterias
	 * @return
	 */
	public static ResultCriteria or(ResultCriteria...criterias)
	{
		return new OrCriteria(criterias);
	}
	
	/**
	 * retazenie podmienok s AND - musi vyhovovat vsetkym kriteriam
	 * @param criterias
	 * @return
	 */
	public static ResultCriteria and(ResultCriteria...criterias)
	{
		return new AndCriteria(criterias);
	}
	
	public static ResultCriteria perexNotEmpty()
	{
		return new PerexNotEmpty();
	}
	
	
	
	
	public static class OrCriteria extends ResultCriteria
	{

		private List<ResultCriteria> criterias = new LinkedList<ResultCriteria>();
		
		public OrCriteria(ResultCriteria...criterias)
		{
			if (criterias!=null)
			{
				for (ResultCriteria rc : criterias)
				{
					this.criterias.add(rc);
				}
			}
		}
		
		@Override
		public boolean filter(DocDetails doc)
		{
			for (ResultCriteria rc : criterias)
			{
				if (rc.filter(doc))
					return true;
			}
			return false;
		}
		
	}
	
	public static class AndCriteria extends ResultCriteria
	{

		private List<ResultCriteria> criterias = new LinkedList<ResultCriteria>();
		
		public AndCriteria(ResultCriteria...criterias)
		{
			if (criterias!=null)
			{
				for (ResultCriteria rc : criterias)
				{
					this.criterias.add(rc);
				}
			}
		}
		
		@Override
		public boolean filter(DocDetails doc)
		{
			for (ResultCriteria rc : criterias)
			{
				if (!rc.filter(doc))
					return false;
			}
			return true;
		}
		
	}
	
	/**
	 * Vyfiltruje len stranky ktore maju vyplneny perex 
	 */
	public static class PerexNotEmpty extends ResultCriteria
	{
		@Override
		public boolean filter(DocDetails doc)
		{
			if (doc.getHtmlData()!=null && doc.getHtmlData().length()>1)
			{
				return true;
			}
			return false;
		}
	}
}
