package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class Plugin : JavaPlugin(), Listener {
    var prefix = "[§5Man10RealEstate§f]"

    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled")
        server.pluginManager.registerEvents(this, this)
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