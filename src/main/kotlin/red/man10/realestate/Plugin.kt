package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import red.man10.realestate.region.Commands
import red.man10.realestate.region.RegionEvent

class Plugin : JavaPlugin(), Listener {
    var prefix = "[§5Man10RealEstate§f]"

    lateinit var regionEvent: RegionEvent
    lateinit var cmd : Commands

    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled.")

        regionEvent = RegionEvent(this)
        cmd = Commands(this)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(regionEvent,this)

        getCommand("mre")!!.setExecutor(cmd)

        saveResource("config.yml", false)
    }

    override fun onDisable() { // Plugin shutdown logic
    }

    fun broadcast(message: String) {
        Bukkit.broadcastMessage("$prefix $message")
    }
    fun sendMessage(player: Player, message: String) {
        player.sendMessage("$prefix $message")
    }
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        this.broadcast("${e.player.displayName} is joined.")
    }

}