package se.troed.plugin.Courier;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;

import java.util.Date;
import java.util.List;
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
    
    // Player is null for console
    private boolean allowed(Player p, String c) {
        boolean a = false;
        if (p != null) {
            if(c.equals(Courier.CMD_POSTMAN) && p.hasPermission(Courier.PM_POSTMAN)) {
                a = true;
            } else if(c.equals(Courier.CMD_COURIER) && p.hasPermission(Courier.PM_SEND)) {
                a = true;
            }
            plugin.getCConfig().clog(Level.FINE, "Player command event");
        } else {
            // console operator is op, no player and no location
            plugin.getCConfig().clog(Level.FINE, "Server command event");
        }
        plugin.getCConfig().clog(Level.FINE, "Permission: " + a);
        return a;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean ret = false;
        /**
         * Minecraftfont isValid(message)
         * getWidth
         * or do I need to know max length myself?
         */
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        // sender is always safe to sendMessage to - console as well as player
        // player can from now on be null!

        String cmd = command.getName().toLowerCase();
        if(cmd.equals(Courier.CMD_COURIER) && allowed(player, cmd)) {
            // not allowed to be run from the console, uses player
            if(args == null || args.length < 1) {
                sender.sendMessage("Courier: Error, no recipient for message!");
            } else if(args.length < 2) {
                sender.sendMessage("Courier: Error, message cannot be empty!");
            } else {
//                todo:     public List<Player> matchPlayer(String name);
// is this resource heavy by any chance?
                OfflinePlayer[] offPlayers = plugin.getServer().getOfflinePlayers();
                OfflinePlayer p = null;
                for(OfflinePlayer o : offPlayers) {
                    if(o.getName().equalsIgnoreCase(args[0])) {
                       p = o;
                       break;
                    }
                }
                if(p == null) {
                    // here we could re-run with matchplayer() and ask if anyone of those was the intended
                    // recipient
                    sender.sendMessage("Courier: There's no player on this server with the name " + args[0]);
                } else {
                    // Q: Maps are saved in the world-folders, "null" isn't valid as world then?
                    MapView map = plugin.getServer().createMap(player.getWorld());
                    plugin.getCConfig().clog(Level.FINE, "Map ID = " + map.getId());
                    // make it ours
                    map.setCenterX(Courier.MAGIC_NUMBER);
                    map.setCenterZ((int)(System.currentTimeMillis() / 1000L)); // oh noes unix y2k issues!!!11
                    List<MapRenderer> renderers = map.getRenderers();
                    for(MapRenderer r : renderers) { // remove existing renderers
                        map.removeRenderer(r);
                    }

                    // offPlayer.getPlayer() returns Player or null depending on onlinestatus
                    sender.sendMessage("Courier: Message to " + p.getName() + " sent!");
                    StringBuffer message = new StringBuffer();
                    for(int i=1; i<args.length; i++) {
                        message.append(args[i]);
                        message.append(" ");
                    }

                    plugin.getCourierdb().storeMessage(map.getId(), p.getName(), sender.getName(),  message.toString());
                }
                ret = true;
            }
        } else if(cmd.equals(Courier.CMD_POSTMAN) && allowed(player, cmd)){
            if(plugin.getCourierdb().gotMessage(player.getName())) {
                short unreadMessageId = plugin.getCourierdb().unreadMessageId(player.getName());
                if(unreadMessageId != -1) {
                    sender.sendMessage("You've got mail!");

                    // Is it the FIRST map viewed on server start that gets the wrong id when rendering?
                    // how can that be?

                    plugin.getCConfig().clog(Level.FINE, "MessageId: " + unreadMessageId);
                    String from = plugin.getCourierdb().getSender(player.getName(), unreadMessageId);
                    String message = plugin.getCourierdb().getMessage(player.getName(), unreadMessageId);
                    plugin.getCConfig().clog(Level.FINE, "Sender: " + from + " Message: " + message);
                    if(from != null && message != null) {
                        Letter letter = new Letter(from, player.getName(), message);
                        MapView map = plugin.getServer().getMap(unreadMessageId);
                        letter.initialize(map); // does this make a difference at all?
                        List<MapRenderer> renderers = map.getRenderers();
                        for(MapRenderer r : renderers) { // remove existing renderers
                            map.removeRenderer(r);
                        }
                        map.addRenderer(letter);
                        plugin.addLetter(unreadMessageId, letter); // keeps track of which maps has active renderers
                        ItemStack letterItem = new ItemStack(Material.MAP,1,map.getId());
                        /**
                         * Instantiate Enderman
                         */
                        Location loc = player.getLocation();
                        loc.add(1,0,1);
                        Enderman ender = (Enderman) player.getWorld().spawnCreature(loc, CreatureType.ENDERMAN);

                        // wtf this became sugarcane - R2 vs R1 issue?
                        MaterialData material = new MaterialData(Material.PAPER);
                        ender.setCarriedMaterial(material);

                        Postman postman = new Postman(ender, plugin);
                        postman.setLetter(letterItem);
                        plugin.addPostman(postman);
                    } else {
                        plugin.getCConfig().clog(Level.FINE, "Gotmail but no sender or message found!");
                    }
                } else {
                    plugin.getCConfig().clog(Level.FINE, "Gotmail but no mailid!");
                }
                // no, but maybe some basic look-at etc?
                // ender.setTarget(player);
            }
            ret = true;
        }
        return ret;
    }

/*    public void onServerCommand(ServerCommandEvent event) {
        plugin.getCConfig().clog(Level.FINE, "Server command event");
    }*/
}


