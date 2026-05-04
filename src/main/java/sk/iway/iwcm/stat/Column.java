package sk.iway.iwcm.stat;
import java.util.Date;
/**
 * 
 *  Column.java - Bean pre ukladanie roadkovych info (hlavne pre statistiku)
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 30.11.2004 11:19:24
 *@modified     $Date: 2004/08/09 08:42:03 $
 */
public class Column
{
   private String[] column;

   private int[] intColumn;
   
   private Date[] dateColumn;
   
   private double[] doubleColumn;
   
   private boolean[] booleanColumn;
   
   private int idKey;
   private String name;
   
   public Column()
   {
   	column = new String[30];
      intColumn = new int[30];
      dateColumn = new Date[30];
      doubleColumn = new double[30];
      booleanColumn= new boolean[30];
   }
   
   /**
    * Konstruktor s nastavenim velkosti poli (ak potrebujeme setrit pamat)
    * @param strings
    * @param integers
    * @param dates
    * @param doubles
    * @param booleans
    */
   public Column(int strings, int integers, int dates, int doubles, int booleans)
   {
   	if (strings>0) column = new String[strings];
   	if (integers>0) intColumn = new int[integers];
   	if (dates>0) dateColumn = new Date[dates];
   	if (doubles>0) doubleColumn = new double[doubles];
   	if (booleans>0) booleanColumn= new boolean[booleans];
   }
   
   public String getColumn(int index)
   {
      return column[index];
   }
   
   public void setColumn(String value, int index)
   {
      this.column[index] = value;
   }
   
   public int getIntColumn(int index)
   {
      return intColumn[index];
   }
   
   public void setIntColumn(int value, int index)
   {
      this.intColumn[index] = value;
   }
   
   public Date getDateColumn(int index)
   {
      return dateColumn[index];
   }
   
   public void setDateColumn(Date value, int index)
   {
      this.dateColumn[index] = value;
   }
   
   public double getDoubleColumn(int index)
   {
      return doubleColumn[index];
   }
   
   public void setDoubleColumn(double value, int index)
   {
      this.doubleColumn[index] = value;
   }

   /**
    *  Gets the column1 attribute of the Column object
    *
    *@return    The column1 value
    */
   public String getColumn1()
   {
      return column[1];
   }

   /**
    *  Sets the column1 attribute of the Column object
    *
    *@param  column1  The new column1 value
    */
   public void setColumn1(String column1)
   {
      this.column[1] = column1;
   }

   /**
    *  Sets the column2 attribute of the Column object
    *
    *@param  column2  The new column2 value
    */
   public void setColumn2(String column2)
   {
      this.column[2] = column2;
   }

   /**
    *  Gets the column2 attribute of the Column object
    *
    *@return    The column2 value
    */
   public String getColumn2()
   {
      return column[2];
   }

   /**
    *  Sets the column3 attribute of the Column object
    *
    *@param  column3  The new column3 value
    */
   public void setColumn3(String column3)
   {
      this.column[3] = column3;
   }

   /**
    *  Gets the column3 attribute of the Column object
    *
    *@return    The column3 value
    */
   public String getColumn3()
   {
      return column[3];
   }

   /**
    *  Sets the column4 attribute of the Column object
    *
    *@param  column4  The new column4 value
    */
   public void setColumn4(String column4)
   {
      this.column[4] = column4;
   }

   /**
    *  Gets the column4 attribute of the Column object
    *
    *@return    The column4 value
    */
   public String getColumn4()
   {
      return column[4];
   }

   /**
    *  Sets the column5 attribute of the Column object
    *
    *@param  column5  The new column5 value
    */
   public void setColumn5(String column5)
   {
      this.column[5] = column5;
   }

   /**
    *  Gets the column5 attribute of the Column object
    *
    *@return    The column5 value
    */
   public String getColumn5()
   {
      return column[5];
   }

   /**
    *  Sets the column6 attribute of the Column object
    *
    *@param  column6  The new column6 value
    */
   public void setColumn6(String column6)
   {
      this.column[6] = column6;
   }

   /**
    *  Gets the column6 attribute of the Column object
    *
    *@return    The column6 value
    */
   public String getColumn6()
   {
      return column[6];
   }

   /**
    *  Sets the column7 attribute of the Column object
    *
    *@param  column7  The new column7 value
    */
   public void setColumn7(String column7)
   {
      this.column[7] = column7;
   }

   /**
    *  Gets the column7 attribute of the Column object
    *
    *@return    The column7 value
    */
   public String getColumn7()
   {
      return column[7];
   }

