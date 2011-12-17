package se.troed.plugin.Courier;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Enderman;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * A Postman is a friendly Enderman, tirelessly carrying around our mail
 *
 * One will be spawned for each Player that will receive mail
 */
public class Postman {

    private Enderman enderman;
    private World world;
    private Courier plugin;
    private ItemStack letter;
    private UUID uuid;
    boolean scheduledForRemoval;

    public Postman(Enderman e, Courier plug) {
        enderman = e;
        world = e.getWorld();
        plugin = plug;
        uuid = e.getUniqueId();
        // possible to create postmen without letters, why?
    }

    public void setLetter(ItemStack l) {
        letter = l;
    }

    public ItemStack getLetter() {
        return letter;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void remove() {
        enderman.remove();
    }

    public boolean scheduledForRemoval() {
        return scheduledForRemoval;
    }

    /**
     * Called when either mail has been delivered or someone is attacking the postman
     */
    public void quickDespawn() {
        plugin.scheduleDespawnPostman();
        scheduledForRemoval = true;
    }

/*    public boolean updateLoverStatus() {
        boolean ret = false;
        if (!world.getLivingEntities().contains(enderman)) {
            // sheep not found in this world anymore, disregard
            plugin.getCConfig().clog(Level.FINE, "Sheep gone from this world");
            // we seem to end up here when players quit (or maybe when there's only one player online that quits?)
            // fix by catching Player.QUIT and resetting the color there
        } else {
            // really, this should ALWAYS return the player we were initialized with, or null
            LivingEntity e = enderman.getTarget();
            Player p = null;
            if(e instanceof Player) { // maybe sheep can have other targets (other plugins etc)
                p = (Player)e;
            }
            if(p != null) {
                if(!p.isOnline()) {
                    // lover no longer online
                    plugin.getCConfig().clog(Level.FINE, "Lover no longer online - return to old color");
                } else {
                    // this sheep is already in love with someone
                    // is it successful? time to drop it? do something here
                    plugin.getCConfig().clog(Level.FINE, "Loved sheep looking for action");

                    // only need to do this if we're outside of normal target-affected distance
                    // whatever that is

                    Location tloc = lookAt(enderman.getLocation(), p.getLocation());
                    Location newLoc = move(tloc, new Vector(0,0,1));
                    if(newLoc.getBlock().getType() != Material.AIR) {
                      // VERY naive approach for testing
                      // seems not to work in (some?) downslopes
                      // would be cool to do fancy pathfinding instead
                      newLoc.add(0,1,0);
                    }
                    // seems in some cases sheep get hurt by teleporting into (?) eachother/the player
                    enderman.teleport(newLoc);

                    ret = true; // keep it up
                }
            } else {
 //               enderman.setTarget(player);
                ret = true;
            }
        }

        return ret;
    }

    // http://forums.bukkit.org/threads/lookat-and-move-functions.26768/#post-505801
    public static Location lookAt(Location loc, Location lookat) {
        //Clone the loc to prevent applied changes to the input loc
        loc = loc.clone();

        // Values of change in distance (make it relative)
        double dx = lookat.getX() - loc.getX();
        double dy = lookat.getY() - loc.getY();
        double dz = lookat.getZ() - loc.getZ();

        // Set yaw
        if (dx != 0) {
            // Set yaw start value based on dx
            if (dx < 0) {
                loc.setYaw((float) (1.5 * Math.PI));
            } else {
                loc.setYaw((float) (0.5 * Math.PI));
            }
            loc.setYaw((float) loc.getYaw() - (float) Math.atan(dz / dx));
        } else if (dz < 0) {
            loc.setYaw((float) Math.PI);
        }

        // Get the distance from dx/dz
        double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));

        // Set pitch
        loc.setPitch((float) -Math.atan(dy / dxz));

        // Set values, convert to degrees (invert the yaw since Bukkit uses a different yaw dimension format)
        loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI);
        loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);

        return loc;
    }

    // http://forums.bukkit.org/threads/lookat-and-move-functions.26768/#post-505801
    public static Location move(Location loc, Vector offset) {
        // Convert rotation to radians
        float ryaw = -loc.getYaw() / 180f * (float) Math.PI;
        float rpitch = loc.getPitch() / 180f * (float) Math.PI;

        //Conversions found by (a lot of) testing
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        z -= offset.getX() * Math.sin(ryaw);
        z += offset.getY() * Math.cos(ryaw) * Math.sin(rpitch);
        z += offset.getZ() * Math.cos(ryaw) * Math.cos(rpitch);
        x += offset.getX() * Math.cos(ryaw);
        x += offset.getY() * Math.sin(rpitch) * Math.sin(ryaw);
        x += offset.getZ() * Math.sin(ryaw) * Math.cos(rpitch);
        y += offset.getY() * Math.cos(rpitch);
        y -= offset.getZ() * Math.sin(rpitch);

        return new Location(loc.getWorld(), x, y, z, loc.getYaw(), loc.getPitch());
    }*/
}