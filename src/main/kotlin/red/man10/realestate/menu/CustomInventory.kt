package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.prefix
import java.util.*
import kotlin.collections.HashMap

class CustomInventory (private val pl: Plugin){

    val invMap = HashMap<Player,InventoryID>()

    fun open(p:Player,id:InventoryID){
        invMap[p] = id
    }

    fun get(p:Player):InventoryID?{
        return invMap[p]
    }

    fun close(p:Player){
        Bukkit.getScheduler().runTask(pl, Runnable {
            p.closeInventory()
        })
        invMap.remove(p)
    }

    fun close(p:Player,inv:Boolean){
        if (inv){
            p.closeInventory()
        }
        invMap.remove(p)
    }


    fun IS(type:Material,name:String,lore:MutableList<String>):ItemStack{
        val item = ItemStack(type)
        val meta = item.itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    fun IS(type:Material,name:String,lore:MutableList<String>,id:Int):ItemStack{
        val item = ItemStack(type)
        val meta = item.itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        item.itemMeta = meta

        setData(item,"id",id.toString())

        return item
    }

    fun IS(type:Material,name:String,lore:MutableList<String>,uuid:UUID,id:Int):ItemStack{
        val item = ItemStack(type)
        val meta = item.itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        item.itemMeta = meta

        setData(item,"id",id.toString())
        setData(item,"uuid",uuid.toString())

        return item
    }

    fun IS(type:Material,name:String):ItemStack{
        val item = ItemStack(type)
        val meta = item.itemMeta
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }

    /**
     * ItemStackにNBTのデータを埋め込む
     * @param key データを取得するキー
     * @param data 埋め込むデータ
     */
    fun setData(item:ItemStack,key:String,data:String): ItemStack {
        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(pl,key), PersistentDataType.STRING,data)
        item.itemMeta = meta

        return item
    }

    /**
     * 埋め込んだデータを取得する
     */
    fun getData(item:ItemStack,key: String):String{

        val meta = item.itemMeta

        return meta.persistentDataContainer[NamespacedKey(pl,key), PersistentDataType.STRING]?:"none"
    }

    fun createInventory(slot:Int, title:String): Inventory {
        return Bukkit.createInventory(null,slot, prefix + title)
    }

    companion object{
        enum class InventoryID{

            MAIN_MENU,
            BOOKMARK,
            REGION_LIST,
            REGION_MENU,
            REGION_STATUS,
            REGION_SPAN,
            USER_LIST,
            REGION_SETTING,
            USER_MENU,
            USER_PERMISSION
        }
    }
}