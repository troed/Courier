package info.avatartoolbox.kaba.TieDyeSheep;

import org.bukkit.DyeColor;
import org.bukkit.util.config.Configuration;
import java.util.*;


public class SheepColorPercentages {
	
	// indiv percentages, not dependent on order
	EnumMap <DyeColor, Float> data = new EnumMap <DyeColor, Float>(DyeColor.class);
	
	// totaled percentages, order dependent.
	EnumMap <DyeColor, Float> balanced_data = new EnumMap <DyeColor, Float>(DyeColor.class);
	
	public SheepColorPercentages( Configuration config )
	{
		Integer white = (Integer) config.getProperty("color.WHITE");
		Integer orange = (Integer) config.getProperty("color.ORANGE");
		Integer magenta = (Integer) config.getProperty("color.MAGENTA");
		Integer light_blue = (Integer) config.getProperty("color.LIGHT_BLUE");
		Integer yellow = (Integer) config.getProperty("color.YELLOW");
		Integer lime = (Integer) config.getProperty("color.LIME");
		Integer pink = (Integer) config.getProperty("color.PINK");
		Integer gray = (Integer) config.getProperty("color.GRAY");
		Integer silver = (Integer) config.getProperty("color.SILVER");
		Integer cyan = (Integer) config.getProperty("color.CYAN");
		Integer purple = (Integer) config.getProperty("color.PURPLE");
		Integer blue = (Integer) config.getProperty("color.BLUE");
		Integer brown = (Integer) config.getProperty("color.BROWN");
		Integer green = (Integer) config.getProperty("color.GREEN");
		Integer red = (Integer) config.getProperty("color.RED");
		Integer black = (Integer) config.getProperty("color.BLACK");

		Integer total = white+orange+magenta+light_blue+
					yellow+lime+pink+gray+silver+cyan+
					purple+blue+brown+green+red+black;
		
		if ( total.intValue() == 0)
		{
			white = 1;
			total = 1;
		}
		
		data.put(DyeColor.WHITE, white.floatValue() / total.floatValue() );
		data.put(DyeColor.ORANGE, orange.floatValue() / total.floatValue() );
		data.put(DyeColor.MAGENTA, magenta.floatValue() / total.floatValue() );
		data.put(DyeColor.LIGHT_BLUE, light_blue.floatValue() / total.floatValue() );
		data.put(DyeColor.YELLOW, yellow.floatValue() / total.floatValue() );
		data.put(DyeColor.LIME, lime.floatValue() / total.floatValue() );
		data.put(DyeColor.PINK, pink.floatValue() / total.floatValue() );
		data.put(DyeColor.GRAY, gray.floatValue() / total.floatValue() );
		data.put(DyeColor.SILVER, silver.floatValue() / total.floatValue() );
		data.put(DyeColor.CYAN, cyan.floatValue() / total.floatValue() );
		data.put(DyeColor.PURPLE, purple.floatValue() / total.floatValue() );
		data.put(DyeColor.BLUE, blue.floatValue() / total.floatValue() );
		data.put(DyeColor.BROWN, brown.floatValue() / total.floatValue() );
		data.put(DyeColor.GREEN, green.floatValue() / total.floatValue() );
		data.put(DyeColor.RED, red.floatValue() / total.floatValue() );
		data.put(DyeColor.BLACK, black.floatValue() / total.floatValue() );
		
		float currentTotal = 0;
		
		for ( DyeColor color : DyeColor.values())
		{
			currentTotal += data.get(color);
			balanced_data.put( color, currentTotal);
		}

	}
	
	public DyeColor PickColor(float f)
	{
		for ( DyeColor color : DyeColor.values())
		{
			if ( f <= balanced_data.get(color))
				return color;
		}
		return DyeColor.WHITE;
	}

}