   /**
    *  Sets the column8 attribute of the Column object
    *
    *@param  column8  The new column8 value
    */
   public void setColumn8(String column8)
   {
      this.column[8] = column8;
   }

   /**
    *  Gets the column8 attribute of the Column object
    *
    *@return    The column8 value
    */
   public String getColumn8()
   {
      return column[8];
   }

   /**
    *  Sets the column9 attribute of the Column object
    *
    *@param  column9  The new column9 value
    */
   public void setColumn9(String column9)
   {
      this.column[9] = column9;
   }

   /**
    *  Gets the column9 attribute of the Column object
    *
    *@return    The column9 value
    */
   public String getColumn9()
   {
      return column[9];
   }

   /**
    *  Sets the column10 attribute of the Column object
    *
    *@param  column10  The new column10 value
    */
   public void setColumn10(String column10)
   {
      this.column[10] = column10;
   }

   /**
    *  Gets the column10 attribute of the Column object
    *
    *@return    The column10 value
    */
   public String getColumn10()
   {
      return column[10];
   }

   /**
    *  Sets the column11 attribute of the Column object
    *
    *@param  column11  The new column11 value
    */
   public void setColumn11(String column11)
   {
      this.column[11] = column11;
   }

   /**
    *  Gets the column11 attribute of the Column object
    *
    *@return    The column11 value
    */
   public String getColumn11()
   {
      return column[11];
   }

   /**
    *  Sets the column12 attribute of the Column object
    *
    *@param  column12  The new column12 value
    */
   public void setColumn12(String column12)
   {
      this.column[12] = column12;
   }

   /**
    *  Gets the column12 attribute of the Column object
    *
    *@return    The column12 value
    */
   public String getColumn12()
   {
      return column[12];
   }

   /**
    *  Sets the column13 attribute of the Column object
    *
    *@param  column13  The new column13 value
    */
   public void setColumn13(String column13)
   {
      this.column[13] = column13;
   }

   /**
    *  Gets the column13 attribute of the Column object
    *
    *@return    The column13 value
    */
   public String getColumn13()
   {
      return column[13];
   }

   /**
    *  Sets the column14 attribute of the Column object
    *
    *@param  column14  The new column14 value
    */
   public void setColumn14(String column14)
   {
      this.column[14] = column14;
   }

   /**
    *  Gets the column14 attribute of the Column object
    *
    *@return    The column14 value
    */
   public String getColumn14()
   {
      return column[14];
   }

   /**
    *  Sets the column15 attribute of the Column object
    *
    *@param  column15  The new column15 value
    */
   public void setColumn15(String column15)
   {
      this.column[15] = column15;
   }

   /**
    *  Gets the column15 attribute of the Column object
    *
    *@return    The column15 value
    */
   public String getColumn15()
   {
      return column[15];
   }

   /**
    *  Sets the column16 attribute of the Column object
    *
    *@param  column16  The new column16 value
    */
   public void setColumn16(String column16)
   {
      this.column[16] = column16;
   }

   /**
    *  Gets the column16 attribute of the Column object
    *
    *@return    The column16 value
    */
   public String getColumn16()
   {
      return column[16];
   }

   /**
    *  Sets the column17 attribute of the Column object
    *
    *@param  column17  The new column17 value
    */
   public void setColumn17(String column17)
   {
      this.column[17] = column17;
   }

   /**
    *  Gets the column17 attribute of the Column object
    *
    *@return    The column17 value
    */
   public String getColumn17()
   {
      return column[17];
   }

   /**
    *  Sets the column18 attribute of the Column object
    *
    *@param  column18  The new column18 value
    */
   public void setColumn18(String column18)
   {
      this.column[18] = column18;
   }

   /**
    *  Gets the column18 attribute of the Column object
    *
    *@return    The column18 value
    */
   public String getColumn18()
   {
      return column[18];
   }

   /**
    *  Sets the column19 attribute of the Column object
    *
    *@param  column19  The new column19 value
    */
   public void setColumn19(String column19)
   {
      this.column[19] = column19;
   }

   /**
    *  Gets the column19 attribute of the Column object
    *
    *@return    The column19 value
    */
   public String getColumn19()
   {
      return column[19];
   }

   /**
    *  Sets the column20 attribute of the Column object
    *
    *@param  column20  The new column20 value
    */
   public void setColumn20(String column20)
   {
      this.column[20] = column20;
   }

   /**
    *  Gets the column20 attribute of the Column object
    *
    *@return    The column20 value
    */
   public String getColumn20()
   {
      return column[20];
   }

