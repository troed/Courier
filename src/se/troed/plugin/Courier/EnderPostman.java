package se.troed.plugin.Courier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

public class EnderPostman extends CreaturePostman {

    EnderPostman(Courier plug, Player p, int id, CreatureType t) {
        super(plug, p, id, t);
    }

    public void spawn(Location l) {
        postman = (Enderman) player.getWorld().spawnCreature(l, CreatureType.ENDERMAN);
        // gah, item vs block ...
        // MaterialData material = new MaterialData(Material.PAPER);
        ((Enderman)postman).setCarriedMaterial(new MaterialData(Material.BOOKSHELF));
        uuid = postman.getUniqueId();
    }

    @Override
    public void drop() {
        ((Enderman)postman).setCarriedMaterial(new MaterialData(Material.AIR));
        super.drop();
    }

}
