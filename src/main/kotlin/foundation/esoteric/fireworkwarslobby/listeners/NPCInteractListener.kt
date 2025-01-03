package foundation.esoteric.fireworkwarslobby.listeners

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import foundation.esoteric.fireworkwarscore.interfaces.Event
import foundation.esoteric.fireworkwarscore.language.Message
import foundation.esoteric.fireworkwarscore.util.format
import foundation.esoteric.fireworkwarscore.util.playSound
import foundation.esoteric.fireworkwarslobby.FireworkWarsLobbyPlugin
import foundation.esoteric.fireworkwarslobby.config.structure.MapType
import foundation.esoteric.fireworkwarslobby.util.lore
import foundation.esoteric.fireworkwarslobby.util.name
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.EquipmentSlot

class NPCInteractListener(private val plugin: FireworkWarsLobbyPlugin) : Event {
    override fun register() {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onNPCInteract(event: PlayerUseUnknownEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        if (event.clickedRelativePosition != null) {
            return
        }

        val fireworkWarsData = plugin.core.fireworkWarsPluginData

        val npc = plugin.npcManager.getNPC(event.entityId)
        val menuData = npc.data.menu

        val gui = Gui.gui()
            .title(menuData.title.format())
            .rows(3)
            .create()

        gui.setOpenGuiAction {
            val player = it.player as Player

            plugin.runTaskLater({ player.playSound(Sound.ITEM_CROSSBOW_QUICK_CHARGE_3) }, 2L)
            plugin.runTaskLater({ player.playSound(Sound.ITEM_CROSSBOW_LOADING_END) }, 10L)
        }

        gui.setDefaultClickAction {
            it.isCancelled = true

            val player = it.whoClicked as Player
            player.closeInventory(InventoryCloseEvent.Reason.PLUGIN)
        }

        val arenas = when (menuData.mapType) {
            MapType.BARRACKS -> fireworkWarsData.getBarracksArenas()
            MapType.TOWN -> fireworkWarsData.getTownArenas()
        }

        val filtered = arenas.filter { !it.isFull() }

        if (filtered.isEmpty()) {
            val item = ItemBuilder.from(Material.GUNPOWDER)
                .name(Message.NO_ARENAS_AVAILABLE)
                .lore(Message.NO_ARENAS_AVAILABLE_LORE)
                .asGuiItem()

            gui.setItem(0, item)
        }

        filtered.forEachIndexed { index, arena ->
            val current = arena.getCurrentPlayers()
            val max = arena.getMaxPlayers()

            val playerCount = plugin.languageManager
                .getMessage(Message.CURRENT_PLAYERS, event.player, current, max)

            val lore = arena.getDescription()
                .split("\n")
                .map { it.format() }
                .toMutableList()
                .apply { add(playerCount) }
                .toList()

            val item = ItemBuilder.from(Material.PAPER)
                .name(arena.getName().format())
                .lore(lore)
                .asGuiItem {
                    fireworkWarsData
                        .getArenaJoinCommand()
                        .executeJoinForPlayer(it.whoClicked as Player, arena.getArenaNumber())
                }

            gui.setItem(index, item)
        }

        gui.open(event.player)
    }
}