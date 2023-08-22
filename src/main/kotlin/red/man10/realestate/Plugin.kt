/*
    Author forest611,takatronix
 */


package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10bank.BankAPI
import red.man10.realestate.region.*
import red.man10.realestate.util.MenuFramework
import red.man10.realestate.util.MySQLManager
import java.time.DayOfWeek
import java.time.LocalDateTime
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
        var penalty = 2.5 //税金の支払いに失敗した時のペナルティ

    }

    override fun onEnable() { // Plugin startup logic
        saveDefaultConfig()

        vault = VaultManager(this)
        bank = BankAPI(this)
        plugin = this
        MenuFramework.setup(this)

        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(Event, this)
        server.pluginManager.registerEvents(MenuFramework.MenuListener,this)

        getCommand("mre")!!.setExecutor(Command)
        getCommand("mreop")!!.setExecutor(Command)

        loadConfig()
        MySQLManager.mysqlQueue(this)

        City.asyncLoad()
        Region.asyncLoad()
        User.asyncLoad()

        runDailyTask()

    }

    override fun onDisable() { // Plugin shutdown logic
        async.shutdown()
    }

    fun loadConfig(){
        reloadConfig()

        disableWorld = config.getStringList("disableWorld")
        serverName = config.getString("server","paper")!!
        penalty = config.getDouble("penalty",2.5)
        Event.maxContainers = config.getInt("containerAmount",24)
        saveResource("config.yml", false)
    }

    private fun runDailyTask(){

        Thread{

            var lastDay = LocalDateTime.now()
            var lastMonth = LocalDateTime.now()

            while (true){

                val now = LocalDateTime.now()
                val isChangeDay = lastDay.dayOfYear != now.dayOfYear
                val isChangeMonth = lastMonth.month != now.month

                //日変更
                if (isChangeDay){
                    Bukkit.getLogger().info("日付の変更を検知！")
                    lastDay = LocalDateTime.now()
                    Region.regionData.filterValues { it.span == 2 }.values.forEach { it.payRent() }
                }

                //週変更(月曜日)
                if (isChangeDay && now.dayOfWeek == DayOfWeek.MONDAY){
                    Bukkit.getLogger().info("週の変更を検知！")
                    Region.regionData.filterValues { it.span == 1 }.values.forEach { it.payRent() }
                }

                //月変更
                if (isChangeMonth){
                    Bukkit.getLogger().info("月の変更を検知！")
                    lastMonth = LocalDateTime.now()
                    Region.regionData.filterValues { it.span == 0 }.values.forEach { it.payRent() }
                    City.payTax()
                }

                Thread.sleep(1000*60)
            }
        }.start()

    }

}