package io.github.jhhan611.trade

import net.kyori.adventure.text.Component
import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin


var ongoingRequests = mutableSetOf<Pair<String, String>>()
var ongoingTrades = HashMap<Player, Player>()
var tradeStatus = HashMap<Player, Int>()
val MenuTitle = Component.text(ChatColor.BLACK.toString() + "Trade Menu (Shift+Click to put)")
var requestTime : Long = 120

val topItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(""))
    }
}
val middleItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(""))
    }
}
val targetSlotItem = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(""))
    }
}
val acceptItem = ItemStack(Material.EMERALD).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(ChatColor.GREEN.toString() + "Accept Trade"))
    }
}
val cancelItem = ItemStack(Material.BARRIER).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(ChatColor.RED.toString() + "Reject Trade"))
    }
}
val targetAcceptItem = ItemStack(Material.LIME_WOOL).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(ChatColor.WHITE.toString() + "Opponent: " + ChatColor.GREEN.toString() + "Trade Accepted"))
    }
}
val playerAcceptItem = ItemStack(Material.LIME_WOOL).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(ChatColor.GREEN.toString() + "Trade Accepted"))
        lore(listOf(Component.text(ChatColor.GRAY.toString() + "Click again to revoke")))
    }
}
val targetNeutralItem = ItemStack(Material.GRAY_WOOL).apply {
    itemMeta = itemMeta.apply {
        displayName(Component.text(ChatColor.WHITE.toString() + "Opponent: " + ChatColor.GRAY.toString() + "Neutral"))
    }
}

class TradePlugin : JavaPlugin() {
    override fun onEnable() {
        logger.info("tradePlugin has been enabled")
        configLoad()
        requestTime = config.getLong("trade-request-timeout")
        getCommand("trade")!!.setExecutor(TradeCommand.RequestTrade())
        getCommand("tradeaccept")!!.setExecutor(TradeCommand.AcceptTrade())
        getCommand("tradedecline")!!.setExecutor(TradeCommand.DeclineTrade())
        this.server.pluginManager.registerEvents(ListenerClass(), this)
    }

    private fun configLoad() {
        config.addDefault("trade-request-timeout", 120)
        config.options().copyDefaults(true)
        saveConfig()
    }
}

