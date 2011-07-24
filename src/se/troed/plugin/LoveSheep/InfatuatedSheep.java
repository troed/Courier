package se.troed.plugin.LoveSheep;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.DyeColor;
import org.bukkit.World;

public class InfatuatedSheep {

    private Sheep sheep;
    private World world;
    private LoveSheep plugin;
    private Player player;

    public InfatuatedSheep(Sheep s, Player p, LoveSheep plug) {
        sheep = s;
        world = s.getWorld();
        player = p;
        plugin = plug;
    }

    public Player owner() {
        return player;
    }

    public boolean loverStatus() {
        boolean ret = false;
        if (!world.getLivingEntities().contains(sheep)) {
            // sheep not found in this world anymore, disregard
//            System.out.println("Sheep gone from this world");
        } else {
            LivingEntity e = sheep.getTarget();
            Player o = null;
            if(e instanceof Player) {
                o = (Player)e;
            }
            if(o != null) {
                // this sheep is already in love with someone
                // is it successful? time to drop it? do something here
                // caveat: we only get here on sheep.spawn - that is, seldom
                // should maybe move stuff from here to EntityMove or PlayerMove
                ret = true; // keep it up
            } else {
                sheep.setTarget(player);
                sheep.setColor(plugin.getConfig().getSheepColor());
                ret = true;
            }
        }

        return ret;
    }
}