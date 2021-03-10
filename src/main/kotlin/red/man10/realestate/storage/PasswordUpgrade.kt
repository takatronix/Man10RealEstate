package red.man10.realestate.storage

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin.Companion.plugin

class PasswordUpgrade : Upgrade() ,Listener{

    override val upgradeName = "password"

    private val numbers = mutableListOf<ItemStack>()
    private val slots = arrayOf(12,13,14,21,22,13,30,31,32,40)
    private val cmd = 567

    init {

        for (i in 0..9){
            val item = ItemStack(Material.IRON_NUGGET)
            val meta = item.itemMeta
            meta.setDisplayName(i.toString())
            meta.setCustomModelData((cmd - i))
            item.itemMeta = meta
            numbers.add(item)
        }

    }

    override fun getUpgrade(): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,"name"), PersistentDataType.STRING,upgradeName)
        meta.setDisplayName("§eパスワードアップグレード")
        meta.lore = mutableListOf("端末に4桁のパスワードを設定することができるようになります","パスワードを「0000」にするとパスワードをオフにできます")

        item.itemMeta = meta
        return item

    }

    fun openNumericPad(p:Player,nowNumber:String,maxDigit:Int,title:String):String{

        if (nowNumber.length>=maxDigit) return "null"

        val inv = Bukkit.createInventory(null,54,title)

        for (i in 0..9){
            inv.setItem(slots[i],numbers[i])
        }

        val numberList = nowNumber.toMutableList()

        for (i in nowNumber.indices){
            inv.setItem(8-i,numbers[numberList[i].toInt()])
        }

        p.openInventory

        return nowNumber
    }

    fun setPassword(p:Player,controller:ItemStack){

        openNumericPad(p,"",4,"パスワードを設定する")

    }

    fun getPassword(controller:ItemStack){

    }

    @EventHandler
    fun clickNumericPad(e:InventoryClickEvent){


    }
}