class ListenerClass : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val clickedItem = event.currentItem
        val einv = event.clickedInventory
        if (event.view.title() == MenuTitle) {
            if (event.action == InventoryAction.COLLECT_TO_CURSOR) event.isCancelled = true
            if ((clickedItem == null || clickedItem.type == Material.AIR) && event.rawSlot <= 53) event.isCancelled = true
            if (clickedItem == null || einv == null) return
            else if (clickedItem.type == Material.EMERALD && event.rawSlot == 45) {
                val target = ongoingTrades[player] as Player

                if (tradeStatus[target] == 1) {
                    tradeAcc(player, target)
                } else {
                    event.isCancelled = true
                    einv.setItem(45, playerAcceptItem)
                    tradeStatus[player] = 1

                    val tInvView = target.openInventory
                    val tinv = tInvView.topInventory
                    tinv.setItem(53, targetAcceptItem)
                }
            } else if (clickedItem.type == Material.BARRIER && event.rawSlot == 48) {
                event.isCancelled = true
                val target = ongoingTrades[player] as Player
                tradeFail(player, target, false)
            } else if (clickedItem.type == Material.LIME_WOOL && event.rawSlot == 45) {
                event.isCancelled = true
                einv.setItem(45, acceptItem)
                tradeStatus[player] = 0
                val target = ongoingTrades[player] as Player
                val tInvView = target.openInventory
                val tinv = tInvView.topInventory
                tinv.setItem(53, targetNeutralItem)
            } else if ((event.rawSlot < 9 || event.rawSlot % 9 >= 4 || event.rawSlot >= 45  ) && event.rawSlot <= 53) {
                event.isCancelled = true
            } else {
                val action = event.action
                if(event.rawSlot <= 53) {
                    if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        event.isCancelled = true
                        einv.setItem(event.slot, ItemStack(Material.AIR))
                        player.openInventory.bottomInventory.addItem(clickedItem)
                        val target = ongoingTrades[player] as Player
                        target.openInventory.topInventory.setItem(event.slot + 5, targetSlotItem)
                    } else if (action == InventoryAction.PICKUP_HALF) {
                        event.isCancelled = true
                        val itemAmount = clickedItem.amount
                        if(itemAmount == 1) {
                            einv.setItem(event.slot, ItemStack(Material.AIR))
                            player.openInventory.bottomInventory.addItem(clickedItem)
                            val target = ongoingTrades[player] as Player
                            target.openInventory.topInventory.setItem(event.slot + 5, targetSlotItem)
                        } else {
                            einv.setItem(event.slot, ItemStack(clickedItem.type, itemAmount/2))
                            if (itemAmount % 2 == 0) {
                                player.openInventory.bottomInventory.addItem(ItemStack(clickedItem.type, itemAmount/2))
                                val target = ongoingTrades[player] as Player
                                target.openInventory.topInventory.setItem(event.slot + 5, ItemStack(clickedItem.type, itemAmount/2))
                            }
                            else {
                                player.openInventory.bottomInventory.addItem(ItemStack(clickedItem.type, itemAmount/2+1))
                                val target = ongoingTrades[player] as Player
                                target.openInventory.topInventory.setItem(event.slot + 5, ItemStack(clickedItem.type, itemAmount/2))
                            }
                        }
                    } else {
                        event.isCancelled = true
                        return
                    }
                    val target = ongoingTrades[player] as Player
                    tradeStatus[player] = 0
                    tradeStatus[target] = 0
                    einv.setItem(45, acceptItem)
                    einv.setItem(53, targetNeutralItem)
                    target.openInventory.topInventory.setItem(45, acceptItem)
                    target.openInventory.topInventory.setItem(53, targetNeutralItem)
                } else {
                    if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        event.isCancelled = true
                        einv.setItem(event.slot, ItemStack(Material.AIR))
                        val fEmpty = player.openInventory.topInventory.firstEmpty()
                        player.openInventory.topInventory.setItem(fEmpty, clickedItem)
                        val target = ongoingTrades[player] as Player
                        target.openInventory.topInventory.setItem(fEmpty + 5, clickedItem)

                        tradeStatus[player] = 0
                        tradeStatus[target] = 0
                        player.openInventory.topInventory.setItem(45, acceptItem)
                        player.openInventory.topInventory.setItem(53, targetNeutralItem)
                        target.openInventory.topInventory.setItem(45, acceptItem)
                        target.openInventory.topInventory.setItem(53, targetNeutralItem)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.title() == MenuTitle) {
            val slots = event.rawSlots
            var containsTradeMenu = false
            for (i in slots) {
                if(i <= 53) {
                    containsTradeMenu = true
                    break
                }
            }
            if(containsTradeMenu) event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as Player
        if (event.view.title() ==  MenuTitle) {
            val target = ongoingTrades[player] as Player
            if ((tradeStatus[player] != 1 && tradeStatus[target] != 1) && (tradeStatus[player] != -1 && tradeStatus[target] != -1)) {
                tradeFail(player, target, true)
            }
        }
    }

    private fun tradeAcc (player : Player, target : Player) {
        val pinv = player.openInventory.topInventory
        val tinv = target.openInventory.topInventory

        for (i: Int in 1..4) {
            for (j: Int in 0..3) {
                val currTItem = tinv.getItem(i * 9 + j)
                if (currTItem != null && currTItem.type != Material.AIR)
                    player.openInventory.bottomInventory.addItem(currTItem)

                val currPItem = pinv.getItem(i * 9 + j)
                if (currPItem != null && currPItem.type != Material.AIR)
                    target.openInventory.bottomInventory.addItem(currPItem)
            }
        }

        player.sendMessage(ChatColor.GREEN.toString() + "The trade has been accomplished!")
        target.sendMessage(ChatColor.GREEN.toString() + "The trade has been accomplished!")
        player.closeInventory()
        target.closeInventory()
        ongoingTrades.remove(player)
        ongoingTrades.remove(target)
        tradeStatus.remove(player)
        tradeStatus.remove(target)
    }

    private fun tradeFail (player : Player, target : Player, oIC : Boolean) {
        val pinv = player.openInventory.topInventory
        val tinv = target.openInventory.topInventory

        for (i: Int in 1..4) {
            for (j: Int in 0..3) {
                val currTItem = tinv.getItem(i * 9 + j)
                if (currTItem != null && currTItem.type != Material.AIR)
                    target.openInventory.bottomInventory.addItem(currTItem)

                val currPItem = pinv.getItem(i * 9 + j)
                if (currPItem != null && currPItem.type != Material.AIR)
                    player.openInventory.bottomInventory.addItem(currPItem)
            }
        }

        player.sendMessage(ChatColor.RED.toString() + "The trade has failed!")
        target.sendMessage(ChatColor.RED.toString() + "The trade has failed!")
        tradeStatus[player] = -1
        tradeStatus[target] = -1
        if(!oIC) player.closeInventory()
        target.closeInventory()
        ongoingTrades.remove(player)
        ongoingTrades.remove(target)
        tradeStatus.remove(player)
        tradeStatus.remove(target)
    }
}