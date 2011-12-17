package se.troed.plugin.Courier;

import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.map.MapView;

import java.util.logging.Level;

public class CourierServerListener extends ServerListener {
    private final Courier plugin;

    public CourierServerListener(Courier instance) {
        plugin = instance;
    }

    // only called when the map is _created_? if so I can never attach my own renderer after server restart???
    // trying to solve this with playeritemheldevent instead - it's also "late" enough to pick up the real
    // X and Z
    public void onMapInitialize(MapInitializeEvent e) {
        MapView map = e.getMap();
        plugin.getCConfig().clog(Level.FINE, "Map " + map.getId() + " initialized.");
    }
}
