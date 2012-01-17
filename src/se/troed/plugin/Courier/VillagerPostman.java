package se.troed.plugin.Courier;

import org.bukkit.Location;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class VillagerPostman extends Postman {

    VillagerPostman(Courier plug, Player p, int id) {
        super(plug, p, id);
    }

/*    public CreatureType getType() {
        return CreatureType.VILLAGER;
    }*/

    public void spawn(Location l) {
        postman = (Villager) player.getWorld().spawnCreature(l, CreatureType.VILLAGER);
        postman.setTarget(player);
        uuid = postman.getUniqueId();
    }

    @Override
    public void drop() {
        postman.setTarget(null);
        super.drop();
    }

}
