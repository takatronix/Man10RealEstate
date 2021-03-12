package red.man10.realestate.storage.upgrade

import com.google.gson.Gson
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin.Companion.plugin

abstract class Upgrade {

    abstract val upgradeName : String

    fun getUpgrade(displayName:String,lore:MutableList<String>):ItemStack{

        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,"name"), PersistentDataType.STRING,upgradeName)
        meta.setDisplayName(displayName)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        meta.addEnchant(Enchantment.LUCK,1,false)

        meta.lore = lore

        item.itemMeta = meta
        return item

    }

    companion object{

        fun itemToUpgradeName(itemStack: ItemStack): String? {
            return itemStack.itemMeta.persistentDataContainer[NamespacedKey(plugin,"name"), PersistentDataType.STRING]
        }

        fun getAllUpgrades(controller:ItemStack):List<String>{

            val jsonStr = controller.itemMeta!!.persistentDataContainer[NamespacedKey(plugin,"upgrades"), PersistentDataType.STRING]?:return emptyList()

            return Gson().fromJson(jsonStr,Array<String>::class.java).toList()
        }

        fun addUpgrade(upgrade:ItemStack,controller: ItemStack):Boolean{

            val upgrades = getAllUpgrades(controller).toMutableList()
            val upgradeName = itemToUpgradeName(upgrade)?:return false

            //すでにアップグレードがついていたらfalseを返す
            if (upgrades.contains(upgradeName))return false

            upgrades.add(upgradeName)

            val meta = controller.itemMeta!!

            meta.persistentDataContainer.set(NamespacedKey(plugin,"upgrades"), PersistentDataType.STRING, Gson().toJson(upgrades))

            controller.itemMeta = meta

            upgrade.amount = upgrade.amount-1

            return true
        }
    }

}