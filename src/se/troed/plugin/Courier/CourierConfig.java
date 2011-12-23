package se.troed.plugin.Courier;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;


public class CourierConfig {
    private static final boolean debug = false;

    private FileConfiguration config;
    private PluginDescriptionFile pdfFile;
    private Logger log;
    
    private static final String LOGPREFIX = "[Courier] ";
    // any config file _older_ than this is invalid - compatibility break
    private static final String VERSIONBREAK = "0.9.0";
    private static final String POSTMAN_QUICK_DESPAWN = "Courier.Postman.QuickDespawn";
    private static final String POSTMAN_DESPAWN = "Courier.Postman.Despawn";
    private static final String ROUTE_INITIALWAIT = "Courier.Route.InitialWait";
    private static final String ROUTE_NEXTROUTE = "Courier.Route.NextRoute";
    private static final String POSTMAN_SPAWNDISTANCE = "Courier.Postman.SpawnDistance";

    private int quickDespawnTime;
    private int despawnTime;
    private int initialWait;
    private int nextRoute;
    private int spawnDistance;

    public CourierConfig(Courier plug) {

        log = plug.getServer().getLogger();

        config = plug.getConfig();
        pdfFile = plug.getDescription();

        // verify config compatibility
        String version = config.getString(".Version" + pdfFile.getName());
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

            parts = VERSIONBREAK.split("\\.");
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
        quickDespawnTime = config.getInt(POSTMAN_QUICK_DESPAWN);
        clog(Level.FINE, POSTMAN_QUICK_DESPAWN + ": " + quickDespawnTime);
        despawnTime = config.getInt(POSTMAN_DESPAWN);
        clog(Level.FINE, POSTMAN_DESPAWN + ": " + despawnTime);
        initialWait = config.getInt(ROUTE_INITIALWAIT);
        clog(Level.FINE, ROUTE_INITIALWAIT + ": " + initialWait);
        nextRoute = config.getInt(ROUTE_NEXTROUTE);
        clog(Level.FINE, ROUTE_NEXTROUTE + ": " + nextRoute);
        spawnDistance = config.getInt(POSTMAN_SPAWNDISTANCE);
        clog(Level.FINE, POSTMAN_SPAWNDISTANCE + ": " + spawnDistance);
    }
    
    public int getQuickDespawnTime() {
        return quickDespawnTime;
    }

    public int getDespawnTime() {
        return despawnTime;
    }

    public int getInitialWait() {
        return initialWait;
    }

    public int getNextRoute() {
        return nextRoute;
    }

    public int getSpawnDistance() {
        return spawnDistance;
    }

    void clog(Level level, String message) {
        if(!debug && (level != Level.SEVERE && level != Level.WARNING && level != Level.INFO)) {
            return;
        }
        // Bukkit doesn't log CONFIG, FINE etc at least with the defaults
        if(level == Level.FINE) {
            level = Level.INFO;
        }
        log.log(level, LOGPREFIX + message);
    }
}
