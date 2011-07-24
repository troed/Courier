package se.troed.plugin.LoveSheep;

//import org.bukkit.Server;

import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class LoveSheep extends JavaPlugin {

    private final LoveSheep_EntityListener entityListener = new LoveSheep_EntityListener(this);
    private LoveSheepConfig config = null;
    private LinkedBlockingQueue<InfatuatedSheep> flock = new LinkedBlockingQueue<InfatuatedSheep>();

    private void generateDefaultConfig() {
        System.out.println(this.getDescription().getName() + " is generating a default config file.");

        PluginDescriptionFile pdfFile = this.getDescription();
        Configuration config = this.getConfiguration();

        // move to LoveSheepConfig
        config.setProperty("LoveSheep;", pdfFile.getVersion());
        config.setProperty("distance", 60);
        config.setProperty("maxLove", 4);
        config.setProperty("bigamyChance", 0.5);
        config.setProperty("sheepColor", DyeColor.PINK.getData()); // see http://www.minecraftwiki.net/wiki/Wool for color ids

        config.save();
    }

    private void readConfig() {
        Configuration config = this.getConfiguration();
        config.load();
        if (config.getString("LoveSheep;") == null) {
            generateDefaultConfig();
        }
        updatedConfig();

    }
    // runs through the list, keeping only sheep still in love
    private void fallInLoveRunner() {
        InfatuatedSheep s = null;
        Iterator iter = flock.iterator();
        while(iter.hasNext()) {
            s = (InfatuatedSheep)iter.next();
            if (!s.loverStatus()) {
                iter.remove(); // sheep fell out of love
                System.out.println("Sheep not in love");
            }
        }
    }

    public void onDisable() {
        // NOTE: All registered events are automatically unregistered when a plugin is disabled

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
//        System.out.println(this.getDescription().getName() + " is now disabled.");
    }

    public void onEnable() {
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Normal, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled!");

        readConfig();
    }

    public void updatedConfig() {
        config = new LoveSheepConfig(getConfiguration());
    }

    public LoveSheepConfig getConfig() {
        return config;
    }

    // linear search through our flock ..
    public Integer ownership(Player p) {
        Integer noSheep = 0;
        Iterator iter = flock.iterator();
        while(iter.hasNext()) {
            // can you really compare players like this?
            if( ((InfatuatedSheep)iter.next()).owner() == p) {
                noSheep++;
            }
        }
        return noSheep;
    }

    public void fallInLove(Sheep s, Player p) {
        InfatuatedSheep sheep = new InfatuatedSheep(s, p, this);
        try {
            flock.add(sheep);
        } catch (IllegalStateException ex) {
            // don't really care.
            return;
        }

        // this whole runnable/delay stuff is to be able to set the sheep color
        // (thanks to TieDyeSheep)
        // start the clock     20 = a second
        getServer().getScheduler().scheduleSyncDelayedTask(this,
                new Runnable() {
                    public void run() {
                        fallInLoveRunner();
                    }
                }, 5);
    }

}
