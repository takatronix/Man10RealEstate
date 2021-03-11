package red.man10.realestate.storage.upgrade

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin

abstract class Upgrade {

    abstract val upgradeName : String

    abstract fun getUpgrade():ItemStack

    companion object{
        fun itemToUpgradeName(itemStack: ItemStack): String? {
            return itemStack.itemMeta.persistentDataContainer[NamespacedKey(Plugin.plugin,"name"), PersistentDataType.STRING]
        }

    }

}