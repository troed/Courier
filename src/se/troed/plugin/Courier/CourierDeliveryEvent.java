package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

class CourierDeliveryEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    public static final String COURIER_DELIVERED = "COURIER_DELIVERED";
    public static final String COURIER_READ = "COURIER_READ";
    private boolean cancelled;
    private final Player player;
    private final int id;

    public CourierDeliveryEvent(String event, Player p, int id) {
        super(event);
        player = p;
        this.id = id;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
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

    public int getId() {
        return id;
    }
}
