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
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class LoveSheep extends JavaPlugin {

    private final LoveSheep_EntityListener entityListener = new LoveSheep_EntityListener(this);
    private final LoveSheep_PlayerListener playerListener = new LoveSheep_PlayerListener(this);
    private LoveSheepConfig config = null;
    private LinkedBlockingQueue<InfatuatedSheep> flock = new LinkedBlockingQueue<InfatuatedSheep>();
    private Runnable runnable = null;
    private int taskId = -1;

    // runs through the list, keeping only sheep still in love
    private void fallInLoveRunner() {
        InfatuatedSheep s = null;
        Iterator iter = flock.iterator();
        while(iter.hasNext()) {
            s = (InfatuatedSheep)iter.next();
            if (!s.updateLoverStatus()) {
                iter.remove(); // sheep fell out of love
                config.lslog(Level.FINE, "Sheep no longer in love");
            }
        }
        if(flock.isEmpty()) {
            stopTask();
        }
    }

    // ok to be called multiple times
    private void startTask() {
        if(taskId >= 0) {
            return;
        }
        if(runnable == null) {
            runnable = new Runnable() {
                    public void run() {
                        fallInLoveRunner();
                    }
                };
        }
        config.lslog(Level.FINE, "Task start");
        // first time after 250ms, then every 1s
        taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, runnable, 5, 20);
        if(taskId < 0) {
            config.lslog(Level.WARNING, "Task scheduling failed");
        }
    }

    private void stopTask() {
        if(taskId < 0) {
            return;
        }
        config.lslog(Level.FINE, "Task stop");
        getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
    //        runnable = null;
    }

    public void onDisable() {

        config.lslog(Level.FINE, this.getDescription().getName() + " is now disabled.");
        stopTask();
    }

    public void onEnable() {
        loadConfig();

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);

        PluginDescriptionFile pdfFile = this.getDescription();
        config.lslog(Level.INFO, pdfFile.getName() + " version v" + pdfFile.getVersion() + " is enabled!");
      }

    // in preparation for plugin config dynamic reloading
    public void loadConfig() {
        getConfig().options().copyDefaults(true);

        config = new LoveSheepConfig(this);
    }

    public LoveSheepConfig getLSConfig() {
        return config;
    }

    // linear search through our flock ..
    public Integer loverCount(Player p) {
        Integer noSheep = 0;
        Iterator iter = flock.iterator();
        while(iter.hasNext()) {
            if( ((InfatuatedSheep)iter.next()).lover() == p) {
                noSheep++;
            }
        }
        if(noSheep > 0) {
            config.lslog(Level.FINE, p.getDisplayName() + " owns " + noSheep + " sheep");
        }
        return noSheep;
    }

    // linear search through our flock ..
    public void loverGone(Player p) {
        Iterator iter = flock.iterator();
        while(iter.hasNext()) {
            InfatuatedSheep s = (InfatuatedSheep)iter.next();
            if( s.lover() == p) {
                s.oldColor();
            }
        }
    }

    public void fallInLove(Sheep s, Player p) {
        InfatuatedSheep sheep = new InfatuatedSheep(s, p, this);
        try {
            flock.add(sheep); // our scheduled task will take care of the rest
            startTask();
        } catch (IllegalStateException ex) {
            return;
        }
    }
}
