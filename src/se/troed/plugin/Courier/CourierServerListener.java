package se.troed.plugin.Courier;

import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.server.ServerListener;

import java.util.logging.Level;

public class CourierServerListener extends ServerListener {
    private final Courier plugin;

    public CourierServerListener(Courier instance) {
        plugin = instance;
    }

    public void onMapInitialize(MapInitializeEvent e) {
        plugin.getCConfig().clog(Level.FINE, "Map + " + e.getMap().getId() + " initialized");
    }
}
