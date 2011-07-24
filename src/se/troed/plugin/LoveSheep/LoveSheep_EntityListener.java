package se.troed.plugin.LoveSheep;

//import java.util.Random;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityListener;


public class LoveSheep_EntityListener extends EntityListener {
    private final LoveSheep plugin;
    public LinkedBlockingQueue<InfatuatedSheep> flock = new LinkedBlockingQueue<InfatuatedSheep>();
//    private Random random = new Random();
//    private SheepColorPercentages colorpicker = null;
//    private int mySheepPercentage = 100;

    public LoveSheep_EntityListener(LoveSheep instance) {
        plugin = instance;
    }

    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getCreatureType() == CreatureType.SHEEP) {
            /*
             Check if player "nearby" (most effective way to do?)
             If so, "fall in love" = turn pink and start following
             */

            // should really move a lot of logic here to minimize the number of new
            // Runnables we create? (those are only needed for the actual color dying)

//            if(flock.size() < somesanevalue)

            Sheep sheep = (Sheep) (e.getEntity());
            InfatuatedSheep s = new InfatuatedSheep(sheep, e.getLocation().getWorld());
            try {
                flock.add(s);
            } catch (IllegalStateException ex) {
                // don't really care.
                return;
            }

            // start the clock     20 = a second
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
                    new Runnable() {
                        public void run() {
                            fallInLove();
                        }
                    }, 5);


        }
    }

    // runs through the list, keeping only sheep in love
    public void fallInLove() {
        InfatuatedSheep s = flock.peek();

        while (s != null) {
            if (s.fellInLove() == false) {
                s = flock.poll(); // remove heart broken [that is, a normal] sheep
            }
            s = null;
            s = flock.peek();
        }
    }

    public void updatedConfig() {
//        colorpicker = new SheepColorPercentages(plugin.getConfiguration());
    }

}
