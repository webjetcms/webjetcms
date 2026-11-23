package sk;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.PerexGroupBean;

/**
 * Filters cached group, document, and perex-group lists by the current cloud domain.
 * Execution pointcuts also cover callers compiled outside this build, such as generated JSP classes.
 */
@Aspect
public class CloudFilter
{
	@Pointcut("execution(public java.util.List<sk.iway.iwcm.doc.GroupDetails> sk.iway.iwcm.doc.GroupsDB.*(..))")
	public void groupsDetailsListMethod()
	{
	}

	@Pointcut("execution(java.util.List<sk.iway.iwcm.doc.DocDetails> sk.iway.iwcm.doc.DocDB.*(..))")
	public void docDetailsListMethod()
	{
	}

	@Pointcut("execution(public java.util.List<sk.iway.iwcm.doc.PerexGroupBean> sk.iway.iwcm.doc.DocDB.*(..))")
	public void docDbPerexListMethod()
	{
	}

	@Around("docDbPerexListMethod()")
	@SuppressWarnings("unchecked")
	public List<PerexGroupBean> filterPerexGroups(ProceedingJoinPoint joinPoint) throws Throwable
	{
		List<PerexGroupBean> originalPerexGroups = (List<PerexGroupBean>) joinPoint.proceed();
		if (InitServlet.isTypeCloud())
		{
			return filterPerexByDomain(originalPerexGroups);
		}
		return originalPerexGroups;
	}

	@Around("docDetailsListMethod()")
	@SuppressWarnings("unchecked")
	public List<DocDetails> filterDocuments(ProceedingJoinPoint joinPoint) throws Throwable
	{
		List<DocDetails> originalDocuments = (List<DocDetails>) joinPoint.proceed();
		List<DocDetails> documents = new ArrayList<>(originalDocuments);
		if (InitServlet.isTypeCloud())
		{
			documents = filterByDomainDocs(documents);
		}
		return documents;
	}

	@Around("groupsDetailsListMethod()")
	@SuppressWarnings("unchecked")
	public List<GroupDetails> filterGroups(ProceedingJoinPoint joinPoint) throws Throwable
	{
		List<GroupDetails> groups = (List<GroupDetails>) joinPoint.proceed();
		if (InitServlet.isTypeCloud())
		{
			groups = filterByDomain(groups);
		}
		return groups;
	}

	private List<GroupDetails> filterByDomain(List<GroupDetails> groups)
	{
		if (groups == null) return null;

		List<GroupDetails> filtered = new ArrayList<>(groups.size());
		String domain = CloudToolsForCore.getDomainName();
		for (GroupDetails group : groups)
		{
			if (group!=null && group.getDomainName()!=null && group.getDomainName().equalsIgnoreCase(domain))
			{
				filtered.add(group);
			}
		}
		return filtered;
	}

	private List<DocDetails> filterByDomainDocs(List<DocDetails> documents)
	{
		List<DocDetails> filtered = new ArrayList<>(documents.size());
		String domain = CloudToolsForCore.getDomainName();
		for (DocDetails document : documents)
		{
			GroupDetails group = document.getGroup();
			if (group!=null && group.getDomainName()!=null && group.getDomainName().equalsIgnoreCase(domain))
			{
				filtered.add(document);
			}
		}
		return filtered;
	}

	private List<PerexGroupBean> filterPerexByDomain(List<PerexGroupBean> originalPerexGroups)
	{
		List<PerexGroupBean> filteredPerexGroups = originalPerexGroups == null
			? new ArrayList<>(0)
			: new ArrayList<>(originalPerexGroups.size());

		if (originalPerexGroups != null)
		{
			for (PerexGroupBean perexGroup : originalPerexGroups)
			{
				int[] availableGroups = perexGroup.getAvailableGroupsInt();
				if (availableGroups == null || availableGroups.length == 0) continue;

				boolean passed = true;
				boolean notEmpty = false;
				for (int groupId : availableGroups)
				{
					GroupDetails group = GroupDetails.getById(groupId);
					if (group != null)
					{
						if (group.getDomainName()==null || !group.getDomainName().equalsIgnoreCase(CloudToolsForCore.getDomainName()))
						{
							passed = false;
						}
						else
						{
							notEmpty = true;
						}
					}
				}
				if (passed && notEmpty)
				{
					filteredPerexGroups.add(perexGroup);
				}
			}
		}
		return filteredPerexGroups;
	}
}
