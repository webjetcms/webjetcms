package sk.iway.iwcm.components.reservation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import javax.imageio.ImageIO;

import sk.iway.iwcm.Tools;

/**
 * ImageUserDay - do outputstreamu posle obrazok s kalendarom
 *@Title        magma schodzky ImageUserDay
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       Roman Hrivik (roman@hrivik.com)
 *@version      $Revision: 1.1 $
 *@created      Utorok, 2002, marec 12
 *@modified     $Date: 2010/02/17 09:38:06 $
 */

public class ImageUserDay
{

   /**
    *  pocet minut na jeden bod na vysku
    */
   private static final int MINUTE_INTERVAL = 5;

   private static final int HOUR_HEIGHT = 39;

   /**
    *  Constructor for the ImageUserDay object
    */
   public ImageUserDay()
   {
      setWidth(200);
      setHeight(20);
      setMyHourPixelWidth(6);
      setLineBar(0);
      setRecount(1.0);
      setImgDate(new Date());
      setBackgroundColor(new Color(250, 250, 250));
      setCalendarFillColor(new Color(217, 17, 17));
      setHoursColor(new Color(200, 200, 200));
      setLineBarColor(Color.red);
      setStopLineBarColor(Color.blue);
      setBoundaryLineColor(new Color(150, 150, 150));
   }

   private Graphics2D graphics;
   private int width;
   private int height;
   private int lineBar;
   private int myHourPixelWidth;
   private double recount;
   private java.text.SimpleDateFormat formatter;
   private java.util.Date imgDate;
   private java.awt.Color backgroundColor;
   private java.awt.Color calendarFillColor;
   private java.awt.Color hoursColor;
   private java.awt.Color lineBarColor;
   private java.awt.Color boundaryLineColor;
   private String startTime;
   private int stopLineBar = 0;
   private String stopTime;
   private java.awt.Color stopLineBarColor;

   /**
    *  Description of the Method
    *
    *@param  x0         Description of the Parameter
    *@param  retHeight  Description of the Parameter
    *@param  color      Description of the Parameter
    */
   private void drawRectangleBar(int x0, int retHeight, Color color)
   {
      graphics.setColor(color);
      graphics.fillRect(x0, 1, retHeight, height - 2);
   }

   /**
    *  Description of the Method
    *
    *@param  x      Description of the Parameter
    *@param  color  Description of the Parameter
    */
   private void drawLineBar(int x, Color color)
   {
      graphics.setColor(color);
      graphics.fillRect(x, 1, 2, height - 2);
      //graphics.draw(new Line2D.Double(x,1,x,height-2)); //|
      //graphics.draw(new Line2D.Double(x+1,1,x+1,height-2)); //|
   }

   /**
    *  Description of the Method
    *
    *@param  color  Description of the Parameter
    */
   private void drawHours(Color color)
   {
      graphics.setColor(color);
      for (int i = 1; i < 24; i++)
      {
         int pos = i * myHourPixelWidth;
         graphics.drawLine(pos, 0, pos, height);
      }
   }

   private int getArrYPos(Date datum)
   {
   	Calendar cal = Calendar.getInstance();
      //preved cas na minuty
      cal.setTime(datum);
      int yPos = 0;
      int hours = cal.get(Calendar.HOUR_OF_DAY);
      //minuta v danom dni
      int minutes = cal.get(Calendar.MINUTE) + (hours * 60);

      //System.out.println("Calendar:getArrYPos hours=" + hours + " min=" + cal.get(Calendar.MINUTE) + " minutes=" + minutes);

      //zaokruhli na intervaly
      yPos = (minutes / MINUTE_INTERVAL);
      //yPos = yPos * MINUTE_INTERVAL;
      return (yPos);
   }

   /**
    *  Description of the Method
    *
    *@param  stream           Description of the Parameter
    *@return                  Description of the Return Value
    *@exception  IOException  Description of the Exception
    */
   public String createImageFromReservation(OutputStream stream, int reservationId) throws IOException
   {
      stream.flush();

      setRecount(((double) getMyHourPixelWidth() / (double) HOUR_HEIGHT));
      setWidth(myHourPixelWidth * 24);

      BufferedImage bi = new BufferedImage(getWidth(),
            getHeight(),
            BufferedImage.TYPE_INT_RGB);

      graphics = bi.createGraphics();
      //basic background
      graphics.setColor(getBackgroundColor());
      graphics.fillRect(0, 0, getWidth(), getHeight());


      ReservationBean rb = ReservationManager.getReservationById(reservationId);
      if(rb != null && rb.getReservationId() > 0)
      {
         int startPos = getArrYPos(rb.getDateFrom());
         int endPos = getArrYPos(rb.getDateTo());
         int height = endPos - startPos; //NOSONAR
         if (height < 1) height = 1;
         float tmp1 = (float) ((height * MINUTE_INTERVAL) / 60.00);
         int pixelHeight = (int) (tmp1 * HOUR_HEIGHT);

         int pixelYPos = (int) ((getArrYPos(rb.getDateFrom()) * MINUTE_INTERVAL / 60.00) * HOUR_HEIGHT);

         drawRectangleBar((int) (pixelYPos * recount), (int) (pixelHeight * recount), getCalendarFillColor());
      }

      drawHours(getHoursColor());

      if (stopLineBar > 0)
      {
         drawLineBar(stopLineBar, getStopLineBarColor());
      }

      if (lineBar > 0)
      {
         drawLineBar(lineBar, getLineBarColor());
      }

      graphics.setColor(getBoundaryLineColor());
      graphics.drawRect(0, 0, getWidth() - 1, getHeight() - 1);


      ImageIO.write(bi, "png", stream);

      stream.flush();

      return ("image/png");
   }

