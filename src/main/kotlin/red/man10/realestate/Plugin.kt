package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10offlinebank.BankAPI
import red.man10.realestate.menu.CustomInventory
import red.man10.realestate.menu.InventoryListener
import red.man10.realestate.region.City
import red.man10.realestate.region.Event
import red.man10.realestate.region.Region
import red.man10.realestate.region.User
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*


class Plugin : JavaPlugin(), Listener {


    var wandStartLocation: Location? = null
    var wandEndLocation: Location? = null
    var particleTime:Int = 0

    lateinit var es : ExecutorService

    companion object{

        lateinit var offlineBank : BankAPI

        lateinit var region : Region
        lateinit var user : User
        lateinit var city : City

        lateinit var vault : VaultManager

        lateinit var customInventory : CustomInventory

        lateinit var plugin: Plugin

        const val WAND_NAME = "範囲指定ワンド"

        var prefix = "[§5Man10RealEstate§f]"

        //キューにクエリを入れる
        val mysqlQueue = LinkedBlockingQueue<String>()

        //保護を無効にするワールド
        var disableWorld = mutableListOf<String>()

        var maxBalance = 100000000.0

        val numbers = mutableListOf<Int>()

        var taxTimer = false

    }

    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled.")
        saveDefaultConfig()

        es = Executors.newCachedThreadPool()//スレッドプールを作成、必要に応じて新規スレッドを作成
        vault = VaultManager(this)
        offlineBank = BankAPI(this)
        region = Region(this)
        user = User(this)
        city = City(this)

        customInventory = CustomInventory(this)

        plugin = this

        disableWorld = config.getStringList("disableWorld")
        maxBalance = config.getDouble("maxBalance",100000000.0)
        taxTimer = config.getBoolean("taxTimer",false)

        saveResource("config.yml", false)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(Event(this), this)
        server.pluginManager.registerEvents(InventoryListener(),this)

        getCommand("mre")!!.setExecutor(Command())
        getCommand("mreop")!!.setExecutor(Command())

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            if(wandStartLocation != null && wandEndLocation != null){

                drawCube(wandStartLocation!!,wandEndLocation!!)
            }
            particleTime++

        },0,10)

        mysqlQueue()

        region.load()
        city.load()


        //賃料スレッド
        es.execute {
//            while (true){
//                user.rentTimer()
//                Thread.sleep(60000)
//            }

            var ranRentTimer = false

            while (true){
                val time = Calendar.getInstance()

                if (time.get(Calendar.MINUTE) == 0 && time.get(Calendar.HOUR) == 0  && !ranRentTimer){
                    Bukkit.getLogger().info("Running rent timer")
                    user.rentTimer()
                    Bukkit.getLogger().info("Ran rent timer")
                    ranRentTimer = true
                }else if(time.get(Calendar.MINUTE) != 0 || time.get(Calendar.HOUR_OF_DAY) != 0){
                    ranRentTimer= false
                }
                Thread.sleep(10000)

            }
        }

    }

    override fun onDisable() { // Plugin shutdown logic

        //起動中のスレッドを全て止める
        try {
            es.shutdownNow()

        }catch (e:InterruptedException){
            logger.info(e.message)
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


    ////////////////////////
    //dbのクエリキュー
    ////////////////////////
    fun mysqlQueue(){

        es.execute {
            val sql = MySQLManager(this,"man10realestate queue")
            try{
                while (true){
                    val take = mysqlQueue.take()
                    sql.execute(take)
                }
            }catch (e:InterruptedException){
            }
        }
    }

}