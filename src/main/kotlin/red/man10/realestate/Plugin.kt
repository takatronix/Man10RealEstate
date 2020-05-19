package red.man10.realestate

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
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
import red.man10.realestate.Constants.Companion.broadcast
import red.man10.realestate.Constants.Companion.mysqlQueue
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.Constants.Companion.sendMessage
import red.man10.realestate.menu.InventoryMenu
import red.man10.realestate.menu.OwnerMenu
import red.man10.realestate.region.ProtectRegionEvent
import red.man10.realestate.region.RegionDatabase
import red.man10.realestate.region.RegionEvent
import red.man10.realestate.region.RegionUserDatabase
import java.util.*


class Plugin : JavaPlugin(), Listener {

    lateinit var regionEvent: RegionEvent
    lateinit var protectEvent: ProtectRegionEvent
    lateinit var cmd : Commands
    lateinit var protocolManager : ProtocolManager
    lateinit var mysql : MySQLManager
    lateinit var vault : VaultManager
    lateinit var invmain : InventoryMenu
    lateinit var ownerInv : OwnerMenu

    lateinit var sqlThread : Thread

    var wandStartLocation: Location? = null
    var wandEndLocation: Location? = null
    var particleTime:Int = 0
    var debugMode = false


    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled.")
        saveDefaultConfig()

        regionEvent = RegionEvent(this)
        protectEvent = ProtectRegionEvent(this)
        cmd = Commands(this)
        protocolManager = ProtocolLibrary.getProtocolManager()
        vault = VaultManager(this)
        invmain = InventoryMenu(this)
        ownerInv = OwnerMenu(this)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(regionEvent,this)
        server.pluginManager.registerEvents(protectEvent,this)
        server.pluginManager.registerEvents(invmain,this)
        server.pluginManager.registerEvents(ownerInv,this)

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

        mysql = MySQLManager(this,"mreRentThread")


        Thread(Runnable {
            while (true){
                rentTimer()
                Thread.sleep(3600000)
            }
        }).start()

        RegionDatabase(this).loadRegion()

        mysqlQueue()
        rentTimer()

    }

    override fun onDisable() { // Plugin shutdown logic

        sqlThread.interrupt()
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

//        val packet = PacketContainer(PacketType.Play.Server.WORLD_PARTICLES)
//        for (l in getCube(pos1,pos2)?:return){
//            packet.doubles.write(0,pos1.x)
//            packet.doubles.write(1,pos1.y)
//            packet.doubles.write(2,pos1.z)
//            packet.particles.write(0,EnumWrappers.Particle.HEART)
//
//            try {
//                protocolManager.sendServerPacket(p,packet)
//            }catch (e:InvocationTargetException){
//            }
//        }

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


    ////////////////////////
    //dbのクエリキュー
    ////////////////////////
    fun mysqlQueue(){
        sqlThread = Thread(Runnable {
            try{
                val sql = MySQLManager(this,"man10realestate queue")
                while (true){
                    val take = Constants.mysqlQueue.take()
                    sql.execute(take)
                }
            }catch (e:InterruptedException){

            }
        })
        sqlThread.start()
    }

    /////////////////////////////////////
    //貸出のタイマー(期限が過ぎたらロックされる)
    //////////////////////////////////////
    fun rentTimer(){
        val rs = mysql.query("SELECT * FROM region_user;")!!
        val db = RegionUserDatabase(this)

        while (rs.next()){

            if (rs.getInt("is_rent") == 0)continue

            val uuid = UUID.fromString( rs.getString("uuid"))
            val id = rs.getInt("region_id")
            val p = Bukkit.getPlayer(uuid)

            //1時間ごと
            val different = (Date().time - rs.getDate("paid_date").time)/1000/3600/24

            val data = regionData[id]?:continue

            if (!debugMode){
                if (data.span == 0 && different < 30)continue
                if (data.span == 1 && different < 7)continue
                if (data.span == 2 && different < 1)continue
            }

            //ユーザーがオンラインのとき
            if (p != null&&regionUserData[p]!=null){

                val pd = regionUserData[p]!![id]?:continue

                if (vault.getBalance(uuid) <data.rent){
                    sendMessage(p,"${data.name}§3§lの賃料が支払えません！支払えるまでロックされます！")
                    pd.status = "Lock"
                }else{
                    sendMessage(p,"${data.name}§3§lの賃料の賃料を支払いました！")
                    vault.withdraw(uuid,data.rent)

                    pd.status = "Share"
                    if (data.owner_uuid != null){
                        db.addProfit(data.owner_uuid!!,data.rent)
                    }
                }
                pd.paid = Date()

                db.saveMap(p,pd,id)
                db.saveUserData(p,id)
                continue
            }

            //オフラインのとき
            if (vault.getBalance(uuid) < data.rent){

                mysqlQueue.add("UPDATE `region_user` SET status='Lock' WHERE uuid='$uuid' AND region_id=$id;")
                continue
            }

            vault.withdraw(uuid,data.rent)
            mysqlQueue.add("UPDATE `region_user` SET paid_date=now(), status='Share' WHERE uuid='$uuid' AND region_id=$id;")
            if (data.owner_uuid != null){
                db.addProfit(data.owner_uuid!!,data.rent)
            }

        }
        rs.close()
        mysql.close()
    }
}