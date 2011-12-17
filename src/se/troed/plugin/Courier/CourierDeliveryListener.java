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
            plugin.getCConfig().clog(Level.FINE, "Delivered letter to " + e.getPlayer().getName() + " with id " + e.getMapId());
            plugin.getCourierdb().delivered(e.getPlayer().getName(), e.getMapId());
        }
    }

    @Override
    public void onCustomEvent(Event e) {
        if(e instanceof CourierDeliveryEvent) {
            onCourierDeliveryEvent((CourierDeliveryEvent) e);
        }
    }

}
