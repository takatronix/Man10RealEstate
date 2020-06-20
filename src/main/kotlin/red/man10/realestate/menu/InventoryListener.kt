package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import red.man10.realestate.Plugin.Companion.customInventory
import red.man10.realestate.Plugin.Companion.user
import red.man10.realestate.Utility
import red.man10.realestate.menu.CustomInventory.Companion.InventoryID.*
import red.man10.realestate.menu.InventoryMenu.Companion.cache
import red.man10.realestate.region.User
import red.man10.realestate.region.User.Companion.Permission.*
import java.util.*

class InventoryListener : Listener{

    val invMenu = InventoryMenu()

    @EventHandler
    fun clickEvent(e:InventoryClickEvent){

        val p = e.whoClicked

        if (p !is Player)return

        val slot = e.slot
        val item = e.currentItem?:return

        if (customInventory.get(p) ==null)return

        e.isCancelled = true

        when(customInventory.get(p)){

            MAIN_MENU -> {

                when(e.slot){

                    1 -> invMenu.openRegionList(p,0)
                    4 -> invMenu.openBookmark(p,0)
                    else ->return

                }
            }

            BOOKMARK ->{

                if (slot <45)return

                when(customInventory.getData(item,"type")){
                    "back" ->invMenu.mainMenu(p)
                    "next" ->{
                        val page = customInventory.getData(item,"page").toInt()
                        invMenu.openBookmark(p,page+1)

                    }
                    "previous" ->{
                        val page = customInventory.getData(item,"page").toInt()
                        invMenu.openBookmark(p,page-1)

                    }
                }

            }

            REGION_LIST ->{
                if (slot <45){

                    val id = customInventory.getData(item,"id").toInt()
                    invMenu.regionMenu(p,id)
                    return
                }

                when(customInventory.getData(item,"type")){
                    "back" ->invMenu.mainMenu(p)
                    "next" ->{
                        val page = customInventory.getData(item,"page").toInt()
                        invMenu.openRegionList(p,page+1)

                    }
                    "previous" ->{
                        val page = customInventory.getData(item,"page").toInt()
                        invMenu.openRegionList(p,page-1)

                    }
                }

            }

            REGION_MENU ->{

                if (slot == 0){
                    invMenu.openRegionList(p,0)
                    return
                }

                val id = customInventory.getData(item,"id").toInt()

                when(slot){
                    11 -> invMenu.regionSetting(p,id)
                    13 -> invMenu.userList(p,id,0)
                    15 -> {
                        customInventory.close(p)
                        Utility.sendSuggest(p,"§a§住人を追加する","mre adduser $id ")
                        return
                    }
                }

            }

            REGION_SETTING ->{

                val id = customInventory.getData(item,"id").toInt()

                when(slot){

                    0->invMenu.regionMenu(p,id)
                    10->invMenu.statusMenu(p,id)
                    13->{
                        customInventory.close(p)
                        Utility.sendSuggest(p,"§a§l土地の値段を設定する","mre setprice $id ")
                    }
                    16->{}
                    38->invMenu.spanMenu(p,id)
                    42->{
                        customInventory.close(p)
                        Utility.sendSuggest(p,"§a§l土地の値段を設定する","mre setowner $id ")
                    }
                }

            }

            REGION_STATUS ->{

                val id = customInventory.getData(item,"id").toInt()

                when(slot){
                    0->invMenu.regionSetting(p,id)
                    1->{
                        customInventory.close(p)
                        p.performCommand("mre setstatus $id Danger")
                    }
                    3->{
                        customInventory.close(p)
                        p.performCommand("mre setstatus $id Free")
                    }
                    5->{
                        customInventory.close(p)
                        p.performCommand("mre setstatus $id OnSale")
                    }
                    7->{
                        customInventory.close(p)
                        p.performCommand("mre setstatus $id Protected")
                    }

                }

            }

            REGION_SPAN ->{

                val id = customInventory.getData(item,"id").toInt()

                when(slot){
                    0 ->invMenu.regionSetting(p,id)
                    1 ->{
                        customInventory.close(p)
                        p.performCommand("mre span $id 2")
                    }
                    4 -> {
                        customInventory.close(p)
                        p.performCommand("mre span $id 1")
                    }
                    7 -> {
                        customInventory.close(p)
                        p.performCommand("mre span $id 0")
                    }
                }
            }


            USER_LIST->{

                val id = customInventory.getData(item,"id").toInt()

                if (slot <45){

                    val uuid = UUID.fromString(customInventory.getData(item,"uuid"))

                    invMenu.userMenu(p,id,uuid)
                    return
                }

                when(customInventory.getData(item,"type")){
                    "back" ->invMenu.regionMenu(p,id)
                    "next" ->{
                        val page = customInventory.getData(item,"page").toInt()
                        invMenu.userList(p,id,page+1)

                    }
                    "previous" ->{
                        val page = customInventory.getData(item,"page").toInt()
                        invMenu.userList(p,id,page-1)
                    }
                }

            }

            USER_MENU->{
                val id = customInventory.getData(item,"id").toInt()
                if (slot == 0){
                    invMenu.userList(p,id,0)
                    return
                }

                val uuid = UUID.fromString(customInventory.getData(item,"uuid"))

                when(slot){

                    0->invMenu.userList(p,id,0)

                    11->invMenu.setPermission(p,id,uuid)
                    13->{
                        customInventory.close(p)
                        Utility.sendSuggest(p,"§a§l賃料を設定する","mre setrent $id" +
                                " ${Bukkit.getOfflinePlayer(uuid).name} ")
                    }
                    15->p.performCommand("mre removeuser $id ${Bukkit.getOfflinePlayer(uuid).name}")

                }

            }

            USER_PERMISSION->{
                val id = customInventory.getData(item,"id").toInt()
                val uuid = UUID.fromString(customInventory.getData(item,"uuid"))

                val cacheData = cache[Pair(uuid,id)]!!

                if (slot == 0){
                    cache.remove(Pair(uuid,id))
                    invMenu.userMenu(p,id,uuid)
                    return
                }

                val value = item.type == Material.RED_STAINED_GLASS_PANE

                when(slot){

                    13 ->{
                        cacheData.allowAll = value
                        user.setPermission(uuid,id, ALL,value)
                    }

                    22 ->{
                        cacheData.allowBlock = value
                        user.setPermission(uuid,id, BLOCK,value)
                    }

                    31 ->{
                        cacheData.allowInv = value
                        user.setPermission(uuid,id, INVENTORY,value)
                    }

                    40 ->{
                        cacheData.allowDoor = value
                        user.setPermission(uuid,id, DOOR,value)
                    }

                }

                invMenu.setPermission(p,id,uuid)
                cache[Pair(uuid,id)] = cacheData

            }

        }
    }


    @EventHandler
    fun closeEvent(e:InventoryCloseEvent){

        val p = e.player
        if (p !is Player)return
        customInventory.close(p,false)
    }


}