package io.github.jhhan611.trade

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent.runCommand
import net.kyori.adventure.text.event.HoverEvent.showText
import net.kyori.adventure.text.format.TextColor
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class TradeCommand {
    class RequestTrade : CommandExecutor {
        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
            if (sender is Player) {
                val player: Player = sender
                if (args.isEmpty()) {
                    player.sendMessage(ChatColor.RED.toString() + "Please enter the target player.")
                } else {
                    when (val target : Player? = Bukkit.getPlayerExact(args[0])) {
                        null -> {
                            player.sendMessage(ChatColor.RED.toString() + "Please enter an online player.")
                        }
                        player -> {
                            player.sendMessage(ChatColor.RED.toString() + "You cannot trade yourself.")
                        }
                        else -> {
                            if(ongoingRequests.contains(Pair(player.name, target.name))) {
                                player.sendMessage(ChatColor.RED.toString() + "You already have an ongoing request for " + target.name + ".")
                            } else {
                                sendTradeReq(player, target)
                                Bukkit.getServer().scheduler.scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("tradePlugin") as JavaPlugin, { killReq(Pair(player, target)) }, requestTime*20)
                            }
                            return true
                        }
                    }
                }
            }
            return false
        }
    }

    class AcceptTrade : CommandExecutor {
        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
            if (sender is Player) {
                val player: Player = sender
                if (args.isEmpty()) {
                    player.sendMessage(ChatColor.RED.toString() + "Please enter the target player.")
                } else {
                    val target : Player? = Bukkit.getPlayerExact(args[0])
                    if(target == null) {
                        player.sendMessage(ChatColor.RED.toString() + "Please enter an online player.")
                    } else if (target == player) {
                        player.sendMessage(ChatColor.RED.toString() + "You cannot trade yourself.")
                    } else {
                        if (ongoingRequests.contains(Pair(target.name, player.name))) {
                            if (ongoingTrades.containsKey(target)) player.sendMessage(ChatColor.RED.toString() + "That player is currently trading with someone else.")
                            else {
                                player.sendMessage(ChatColor.GREEN.toString() + "You've accepted the trade request.")
                                startTrade(target, player)
                            }
                        } else {
                            player.sendMessage(ChatColor.RED.toString() + target.name + " has never sent you a trade request.")
                        }
                    }
                }

            }
            return true
        }
    }

    class DeclineTrade : CommandExecutor {
        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
            if (sender is Player) {
                val player: Player = sender
                if (args.isEmpty()) {
                    player.sendMessage(ChatColor.RED.toString() + "Please enter the target player.")
                } else {
                    val target : Player? = Bukkit.getPlayerExact(args[0])
                    if(target == null || target == player) {
                        player.sendMessage(ChatColor.RED.toString() + "Please enter a valid player name.")
                    } else {
                        if (ongoingRequests.contains(Pair(target.name, player.name))) {
                            player.sendMessage(ChatColor.RED.toString() + "You've declined the trade request.")

                            ongoingRequests.remove(Pair(target.name, player.name))
                            target.sendMessage(ChatColor.RED.toString() + "Your trade request to " + player.name + " has been declined.")

                        } else {
                            player.sendMessage(ChatColor.RED.toString() + target.name + " has never sent you a trade request.")
                        }
                    }
                }

            }
            return true
        }
    }
}

fun sendTradeReq (sender : Player, receiver : Player) {
    ongoingRequests.add(Pair(sender.name, receiver.name))

    sender.sendMessage(ChatColor.GOLD.toString() + "You've sent a trade request to " + receiver.name + ".")
    sender.sendMessage(ChatColor.GOLD.toString() + "They have " + requestTime + " seconds to respond.")

    val buttons = Component.text("[Accept]").toBuilder()
    buttons.color(TextColor.color(85,255,85))
    buttons.clickEvent(runCommand("/tradeaccept " + sender.name))
    buttons.hoverEvent(showText(Component.text("Click to accept trade")))
    val decline = Component.text("[Decline]").toBuilder()
    decline.color(TextColor.color(255,85,85))
    decline.clickEvent(runCommand("/tradedecline " + sender.name))
    decline.hoverEvent(showText(Component.text("Click to decline trade")))
    buttons.append(Component.text(" "))
    buttons.append(decline)

    receiver.sendMessage(ChatColor.GOLD.toString() + sender.name + " has sent you a trade request")
    receiver.sendMessage(buttons)
}

fun killReq (requestPair : Pair<Player, Player>) {
    if (ongoingRequests.contains(Pair(requestPair.first.name, requestPair.second.name))) {
        ongoingRequests.remove(Pair(requestPair.first.name, requestPair.second.name))
        requestPair.first.sendMessage(ChatColor.GOLD.toString() + "Your trade request to " + requestPair.second.name + " timed out.")
        requestPair.second.sendMessage(ChatColor.GOLD.toString() + "The trade request from " + requestPair.first.name + " timed out.")
    }
}

fun startTrade (_sender: Player, _receiver: Player) {
    val sender: Player = _sender
    val receiver: Player = _receiver

    ongoingRequests.remove(Pair(sender.name, receiver.name))
    ongoingTrades[sender] = receiver
    ongoingTrades[receiver] = sender
    tradeStatus[sender] = 0
    tradeStatus[receiver] = 0

    val sinv = TradeInventory(sender, receiver)
    val rinv = TradeInventory(receiver, sender)

    sinv.openInv()
    rinv.openInv()
}