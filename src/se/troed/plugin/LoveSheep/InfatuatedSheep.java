package se.troed.plugin.LoveSheep;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.util.Vector;
import sun.security.krb5.Config;

public class InfatuatedSheep {

    private Sheep sheep;
    private World world;
    private LoveSheep plugin;
    private Player player;
    private DyeColor oldColor;

    public InfatuatedSheep(Sheep s, Player p, LoveSheep plug) {
        sheep = s;
        world = s.getWorld();
        player = p;
        plugin = plug;
    }

    public Player lover() {
        return player;
    }

    public boolean updateLoverStatus() {
        boolean ret = false;
        if (!world.getLivingEntities().contains(sheep)) {
            // sheep not found in this world anymore, disregard
            plugin.getConfig().lslog(Level.FINE, "Sheep gone from this world");
            // we seem to end up here when players quit (or maybe when there's only one player online that quits?)
            // fix by catching Player.QUIT and resetting the color there
        } else {
            // really, this should ALWAYS return the player we were initialized with, or null
            LivingEntity e = sheep.getTarget();
            Player p = null;
            if(e instanceof Player) { // maybe sheep can have other targets (other plugins etc)
                p = (Player)e;
            }
            if(p != null) {
                if(!p.isOnline()) {
                    // lover no longer online
                    plugin.getConfig().lslog(Level.FINE, "Lover no longer online - return to old color");
                    sheep.setColor(oldColor);
                } else {
                    // this sheep is already in love with someone
                    // is it successful? time to drop it? do something here
                    plugin.getConfig().lslog(Level.FINE, "Loved sheep looking for action");

                    // only need to do this if we're outside of normal target-affected distance
                    // whatever that is

                    Location tloc = lookAt(sheep.getLocation(), p.getLocation());
                    // todo: check to see we're not teleporting into a non-air block
                    Location newLoc = move(tloc, new Vector(0,0,1));
                    if(newLoc.getBlock().getType() != Material.AIR) {
                      // VERY naive approach for testing
                      // seems not to work in (some?) downslopes
                      // would be cool to do fancy pathfinding instead
                      newLoc.add(0,1,0);
                    }
                    // seems in some cases sheep get hurt by teleporting into (?) eachother/the player
                    sheep.teleport(newLoc);

                    ret = true; // keep it up
                }
            } else {
                sheep.setTarget(player);
                oldColor = sheep.getColor();
                sheep.setColor(plugin.getConfig().getSheepColor());
                ret = true;
            }
        }

        return ret;
    }

    public void oldColor() {
        plugin.getConfig().lslog(Level.FINE, "Lover no longer online - return to old color");
        sheep.setColor(oldColor);
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
    }
}