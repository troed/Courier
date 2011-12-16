package se.troed.plugin.Courier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Naughty: Implementing ServerCommands and onCommand in the same class
 * Nice: Implementing ServerCommands and onCommand in the same class
 */
public class CourierCommands /*extends ServerListener*/ implements CommandExecutor {
    private final Courier plugin;

    public CourierCommands(Courier instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        boolean execute = false;
        if (sender instanceof Player) {
            player = (Player) sender;
            if(player.hasPermission("postman")) {
                execute = true;
            }
            plugin.getCConfig().clog(Level.FINE, "Player command event");
        } else {
            // console operator is op
            execute = true;
            plugin.getCConfig().clog(Level.FINE, "Server command event");
        }

        if(execute) {
            sender.sendMessage("You've got mail!");

            /**
             * Instantiate Enderman
             */
            Location loc = player.getLocation();
            loc.add(1,0,1);
            Enderman ender = (Enderman) player.getWorld().spawnCreature(loc, CreatureType.ENDERMAN);

            // wtf this became sugarcane - R2 vs R1 issue?
            MaterialData material = new MaterialData(Material.PAPER);
            ender.setCarriedMaterial(material);

            Postman postman = new Postman(ender, player, plugin);
            // megawork, replace this with our own letter renderer
            MapView newmap = plugin.getServer().createMap(player.getWorld());
            ItemStack letter = new ItemStack(Material.MAP,1,newmap.getId());
            postman.addLetter(letter);
            plugin.addPostman(postman);

            // no, but maybe some basic look-at etc?
            // ender.setTarget(player);

            return true;
        }
        return false;
    }

/*    public void onServerCommand(ServerCommandEvent event) {
        plugin.getCConfig().clog(Level.FINE, "Server command event");
    }*/
}


