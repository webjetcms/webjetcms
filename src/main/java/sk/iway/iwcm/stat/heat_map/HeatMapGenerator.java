package sk.iway.iwcm.stat.heat_map;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.RGBImageFilter;
import java.awt.image.Raster;
import java.awt.image.ShortLookupTable;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmOutputStream;

/**
 *  HeatMapGenerator.java
 *  
 *  
 *  	Generates a heat map image into a given file, created from data from given clicks.
 *  	Called from {@link HeatMapDB}.
 *  
 *   Creating the image comes in 3 phases:
 *   	1. Create a monochrome black and white image. Black and white is chosen so that multiple circles do NOT override each other. Instead, they add
 *   		up in intensity 
 *   	2. Create a gradient coloring table that maps shades of black and white into an appropriate color
 *   	3. Use gradient transformation filter to create the colored image
 *   	4. Transform black background to transparent black
 *   
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 31.5.2010 14:22:02
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class HeatMapGenerator
{
	private IwcmFile outFile;
	List<Click> clicks;
	private int xLimit;
	private int yLimit;
	private GeneratorSizeAdjustment sizeAdjustment;
	
	public HeatMapGenerator(List<Click> clicks, IwcmFile output)
	{
		if (clicks.size() == 0)
			throw new NoRecordException();
		outFile = output;
		this.clicks = clicks; 
		this.sizeAdjustment = new LogarithmicDecreasingWithSizeAdjustment(this);
	}

	
	public void generate() throws IOException
	{
		BufferedImage heatMap = createImageFromClicks();
		sk.iway.iwcm.Logger.println(HeatMapGenerator.class, "About to write heat map to "+outFile.getAbsolutePath());
		outFile.getParentFile().mkdirs();
		ImageIO.write(heatMap, "png", new IwcmOutputStream(outFile));
	}


	private BufferedImage createImageFromClicks()
	{
		// Jeeff decided the image should be generated using ALL the clicks, not by a statistical sample
		//if (clicks.size() > UPPER_CLICK_COUNT)
		//	pickRandomOnes();
	
		inferOffset();
		return generateImage();
	}

	
	private void inferOffset()
	{
		xLimit = yLimit = 0;
		
		for (Click click : clicks)
		{
			if (click.x > xLimit)
				xLimit = click.x;
			if (click.y > yLimit)
				yLimit = click.y;
		}
	}
	
	
	public BufferedImage generateImage()
	{
		//adjust circle size to the number of clicks. THe bigger the number, the lower the circles radius
		int CIRCLE_SIZE = sizeAdjustment.calculateCircleSize();
		xLimit += CIRCLE_SIZE / 2;
		yLimit += CIRCLE_SIZE / 2;
		
		sk.iway.iwcm.Logger.println(HeatMapGenerator.class, "About to generate image from "+clicks.size()+" clicks");
		sk.iway.iwcm.Logger.println(HeatMapGenerator.class, String.format("Size: [%d, %d]", xLimit, yLimit));
		BufferedImage monochromeImage = new BufferedImage(xLimit, yLimit, BufferedImage.TRANSLUCENT);
		
		
		BufferedImage gradientImage = createGradientImage();
		float ALPHA = .35f;
		LookupTable colorTable = createColorLookupTable(gradientImage, ALPHA);
		LookupOp colorOp = new LookupOp(colorTable, null);
		
		Graphics2D graphics = (Graphics2D)monochromeImage.getGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, xLimit, yLimit);
		BufferedImage circle = createFadedCircleImage(CIRCLE_SIZE);
		//this causes multiple circles to combine with each other, instead of overriding
		graphics.setComposite(BlendComposite.Multiply.derive(0.75f));
		
		for (Click point : clicks)
		{
			double x = ((Integer)point.x).doubleValue();
			double y = ((Integer)point.y).doubleValue();

			int xCoordinate = (int)x - CIRCLE_SIZE/2;
			int yCoordinate = (int)y - CIRCLE_SIZE/2;
			Logger.debug(HeatMapGenerator.class, "drawing circle: x="+x+" y="+y+" size="+CIRCLE_SIZE);
			graphics.drawImage(circle, null, xCoordinate, yCoordinate);
		}
		
		sk.iway.iwcm.Logger.println(HeatMapGenerator.class, "Crayoning image");
		BufferedImage heatMap = colorOp.filter(monochromeImage, null);
		heatMap = makeBlackTransparent(heatMap);
		
		return heatMap;
	}

	private BufferedImage makeBlackTransparent(BufferedImage heatMap)
	{
		sk.iway.iwcm.Logger.println(HeatMapGenerator.class, "Transforming background to alpha");
		ImageFilter filter = new RGBImageFilter() {
			@Override
			public final int filterRGB(int x, int y, int rgb) {
				Color original = new Color(rgb, true);
				if (original.getRed() < 10 && original.getBlue() < 10 && original.getGreen() < 10) {
					// Mark the alpha bits as zero - transparent
					return 0x00FFFFFF & rgb;
				} else {
					// nothing to do
					return rgb;
				}
			}
		};

		ImageProducer ip = new FilteredImageSource(heatMap.getSource(), filter);
      return imageToBufferedImage(Toolkit.getDefaultToolkit().createImage(ip));
	}
	
	 private static BufferedImage imageToBufferedImage(Image image) {
       BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
       Graphics2D g2 = bufferedImage.createGraphics();
       g2.drawImage(image, 0, 0, null);
       g2.dispose();

       return bufferedImage;
   }


	private BufferedImage createGradientImage()
	{
		Color trasnparentCyan = new Color(Color.CYAN.getRed(), Color.CYAN.getGreen(), Color.CYAN.getBlue(), Color.CYAN.getAlpha());
		Color[] colors = new Color[]{Color.RED.darker(), Color.ORANGE, Color.YELLOW, Color.GREEN.darker(), 
					trasnparentCyan, Color.BLUE, new Color(0.0f, 0.0f, 0.2f, 0.1f)};
		float[] fractions = new float[colors.length];
		float step = 1f / colors.length;

		for (int i = 0; i < colors.length; ++i) {
			fractions[i] = i * step;
			Logger.debug(HeatMapGenerator.class, "fractions[i]="+fractions[i]);
		}

		Dimension size = new Dimension(256, 10);
		LinearGradientPaint gradient = new LinearGradientPaint(
					0, 0, size.width, 1, fractions, colors,
					MultipleGradientPaint.CycleMethod.REPEAT);

		BufferedImage gradientImage = new BufferedImage(256, 10, BufferedImage.TRANSLUCENT);
		Graphics2D g = gradientImage.createGraphics();

		g.setPaint(gradient);
		g.fillRect(0, 0, size.width, size.height);
		g.dispose();
		Logger.debug(HeatMapGenerator.class, "Heat map's gradient image ready");
		
		return gradientImage;
	}
	
	private LookupTable createColorLookupTable(BufferedImage im, float alpha)
	{
		int tableSize = 256;
		Raster imageRaster = im.getData();
		double sampleStep = 1D * im.getWidth() / tableSize; // Sample pixels
																				// evenly
		short[][] colorTable = new short[4][tableSize];
		int[] pixel = new int[1]; // Sample pixel
		Color c;
		for (int i = 0; i < tableSize; ++i)
		{
			imageRaster.getDataElements((int) (i * sampleStep), 0, pixel);
			c = new Color(pixel[0]);
			colorTable[0][i] = (short) c.getRed();
			colorTable[1][i] = (short) c.getGreen();
			colorTable[2][i] = (short) c.getBlue();
			colorTable[3][i] = (short) (0xff);
		}
		LookupTable lookupTable = new ShortLookupTable(0, colorTable);
		Logger.debug(HeatMapGenerator.class, "Prepared heat maps' coloring lookup table");
		return lookupTable;
	}
	
	private BufferedImage createFadedCircleImage(int size)
	{
      BufferedImage im = new BufferedImage(size, size, BufferedImage.TRANSLUCENT); 
      float radius = size / 2f;
      
      //this limit will adjust fading to the number of clicks got as input
      float fadeToLimit = sizeAdjustment.calculateMaximumAlphaForOneCircle();
//      fadeToLimit = 1.0f;
      sk.iway.iwcm.Logger.println(HeatMapGenerator.class, "Circle size: "+size);
      sk.iway.iwcm.Logger.println(HeatMapGenerator.class, "Circle fading: "+fadeToLimit);
      RadialGradientPaint gradient = new RadialGradientPaint(
          radius, radius, radius, new float[] { 0f, fadeToLimit},
          new Color[]{Color.BLACK, new Color(1.0f,1.0f,1.0f,1.0f) });

      Graphics2D g = (Graphics2D) im.getGraphics();

      g.setPaint(gradient);
      g.fillRect(0, 0, size, size);

      g.dispose();
      Logger.debug(HeatMapGenerator.class, "Created circle image for heat map");
      
      return im;
  }
}