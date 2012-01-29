package se.troed.plugin.Courier;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;

/* Allows all Creatures to be Postmen, although we only test with Villagers (and Enderman)
 *
 * There is an issue with Creatures "pushing" the player, possibly into lava. I would need
 * https://bukkit.atlassian.net/browse/BUKKIT-127 (onEntityMove) to solve that, or
 * I could refrain from doing .setTarget() of course
 *
 */
public class CreaturePostman extends Postman {

    CreaturePostman(Courier plug, Player p, int id, CreatureType t) {
        super(plug, p, id, t);
    }

    public void spawn(Location l) {
        postman = (Creature) player.getWorld().spawnCreature(l, type);
        postman.setTarget(player);
        uuid = postman.getUniqueId();
    }

    @Override
    public void drop() {
        postman.setTarget(null);
        super.drop();
    }

}
