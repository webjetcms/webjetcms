package sk.iway.iwcm.form;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 *  Export udajov formularu do excelu
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Pondelok, 2002, júl 1
 *@modified     $Date: 2003/11/03 17:28:20 $
 */
public class XLSServlet extends HttpServlet
{
	private static final long serialVersionUID = 2042587682339189238L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doExport(request, response); //NOSONAR
	}

	/**
    *  Description of the Method
    *
    *@param  request               Description of the Parameter
    *@param  response              Description of the Parameter
    *@exception  ServletException  Description of the Exception
    *@exception  IOException       Description of the Exception
    */
	@Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
   	doExport(request, response); //NOSONAR
   }

   public static void doExport(HttpServletRequest request, HttpServletResponse response) throws IOException
   {
   	Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
		if (user != null && user.isAdmin())
		{
			//ok
		}
		else
		{
			return;
		}

		Prop prop = Prop.getInstance(Constants.getServletContext(), request);

      //zisti ci mas data
      @SuppressWarnings("unchecked")
      List<FormDetails> data = (List<FormDetails>) request.getAttribute("data");
      if (data != null)
      {
         String formName = DB.internationalToEnglish(request.getParameter("formname"));
         SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy");
         String fileName = DocTools.removeChars(formName)+"-"+sdf.format(new Date())+".xls";
         response.setContentType("application/vnd.ms-excel");
         response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet("Sheet 1");

         HSSFFont font = wb.createFont();
         font.setFontHeightInPoints((short) 10);
         font.setFontName("Arial");

         HSSFCellStyle style = wb.createCellStyle();
         style.setFont(font);
         style.setBorderLeft(BorderStyle.THIN);
         style.setBorderRight(BorderStyle.THIN);
         style.setBorderTop(BorderStyle.THIN);
         style.setBorderBottom(BorderStyle.THIN);
         style.setVerticalAlignment(VerticalAlignment.TOP);
         style.setWrapText(true);

         HSSFFont fontHeader = wb.createFont();
         fontHeader.setFontHeightInPoints((short) 10);
         fontHeader.setFontName("Arial");
         fontHeader.setBold(true);

         HSSFCellStyle styleHeader = wb.createCellStyle();
         styleHeader.setFont(fontHeader);
         styleHeader.setAlignment(HorizontalAlignment.CENTER);
         styleHeader.setBorderLeft(BorderStyle.THIN);
         styleHeader.setBorderRight(BorderStyle.THIN);
         styleHeader.setBorderTop(BorderStyle.THIN);
         styleHeader.setBorderBottom(BorderStyle.THIN);
         styleHeader.setFillForegroundColor(HSSFColorPredefined.GREY_25_PERCENT.getIndex());
         styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

         HSSFCellStyle styleHeaderUser = wb.createCellStyle();
         styleHeaderUser.setFont(fontHeader);
         styleHeaderUser.setAlignment(HorizontalAlignment.CENTER);
         styleHeaderUser.setBorderLeft(BorderStyle.THIN);
         styleHeaderUser.setBorderRight(BorderStyle.THIN);
         styleHeaderUser.setBorderTop(BorderStyle.THIN);
         styleHeaderUser.setBorderBottom(BorderStyle.THIN);
         styleHeaderUser.setFillForegroundColor(HSSFColorPredefined.GREY_40_PERCENT.getIndex());
         styleHeaderUser.setFillPattern(FillPatternType.SOLID_FOREGROUND);

         HSSFCellStyle styleDate = wb.createCellStyle();
         styleDate.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm"));
         styleDate.setFont(font);
         styleDate.setBorderLeft(BorderStyle.THIN);
         styleDate.setBorderRight(BorderStyle.THIN);
         styleDate.setBorderTop(BorderStyle.THIN);
         styleDate.setBorderBottom(BorderStyle.THIN);
         styleDate.setVerticalAlignment(VerticalAlignment.TOP);
         styleDate.setAlignment(HorizontalAlignment.CENTER);

         HSSFRow row;
         HSSFCell cell;

         int row_index = 0;
         int cell_index = 0;
         String s_row;
         String s_tmp;
         StringTokenizer st;
         sheet.setDefaultColumnWidth(30);
         int text_width = 10;
         String text;

         String field;
         int i;
         Integer iInteger;
         int rowLength = -1;
         Date d;

         UserDetails formUser;

         for (FormDetails formDetails : data)
         {
            row = sheet.createRow(row_index);
            cell_index = 0x0;
            s_row = formDetails.getData();

            st = new StringTokenizer(s_row, "|");

            //datum
            cell = row.createCell(cell_index++);

            //text_width = fontMetrics.stringWidth("Dátum vytvorenia");
            //sirka je ako 1/256 pismena
            text_width = 20 * 256;
            if (text_width > sheet.getColumnWidth(0))
            {
               sheet.setColumnWidth(0, text_width);
            }

            if (formDetails.isHeader())
            {
            	//datum vytvorenia

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(prop.getText("formslist.createDate"));
               cell.setCellStyle(styleHeader);

               // poznamka
               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(prop.getText("formslist.note"));
               cell.setCellStyle(styleHeader);
            }
            else
            {

            	d = new Date(formDetails.getCreateDate());
               cell.setCellValue(Tools.formatDate(d) + " " + Tools.formatTime(d)); //new Date(formDetails.getCreateDate()));
               //cell.setCellValue(new Date());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               cell.setCellValue(formDetails.getNote());
               cell.setCellStyle(style);
            }

            if (rowLength == -1)
            {
	            rowLength = st.countTokens();
	      		if (formDetails.getColNames()!=null && formDetails.getColNames().size()>1)
	      		{
	      			rowLength = formDetails.getColNames().size();
	      		}
            }

      		//vytvor bunky
      		for (i=0; i<rowLength; i++)
      		{
      			cell = row.createCell(i+2);

      			//cell.setEncoding(HSSFCell.ENCODING_UTF_16);
      		}

            while (st.hasMoreTokens())
            {
               text = st.nextToken();

      			field = null;
      			try
      			{
      				i = text.indexOf('~');
      				if (i>0)
      				{
      					field = text.substring(0, i);
      					if (i<text.length())
      					{
      						text = text.substring(i+1);
      					}
      				}
      				else if (i==0)
      				{
      					if (text.length() == 1)
      					{
      						text = "";
      					}
      					else
      					{
      						text = text.substring(1);
      					}
      				}
      			}
      			catch (Exception ex)
      			{

      			}

               if (formDetails.isHeader())
               {
                  text = FormDB.getValueNoDash(text);
               }
               else
               {
                   text = Tools.unescapeHtmlEntities(CryptoFactory.decrypt(text));
               }
               text = text.trim();

               s_tmp = text;
               s_tmp = s_tmp.replace('\r', '\n');

               if (formDetails.isHeader())
               {
               	if (cell_index < (rowLength+2))
               	{
	      				cell = row.getCell(cell_index);
	                  cell.setCellValue(s_tmp);
	                  cell.setCellStyle(styleHeader);
               	}
               }
               else
               {

	               i = -1;
	      			if (formDetails.getColNames()!=null && field!=null && field.length()>0)
	      			{
	      				iInteger = formDetails.getColNames().get(field);
	      				if (iInteger!=null)
	      				{
	      					i = iInteger.intValue() + 1;
	      				}
	      			}
	      			if (i>0 && i<=rowLength)
	      			{
	      				cell = row.getCell(i+1);
	      				if (cell == null)
	      				{
	      					Logger.println(XLSServlet.class,"cell je null, vytvaram");
	      					cell = row.createCell(i);
	      				}
	                  //
	                  //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	                  if (s_tmp.length() < 1)
	                  {
	                     cell.setCellValue(" ");
	                  }
	                  else
	                  {
	                     cell.setCellValue(s_tmp);
	                  }

	                  cell.setCellStyle(style);

	                  //text_width = fontMetrics.stringWidth(s_tmp);
	                  text_width = s_tmp.length() + 2;
	                  if (text_width > 50)
	                  {
	                     text_width = 50;
	                  }
	                  if (formDetails.isHeader())
	                  {
	                     text_width = text_width * 335;
	                  }
	                  else
	                  {
	                     text_width = text_width * 260;
	                  }
	                  if (text_width > sheet.getColumnWidth(cell_index))
	                  {
	                     sheet.setColumnWidth(cell_index, (short) text_width);
	                  }
	      			}
               }



               cell_index++;
            }

            cell_index = (short)(rowLength + 2);
            if (formDetails.isHeader())
            {
            	//          user id, login, email, first name, last name
	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue("userId");
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.firstName"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.lastName"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.login"));
	            cell.setCellStyle(styleHeaderUser);

               cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.company"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.address"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.city"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.ZIP"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.country"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.email"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.phone"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.fieldA"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.fieldB"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.fieldC"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.fieldD"));
	            cell.setCellStyle(styleHeaderUser);

	            cell = row.createCell(cell_index++);

	            //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
	            cell.setCellValue(prop.getText("user.fieldE"));
	            cell.setCellStyle(styleHeaderUser);
            }
            else
            {
            	formUser = formDetails.getUserDetails();
               if (formUser == null)
               {
               	formUser = new UserDetails();
               }

               // user id, login, email, first name, last name
               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               if (formUser.getUserId()>0)
               {
               	cell.setCellValue(formUser.getUserId());
               }
               else
               {
               	cell.setCellValue("");
               }
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getFirstName());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getLastName());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getLogin());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getCompany());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getAdress());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getCity());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getPSC());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getCountry());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getEmail());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getPhone());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getFieldA());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getFieldB());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getFieldC());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getFieldD());
               cell.setCellStyle(style);

               cell = row.createCell(cell_index++);

               //cell.setEncoding(HSSFCell.ENCODING_UTF_16);
               cell.setCellValue(formUser.getFieldE());
               cell.setCellStyle(style);
            }


            row_index++;
         }

         try {
            ServletOutputStream out = response.getOutputStream();
            wb.write(out);
            wb.close();
            out.flush();
            out.close();
         } catch (IOException e) {
            Logger.error(XLSServlet.class, e);
         }
      }
      else
      {
         Logger.println(XLSServlet.class,"data is null");
      }
   }

}
