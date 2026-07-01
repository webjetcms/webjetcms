package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.LabelValueDetails;

public class SearchActionOutput {

	private String forward = "success";
	private List<SearchDetails> results = new ArrayList<>();
	private List<LabelValueDetails> pages;
	private String words;
	private String paramsLink;
	private Integer fromIndex;
	private Integer toIndex;
	private Integer totalResults;
	private Integer waitMinutes;
	private String next;
	private String nextHref;
	private String prev;
	private String prevHref;
	private String errorMessage;
	private boolean wrong;
	private boolean emptyRequest;
	private boolean crossHourlyLimit;
	private boolean crossTimeout;
	private boolean notFound;
	private boolean notFoundPublishStartEnd;

	public void applyToRequest(HttpServletRequest request)
	{
		request.setAttribute("words", words);
		request.setAttribute("aList", results);

		if (wrong) request.setAttribute("wrong", "true");
		if (emptyRequest) request.setAttribute("emptyrequest", "true");
		if (crossHourlyLimit) request.setAttribute("crossHourlyLimit", "true");
		if (crossTimeout) request.setAttribute("crossTimeout", "true");
		if (notFound) request.setAttribute("notfound", "true");
		if (waitMinutes != null) request.setAttribute("wait", waitMinutes);
		if (errorMessage != null) request.setAttribute("err_msg", errorMessage);
		request.setAttribute("notFoundPublishStartEnd", String.valueOf(notFoundPublishStartEnd));

		if (paramsLink != null) request.setAttribute("paramsLink", paramsLink);
		if (fromIndex != null) request.setAttribute("fromIndex", String.valueOf(fromIndex));
		if (toIndex != null) request.setAttribute("toIndex", String.valueOf(toIndex));
		if (totalResults != null) request.setAttribute("totalResults", String.valueOf(totalResults));
		if (next != null) request.setAttribute("next", next);
		if (nextHref != null) request.setAttribute("nextHref", nextHref);
		if (prev != null) request.setAttribute("prev", prev);
		if (prevHref != null) request.setAttribute("prevHref", prevHref);
		if (pages != null && pages.isEmpty() == false) request.setAttribute("pages", pages);
	}

	public String getForward()
	{
		return forward;
	}

	public void setForward(String forward)
	{
		this.forward = forward;
	}

	public List<SearchDetails> getResults()
	{
		return results;
	}

	public void setResults(List<SearchDetails> results)
	{
		this.results = results;
	}

	public List<LabelValueDetails> getPages()
	{
		return pages;
	}

	public void setPages(List<LabelValueDetails> pages)
	{
		this.pages = pages;
	}

	public String getWords()
	{
		return words;
	}

	public void setWords(String words)
	{
		this.words = words;
	}

	public String getParamsLink()
	{
		return paramsLink;
	}

	public void setParamsLink(String paramsLink)
	{
		this.paramsLink = paramsLink;
	}

	public Integer getFromIndex()
	{
		return fromIndex;
	}

	public void setFromIndex(Integer fromIndex)
	{
		this.fromIndex = fromIndex;
	}

	public Integer getToIndex()
	{
		return toIndex;
	}

	public void setToIndex(Integer toIndex)
	{
		this.toIndex = toIndex;
	}

	public Integer getTotalResults()
	{
		return totalResults;
	}

	public void setTotalResults(Integer totalResults)
	{
		this.totalResults = totalResults;
	}

	public Integer getWaitMinutes()
	{
		return waitMinutes;
	}

	public void setWaitMinutes(Integer waitMinutes)
	{
		this.waitMinutes = waitMinutes;
	}

	public String getNext()
	{
		return next;
	}

	public void setNext(String next)
	{
		this.next = next;
	}

	public String getNextHref()
	{
		return nextHref;
	}

	public void setNextHref(String nextHref)
	{
		this.nextHref = nextHref;
	}

	public String getPrev()
	{
		return prev;
	}

	public void setPrev(String prev)
	{
		this.prev = prev;
	}

	public String getPrevHref()
	{
		return prevHref;
	}

	public void setPrevHref(String prevHref)
	{
		this.prevHref = prevHref;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public boolean isWrong()
	{
		return wrong;
	}

	public void setWrong(boolean wrong)
	{
		this.wrong = wrong;
	}

	public boolean isEmptyRequest()
	{
		return emptyRequest;
	}

	public void setEmptyRequest(boolean emptyRequest)
	{
		this.emptyRequest = emptyRequest;
	}

	public boolean isCrossHourlyLimit()
	{
		return crossHourlyLimit;
	}

	public void setCrossHourlyLimit(boolean crossHourlyLimit)
	{
		this.crossHourlyLimit = crossHourlyLimit;
	}

	public boolean isCrossTimeout()
	{
		return crossTimeout;
	}

	public void setCrossTimeout(boolean crossTimeout)
	{
		this.crossTimeout = crossTimeout;
	}

	public boolean isNotFound()
	{
		return notFound;
	}

	public void setNotFound(boolean notFound)
	{
		this.notFound = notFound;
	}

	public boolean isNotFoundPublishStartEnd()
	{
		return notFoundPublishStartEnd;
	}

	public void setNotFoundPublishStartEnd(boolean notFoundPublishStartEnd)
	{
		this.notFoundPublishStartEnd = notFoundPublishStartEnd;
	}
}