	public int getIntColumn1()
	{
		return intColumn[1];
	}
	public void setIntColumn1(int intColumn1)
	{
		this.intColumn[1] = intColumn1;
	}
	public int getIntColumn11()
	{
		return intColumn[11];
	}
	public void setIntColumn11(int intColumn11)
	{
		this.intColumn[11] = intColumn11;
	}
	public int getIntColumn2()
	{
		return intColumn[2];
	}
	public void setIntColumn2(int intColumn2)
	{
		this.intColumn[2] = intColumn2;
	}
	public int getIntColumn3()
	{
		return intColumn[3];
	}
	public void setIntColumn3(int intColumn3)
	{
		this.intColumn[3] = intColumn3;
	}
	public int getIntColumn4()
	{
		return intColumn[4];
	}
	public void setIntColumn4(int intColumn4)
	{
		this.intColumn[4] = intColumn4;
	}
	public int getIntColumn5()
	{
		return intColumn[5];
	}
	public void setIntColumn5(int intColumn5)
	{
		this.intColumn[5] = intColumn5;
	}
	public int getIntColumn6()
	{
		return intColumn[6];
	}
	public void setIntColumn6(int intColumn6)
	{
		this.intColumn[6] = intColumn6;
	}
	public int getIntColumn7()
	{
		return intColumn[7];
	}
	public void setIntColumn7(int intColumn7)
	{
		this.intColumn[7] = intColumn7;
	}
	public int getIntColumn8()
	{
		return intColumn[8];
	}
	public void setIntColumn8(int intColumn8)
	{
		this.intColumn[8] = intColumn8;
	}
	public int getIntColumn9()
	{
		return intColumn[9];
	}
	public void setIntColumn9(int intColumn9)
	{
		this.intColumn[9] = intColumn9;
	}
	public int getIntColumn10()
	{
		return intColumn[10];
	}
	public void setIntColumn10(int intColumn10)
	{
		this.intColumn[10] = intColumn10;
	}
	public Date getDateColumn1()
	{
		return dateColumn[1];
	}
	public void setDateColumn1(Date dateColumn1)
	{
		this.dateColumn[1] = dateColumn1;
	}
	public Date getDateColumn2()
	{
		return dateColumn[2];
	}
	public void setDateColumn2(Date dateColumn2)
	{
		this.dateColumn[2] = dateColumn2;
	}
	public Date getDateColumn3()
	{
		return dateColumn[3];
	}
	public void setDateColumn3(Date dateColumn3)
	{
		this.dateColumn[3] = dateColumn3;
	}
	public Date getDateColumn4()
	{
		return dateColumn[4];
	}
	public void setDateColumn4(Date dateColumn4)
	{
		this.dateColumn[4] = dateColumn4;
	}
	public Date getDateColumn5()
	{
		return dateColumn[5];
	}
	public void setDateColumn5(Date dateColumn5)
	{
		this.dateColumn[5] = dateColumn5;
	}
	public double getDoubleColumn1()
	{
		return doubleColumn[1];
	}
	public void setDoubleColumn1(double doubleColumn1)
	{
		this.doubleColumn[1] = doubleColumn1;
	}
	public double getDoubleColumn2()
	{
		return doubleColumn[2];
	}
	public void setDoubleColumn2(double doubleColumn2)
	{
		this.doubleColumn[2] = doubleColumn2;
	}
	public double getDoubleColumn3()
	{
		return doubleColumn[3];
	}
	public void setDoubleColumn3(double doubleColumn3)
	{
		this.doubleColumn[3] = doubleColumn3;
	}
	public double getDoubleColumn4()
	{
		return doubleColumn[4];
	}
	public void setDoubleColumn4(double doubleColumn4)
	{
		this.doubleColumn[4] = doubleColumn4;
	}
	public double getDoubleColumn5()
	{
		return doubleColumn[5];
	}
	public void setDoubleColumn5(double doubleColumn5)
	{
		this.doubleColumn[5] = doubleColumn5;
	}
	public int getIdKey()
	{
		return idKey;
	}
	public void setIdKey(int idKey)
	{
		this.idKey = idKey;
	}
	/**
	 * @return Returns the booleanColumn.
	 */
	public boolean isBooleanColumn1()
	{
		return booleanColumn[0];
	}
	/**
	 * @param booleanColumn The booleanColumn to set.
	 */
	public void setBooleanColumn1(boolean booleanColumn)
	{
		this.booleanColumn[0] = booleanColumn;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}
