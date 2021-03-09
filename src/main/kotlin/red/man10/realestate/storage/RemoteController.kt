package red.man10.realestate.storage

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Utility

object RemoteController : Listener{

    val controllerName = "§b特殊樽アクセス端末"
    val customModel = 370

    val key = "Ver.1.0"

    val pageMap = HashMap<Player,Pair<Int,ItemStack>>()

    val gson = Gson()

    fun getController():ItemStack{

        val controller = ItemStack(Material.IRON_HOE)

        controller.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        controller.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
        controller.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)

        val meta = controller.itemMeta
        meta.setCustomModelData(customModel)
        meta.setDisplayName(controllerName)
        meta.isUnbreakable = true

        meta.persistentDataContainer.set(NamespacedKey(plugin,"controller"), PersistentDataType.STRING,key)

        controller.itemMeta = meta

        return controller

    }

    fun isController(item:ItemStack):Boolean{

        if (item.hasItemMeta()){
            val key = item.itemMeta.persistentDataContainer[NamespacedKey(plugin, "controller"), PersistentDataType.STRING]

            if (key == this.key)return true
        }
        return false
    }

    fun editLocation(controller:ItemStack,loc:Location):Int{

        var ret = 0

        if (!isController(controller))return ret

        val jsonLoc = Utility.locationToJson(loc)

        val list = getStringLocationList(controller).toMutableList()

        ret = if (list.contains(jsonLoc)){
            list.remove(jsonLoc)
            1
        }else{
            list.add(jsonLoc)
            2
        }

        setStringLocationList(controller,list)

        return ret
    }

    fun getStringLocationList(controller: ItemStack):List<String>{

        if (!isController(controller))return emptyList()

        val str = controller.itemMeta!!.persistentDataContainer[NamespacedKey(plugin,"location"), PersistentDataType.STRING]?:return emptyList()

        return gson.fromJson(str, Array<String>::class.java).toList()
    }

    fun setStringLocationList(controller: ItemStack, locList:List<String>){

        if (!isController(controller))return

        val meta = controller.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,"location"), PersistentDataType.STRING, gson.toJson(locList))
        controller.itemMeta = meta

    }

    fun openInventory(controller:ItemStack,p:Player,page:Int){

        if (!isController(controller))return

        val locList = getStringLocationList(controller)

        if (locList.isNullOrEmpty())return

        val loc = Utility.jsonToLocation(locList[page])

        val block = loc.block

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is org.bukkit.block.Barrel)return

        if(!Barrel.isSpecialBarrel(barrelState))return

        Barrel.openStorage(barrelState,p)

        pageMap[p] = Pair(page,controller)

    }


    @EventHandler
    fun changePageEvent(e:InventoryClickEvent){

        if (e.view.title != Barrel.title)return

        val p = e.whoClicked as Player

        if (!pageMap.containsKey(p))return

        val page = pageMap[p]!!.first
        val controller = pageMap[p]!!.second

        if (e.hotbarButton == 0){

            e.isCancelled = true

            if ((page-1)<0)return

            val block = Utility.jsonToLocation(getStringLocationList(controller)[page]).block
            Barrel.setStorageItem(e.inventory,block)

            openInventory(controller,p, (page-1))

        }

        if (e.hotbarButton == 1){

            e.isCancelled = true

            if (getStringLocationList(controller).size==(page+1))return

            val block = Utility.jsonToLocation(getStringLocationList(controller)[page]).block
            Barrel.setStorageItem(e.inventory,block)

            openInventory(controller,p, (page+1))

        }

        //コントローラーはさわれないようにする
        if (isController(e.currentItem?:return)){
            e.isCancelled = true
            return
        }
    }

    @EventHandler
    fun openController(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_AIR)return

        val item = e.item?:return

        if (!isController(item))return

        val p = e.player

        openInventory(item,p,0)

        p.sendMessage("[1]ボタンで前のページ [2]ボタンで次のページ")
    }

    @EventHandler
    fun closeController(e:InventoryCloseEvent){

        if (e.view.title != Barrel.title)return

        val p = e.player

        if (!pageMap.containsKey(p))return

        val page = pageMap[p]!!.first
        val controller = pageMap[p]!!.second

        val block = Utility.jsonToLocation(getStringLocationList(controller)[page]).block
        Barrel.setStorageItem(e.inventory,block)

        pageMap.remove(p)

    }


}