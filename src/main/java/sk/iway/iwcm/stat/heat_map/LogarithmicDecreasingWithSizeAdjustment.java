package sk.iway.iwcm.stat.heat_map;

/**
 * Adjusts the circle diameter and radial fade using the total number of clicks represented in the image.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 8.6.2010 12:57:37
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class LogarithmicDecreasingWithSizeAdjustment implements GeneratorSizeAdjustment
{
	
	private final HeatMapGenerator generator;

	public LogarithmicDecreasingWithSizeAdjustment(HeatMapGenerator heatMapGenerator)
	{
		this.generator = heatMapGenerator;
	}

	@Override
	public int calculateCircleSize()
	{
		int size = (int) (80.0 - 10.0* Math.log(generator.totalClicks) / Math.log(15.0));
		if (size < 30) size = 30;
		return size;
	}
	@Override
	public float calculateMaximumAlphaForOneCircle()
	{
		if (generator.totalClicks < 100)
			return 1.0f;
		else if (generator.totalClicks < 500)
			return 0.6f;
		return (float) (1.0 / Math.log(generator.totalClicks) * Math.log(15.0));
	}
}
