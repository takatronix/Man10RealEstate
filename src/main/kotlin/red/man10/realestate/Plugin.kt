/*
    Author forest611,takatronix
 */


package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10bank.BankAPI
import red.man10.realestate.menu.InventoryListener
import red.man10.realestate.region.CityOld
import red.man10.realestate.region.Event
import red.man10.realestate.region.RegionOld
import red.man10.realestate.region.UserOld
import red.man10.realestate.util.MySQLManager
import java.util.*
import java.util.concurrent.Executors


class Plugin : JavaPlugin(), Listener {

    companion object{

        lateinit var bank : BankAPI
        lateinit var vault : VaultManager
        lateinit var plugin: Plugin

        val async = Executors.newSingleThreadExecutor()

        const val WAND_NAME = "範囲指定ワンド"
        const val prefix = "[§5Man10RealEstate§f]"

        //保護を無効にするワールド
        var disableWorld = mutableListOf<String>()

        var serverName = "paper"

    }

    private var lastTax = Date()
    private var lastRent = Date()

    override fun onEnable() { // Plugin startup logic
        saveDefaultConfig()

        vault = VaultManager(this)
        bank = BankAPI(this)
        plugin = this

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(Event, this)
        server.pluginManager.registerEvents(InventoryListener,this)

        getCommand("mre")!!.setExecutor(Command)
        getCommand("mreop")!!.setExecutor(Command)

        loadConfig()
        MySQLManager.mysqlQueue(this)

        CityOld.load()
        RegionOld.load()

        batchSchedule()

    }

    private fun batchSchedule(){
        Bukkit.getScheduler().runTaskAsynchronously(this,Runnable {

            val now = Calendar.getInstance()

            while (true){

                now.time = Date()

                val rent = Calendar.getInstance()
                rent.time = lastRent

                //賃料の支払い処理、日付が変更されたタイミングで走る
                if (now.get(Calendar.DAY_OF_MONTH) != rent.get(Calendar.DAY_OF_MONTH)){
                    UserOld.rent()
                    lastRent = Date()
                    config.set("lastRent",lastRent.time)
                }

                val tax = Calendar.getInstance()
                tax.time = lastTax

                //税金の支払い処理、月が変わったタイミングで走る
                if (now.get(Calendar.MONTH) != tax.get(Calendar.MONTH)){
                    UserOld.tax()
                    lastTax = Date()
                    config.set("lastTax",lastTax.time)
                }

                saveConfig()
                Thread.sleep(100000)
            }
        })

    }

    fun loadConfig(){
        reloadConfig()

        disableWorld = config.getStringList("disableWorld")
        serverName = config.getString("server","paper")!!
        lastTax.time = config.getLong("lastTax")
        lastRent.time = config.getLong("lastRent")
        Event.maxContainers = config.getInt("containerAmount",24)

        saveResource("config.yml", false)

    }

    override fun onDisable() { // Plugin shutdown logic
//        es.shutdownNow()
        Bukkit.getScheduler().cancelTasks(this)
    }


}