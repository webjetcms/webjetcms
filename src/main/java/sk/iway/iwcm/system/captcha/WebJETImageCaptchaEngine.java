package sk.iway.iwcm.system.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.ImageFilter;
import java.util.List;

import com.octo.captcha.component.image.backgroundgenerator.BackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.EllipseBackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.FunkyBackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.MultipleShapeBackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.UniColorBackgroundGenerator;
import com.octo.captcha.component.image.color.ColorGenerator;
import com.octo.captcha.component.image.color.RandomRangeColorGenerator;
import com.octo.captcha.component.image.color.SingleColorGenerator;
import com.octo.captcha.component.image.deformation.ImageDeformation;
import com.octo.captcha.component.image.deformation.ImageDeformationByFilters;
import com.octo.captcha.component.image.fontgenerator.FontGenerator;
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator;
import com.octo.captcha.component.image.textpaster.DecoratedRandomTextPaster;
import com.octo.captcha.component.image.textpaster.DoubleRandomTextPaster;
import com.octo.captcha.component.image.textpaster.DoubleTextPaster;
import com.octo.captcha.component.image.textpaster.LineRandomTextPaster;
import com.octo.captcha.component.image.textpaster.NonLinearTextPaster;
import com.octo.captcha.component.image.textpaster.RandomTextPaster;
import com.octo.captcha.component.image.textpaster.TextPaster;
import com.octo.captcha.component.image.textpaster.textdecorator.BaffleTextDecorator;
import com.octo.captcha.component.image.textpaster.textdecorator.TextDecorator;
import com.octo.captcha.component.image.wordtoimage.DeformedComposedWordToImage;
import com.octo.captcha.component.image.wordtoimage.WordToImage;
import com.octo.captcha.component.word.wordgenerator.WordGenerator;
import com.octo.captcha.engine.image.ListImageCaptchaEngine;
import com.octo.captcha.image.gimpy.GimpyFactory;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 *  WebJETImageCaptchaEngine.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 26.8.2011 16:31:31
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@SuppressWarnings("deprecation")
public class WebJETImageCaptchaEngine extends ListImageCaptchaEngine
{
   @Override
	protected void buildInitialFactories()
   {
      WordGenerator wgen = CaptchaWordGeneratorFactory.getWordGenerator();
      ColorGenerator colorGen=null;
      boolean isColorSet=false;
      String fontcolor = Constants.getString("captchaFontColorRGB");
      //skusim setnut farbu pisma na konstantnu, ak by to nevyslo pouzijem povodne pouzivany randomcolorgen
      if (Tools.isNotEmpty(fontcolor) && fontcolor.indexOf(",")!=-1)
      {
    	  try
    	  {
    		  int[] rgb = Tools.getTokensInt(fontcolor, ",");
    		  Color color = new Color(rgb[0],rgb[1],rgb[2]);
    		  colorGen = new SingleColorGenerator(color);
    		  isColorSet=true;
    	  }
    	  catch (Exception ex)
    	  {
    		  //ak by to zlyhalo
    		  isColorSet=false;
    	  }
      }
      if (!isColorSet)
      {
    	  colorGen =  new RandomRangeColorGenerator( new int[] {0, 100}, new int[] {0, 100}, new int[] {0, 100});
      }



      TextPaster textPaster = null;

      if (Constants.getBoolean("captchaDictionaryEnabled"))
      {
    	  CaptchaDictionaryDB cddb = new CaptchaDictionaryDB();
    	  List<CaptchaDictionaryBean> slova = cddb.getAll();
    	  Integer minLen=Integer.MAX_VALUE;
    	  Integer maxLen=Integer.MIN_VALUE;
    	  for (CaptchaDictionaryBean slovo : slova)
    	  {
    		  if (slovo.getWord().length()<minLen)
    			  minLen = Integer.valueOf(slovo.getWord().length());
    		  if (slovo.getWord().length()>maxLen)
    			  maxLen = Integer.valueOf(slovo.getWord().length());
    	  }
    	  textPaster = new RandomTextPaster(minLen, maxLen, colorGen, true);
      }
      else
    	  textPaster = new RandomTextPaster(Integer.valueOf(Constants.getInt("captchaMinLength")), Integer.valueOf(Constants.getInt("captchaMaxLength")), colorGen, true);

      isColorSet=false;
      BackgroundGenerator backgroundGenerator=null;
      String background = Constants.getString("captchaBackgroundColorRGB");
      // skusim setnut farbu pozadia na konstantnu, ak by sa to niekde zosypalo, pouzije sa povodne pouzivany funkyback.....
      if (Tools.isNotEmpty(background) && background.indexOf(",")!=-1)
      {
    	  try
    	  {
    		  int[] rgb = Tools.getTokensInt(background, ",");
    		  Color color = new Color(rgb[0],rgb[1],rgb[2]);

    		  backgroundGenerator = new UniColorBackgroundGenerator(Integer.valueOf(200), Integer.valueOf(60), color);
    		  isColorSet=true;
    	  }
    	  catch (Exception ex)
    	  {
    		  //ak by to zlyhalo
    		  isColorSet=false;
    	  }
      }
      if (!isColorSet)
      {
      	String captchaBackgroundMode = Constants.getString("captchaBackgroundMode");
      	if ("multishape".equals(captchaBackgroundMode))
      	{
      		backgroundGenerator = new MultipleShapeBackgroundGenerator(Integer.valueOf(200), Integer.valueOf(60));
      	}
      	else if ("elipse".equals(captchaBackgroundMode))
      	{
      		backgroundGenerator = new EllipseBackgroundGenerator(Integer.valueOf(200), Integer.valueOf(60));
      	}
      	else
      	{
      		backgroundGenerator = new FunkyBackgroundGenerator(Integer.valueOf(200), Integer.valueOf(60), new RandomRangeColorGenerator( new int[] {100, 255}, new int[] {100, 255}, new int[] {100, 255}));
      	}



      }
      Font[] fontsList = new Font[] {
               new Font(Font.SANS_SERIF, Font.PLAIN, 10),
               new Font(Font.SERIF, Font.BOLD, 10)
            };

      FontGenerator fontGenerator = new RandomFontGenerator(Integer.valueOf(20), Integer.valueOf(30), fontsList);

      //WordToImage wordToImage = new ComposedWordToImage(fontGenerator, backgroundGenerator, textPaster);
      com.jhlabs.image.WaterFilter water = new com.jhlabs.image.WaterFilter();

      water.setAmplitude(3f);
      //water.setAntialias(true);
      water.setPhase(20f);
      water.setWavelength(Float.valueOf(Constants.getInt("captchaWaveSize")));

      ImageDeformation backDef = new ImageDeformationByFilters(
               new ImageFilter[]{}); //water
       ImageDeformation textDef = new ImageDeformationByFilters(
               new ImageFilter[]{});
       ImageDeformation postDef = new ImageDeformationByFilters(
               new ImageFilter[]{}); //water

       //fontGenerator = new TwistedAndShearedRandomFontGenerator(Integer.valueOf(25), Integer.valueOf(30));
       String captchaMode = Constants.getString("captchaMode");
       if ("line".equals(captchaMode))
       {
      	 textPaster = new LineRandomTextPaster(Integer.valueOf(Constants.getInt("captchaMinLength")), Integer.valueOf(Constants.getInt("captchaMaxLength")), colorGen, true, Integer.valueOf(2), colorGen);
       }
       else if ("double".equals(captchaMode))
       {
      	 textPaster = new DoubleTextPaster(Integer.valueOf(Constants.getInt("captchaMinLength")), Integer.valueOf(Constants.getInt("captchaMaxLength")), colorGen, true);
       }
       else if ("doubleRandom".equals(captchaMode))
       {
      	 textPaster = new DoubleRandomTextPaster(Integer.valueOf(Constants.getInt("captchaMinLength")), Integer.valueOf(Constants.getInt("captchaMaxLength")), colorGen, true);
       }
       else if ("decorated".equals(captchaMode))
       {
      	 textPaster = new DecoratedRandomTextPaster(Integer.valueOf(Constants.getInt("captchaMinLength")), Integer.valueOf(Constants.getInt("captchaMaxLength")), colorGen, true, new TextDecorator[]{new BaffleTextDecorator(Integer.valueOf(3), colorGen)});
       }
       else if ("baffle".equals(captchaMode))
       {
      	 //textPaster = new BaffleRandomTextPaster(Integer.valueOf(Constants.getInt("captchaMinLength")), Integer.valueOf(Constants.getInt("captchaMaxLength")), colorGen, true, 3, colorGen);
       }
       else if ("nonlinear".equals(captchaMode))
       {
      	 textPaster = new NonLinearTextPaster(Integer.valueOf(Constants.getInt("captchaMinLength")), Integer.valueOf(Constants.getInt("captchaMaxLength")), colorGen, true);
       }


      WordToImage wordToImage = new DeformedComposedWordToImage(fontGenerator, backgroundGenerator, textPaster,
               backDef,
               textDef,
               postDef);

      this.addFactory(new GimpyFactory(wgen, wordToImage));
   }
}
