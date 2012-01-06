package se.troed.plugin.Courier;

import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

import java.util.logging.Level;

class CourierDeliveryListener extends CustomEventListener{
    private final Courier plugin;

    public CourierDeliveryListener(Courier instance) {
        plugin = instance;
    }

    void onCourierDeliveryEvent(CourierDeliveryEvent e) {
        if(e.getPlayer()!=null && e.getId()!=-1) {
            if(e.getEventName().equals(CourierDeliveryEvent.COURIER_DELIVERED)) {
                plugin.getCConfig().clog(Level.FINE, "Delivered letter to " + e.getPlayer().getName() + " with id " + e.getId());
                plugin.getCourierdb().setDelivered(e.getPlayer().getName(), e.getId());
            } else if(e.getEventName().equals(CourierDeliveryEvent.COURIER_READ)) {
                plugin.getCConfig().clog(Level.FINE, e.getPlayer().getName() + " has read the letter with id " + e.getId());
                plugin.getCourierdb().setRead(e.getPlayer().getName(), e.getId());
            } else {
                // dude, what?
                plugin.getCConfig().clog(Level.WARNING, "Unknown Courier event " + e.getEventName() + " received!");
            }
                
        }
    }

    @Override
    public void onCustomEvent(Event e) {
        if(e instanceof CourierDeliveryEvent) {
            onCourierDeliveryEvent((CourierDeliveryEvent) e);
        }
    }

}
