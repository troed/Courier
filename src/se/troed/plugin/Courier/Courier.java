package se.troed.plugin.Courier;

//import org.bukkit.Server;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import sun.awt.image.PNGImageDecoder;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class Courier extends JavaPlugin {

    private final CourierEntityListener entityListener = new CourierEntityListener(this);
    private final CourierPlayerListener playerListener = new CourierPlayerListener(this);
    private final CourierCommands courierCommands = new CourierCommands(this);
    private CourierConfig config = null;
    private Runnable runnable = null;
    private int taskId = -1;
    private Postman postman; // temp

    public void addPostman(Postman p) {
        // currently setPostman ..
        postman = p;
    }

    public Postman getPostman() {
        return postman;
    }

    public void scheduleDespawnPostman() {
        startTask();
    }

    private void despawnPostman() {
        config.clog(Level.FINE, "Removing postman");
        if(postman != null) {
            postman.remove();
        }
        postman = null;
    }

    // ok to be called multiple times
    private void startTask() {
        if(taskId >= 0) {
            return;
        }
        if(runnable == null) {
            runnable = new Runnable() {
                    public void run() {
                        despawnPostman();
                    }
                };
        }
        config.clog(Level.FINE, "Task start");
        // in ticks. one tick = 50ms
        taskId = getServer().getScheduler().scheduleSyncDelayedTask(this, runnable, 60);
        if(taskId < 0) {
            config.clog(Level.WARNING, "Task scheduling failed");
        }
    }

    private void stopTask() {
        if(taskId < 0) {
            return;
        }
        config.clog(Level.FINE, "Task stop");
        getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
    //        runnable = null;
    }

    public void onDisable() {

        config.clog(Level.FINE, this.getDescription().getName() + " is now disabled.");
        stopTask();
        despawnPostman();
    }

    public void onEnable() {
        this.loadConfig();
        saveConfig();

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENDERMAN_PICKUP, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, playerListener, Priority.Normal, this);

//        pm.registerEvent(Event.Type.SERVER_COMMAND, courierCommands, Priority.Normal, this);
        // don't like hardcoding the strings like this, fix
        getCommand("postman").setExecutor(courierCommands);

        PluginDescriptionFile pdfFile = this.getDescription();
        config.clog(Level.INFO, pdfFile.getName() + " version v" + pdfFile.getVersion() + " is enabled!");
      }

    // in preparation for plugin config dynamic reloading
    public void loadConfig() {
        getConfig().options().copyDefaults(true);

        config = new CourierConfig(this);
    }

    public CourierConfig getCConfig() {
        return config;
    }

}
