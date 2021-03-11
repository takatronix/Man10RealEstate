package red.man10.realestate.storage.upgrade

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin

class SearchUpgrade : Upgrade() ,Listener{

    override val upgradeName: String = "search"


    override fun getUpgrade(): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(Plugin.plugin,"name"), PersistentDataType.STRING,upgradeName)
        meta.setDisplayName("§eSearchApp")
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        meta.addEnchant(Enchantment.LUCK,1,false)
        meta.lore = mutableListOf("検索したアイテムが格納されている特種樽を表示します")

        item.itemMeta = meta
        return item
    }



}