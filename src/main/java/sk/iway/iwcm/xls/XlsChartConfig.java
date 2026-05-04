package sk.iway.iwcm.xls;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Tools;

/**
 *  XlsChartConfig.java - trieda s konfiguraciou grafu
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 7.10.2005 13:58:41
 *@modified     $Date: 2005/12/06 16:38:51 $
 */
public class XlsChartConfig
{
	int width = 550;
	int height = 300;
	
	String title = "";
	String xAxisLabel = "";
	String yAxisLabel = "";
	
	String graphMode = "line";
	int sheetIndex = 0;
	
	String xlsUrl = null;
	String templateUrl = null;
	
	int titleRowIndex = -1;
	int startRowIndex = -1;
	int endRowIndex = -1;
	//poradie stlpca, kde sa zacinaju data (zvycajne 0 bunka)
	int startColumn = 0;
	
	int pieColumnLegend = -1;
	int pieColumnData = -1;
	String pieLabelGenerator = "{0} - {2}";
	
	private int maxLegendCharLength = -1;
	
	String cacheImagePath = "/images/cache/xlscart/";
	
	public XlsChartConfig()
	{
		
	}
	
	public XlsChartConfig(HttpServletRequest request)
	{
		width = Tools.getIntValue(request.getParameter("width"), 550);
		height = Tools.getIntValue(request.getParameter("height"), 300);
		
		title = "";
		xAxisLabel = "";
		yAxisLabel = "";
		
		graphMode = request.getParameter("graphMode");
		sheetIndex = Tools.getIntValue(request.getParameter("sheetIndex"), 0);
		
		xlsUrl = request.getParameter("xlsUrl");
		
		titleRowIndex = Tools.getIntValue(request.getParameter("titleRow"), -1);
		startRowIndex = Tools.getIntValue(request.getParameter("startRow"), -1);
		endRowIndex = Tools.getIntValue(request.getParameter("endRow"), -1);
	}

	public int getEndRowIndex()
	{
		return endRowIndex;
	}

	public void setEndRowIndex(int endRowIndex)
	{
		this.endRowIndex = endRowIndex;
	}

	public String getXlsUrl()
	{
		return xlsUrl;
	}

	public void setXlsUrl(String fileUrl)
	{
		this.xlsUrl = fileUrl;
	}

	public String getGraphMode()
	{
		return graphMode;
	}

	public void setGraphMode(String graphMode)
	{
		this.graphMode = graphMode;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public int getSheetIndex()
	{
		return sheetIndex;
	}

	public void setSheetIndex(int sheetIndex)
	{
		this.sheetIndex = sheetIndex;
	}

	public int getStartRowIndex()
	{
		return startRowIndex;
	}

	public void setStartRowIndex(int startRowIndex)
	{
		this.startRowIndex = startRowIndex;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public int getTitleRowIndex()
	{
		if (titleRowIndex < 0 && startRowIndex > 0)
		{
			titleRowIndex = startRowIndex - 1;
		}
		return titleRowIndex;
	}

	public void setTitleRowIndex(int titleRowIndex)
	{
		this.titleRowIndex = titleRowIndex;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public String getXAxisLabel()
	{
		return xAxisLabel;
	}

	public void setXAxisLabel(String axisLabel)
	{
		xAxisLabel = axisLabel;
	}

	public String getYAxisLabel()
	{
		return yAxisLabel;
	}

	public void setYAxisLabel(String axisLabel)
	{
		yAxisLabel = axisLabel;
	}

	public String getTemplateUrl()
	{
		return templateUrl;
	}

	public void setTemplateUrl(String templateUrl)
	{
		this.templateUrl = templateUrl;
	}

	public String getCacheImagePath()
	{
		return cacheImagePath;
	}

	public void setCacheImagePath(String cacheImagePath)
	{
		this.cacheImagePath = cacheImagePath;
	}

	public int getPieColumnData()
	{
		return pieColumnData;
	}

	public void setPieColumnData(int pieColumnData)
	{
		this.pieColumnData = pieColumnData;
	}

	public int getPieColumnLegend()
	{
		return pieColumnLegend;
	}

	public void setPieColumnLegend(int pieColumnLegend)
	{
		this.pieColumnLegend = pieColumnLegend;
	}

	public int getStartColumn()
	{
		return startColumn;
	}

	public void setStartColumn(int startColumn)
	{
		this.startColumn = startColumn;
	}

	public String getPieLabelGenerator()
	{
		return pieLabelGenerator;
	}

	public void setPieLabelGenerator(String pieLabelGenerator)
	{
		this.pieLabelGenerator = pieLabelGenerator;
	}

	public int getMaxLegendCharLength()
	{
		return maxLegendCharLength;
	}

	public void setMaxLegendCharLength(int maxLegendCharLength)
	{
		this.maxLegendCharLength = maxLegendCharLength;
	}
}
