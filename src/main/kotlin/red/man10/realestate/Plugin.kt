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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class Plugin : JavaPlugin(), Listener {

    companion object{

        lateinit var bank : BankAPI

        lateinit var es : ExecutorService

        lateinit var vault : VaultManager

        lateinit var plugin: Plugin

        const val WAND_NAME = "範囲指定ワンド"

        var prefix = "[§5Man10RealEstate§f]"


        //保護を無効にするワールド
        var disableWorld = mutableListOf<String>()

        var serverName = "paper"

    }

    override fun onEnable() { // Plugin startup logic
        saveDefaultConfig()

        es = Executors.newCachedThreadPool()//スレッドプールを作成、必要に応じて新規スレッドを作成
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

        Region.load()
        City.load()

        //TODO:賃料スレッド
        es.execute {
        }

    }

    fun loadConfig(){
        reloadConfig()

        disableWorld = config.getStringList("disableWorld")
        serverName = config.getString("server","paper")!!

        saveResource("config.yml", false)

    }

    override fun onDisable() { // Plugin shutdown logic
        es.shutdownNow()
    }


}