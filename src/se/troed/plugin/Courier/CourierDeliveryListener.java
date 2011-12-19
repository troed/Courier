package se.troed.plugin.Courier;

import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

import java.util.logging.Level;

public class CourierDeliveryListener extends CustomEventListener{
    private final Courier plugin;

    public CourierDeliveryListener(Courier instance) {
        plugin = instance;
    }

    public void onCourierDeliveryEvent(CourierDeliveryEvent e) {
        if(e.getPlayer()!=null && e.getMapId()!=-1) {
            if(e.getEventName().equals(CourierDeliveryEvent.COURIER_DELIVERED)) {
                plugin.getCConfig().clog(Level.FINE, "Delivered letter to " + e.getPlayer().getName() + " with id " + e.getMapId());
                plugin.getCourierdb().setDelivered(e.getPlayer().getName(), e.getMapId());
            } else if(e.getEventName().equals(CourierDeliveryEvent.COURIER_READ)) {
                plugin.getCConfig().clog(Level.FINE, e.getPlayer().getName() + " has read the letter with id " + e.getMapId());
                plugin.getCourierdb().setRead(e.getPlayer().getName(), e.getMapId());
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
