package foundation.esoteric.fireworkwarslobby.listeners

import foundation.esoteric.fireworkwarscore.interfaces.Event
import foundation.esoteric.fireworkwarslobby.FireworkWarsLobbyPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener(private val plugin: FireworkWarsLobbyPlugin) : Event {
    private val lobbyWorld = plugin.configManager.lobbyConfig.getWorld()

    override fun register() {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        if (player.world.uid != lobbyWorld.uid) {
            return
        }

        plugin.npcManager.npcList.forEach { it.sendInitPackets(player) }

        //todo: language manager pls

        lobbyWorld.players.forEach {
            it.sendMessage("${player.name} has joined the lobby!")

            if (it == player) {
                it.sendMessage("Welcome to Firework Wars!")
            }
        }

        event.joinMessage(null)
    }
}