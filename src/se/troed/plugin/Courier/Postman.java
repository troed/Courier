package se.troed.plugin.Courier;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

/**
 * A Postman is a friendly Creature, tirelessly carrying around our mail
 *
 * One will be spawned for each Player that will receive mail
 */
public abstract class Postman {

    protected Creature postman;
    protected final Courier plugin;
    protected final ItemStack letterItem;
    protected UUID uuid;
    protected boolean scheduledForQuickRemoval;
    protected int taskId;
    protected Runnable runnable;
    protected final Player player;
    
    protected Postman(Courier plug, Player p, int id) {
        plugin = plug;
        player = p;
        // Postmen, like players doing /letter, can create actual Items
        letterItem = new ItemStack(Material.MAP, 1, plug.getCourierdb().getCourierMapId());
        letterItem.addUnsafeEnchantment(Enchantment.DURABILITY, id);
    }
    
    static Postman create(Courier plug, Player p, int id) {
        if(Courier.POSTMANTYPE == CreatureType.ENDERMAN) {
            return new EnderPostman(plug, p, id);
        } else if (Courier.POSTMANTYPE == CreatureType.VILLAGER) {
            return new VillagerPostman(plug, p, id);
        }
        return null;
    }

    // must be implemented
//    public abstract CreatureType getType();
    public abstract void spawn(Location l);

    public int getHeight() {
        plugin.getCConfig().clog(Level.FINE, "Postman is " + (int)Math.round(postman.getEyeHeight(true)) + " blocks high");
        return (int)Math.round(postman.getEyeHeight(true));
    }

    public ItemStack getLetterItem() {
        return letterItem;
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
        postman.getWorld().dropItemNaturally(postman.getLocation(), letterItem);
        String maildrop = plugin.getCConfig().getMailDrop();
        if(maildrop != null && !maildrop.isEmpty()) {
            player.sendMessage(maildrop);
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void remove() {
        postman.remove();
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

    // Called when either mail has been delivered or someone is attacking the postman
    public void quickDespawn() {
        plugin.schedulePostmanDespawn(this.uuid, plugin.getCConfig().getQuickDespawnTime());
        scheduledForQuickRemoval = true;
    }
}