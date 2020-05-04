package red.man10.realestate

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.region.ProtectRegionEvent
import red.man10.realestate.region.RegionDatabase
import red.man10.realestate.region.RegionEvent
import red.man10.realestate.region.RegionUserDatabase
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap


class Plugin : JavaPlugin(), Listener {
    var prefix = "[§5Man10RealEstate§f]"

    lateinit var regionEvent: RegionEvent
    lateinit var protectEvent: ProtectRegionEvent
    lateinit var cmd : Commands

    var wandStartLocation: Location? = null
    var wandEndLocation: Location? = null
    var particleTime:Int = 0

    val vault = VaultManager(this)

    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled.")
        saveDefaultConfig()

        regionEvent = RegionEvent(this)
        protectEvent = ProtectRegionEvent(this)
        cmd = Commands(this)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(regionEvent,this)
        server.pluginManager.registerEvents(protectEvent,this)
        server.pluginManager.registerEvents(InventoryMenu(this),this)

        getCommand("mre")!!.setExecutor(cmd)
        getCommand("mreop")!!.setExecutor(cmd)

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

        RegionDatabase(this).loadRegion()

        mysqlQueue()
        rentTimer()

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

    //  マインクラフトチャットに、ホバーテキストや、クリックコマンドを設定する関数
    // [例1] sendHoverText(player,"ここをクリック",null,"/say おはまん");
    // [例2] sendHoverText(player,"カーソルをあわせて","ヘルプメッセージとか",null);
    // [例3] sendHoverText(player,"カーソルをあわせてクリック","ヘルプメッセージとか","/say おはまん");
    fun sendHoverText(p: Player, text: String, hoverText: String, command: String) {
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        val hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(hoverText).create())

        //////////////////////////////////////////
        //   クリックイベントを作成する
        val clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$command")
        val message = ComponentBuilder(text).event(hoverEvent).event(clickEvent).create()
        p.spigot().sendMessage(*message)
    }

    ////////////////////////
    //dbのクエリキュー
    ////////////////////////
    fun mysqlQueue(){
        Thread(Runnable {
            try{
                val sql = MySQLManager(this,"man10realestate queue")
                while (true){
                    val take = Constants.mysqlQueue.take()
                    sql.execute(take)
                }
            }catch (e:InterruptedException){

            }
        }).start()
    }

    /////////////////////////////////////
    //貸出のタイマー(期限が過ぎたらロックされる)
    //////////////////////////////////////
    fun rentTimer(){
        Thread(Runnable {

            for (pd in regionUserData){

                val data = regionData[pd.key.second]?:continue
                val pdata = pd.value

                if (!pdata.isRent)continue

                val different = (Date().time - pdata.paid.time)/1000/3600/24

                if (data.span == 0 && different < 30)continue
                if (data.span == 1 && different < 7)continue
                if (data.span == 2 && different < 1)continue

                if (pdata.deposit <data.rent){
                    if (pd.key.first.isOnline){
                        sendMessage(pd.key.first,"${data.name}§3§lの賃料が支払えません！支払えるまでロックされます！")
                    }
                    pdata.statsu = "Lock"
                }else{
                    pdata.deposit -= data.rent
                    pdata.paid = Date()
                }

                regionUserData[pd.key] = pdata
                RegionUserDatabase(this).saveUserData(pd.key.first,pd.key.second)
            }

            Thread.sleep(3600000)
        }).start()
    }
}