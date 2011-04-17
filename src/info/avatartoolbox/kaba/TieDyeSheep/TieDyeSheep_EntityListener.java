package info.avatartoolbox.kaba.TieDyeSheep;

//import java.util.Random;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityListener;


public class TieDyeSheep_EntityListener extends EntityListener {
    private final TieDyeSheep plugin;
    public LinkedBlockingQueue<SheepToDye> myFlock = new LinkedBlockingQueue<SheepToDye>();
    private Random random = new Random();
    private SheepColorPercentages colorpicker = null;
    private int mySheepPercentage = 100;

    public TieDyeSheep_EntityListener(TieDyeSheep instance) {
        plugin = instance;
    }
    
    public void onCreatureSpawn( CreatureSpawnEvent e ){
    	if ( e.getCreatureType() == CreatureType.SHEEP ){
    		
    		if ( random.nextInt(100) > mySheepPercentage ) return;
    		
    		Sheep sheep = (Sheep)(e.getEntity());
    		//e.getEntity().setFireTicks(500);  		
    		SheepToDye s = new SheepToDye( sheep, e.getLocation().getWorld() );
    		try{
    			myFlock.add( s );
    		}catch (IllegalStateException ex){
    			// don't really care.
    			return;
    		}
    		
    		// start the clock     20 = a second
    		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, 
    														new Runnable() { public void run() { DyeSheep(); } } ,
    														5 );


    	}
    }
    
    public void DyeSheep()
    {
    	SheepToDye s = myFlock.peek();
    	if ( s == null ) return;
    	
    	while ( s != null ){
    		if ( s.TryToDye(colorpicker) == true ){
    			s = myFlock.poll();
    			s = null;
    			s = (SheepToDye)(myFlock.peek());
    		}
    		else{
    			s = null;
    		}
    	}
    }
    
    public void updatedConfig()
    {
    	colorpicker = new SheepColorPercentages( plugin.getConfiguration())	;
    }

}
