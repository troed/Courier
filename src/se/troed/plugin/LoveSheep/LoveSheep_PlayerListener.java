package se.troed.plugin.LoveSheep;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

public class LoveSheep_PlayerListener extends PlayerListener {
    private final LoveSheep plugin;

    public LoveSheep_PlayerListener(LoveSheep instance) {
        plugin = instance;
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLSConfig().lslog(Level.FINE, event.getPlayer().getDisplayName() + " has left the building");
        plugin.loverGone(event.getPlayer());
    }
}