   /**
    *  Description of the Method
    *
    *@param  hour  Description of the Parameter
    *@param  min   Description of the Parameter
    *@return       Description of the Return Value
    */
   private int countLineBar(int hour, int min)
   {
      return ((int) ((((double) hour) * ((double) getMyHourPixelWidth())) + ((((double) getMyHourPixelWidth()) / ((double) 60)) * (min))));
   }

   public void setBoundaryLineColor(java.awt.Color boundaryLineColor)
   {
      this.boundaryLineColor = boundaryLineColor;
   }

   public java.awt.Color getBoundaryLineColor()
   {
      return boundaryLineColor;
   }

   public void setHoursColor(java.awt.Color hoursColor)
   {
       this.hoursColor = hoursColor;
   }

   public java.awt.Color getHoursColor()
   {
      return hoursColor;
   }

    public void setImgDate(java.util.Date imgDate)
    {
       this.imgDate = imgDate;
    }

    public void setImgDate(String strDate)
    {
       if (Tools.isNotEmpty(strDate))
       {
          try
          {
             this.imgDate = (formatter.parse(strDate));
          }
          catch (Exception ex)
          {
             this.imgDate = new Date();
          }
       }
       else
       {
          this.imgDate = new Date();
       }
    }

    public java.util.Date getImgDate()
    {
       return imgDate;
    }

   public void setStartTime(String startTime)
   {
      this.startTime = startTime;

      //try to count linebar position
      int hour = 0;
      int min = 0;
      java.util.StringTokenizer token = new java.util.StringTokenizer(startTime, ":");
      //get hour
      if (token.hasMoreTokens())
      {
         try
         {
            hour = Integer.parseInt(token.nextToken());
         }
         catch (Exception ex)
         {
            hour = 0;
         }
      }
      //get min
      if (token.hasMoreTokens())
      {
         try
         {
            min = Integer.parseInt(token.nextToken());
         }
         catch (Exception ex)
         {
            min = 0;
         }
      }

      // count linebar
      setLineBar(countLineBar(hour, min));
   }

   public String getStartTime()
   {
      return startTime;
   }

      /**
    *  Sets the stopTime attribute of the ImageUserDay object
    *
    *@param  stopTime  The new stopTime value
    */
    public void setStopTime(String stopTime)
    {
       this.stopTime = stopTime;

       //try to count linebar position
       int hour = 0;
       int min = 0;
       java.util.StringTokenizer token = new java.util.StringTokenizer(stopTime, ":");
       //get hour
       if (token.hasMoreTokens())
       {
          try
          {
             hour = Integer.parseInt(token.nextToken());
          }
          catch (Exception ex)
          {
             hour = 0;
          }
       }
       //get min
       if (token.hasMoreTokens())
       {
          try
          {
             min = Integer.parseInt(token.nextToken());
          }
          catch (Exception ex)
          {
             min = 0;
          }
       }

       // count linebar
       setStopLineBar(countLineBar(hour, min));

    }

    public String getStopTime()
    {
       return stopTime;
    }

   public int getWidth() {
      return width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public int getHeight() {
      return height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public int getLineBar() {
      return lineBar;
   }

   public void setLineBar(int lineBar) {
      this.lineBar = lineBar;
   }

   public int getMyHourPixelWidth() {
      return myHourPixelWidth;
   }

   public void setMyHourPixelWidth(int myHourPixelWidth) {
      this.myHourPixelWidth = myHourPixelWidth;
   }

   public double getRecount() {
      return recount;
   }

   public void setRecount(double recount) {
      this.recount = recount;
   }

   public java.awt.Color getBackgroundColor() {
      return backgroundColor;
   }

   public void setBackgroundColor(java.awt.Color backgroundColor) {
      this.backgroundColor = backgroundColor;
   }

   public java.awt.Color getCalendarFillColor() {
      return calendarFillColor;
   }

   public void setCalendarFillColor(java.awt.Color calendarFillColor) {
      this.calendarFillColor = calendarFillColor;
   }

   public java.awt.Color getLineBarColor() {
      return lineBarColor;
   }

   public void setLineBarColor(java.awt.Color lineBarColor) {
      this.lineBarColor = lineBarColor;
   }

   public int getStopLineBar() {
      return stopLineBar;
   }

   public void setStopLineBar(int stopLineBar) {
      this.stopLineBar = stopLineBar;
   }

   public java.awt.Color getStopLineBarColor() {
      return stopLineBarColor;
   }

   public void setStopLineBarColor(java.awt.Color stopLineBarColor) {
      this.stopLineBarColor = stopLineBarColor;
   }


}
