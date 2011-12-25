package se.troed.plugin.Courier;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.List;
import java.util.logging.Level;

/**
 * Naughty: Implementing ServerCommands and onCommand in the same class
 * Nice: Implementing ServerCommands and onCommand in the same class
 */
class CourierCommands /*extends ServerListener*/ implements CommandExecutor {
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
            } else if(c.equals(Courier.CMD_POST) && p.hasPermission(Courier.PM_SEND)) {
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

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        // sender is always safe to sendMessage to - console as well as player
        // player can from now on be null!

        String cmd = command.getName().toLowerCase();
        if((cmd.equals(Courier.CMD_COURIER) || cmd.equals(Courier.CMD_POST)) && allowed(player, cmd)) {
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
                    // todo: figure out max length and show if a cutoff was made
                    // Minecraftfont isValid(message)

                    StringBuilder message = new StringBuilder();
                    for(int i=1; i<args.length; i++) {
                        message.append(args[i]);
                        message.append(" ");
                    }

                    plugin.getCourierdb().storeMessage(map.getId(), p.getName(), sender.getName(),  message.toString());
                }
                ret = true;
            }
        } else if(cmd.equals(Courier.CMD_POSTMAN) && allowed(player, cmd)){
            // not allowed to be run from the console, uses player
            if(plugin.getCourierdb().undeliveredMail(player.getName())) {
                short undeliveredMessageId = plugin.getCourierdb().undeliveredMessageId(player.getName());
                if(undeliveredMessageId != -1) {
                    sender.sendMessage("You've got mail!");

                    // Is it the FIRST map viewed on server start that gets the wrong id when rendering?
                    // how can that be? if it's my code I don't see where ...

                    plugin.getCConfig().clog(Level.FINE, "MessageId: " + undeliveredMessageId);
                    String from = plugin.getCourierdb().getSender(player.getName(), undeliveredMessageId);
                    String message = plugin.getCourierdb().getMessage(player.getName(), undeliveredMessageId);
                    plugin.getCConfig().clog(Level.FINE, "Sender: " + from + " Message: " + message);
                    if(from != null && message != null) {
                        Location spawnLoc = plugin.findSpawnLocation(player);
                        if(spawnLoc != null) {
                            Postman postman = new Postman(plugin, player, spawnLoc, undeliveredMessageId);
                            plugin.addPostman(postman);
                        }

                    } else {
                        plugin.getCConfig().clog(Level.SEVERE, "Gotmail but no sender or message found! mapId=" + undeliveredMessageId);
                    }
                } else {
                    plugin.getCConfig().clog(Level.WARNING, "Gotmail but no mailid!");
                }
            }
            ret = true;
        }
        return ret;
    }

/*    public void onServerCommand(ServerCommandEvent event) {
        plugin.getCConfig().clog(Level.FINE, "Server command event");
    }*/
}


