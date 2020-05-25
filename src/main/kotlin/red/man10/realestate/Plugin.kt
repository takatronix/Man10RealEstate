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
import red.man10.realestate.menu.InventoryMenu
import red.man10.realestate.menu.OwnerMenu
import red.man10.realestate.region.ProtectRegionEvent
import red.man10.realestate.region.RegionDatabase
import red.man10.realestate.region.RegionEvent
import red.man10.realestate.region.RegionUserDatabase
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue


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

    companion object{

        lateinit var regionDatabase :RegionDatabase
        lateinit var regionUserDatabase : RegionUserDatabase


        const val WAND_NAME = "範囲指定ワンド"

        var prefix = "[§5Man10RealEstate§f]"

        //リージョンのデータ
        val regionData = ConcurrentHashMap<Int, RegionDatabase.RegionData>()
        //プレイヤーごとのリージョン情報
        val regionUserData = ConcurrentHashMap<Player, HashMap<Int,RegionUserDatabase.RegionUserData>>()
        //worldごとのリージョンid <ワールド名,ワールドないにあるリージョンのidのlist>
        val worldRegion = HashMap<String,MutableList<Int>>()

        val mysqlQueue = LinkedBlockingQueue<String>()

        val likedRegion = HashMap<Player,MutableList<Int>>()//いいねをしたリージョン

        val ownerData = HashMap<Player,MutableList<Int>>()//管理できるリージョン

        var disableWorld = mutableListOf<String>()

        var maxBalance = 100000000.0


        val numbers = mutableListOf<Int>()

        //  マインクラフトチャットに、ホバーテキストや、クリックコマンドを設定する関数
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

        //サジェストメッセージ
        fun sendSuggest(p: Player, text: String?, command: String?) {

            //////////////////////////////////////////
            //   クリックイベントを作成する
            var clickEvent: ClickEvent? = null
            if (command != null) {
                clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)
            }

            val message = ComponentBuilder("$text§a§l[ここをクリックで自動入力！]").event(clickEvent).create()
            p.spigot().sendMessage(*message)
        }

        //prefix付きのメッセージ
        fun sendMessage(player: Player, message: String) {
            player.sendMessage("$prefix $message")
        }


    }

    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled.")
        saveDefaultConfig()

        regionEvent = RegionEvent(this)
        protectEvent = ProtectRegionEvent()
        cmd = Commands(this)
        protocolManager = ProtocolLibrary.getProtocolManager()
        vault = VaultManager(this)
        invmain = InventoryMenu(this)
        ownerInv = OwnerMenu(this)

        regionDatabase = RegionDatabase(this)
        regionUserDatabase = RegionUserDatabase(this)

        disableWorld = config.getStringList("disableWorld")
        maxBalance = config.getDouble("maxBalance",100000000.0)

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

        regionDatabase.loadRegion()

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
            val sql = MySQLManager(this,"man10realestate queue")
            try{
                while (true){
                    val take = mysqlQueue.take()
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

        while (rs.next()){

            val rentPrice = rs.getDouble("rent")

            if (rs.getInt("is_rent") == 0)continue
            if (rentPrice == 0.0)continue

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

                if (vault.getBalance(uuid) <rentPrice){
                    sendMessage(p,"${data.name}§3§lの賃料が支払えません！支払えるまでロックされます！")
                    pd.status = "Lock"
                }else{
                    sendMessage(p,"${data.name}§3§lの賃料の賃料を支払いました！")
                    vault.withdraw(uuid,rentPrice)

                    pd.status = "Share"
                    if (data.owner_uuid != null){
                        regionUserDatabase.addProfit(data.owner_uuid!!,rentPrice)
                    }
                }
                pd.paid = Date()

                regionUserDatabase.saveMap(p,pd,id)
                regionUserDatabase.saveUserData(p,id)
                continue
            }

            //オフラインのとき
            if (vault.getBalance(uuid) < rentPrice){

                mysqlQueue.add("UPDATE `region_user` SET status='Lock' WHERE uuid='$uuid' AND region_id=$id;")
                continue
            }

            vault.withdraw(uuid,rentPrice)
            mysqlQueue.add("UPDATE `region_user` SET paid_date=now(), status='Share' WHERE uuid='$uuid' AND region_id=$id;")
            if (data.owner_uuid != null){
                regionUserDatabase.addProfit(data.owner_uuid!!,rentPrice)
            }

        }
        rs.close()
        mysql.close()
    }
}