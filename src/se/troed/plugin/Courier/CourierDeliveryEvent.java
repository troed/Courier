package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class CourierDeliveryEvent extends Event implements Cancellable {
    private boolean cancelled;
    private Player player;
    private short mapId;

    public CourierDeliveryEvent(String event, Player p, short id) {
        super(event);
        player = p;
        mapId = id;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
    
    public Player getPlayer() {
        return player;
    }

    public short getMapId() {
        return mapId;
    }
}
