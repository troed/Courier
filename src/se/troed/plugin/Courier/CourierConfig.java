package se.troed.plugin.Courier;

import org.bukkit.DyeColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;


public class CourierConfig {

    private FileConfiguration config;
    private PluginDescriptionFile pdfFile;
    private Logger log;
    private static final boolean debug = true;

    private static final String logPrefix = "[Courier] ";
    // any config file _older_ than this is invalid - compatibility break
    private static final String versionBreak = "0.0.1";

    public CourierConfig(Courier plug) {

        log = plug.getServer().getLogger();

        config = plug.getConfig();
        pdfFile = plug.getDescription();

        // verify config compatibility
        String version = config.getString(pdfFile.getName());
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
            clog(Level.FINE, "Decoded version: Major: " + major + " Minor: " + minor + " Revision: " + revision);

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
            clog(Level.FINE, "Comp break: Major: " + major + " Minor: " + minor + " Revision: " + revision);

            int breakVersion = major*1000000+minor*1000+revision;

            if(existingVersion < breakVersion) {
                // config file not valid - abort plugin load
                clog(Level.SEVERE, "Config file version too old - unexpected behaviour might occur!");
            }
        }

    }

    void clog(Level level, String message) {
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
