/*
    Author forest611,takatronix
 */


package red.man10.realestate

import org.bukkit.Material
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10bank.BankAPI
import red.man10.realestate.menu.InventoryListener
import red.man10.realestate.region.City
import red.man10.realestate.region.Event
import red.man10.realestate.region.Region
import red.man10.realestate.region.User
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue


class Plugin : JavaPlugin(), Listener {

    companion object{

        lateinit var offlineBank : BankAPI

        lateinit var es : ExecutorService

        lateinit var vault : VaultManager

        lateinit var plugin: Plugin

        lateinit var votingDiamond : ItemStack

        const val WAND_NAME = "範囲指定ワンド"

        var prefix = "[§5Man10RealEstate§f]"

        //キューにクエリを入れる
        val mysqlQueue = LinkedBlockingQueue<String>()

        //保護を無効にするワールド
        var disableWorld = mutableListOf<String>()

        var maxBalance = 100000000.0

        var teleportPrice = 1000.0

        var setTPPrice = 10000.0

//        var setOwnerPrice = 10000.0

        var defaultPrice = 2000000.0

        var serverName = "paper"

    }

    override fun onEnable() { // Plugin startup logic
        logger.info("Man10 Real Estate plugin enabled.")
        saveDefaultConfig()

        es = Executors.newCachedThreadPool()//スレッドプールを作成、必要に応じて新規スレッドを作成
        vault = VaultManager(this)

        try {
            offlineBank = BankAPI(this)
        }catch (e:Exception){
            logger.warning("Man10OfflineBankが入っていません")
            logger.warning(e.message)
        }


        plugin = this

        disableWorld = config.getStringList("disableWorld")
        maxBalance = config.getDouble("maxBalance",100000000.0)
        teleportPrice = config.getDouble("teleportPrice",1000.0)
        defaultPrice = config.getDouble("defaultPrice",2000000.0)
        serverName = config.getString("server","paper")!!
        votingDiamond = config.getItemStack("votingDiamond")?:ItemStack(Material.DIAMOND)

        saveResource("config.yml", false)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(Event, this)
        server.pluginManager.registerEvents(InventoryListener,this)

        getCommand("mre")!!.setExecutor(Command)
        getCommand("mreop")!!.setExecutor(Command)



        mysqlQueue()

        Region.load()
        City.load()


        //賃料スレッド
        es.execute {


            var isRent = false
            var isTax = false
            var isTaxMail = false

            while (true){

                val time = Calendar.getInstance()

                val day = time.get(Calendar.DAY_OF_MONTH)
                val minute = time.get(Calendar.MINUTE)
                val hour = time.get(Calendar.HOUR_OF_DAY)


                //賃料(一日一回)
                if (minute == 0 && hour == 0  && !isRent){

                    User.rent()
                    isRent = true
                }else if(minute != 0){
                    isRent = false
                }

                //税金(月イチ8時)
                if (minute == 0 && hour == 8 && day == 1 && !isTax){
                    User.tax()
                    isTax = true
                }else if (minute !=0){
                    isTax = false
                }

                //税金メール(25日9時)
                if (minute == 0 && hour == 9 && day == 25 && !isTaxMail){
                    User.taxMail()
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

    ////////////////////////
    //dbのクエリキュー
    ////////////////////////
    private fun mysqlQueue(){

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