package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import red.man10.realestate.region.Commands
import red.man10.realestate.region.RegionEvent
import java.util.*


class Plugin : JavaPlugin(), Listener {
    var prefix = "[§5Man10RealEstate§f]"

    lateinit var regionEvent: RegionEvent
    lateinit var cmd : Commands

    var wandStartLocation: Location? = null;
    var wandEndLocation: Location? = null;
    var particleTime:Int = 0;

    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled.")

        regionEvent = RegionEvent(this)
        cmd = Commands(this)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(regionEvent,this)

        getCommand("mre")!!.setExecutor(cmd)

        saveResource("config.yml", false)

        object : BukkitRunnable() {
            override fun run() {
               //  broadcast("timer")
                if(wandStartLocation != null && wandEndLocation != null){

                    drawCube(wandStartLocation!!,wandEndLocation!!)
                }
                particleTime++;
            }
        }.runTaskTimer(this, 0, 10)

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


    fun drawLine(point1: Location, point2: Location, space: Double) {
        //broadcast("draw {${point1.toString()}- {${point2.toString()}}}")
        val world = point1.world
        val distance = point1.distance(point2)
        val p1 = point1.toVector()
        val p2 = point2.toVector()
        val vector = p2.clone().subtract(p1).normalize().multiply(space)
        var length = 0.0
        while (length < distance) {
            world.spawnParticle(Particle.HEART, p1.getX(), p1.getY(), p1.getZ(), 1)
            length += space
            p1.add(vector)
        }
    }

    fun drawCube(pos1:Location,pos2:Location){
        getCube(pos1,pos2)?.forEach { ele->
            ele.world.spawnParticle(Particle.HEART, ele.getX(), ele.getY(), ele.getZ(), 1)
        }
    }

    fun getCube(corner1: Location, corner2: Location): List<Location>? {
        val result: MutableList<Location> = ArrayList()
        val world = corner1.world
        val minX = Math.min(corner1.x, corner2.x)
        val minY = Math.min(corner1.y, corner2.y)
        val minZ = Math.min(corner1.z, corner2.z)
        val maxX = Math.max(corner1.x, corner2.x)
        val maxY = Math.max(corner1.y, corner2.y)
        val maxZ = Math.max(corner1.z, corner2.z)
        var x = minX
        while (x <= maxX) {
            var y = minY
            while (y <= maxY) {
                var z = minZ
                while (z <= maxZ) {
                    var components = 0
                    if (x == minX || x == maxX) components++
                    if (y == minY || y == maxY) components++
                    if (z == minZ || z == maxZ) components++
                    if (components >= 2) {
                        result.add(Location(world, x, y, z))
                    }
                    z++
                }
                y++
            }
            x++
        }
        return result
    }
}