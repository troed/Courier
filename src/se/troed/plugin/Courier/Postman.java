package se.troed.plugin.Courier;

import java.util.UUID;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Enderman;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/**
 * A Postman is a friendly Enderman, tirelessly carrying around our mail
 *
 * One will be spawned for each Player that will receive mail
 */
public class Postman {

    private Enderman enderman;
    private final Courier plugin;
    private final ItemStack letter;
    private UUID uuid;
    private boolean scheduledForQuickRemoval;
    private int taskId;
    private Runnable runnable;
    private final Player player;
    
    public Postman(Courier plug, Player p, short id) {
        plugin = plug;
        player = p;
        letter = new ItemStack(Material.MAP,1,id);
    }
    
    public ItemStack getLetter() {
        return letter;
    }
    
    public void spawn(Location l) {
        enderman = (Enderman) player.getWorld().spawnCreature(l, CreatureType.ENDERMAN);
        // gah, item vs block ...
        // MaterialData material = new MaterialData(Material.PAPER);
        enderman.setCarriedMaterial(new MaterialData(Material.BOOKSHELF));
        uuid = enderman.getUniqueId();
    }

    public void cannotDeliver() {
        String cannotDeliver = plugin.getCConfig().getCannotDeliver();
        if(cannotDeliver != null && !cannotDeliver.isEmpty()) {
            player.sendMessage(cannotDeliver);
        }
    }

    public void announce(Location l) {
        // todo: if in config, play effect
        player.playEffect(l, Effect.BOW_FIRE, 100);
        String greeting = plugin.getCConfig().getGreeting();
        if(greeting != null && !greeting.isEmpty()) {
            player.sendMessage(greeting);
        }
    }
    
    public void drop() {
        enderman.getWorld().dropItemNaturally(enderman.getLocation(), letter);
        enderman.setCarriedMaterial(new MaterialData(Material.AIR));
        String maildrop = plugin.getCConfig().getMailDrop();
        if(maildrop != null && !maildrop.isEmpty()) {
            player.sendMessage(maildrop);
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void remove() {
        enderman.remove();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean scheduledForQuickRemoval() {
        return scheduledForQuickRemoval;
    }
    
    public void setTaskId(int t) {
        taskId = t;
    }
    
    public int getTaskId() {
        return taskId;
    }

    public void setRunnable(Runnable r) {
        runnable = r;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    /**
     * Called when either mail has been delivered or someone is attacking the postman
     */
    public void quickDespawn() {
        plugin.schedulePostmanDespawn(this.uuid, plugin.getCConfig().getQuickDespawnTime());
        scheduledForQuickRemoval = true;
    }
}