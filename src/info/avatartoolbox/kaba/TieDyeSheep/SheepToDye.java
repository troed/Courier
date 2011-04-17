package info.avatartoolbox.kaba.TieDyeSheep;

import java.util.Random;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;
import org.bukkit.DyeColor;
import org.bukkit.World;

public class SheepToDye {

	private Sheep mySheep;
	private long myStartTime;
	private World myWorld;
	
	private static long myBuffer = 50;
	
    public static final DyeColor dyeColors[] = {
        DyeColor.WHITE,
        DyeColor.ORANGE,
        DyeColor.MAGENTA,
        DyeColor.LIGHT_BLUE,
        DyeColor.YELLOW,
        DyeColor.LIME,
        DyeColor.PINK,
        DyeColor.GRAY,
        DyeColor.SILVER,
        DyeColor.CYAN,
        DyeColor.PURPLE,
        DyeColor.BLUE,
        DyeColor.BROWN,
        DyeColor.GREEN,
        DyeColor.RED,
        DyeColor.BLACK
        };
    
	public SheepToDye( Sheep s, World w ){
		mySheep = s;
		myStartTime = w.getTime();
		myWorld = w;
	}
	
	public void Dye(SheepColorPercentages p)
	{
		Random r = new Random();
		//mySheep.setColor(dyeColors[r.nextInt(dyeColors.length)]);
		//System.out.println("found the sheep");
		
		mySheep.setColor(p.PickColor(r.nextFloat()));
	}
	public boolean TryToDye( SheepColorPercentages p )
	{
		Entity e = (Entity)mySheep;
		if ( myWorld.getEntities().contains(e) == false ){
			return true;
		}
		long ctime = myWorld.getTime();
		if( ctime >= myStartTime){
			if ( myStartTime + myBuffer > ctime ){
				Dye(p);
				return true;
			}
			else {
				return false;
			}
		}
		else{
			if ( myStartTime - 24000 + myBuffer > ctime ){
				Dye(p);
				return true;
			}
			else {
				return false;
			}
		}
	}
}
