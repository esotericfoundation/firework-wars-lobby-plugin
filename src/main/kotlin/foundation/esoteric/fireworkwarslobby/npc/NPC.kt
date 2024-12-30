package foundation.esoteric.fireworkwarslobby.npc

import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import foundation.esoteric.fireworkwarscore.util.NMSUtil
import foundation.esoteric.fireworkwarscore.util.format
import foundation.esoteric.fireworkwarslobby.FireworkWarsLobbyPlugin
import foundation.esoteric.fireworkwarslobby.config.structure.NPCData
import foundation.esoteric.fireworkwarslobby.npc.connection.EmptyConnection
import foundation.esoteric.fireworkwarslobby.util.PacketUtil
import net.minecraft.network.protocol.game.*
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.createAddOrModifyPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.createPlayerPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import java.util.*
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class NPC(private val plugin: FireworkWarsLobbyPlugin, val data: NPCData) {
    private val npcLocation = data.location.toBukkit()

    private val world = npcLocation.world
    private val nmsWorld = NMSUtil.toNMSWorld(world)

    private val handle: ServerPlayer
    val id: Int

    private val nameTag: TextDisplay

    private val scoreboard = Scoreboard()
    private val team = scoreboard.addPlayerTeam("lobby_npc_${UUID.randomUUID()}")

    init {
        this.handle = createServerPlayer()
        this.id = handle.id
        this.nameTag = createNameTag()
    }

    private fun createServerPlayer(): ServerPlayer {
        val profile = plugin.server.createProfile(UUID.randomUUID()) as CraftPlayerProfile
        val texturesProperty = ProfileProperty("textures", data.skin.value, data.skin.signature)

        profile.properties.add(texturesProperty)

        val npc = ServerPlayer(
            nmsWorld.server!!,
            nmsWorld as ServerLevel,
            profile.gameProfile,
            ClientInformation.createDefault())

        val entityData = npc.entityData
        val bitmask = (0x01 or 0x04 or 0x08 or 0x10 or 0x20 or 0x40).toByte()

        entityData.set(EntityDataAccessor(17, EntityDataSerializers.BYTE), bitmask)

        npc.connection = ServerGamePacketListenerImpl(
            npc.server,
            EmptyConnection(null),
            npc,
            CommonListenerCookie.createInitial(npc.getGameProfile(), false))

        return npc
    }

    private fun createNameTag(): TextDisplay {
        val display = world.spawn(npcLocation, TextDisplay::class.java)

        display.text(data.name.format().appendNewline().append(data.subtitle.format()))
        display.alignment = TextDisplay.TextAlignment.CENTER
        display.billboard = Display.Billboard.VERTICAL

        val location = Location(world, npcLocation.x, npcLocation.y + 2.0F, npcLocation.z)
        display.teleport(location)

        return display
    }

    fun sendInitPackets(player: Player) {
        val connection: ServerGamePacketListenerImpl = NMSUtil.toNMSEntity<ServerPlayer>(player).connection

        this.updateScoreboardTeamSettings()

        handle.setPos(npcLocation.x, npcLocation.y, npcLocation.z)

        connection.send(ClientboundPlayerInfoUpdatePacket(Action.ADD_PLAYER, handle))
        connection.send(PacketUtil.getEntityAddPacket(handle))
        connection.send(ClientboundSetEntityDataPacket(id, handle.entityData.packAll()!!))

        connection.send(createAddOrModifyPacket(team, true))
        connection.send(createPlayerPacket(team, handle.scoreboardName, ClientboundSetPlayerTeamPacket.Action.ADD))

        plugin.runTaskLater({
            connection.send(ClientboundPlayerInfoRemovePacket(listOf(handle.uuid)))
        }, 20L)
    }

    private fun updateScoreboardTeamSettings() {
        scoreboard.addPlayerToTeam(handle.scoreboardName, team)

        team.nameTagVisibility = Team.Visibility.NEVER
        team.collisionRule = Team.CollisionRule.NEVER
    }

    fun lookAtEachPlayer() {
        world.players.forEach {
            val playerLocation = it.eyeLocation
            val npcLocation = npcLocation.clone().add(0.0, 1.62, 0.0)

            val playerX = playerLocation.x
            val playerZ = playerLocation.z

            val npcX = npcLocation.x
            val npcZ = npcLocation.z

            val dx = playerX - npcX
            val dz = playerZ - npcZ

            val horizontalDistance = sqrt((dx).pow(2.0) + (dz).pow(2.0))

            val yaw = 90.0 - Math.toDegrees(atan2(dz, npcX - playerX))
            val pitch = -Math.toDegrees(atan2(playerLocation.y - npcLocation.y, horizontalDistance))

            val nmsPlayer = NMSUtil.toNMSEntity<ServerPlayer>(it)

            nmsPlayer.connection.send(ClientboundRotateHeadPacket(
                handle,
                (yaw * 256.0 / 360.0).toInt().toByte()))

            nmsPlayer.connection.send(ClientboundMoveEntityPacket.Rot(
                handle.id,
                (yaw * 256.0 / 360.0).toInt().toByte(),
                (pitch * 256.0 / 360.0).toInt().toByte(),
                true))
        }
    }
}