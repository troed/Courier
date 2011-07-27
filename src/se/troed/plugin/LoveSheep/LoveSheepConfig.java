package se.troed.plugin.LoveSheep;

import org.bukkit.DyeColor;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.config.Configuration;
import sun.plugin2.main.server.Plugin;
import sun.security.util.Debug;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LoggingMXBean;


public class LoveSheepConfig {

    private Integer distance;
    private Integer maxLove;
    private Double bigamyChance;
    private Double loveChance;
    private DyeColor sheepColor;
    private Configuration config;
    private PluginDescriptionFile pdfFile;
    private Logger log;
    private static final boolean debug = false;

    private static final String logPrefix = "[LoveSheep] ";
    // any config file _older_ than this is invalid - compatibility break
    private static final String versionBreak = "0.0.5";

    private void generateDefaultConfig() {
        lslog(Level.INFO, pdfFile.getName() + " is generating a default config file.");

        config.setProperty(pdfFile.getName(), pdfFile.getVersion());
        config.setProperty("distance", 100);
        config.setProperty("maxLove", 4);
        config.setProperty("bigamyChance", 0.5);
        config.setProperty("loveChance", 0.1);
        Byte temp = DyeColor.PINK.getData();
        // see http://www.minecraftwiki.net/wiki/Wool for color ids
        config.setProperty("sheepColor", temp.intValue());

        config.save();
    }

    public LoveSheepConfig(LoveSheep plug) {

        log = plug.getServer().getLogger();

        config = plug.getConfiguration();
        pdfFile = plug.getDescription();
        config.load();
        if (config.getString(pdfFile.getName()) == null) {
            generateDefaultConfig();
        }

        // verify config compatibility
        String version = (String)config.getProperty(pdfFile.getName());
        if(version!=null) {
            int major = 0;
            int minor = 0;
            int revision = 0;

            String[] parts = version.split("\\.");
            if(parts.length > 0 && parts[0] != null) {
                major = Integer.decode(parts[0]);
            }
            if(parts.length > 1 && parts[1] != null) {
                minor = Integer.decode(parts[1]);
            }
            if(parts.length > 2 && parts[2] != null) {
                revision = Integer.decode(parts[2]);
            }
            lslog(Level.FINE, "Decoded version: Major: " + major + " Minor: " + minor + " Revision: " + revision);

            int existingVersion = major*1000000+minor*1000+revision;

            parts = versionBreak.split("\\.");
            if(parts.length > 0 && parts[0] != null) {
                major = Integer.decode(parts[0]);
            }
            if(parts.length > 1 && parts[1] != null) {
                minor = Integer.decode(parts[1]);
            }
            if(parts.length > 2 && parts[2] != null) {
                revision = Integer.decode(parts[2]);
            }
            lslog(Level.FINE, "Comp break: Major: " + major + " Minor: " + minor + " Revision: " + revision);

            int breakVersion = major*1000000+minor*1000+revision;

            if(existingVersion < breakVersion) {
                // config file not valid - abort plugin load
                lslog(Level.SEVERE, "Config file version too old - unexpected behaviour might occur!");


            }
        }
        distance = (Integer)config.getProperty("distance");
        maxLove = (Integer)config.getProperty("maxLove");
        bigamyChance = (Double)config.getProperty("bigamyChance");
        loveChance = (Double)config.getProperty("loveChance");
        Integer temp = (Integer)config.getProperty("sheepColor");
        sheepColor = DyeColor.getByData(temp.byteValue());
 //      sheepColor = DyeColor.getByData((Byte)config.getProperty("sheepColor"));

        // add code to set properties to default values if they weren't in the config
     }

    public Integer getDistance() {
        return distance;
    }

    public Integer getMaxLove() {
        return maxLove;
    }

    public Double getBigamyChance() {
        return bigamyChance;
    }

    public Double getLoveChance() {
        return loveChance;
    }

    public DyeColor getSheepColor() {
        return sheepColor;
    }

    void lslog(Level level, String message) {
        if(!debug && (level != Level.SEVERE && level != Level.WARNING && level != Level.INFO)) {
            return;
        }
        // Bukkit doesn't log CONFIG, FINE etc at least with the defaults
        if(level == Level.FINE) {
            level = Level.INFO;
        }
        log.log(level, logPrefix + message);
    }
}
