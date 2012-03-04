package se.troed.plugin.Courier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

public class EnderPostman extends Postman {

    EnderPostman(Courier plug, Player p, int id, EntityType t) {
        super(plug, p, id, t);
    }

    public void spawn(Location l) {
        postman = (Enderman) player.getWorld().spawnCreature(l, EntityType.ENDERMAN);
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
