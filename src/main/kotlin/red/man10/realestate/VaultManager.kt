package red.man10.realestate

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import java.util.*


/**
 * Created by takatronix on 2017/03/04.
 */
class VaultManager(private val plugin: JavaPlugin) {
    private fun setupEconomy(): Boolean {
        plugin.logger.info("setupEconomy")
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            plugin.logger.warning("Vault plugin is not installed")
            return false
        }
        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            plugin.logger.warning("Can't get vault service")
            return false
        }
        economy = rsp.provider
        plugin.logger.info("Economy setup")
        return economy != null
    }

    /////////////////////////////////////
    //      残高確認
    /////////////////////////////////////
    fun getBalance(uuid: UUID?): Double {

        return economy!!.getBalance(Bukkit.getOfflinePlayer(uuid!!).name)
    }

    /////////////////////////////////////
    //      残高確認
    /////////////////////////////////////
    fun showBalance(uuid: UUID?) {
        val p: OfflinePlayer? = Bukkit.getOfflinePlayer(uuid!!).player
        val money = getBalance(uuid)
        p!!.player!!.sendMessage(ChatColor.YELLOW.toString() + "あなたの所持金は$" + money)
    }

    /////////////////////////////////////
    //      引き出し
    /////////////////////////////////////
    fun withdraw(uuid: UUID, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(uuid)
        val resp = economy!!.withdrawPlayer(p.name, money)
        if (resp.transactionSuccess()) {
            if (p.isOnline) {
                p.player!!.sendMessage(ChatColor.YELLOW.toString() + "$" + money + "支払いました")
            }
            return true
        }
        return false
    }

    /////////////////////////////////////
    //      お金を入れる
    /////////////////////////////////////
    fun deposit(uuid: UUID, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(uuid)
        val resp = economy!!.depositPlayer(p.name, money)
        if (resp.transactionSuccess()) {
            if (p.isOnline) {
                p.player!!.sendMessage(ChatColor.YELLOW.toString() + "$" + money + "受取りました")
            }
            return true
        }
        return false
    }

    companion object {
        var economy: Economy? = null
    }

    init {
        setupEconomy()
    }
}