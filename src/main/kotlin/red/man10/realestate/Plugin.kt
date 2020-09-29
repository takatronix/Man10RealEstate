/*
    Author forest611,takatronix
 */


package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10offlinebank.BankAPI
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.fly.Fly
import red.man10.realestate.menu.CustomInventory
import red.man10.realestate.menu.InventoryListener
import red.man10.realestate.region.City
import red.man10.realestate.region.Event
import red.man10.realestate.region.Region
import red.man10.realestate.region.User
import red.man10.realestate.storage.Barrel
import red.man10.realestate.storage.BarrelEvent
import java.util.*
import java.util.concurrent.*


class Plugin : JavaPlugin(), Listener {


//    var wandStartLocation: Location? = null
//    var wandEndLocation: Location? = null
//    var particleTime:Int = 0

    companion object{

        lateinit var offlineBank : BankAPI

        lateinit var es : ExecutorService

        lateinit var region : Region
        lateinit var user : User
        lateinit var city : City

        lateinit var barrel : Barrel

        lateinit var fly: Fly

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

        var teleportPrice = 1000.0

        var defaultPrice = 400000.0

        val numbers = mutableListOf<Int>()

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

        barrel = Barrel()

        fly = Fly()

        customInventory = CustomInventory(this)

        plugin = this

        disableWorld = config.getStringList("disableWorld")
        maxBalance = config.getDouble("maxBalance",100000000.0)
        teleportPrice = config.getDouble("teleportPrice",1000.0)

        saveResource("config.yml", false)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(Event(this), this)
        server.pluginManager.registerEvents(InventoryListener(),this)
        server.pluginManager.registerEvents(BarrelEvent(),this)

        getCommand("mre")!!.setExecutor(Command())
        getCommand("mreop")!!.setExecutor(Command())

//        Bukkit.getScheduler().runTaskTimer(this, Runnable {
//            if(wandStartLocation != null && wandEndLocation != null){
//
//                drawCube(wandStartLocation!!,wandEndLocation!!)
//            }
//            particleTime++
//
//        },0,10)

        mysqlQueue()

        region.load()
        city.load()


        //賃料スレッド
        es.execute {


            var isRent = false
            var isTax = false
            var isTaxMail = false
            var isCheckPermission = false

            while (true){

                fly.checkFly()

                val time = Calendar.getInstance()

                val day = time.get(Calendar.DAY_OF_MONTH)
                val minute = time.get(Calendar.MINUTE)
                val hour = time.get(Calendar.HOUR_OF_DAY)

                //権限チェック(1時間に一度)
                if (minute == 0 && !isCheckPermission){

                    for (rg in region.map()){

                        val p = Bukkit.getPlayer(rg.value.ownerUUID?:continue)?:continue

                        if (!city.hasCityPermission(p,rg.key)){
                            sendMessage(p,"§c§lあなたはID:${rg.key}の土地に住むことができなくなりました")
                            region.initRegion(rg.key,defaultPrice)
                        }

                    }

                    isCheckPermission = true
                }else if (minute!=0){
                    isCheckPermission = false
                }

                //賃料(一日一回)
                if (minute == 0 && hour == 0  && !isRent){

                    user.rent()
                    isRent = true
                }else if(minute != 0){
                    isRent = false
                }

                //税金(月イチ8時)
                if (minute == 0 && hour == 8 && day == 1 && !isTax){
                    user.tax()
                    isTax = true
                }else if (minute !=0){
                    isTax = false
                }

                //税金メール(25日9時)
                if (minute == 0 && hour == 9 && day == 25 && !isTaxMail){
                    user.taxMail()
                    isTaxMail = true
                }else if (minute != 0){
                    isTaxMail = false
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

//    fun drawCube(pos1:Location,pos2:Location){
//        getCube(pos1,pos2)?.forEach { ele->
//            ele.world.spawnParticle(Particle.HEART, ele.getX(), ele.getY(), ele.getZ(), 1)
//        }
//
//    }
//
//    fun getCube(corner1: Location, corner2: Location): List<Location>? {
//        val result: MutableList<Location> = ArrayList()
//        val world = corner1.world
//        val minX = Math.min(corner1.x, corner2.x)
//        val minY = Math.min(corner1.y, corner2.y)
//        val minZ = Math.min(corner1.z, corner2.z)
//        val maxX = Math.max(corner1.x, corner2.x)
//        val maxY = Math.max(corner1.y, corner2.y)
//        val maxZ = Math.max(corner1.z, corner2.z)
//        var x = minX
//        while (x <= maxX) {
//            var y = minY
//            while (y <= maxY) {
//                var z = minZ
//                while (z <= maxZ) {
//                    var components = 0
//                    if (x == minX || x == maxX) components++
//                    if (y == minY || y == maxY) components++
//                    if (z == minZ || z == maxZ) components++
//                    if (components >= 2) {
//                        result.add(Location(world, x, y, z))
//                    }
//                    z++
//                }
//                y++
//            }
//            x++
//        }
//        return result
//    }


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