package se.troed.plugin.Courier;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/* Allows all Creatures to be Postmen, although we only test with Villagers (and Enderman)
 *
 * There is an issue with Creatures "pushing" the player, possibly into lava. I would need
 * https://bukkit.atlassian.net/browse/BUKKIT-127 (onEntityMove) to solve that, or
 * I could refrain from doing .setTarget() of course - see getWalkToPlayer()
 *
 */
public class CreaturePostman extends Postman {

    CreaturePostman(Courier plug, Player p, int id, EntityType t) {
        super(plug, p, id, t);
    }

    public void spawn(Location l) {
        postman = (Creature) player.getWorld().spawnCreature(l, type);
        if(plugin.getCConfig().getWalkToPlayer()) {
            postman.setTarget(player);
        }
        uuid = postman.getUniqueId();
    }

    @Override
    public void drop() {
        if(plugin.getCConfig().getWalkToPlayer()) {
            postman.setTarget(null);
        }
        super.drop();
    }

}
