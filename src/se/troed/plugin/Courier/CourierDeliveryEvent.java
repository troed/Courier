package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

class CourierDeliveryEvent extends Event implements Cancellable {
    public static final String COURIER_DELIVERED = "COURIER_DELIVERED";
    public static final String COURIER_READ = "COURIER_READ";
    private boolean cancelled;
    private final Player player;
    private final short mapId;

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
