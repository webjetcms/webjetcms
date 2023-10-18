package sk.iway.iwcm.stat.heat_map;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;

/**
 *  HeatMapJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 26.5.2010 16:17:10
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class HeatMapJUnit
{

	@BeforeAll
	public static void eraseDB()
	{
		Constants.setServletContext(new MockServletContext("nieco"){
			@Override
			public String getRealPath(String path)
			{
				return new File(path).getAbsolutePath();
			}
		});
	//	new SimpleQuery().execute("DELETE FROM heat_map");
	}

	/*@Test
	public void blabal(){
		try
		{
			IwcmFile out = new IwcmFile("/Users/marosurbanec/Desktop/nieco.png");
			int docId = 103;
			Calendar weekBefore = Calendar.getInstance();
			weekBefore.add(Calendar.DAY_OF_MONTH, -1);
			long from = weekBefore.getTimeInMillis();
			long now = System.currentTimeMillis();

			//HEAT MAP JE AKTUALNE OFF
			HeatMapDB.createHeatMap(out, docId, from, now);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}*/

	/*
	@Test
	public void insertRandomClicks()
	{
		Random random = new Random();
		int centerX = 512;
		int centerY = 384;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("INSERT INTO heat_map(x, y, document_id, day_of_month) VALUES(?,?,?,?)");

			for (int i = 0; i < 5000000 ; i++)
			{
				int x = centerX + (random.nextInt(1024) - 512);
				int y = centerY + (random.nextInt(768) - 384);
				int doc_id = random.nextInt(500);
				int day = random.nextInt(31);
				ps.setInt(1, x);
				ps.setInt(2, y);
				ps.setInt(3, doc_id);
				ps.setInt(4, day);
				ps.execute();
				if (i % 50000 == 0)
					System.out.println(i);
			}

			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.err);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		for (int i = 0; i < 100 ; i++)
		{
			int x = centerX + (random.nextInt(300) - 150);
			int y = centerY + (random.nextInt(300) - 150);
			new SimpleQuery().execute("INSERT INTO heat_map(x,y) VALUES(?,?)", x, y);
		}
	}*/
	/*
	@Test
	public void generateImage()
	{
		List<DynaBean> values = DB.getDynaList("SELECT * FROM heat_map");
		BufferedImage monochromeImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(1024, 768, Transparency.TRANSLUCENT);

		BufferedImage gradientImage = createGradientImage();
		LookupTable colorTable = createColorLookupTable(gradientImage, .35f);
		LookupOp colorOp = new LookupOp(colorTable, null);

		Graphics2D graphics = (Graphics2D)monochromeImage.getGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, 1024, 768);

		BufferedImage circle = createFadedCircleImage(50);
		graphics.setComposite(BlendComposite.Multiply.derive(0.75f));

		for (DynaBean point : values)
		{
			double x = ((Integer)point.get("x")).doubleValue();
			double y = ((Integer)point.get("y")).doubleValue();
			//System.out.println("Exporting circle to: "+x+", "+y);

			//RadialGradientPaint circle = new RadialGradientPaint(
			//			(float)x, (float)y, 15.0f, new float[]{0.0f, 1.0f}, new Color[]{new Color(0.3f, 0.0f, 0.0f, 0.2f), new Color(0.0f, 0.3f, 0.0f, 0.0f)});
			graphics.drawImage(circle, null, (int)x - 25, (int)y - 25);
			//graphics.fillRect(0, 0, 300, 300);
		}
		BufferedImage heatMap = colorOp.filter(monochromeImage, null);

		try
		{
			ImageIO.write(monochromeImage, "png", new File("/Users/marosurbanec/Desktop/heat_map_monochrome.png"));
			ImageIO.write(heatMap, "png", new File("/Users/marosurbanec/Desktop/heat_map_.png"));
			ImageIO.write(gradientImage, "png", new File("/Users/marosurbanec/Desktop/heat_map_gradient.png"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private BufferedImage createGradientImage()
	{
		Color trasnparentCyan = new Color(Color.CYAN.getRed(), Color.CYAN.getGreen(), Color.CYAN.getBlue(), 0x60);
		Color[] colors = new Color[]{Color.WHITE, Color.RED, Color.YELLOW, Color.GREEN.darker(),
					trasnparentCyan, new Color(0.0f, 0.0f, 1.0f, 0.3f), new Color(0.0f, 0.0f, 0.2f, 0.1f)};
		float[] fractions = new float[colors.length];
		float step = 1f / colors.length;

		for (int i = 0; i < colors.length; ++i) {
			fractions[i] = i * step;
		}

		Dimension size = new Dimension(256, 10);
		LinearGradientPaint gradient = new LinearGradientPaint(
					0, 0, size.width, 1, fractions, colors,
					MultipleGradientPaint.CycleMethod.REPEAT);

		BufferedImage gradientImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(256, 10, Transparency.TRANSLUCENT);
		Graphics2D g = gradientImage.createGraphics();

		g.setPaint(gradient);
		g.fillRect(0, 0, size.width, size.height);
		g.dispose();
		return gradientImage;
	}

	public void blenders()
	{
		for (BlendComposite.BlendingMode mode : BlendComposite.BlendingMode.values())
		{
			try
			{
				//generateImage(mode);
			}
			catch (Exception e)
			{
			}
		}
	}

	public static LookupTable createColorLookupTable(BufferedImage im, float alpha) {
		int tableSize = 256;
		Raster imageRaster = im.getData();
		double sampleStep = 1D * im.getWidth() / tableSize; // Sample pixels evenly
		byte[][] colorTable = new byte[4][tableSize];
		int[] pixel = new int[1]; // Sample pixel
		Color c;

		for (int i = 0; i < tableSize; ++i) {
			imageRaster.getDataElements((int) (i * sampleStep), 0, pixel);

			c = new Color(pixel[0]);

			colorTable[0][i] = (byte) c.getRed();
			colorTable[1][i] = (byte) c.getGreen();
			colorTable[2][i] = (byte) c.getBlue();
			colorTable[3][i] = (byte) (alpha * 0xff);
			if (c.getBlue() == 0xff && c.getRed() == 0 && c.getGreen() == 0)
			{
				colorTable[3][i] = (byte) (0);
			}
		}

		LookupTable lookupTable = new ByteLookupTable(0, colorTable);

		return lookupTable;
	}

	public static BufferedImage createFadedCircleImage(int size) {
      BufferedImage im = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(size, size, BufferedImage.TRANSLUCENT);
      float radius = size / 2f;

      RadialGradientPaint gradient = new RadialGradientPaint(
          radius, radius, radius, new float[] { 0f, 1f }, new Color[] {
              Color.BLACK, new Color(0xffffffff, true) });

      Graphics2D g = (Graphics2D) im.getGraphics();

      g.setPaint(gradient);
      g.fillRect(0, 0, size, size);

      g.dispose();

      return im;
  }*/
}
