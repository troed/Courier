package se.troed.plugin.Courier;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

class CourierCommands /*extends ServerListener*/ implements CommandExecutor {
    private final Courier plugin;

    public CourierCommands(Courier instance) {
        plugin = instance;
    }
    
    // Player is null for console
    // This method didn't turn out that well. Should send a message to sender, when console,
    // about why the command fails when we need a player object
    private boolean allowed(Player p, String c) {
        boolean a = false;
        if (p != null) {
            if(c.equals(Courier.CMD_POSTMAN) && p.hasPermission(Courier.PM_POSTMAN)) {
                a = true;
            } else if(c.equals(Courier.CMD_COURIER) && p.hasPermission(Courier.PM_INFO)) {
                a = true;
            } else if(c.equals(Courier.CMD_POST) && p.hasPermission(Courier.PM_SEND)) {
                a = true;
            } else if(c.equals(Courier.CMD_LETTER) && p.hasPermission(Courier.PM_WRITE)) {
                a = true;
            } if(c.equals(Courier.CMD_COURIER)) {
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

    /*
    postman:
        description: Spawns a postman
        aliases: [mailman, postie]
        permission: courier.postman
        usage: /postman
    */
    boolean commandInformation(Player player, String[] args) {
        boolean retVal = false;
        if (args != null && args.length > 0) {
            if (args[0].equalsIgnoreCase("fees")) {
                if(plugin.getEconomy() != null) {
                    double fee = plugin.getCConfig().getFeeSend();
                    player.sendMessage(plugin.getCConfig().getInfoFee(plugin.getEconomy().format(fee)));
                } else {
                    player.sendMessage(plugin.getCConfig().getInfoNoFee());
                }
                retVal = true;
            } else if(args[0].equalsIgnoreCase("unread")) {
                if(plugin.getCourierdb().deliverUnreadMessages(player.getName())) {
                    player.sendMessage(plugin.getCConfig().getPostmanExtraDeliveries());
                } else {
                    player.sendMessage(plugin.getCConfig().getPostmanNoUnreadMail());
                }
                retVal = true;
            }
            // todo: implement /courier list
        } else {
            player.sendMessage(plugin.getCConfig().getInfoLine1());
            player.sendMessage(plugin.getCConfig().getInfoLine2());
            player.sendMessage(plugin.getCConfig().getInfoLine3());
            player.sendMessage(plugin.getCConfig().getInfoLine4());
            retVal = true;
        }
        return retVal;
    }

    /*
    postman:
        description: Spawns a postman
        aliases: [mailman, postie]
        permission: courier.postman
        usage: /postman
     */
    boolean commandPostman(Player player) {
        if(plugin.getCourierdb().undeliveredMail(player.getName())) {
            int undeliveredMessageId = plugin.getCourierdb().undeliveredMessageId(player.getName());
            if(undeliveredMessageId != -1) {
                // this is really a command meant for testing, no need for translation
                player.sendMessage("You've got mail waiting for delivery!");

                // Is it the FIRST map viewed on server start that gets the wrong id when rendering?
                // how can that be? if it's my code I don't see where ...

                plugin.getCConfig().clog(Level.FINE, "MessageId: " + undeliveredMessageId);
                String from = plugin.getCourierdb().getSender(player.getName(), undeliveredMessageId);
                String message = plugin.getCourierdb().getMessage(player.getName(), undeliveredMessageId);
                plugin.getCConfig().clog(Level.FINE, "Sender: " + from + " Message: " + message);
                if(from != null && message != null) {
                    Location spawnLoc = plugin.findSpawnLocation(player);
                    if(spawnLoc != null) {
//                        Postman postman = new CreaturePostman(plugin, player, undeliveredMessageId);
                        Postman postman = Postman.create(plugin, player, undeliveredMessageId);
                        plugin.addSpawner(spawnLoc, postman);
                        postman.spawn(spawnLoc);
                        plugin.addPostman(postman);
                    }

                } else {
                    plugin.getCConfig().clog(Level.SEVERE, "Gotmail but no sender or message found! mapId=" + undeliveredMessageId);
                }
            } else {
                plugin.getCConfig().clog(Level.WARNING, "Gotmail but no mailid!");
            }
        }
        return true;
    }

    /*
    post:
        description: Sends the letter currently held in hand to someone
        aliases: [mail, send]
        permission: courier.send
        usage: /post playername
    */
    boolean commandPost(Player player, String[] args) {
        boolean ret = false;
        ItemStack item = player.getItemInHand();
        Letter letter = null;
        if(item != null && item.getType() == Material.MAP) {
            letter = plugin.getLetter(item);
        }
        if(letter != null) {
            if(plugin.getEconomy() != null &&
                    plugin.getEconomy().getBalance(player.getName()) < plugin.getCConfig().getFeeSend() &&
                    !player.hasPermission(Courier.PM_THEONEPERCENT)) {
                player.sendMessage(plugin.getCConfig().getPostNoCredit(plugin.getEconomy().format(plugin.getCConfig().getFeeSend())));
                ret = true;
            } else if(args == null || args.length < 1) {
                player.sendMessage(plugin.getCConfig().getPostNoRecipient());
            // /post player1 player2 player3 etc in the future?
            } else {
                OfflinePlayer[] offPlayers = plugin.getServer().getOfflinePlayers();
                OfflinePlayer p = null;
                for(OfflinePlayer o : offPlayers) {
                    if(o.getName().equalsIgnoreCase(args[0])) {
                        p = o;
                        plugin.getCConfig().clog(Level.FINE, "Found " + p.getName() + " in OfflinePlayers");
                        break;
                    }
                }
                if(p == null) { // todo: remove this section for 1.0.1-R2
                    // See https://bukkit.atlassian.net/browse/BUKKIT-404 by GICodeWarrior
                    // https://github.com/troed/Courier/issues/2
                    // We could end up here if this is to a player who's on the server for the first time
                    p = plugin.getServer().getPlayerExact(args[0]);
                    if(p != null) {
                        plugin.getCConfig().clog(Level.FINE, "Found " + p.getName() + " in getPlayerExact");
                    }
                }
                if(p == null) {
                    // still not found, try lazy matching and display suggestions
                    // (searches online players only)
                    List<Player> players = plugin.getServer().matchPlayer(args[0]);
                    if(players != null && players.size() == 1) {
                        // we got one exact match
                        // p = players.get(0); // don't, could be embarrassing if wrong
                        player.sendMessage(plugin.getCConfig().getPostDidYouMean(args[0], players.get(0).getName()));
                    } else if (players != null && players.size() > 1 && player.hasPermission(Courier.PM_LIST)) {
                        // more than one possible match found
                        StringBuilder suggestList = new StringBuilder();
                        int width = 0;
                        for(Player pl : players) {
                            suggestList.append(pl.getName());
                            suggestList.append(" ");
                            width += pl.getName().length()+1;
                            if(width >= 60) { // console max seems to be 60 .. or 61
                                suggestList.append("\n");
                                width = 0;
                            }
                        }
                        // players listing who's online. If so, that could be a permission also valid for /courier list
                        player.sendMessage(plugin.getCConfig().getPostDidYouMeanList(args[0]));
                        player.sendMessage(plugin.getCConfig().getPostDidYouMeanList2(suggestList.toString()));
                    } else {
                        // time to give up
                        player.sendMessage(plugin.getCConfig().getPostNoSuchPlayer(args[0]));
                    }
                }
                if(p != null) {
                    boolean send = false;

                    if(plugin.getEconomy() != null && !player.hasPermission(Courier.PM_THEONEPERCENT)) {
                        // withdraw postage fee
                        double fee = plugin.getCConfig().getFeeSend();
                        EconomyResponse er = plugin.getEconomy().withdrawPlayer(player.getName(), fee);
                        if(er.transactionSuccess()) {
                            player.sendMessage(plugin.getCConfig().getPostLetterSentFee(p.getName(), plugin.getEconomy().format(fee)));
                            send = true;
                        } else {
                            player.sendMessage(plugin.getCConfig().getPostFundProblem());
                            plugin.getCConfig().clog(Level.WARNING, "Could not withdraw postage fee from " + p.getName());
                        }
                    } else {
                        player.sendMessage(plugin.getCConfig().getPostLetterSent(p.getName()));
                        send = true;
                    }

                    if(send) {
                        // sign over this letter to recipient
                        if(plugin.getCourierdb().sendMessage(letter.getId(), p.getName(), player.getName())) {
                            // existing Letter now has outdated info, will automatically be recreated from db
                            plugin.removeLetter(letter.getId());
                            plugin.getLetterRenderer().forceClear();

                            // remove item from hands, which kills the ItemStack association. It's now "gone"
                            // from the control of this player. (if I implement additional receivers you could of course
                            //  cc: yourself)
                            player.setItemInHand(null);
                        } else {
                            plugin.getCConfig().clog(Level.WARNING, "Could not send message with ID: " + letter.getId());
                        }
                    }
                }
                ret = true;
            }
        } else {
            player.sendMessage(plugin.getCConfig().getPostNoLetter());
        }
        return ret;
    }

    /*
    letter:
        description: Creates a letter
        aliases: [note, write]
        permission: courier.write
        usage: /letter message        # Can be repeated to add additional text to existing held letter
     */
    boolean commandLetter(Player player, String[] args) {
        // letter - no argument - chatmode?
        // letter message - builds upon message in hand. We must be sender, I think.
        boolean ret = false;
        if(args == null || args.length < 1) {
            player.sendMessage(plugin.getCConfig().getLetterNoText());
        } else {
            ItemStack item = player.getItemInHand();
            Letter letter = null;
            if(item != null && item.getType() == Material.MAP) {
                letter = plugin.getLetter(item);
            }
            int id;
            if(letter == null) {
                // player had no Courier Letter in hand, create a new one
                // todo: this is a good place to add crafted Letter requirement (or other Item based cost)
                // see: http://dev.bukkit.org/server-mods/courier/tickets/16-postage-charges/
                id = plugin.getCourierdb().generateUID();
            } else {
                id = letter.getId();
            }
            if(id != -1) {
                boolean useCached = true;
                StringBuilder message = new StringBuilder();
                if(letter != null) {
                    // new stuff appended to the old
                    // fetch from db, letter.getMessage contains newline formatted text
                    message.append(plugin.getCourierdb().getMessage(letter.getReceiver(), id));
                    if(!player.getName().equalsIgnoreCase(letter.getSender())) {
                        // we're adding to existing text from someone else, add newlines
                        // extra credits: detect if we were going to be on a new line anyway, then only append one
                        message.append("\\n \\n "); // replaced with actual newlines by Letter, later
                        useCached = false;
                    }
                }
                // hey I added &nl a newline -> &nl      -> &nl
                // hey I added\n a newline   -> added\n  -> added \n
                // hey I added \na newline   -> \na      -> \n a
                // hey I added\na newline    -> added\na -> added \n a
                Pattern newlines = Pattern.compile("(\\s*\\\\n\\s*|\\s*&nl\\s*)");
                boolean invalid = false;
                try {
                    for (String arg : args) {
                        // http://dev.bukkit.org/server-mods/courier/tickets/34-illegal-argument-exception-in-map-font/
                        if(!MinecraftFont.Font.isValid(arg)) {
                            invalid = true;
                            continue;
                        }
                        // %loc -> [X,Y,Z] and such
                        // if this grows, break it out and make it configurable
                        if (arg.equalsIgnoreCase("%loc") || arg.equalsIgnoreCase("%pos")) {
                            Location loc = player.getLocation();
                            message.append("[" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "]");
                        } else {
                            message.append(newlines.matcher(arg).replaceAll(" $1 ").trim()); // tokenize
                        }
                        message.append(" ");
                    }
                } catch (Exception e) {
                    plugin.getCConfig().clog(Level.SEVERE, "Caught Exception in MinecraftFont.isValid()");
                    invalid = true;
                }

                if(invalid) {
                    player.sendMessage(plugin.getCConfig().getLetterSkippedText());
                }

                if (plugin.getCourierdb().storeMessage(id,
                        player.getName(),
                        message.toString(), // .trim() but then /letter on /letter needs added space anyway
                        (int)(System.currentTimeMillis() / 1000L))) { // oh noes unix y2k issues!!!11
    
                    // no letter == we create and put in hands, or in inventory, or drop to ground
                    // see CourierEventListener for similar code when Postman delivers letters
                    if(letter == null) {
                        ItemStack letterItem = new ItemStack(Material.MAP, 1, plugin.getCourierdb().getCourierMapId());
                        letterItem.addUnsafeEnchantment(Enchantment.DURABILITY, id);
                        if(item != null && item.getAmount() > 0) {
                            plugin.getCConfig().clog(Level.FINE, "Player hands not empty");
                            HashMap<Integer, ItemStack> items = player.getInventory().addItem(letterItem);
                            if(items.isEmpty()) {
                                plugin.getCConfig().clog(Level.FINE, "Letter added to inventory");
                                String inventory = plugin.getCConfig().getLetterInventory();
                                if(inventory != null && !inventory.isEmpty()) {
                                    player.sendMessage(inventory);
                                }
                            } else {
                                plugin.getCConfig().clog(Level.FINE, "Inventory full, letter dropped");
                                String drop = plugin.getCConfig().getLetterDrop();
                                if(drop != null && !drop.isEmpty()) {
                                    player.sendMessage(drop);
                                }
                                player.getWorld().dropItemNaturally(player.getLocation(), letterItem);
                            }
                        } else {
                            plugin.getCConfig().clog(Level.FINE, "Letter delivered into player's hands");
                            player.setItemInHand(letterItem); // REALLY replaces what's there
    
                            // quick render
                            player.sendMap(plugin.getServer().getMap(plugin.getCourierdb().getCourierMapId()));
                        }
                    } else {
                        if(useCached) {
                            // set Message directly, we know no other info changed and want to keep current page info
                            letter.setMessage(message.toString());
                        } else {
                            // existing Letter now has outdated info, will automatically be recreated from db
                            plugin.removeLetter(id);
                            plugin.getLetterRenderer().forceClear();
                        } 
                    }
                } else {
                    player.sendMessage(plugin.getCConfig().getLetterCreateFailed());
                    plugin.getCConfig().clog(Level.SEVERE, "Could not store letter in database!");
                }
                ret = true;
            } else {
                player.sendMessage(plugin.getCConfig().getLetterNoMoreUIDs());
                plugin.getCConfig().clog(Level.SEVERE, "Out of unique message IDs!");
                ret = true;
            }
        }
        return ret;
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
        if(cmd.equals(Courier.CMD_COURIER) && allowed(player, cmd)) {
            // not allowed to be run from the console, uses player
            ret = commandInformation(player, args);
        } else if((cmd.equals(Courier.CMD_LETTER)) && allowed(player, cmd)) {
            // not allowed to be run from the console, uses player
            ret = commandLetter(player, args);
        } else if((cmd.equals(Courier.CMD_POST)) && allowed(player, cmd)) {
            // not allowed to be run from the console, uses player
            ret = commandPost(player, args);
        } else if(cmd.equals(Courier.CMD_POSTMAN) && allowed(player, cmd)){
            // not allowed to be run from the console, uses player
            ret = commandPostman(player);
        }
        return ret;
    }

/*    public void onServerCommand(ServerCommandEvent event) {
        plugin.getCConfig().clog(Level.FINE, "Server command event");
    }*/
}
