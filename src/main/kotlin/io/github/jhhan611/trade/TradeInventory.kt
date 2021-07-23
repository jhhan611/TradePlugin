package io.github.jhhan611.trade

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class TradeInventory (_player : Player, _target :Player) {
    private val inv = Bukkit.createInventory(null, 54, MenuTitle)
    private val player: Player = _player
    private val target: Player = _target

    init {
        for (i: Int in 0..8) {
            inv.setItem(i + 45, topItem)
            inv.setItem(i, topItem)
        }
        for (i: Int in 0..5)
            inv.setItem(i * 9 + 4, middleItem)
        for (i: Int in 1..4) {
            inv.setItem(i * 9 + 5, targetSlotItem)
            inv.setItem(i * 9 + 6, targetSlotItem)
            inv.setItem(i * 9 + 7, targetSlotItem)
            inv.setItem(i * 9 + 8, targetSlotItem)
        }
        inv.setItem(45, acceptItem)
        inv.setItem(48, cancelItem)
        inv.setItem(53, targetNeutralItem)
        val skull = ItemStack(Material.PLAYER_HEAD, 1)
        val meta = Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD) as SkullMeta
        meta.owningPlayer = player
        meta.displayName(Component.text(player.name))
        skull.itemMeta = meta
        inv.setItem(0, skull)
        meta.owningPlayer = target
        meta.displayName(Component.text(target.name))
        skull.itemMeta = meta
        inv.setItem(8, skull)
    }

    fun openInv() {
        player.openInventory(inv)
    }
}
