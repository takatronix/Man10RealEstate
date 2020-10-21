package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import red.man10.realestate.Utility
import red.man10.realestate.menu.CustomInventory.InventoryID.*
import red.man10.realestate.menu.InventoryMenu.cache
import red.man10.realestate.region.User
import java.util.*

object InventoryListener : Listener{

    @EventHandler
    fun clickEvent(e:InventoryClickEvent){

        val p = e.whoClicked

        if (p !is Player)return

        val slot = e.slot
        val item = e.currentItem?:return

        if (CustomInventory.get(p) ==null)return

        e.isCancelled = true

        when(CustomInventory.get(p)){

            MAIN_MENU -> {

                when(e.slot){

                    1 -> InventoryMenu.openRegionList(p,0)
                    4 -> InventoryMenu.openBookmark(p,0)
                    else ->return

                }
            }

            BOOKMARK ->{

                if (slot <45)return

                when(CustomInventory.getData(item,"type")){
                    "back" ->InventoryMenu.mainMenu(p)
                    "next" ->{
                        val page = CustomInventory.getData(item,"page").toInt()
                        InventoryMenu.openBookmark(p,page+1)

                    }
                    "previous" ->{
                        val page = CustomInventory.getData(item,"page").toInt()
                        InventoryMenu.openBookmark(p,page-1)

                    }
                }

            }

            REGION_LIST ->{
                if (slot <45){

                    val id = CustomInventory.getData(item,"id").toInt()
                    InventoryMenu.regionMenu(p,id)
                    return
                }

                when(CustomInventory.getData(item,"type")){
                    "back" ->InventoryMenu.mainMenu(p)
                    "next" ->{
                        val page = CustomInventory.getData(item,"page").toInt()
                        InventoryMenu.openRegionList(p,page+1)

                    }
                    "previous" ->{
                        val page = CustomInventory.getData(item,"page").toInt()
                        InventoryMenu.openRegionList(p,page-1)

                    }
                }

            }

            REGION_MENU ->{

                if (slot == 0){
                    InventoryMenu.openRegionList(p,0)
                    return
                }

                val id = CustomInventory.getData(item,"id").toInt()

                when(slot){
                    11 -> InventoryMenu.regionSetting(p,id)
                    13 -> InventoryMenu.userList(p,id,0)
                    15 -> {
                        CustomInventory.close(p)
                        Utility.sendSuggest(p,"§a§住人を追加する","mre adduser $id ")
                        return
                    }
                }

            }

            REGION_SETTING ->{

                val id = CustomInventory.getData(item,"id").toInt()

                when(slot){

                    0->InventoryMenu.regionMenu(p,id)
                    10->InventoryMenu.statusMenu(p,id)
                    13->{
                        CustomInventory.close(p)
                        Utility.sendSuggest(p,"§a§l土地の値段を設定する","mre setprice $id ")
                    }
                    16->{
                        p.performCommand("mre settp $id")
                        CustomInventory.close(p)
                    }
                    38->InventoryMenu.spanMenu(p,id)
                    42->{
                        CustomInventory.close(p)
                        Utility.sendSuggest(p,"§a§l土地の値段を設定する","mre setowner $id ")
                    }
                }

            }

            REGION_STATUS ->{

                val id = CustomInventory.getData(item,"id").toInt()

                when(slot){
                    0->InventoryMenu.regionSetting(p,id)
                    1->{
                        CustomInventory.close(p)
                        p.performCommand("mre setstatus $id Danger")
                    }
                    3->{
                        CustomInventory.close(p)
                        p.performCommand("mre setstatus $id Free")
                    }
                    5->{
                        CustomInventory.close(p)
                        p.performCommand("mre setstatus $id OnSale")
                    }
                    7->{
                        CustomInventory.close(p)
                        p.performCommand("mre setstatus $id Protected")
                    }

                }

            }

            REGION_SPAN ->{

                val id = CustomInventory.getData(item,"id").toInt()

                when(slot){
                    0 ->InventoryMenu.regionSetting(p,id)
                    1 ->{
                        CustomInventory.close(p)
                        p.performCommand("mre span $id 2")
                    }
                    4 -> {
                        CustomInventory.close(p)
                        p.performCommand("mre span $id 1")
                    }
                    7 -> {
                        CustomInventory.close(p)
                        p.performCommand("mre span $id 0")
                    }
                }
            }


            USER_LIST->{

                val id = CustomInventory.getData(item,"id").toInt()

                if (slot <45){

                    val uuid = UUID.fromString(CustomInventory.getData(item,"uuid"))

                    InventoryMenu.userMenu(p,id,uuid)
                    return
                }

                when(CustomInventory.getData(item,"type")){
                    "back" ->InventoryMenu.regionMenu(p,id)
                    "next" ->{
                        val page = CustomInventory.getData(item,"page").toInt()
                        InventoryMenu.userList(p,id,page+1)

                    }
                    "previous" ->{
                        val page = CustomInventory.getData(item,"page").toInt()
                        InventoryMenu.userList(p,id,page-1)
                    }
                }

            }

            USER_MENU->{
                val id = CustomInventory.getData(item,"id").toInt()
                if (slot == 0){
                    InventoryMenu.userList(p,id,0)
                    return
                }

                val uuid = UUID.fromString(CustomInventory.getData(item,"uuid"))

                when(slot){

                    0->InventoryMenu.userList(p,id,0)

                    11->InventoryMenu.setPermission(p,id,uuid)
                    13->{
                        CustomInventory.close(p)
                        Utility.sendSuggest(p,"§a§l賃料を設定する","mre setrent $id" +
                                " ${Bukkit.getOfflinePlayer(uuid).name} ")
                    }
                    15->{
                        val user1 = Bukkit.getOfflinePlayer(uuid)
                        if (user1.isOnline){
                            p.performCommand("mre removeuser $id ${user1.name}")
                        }else{
                            User.remove(user1.uniqueId,id)
                            Utility.sendMessage(p,"§a§l住人を削除しました!")
                            return
                        }
                    }

                }

            }

            USER_PERMISSION->{
                val id = CustomInventory.getData(item,"id").toInt()
                val uuid = UUID.fromString(CustomInventory.getData(item,"uuid"))

                val cacheData = cache[Pair(uuid,id)]!!

                if (slot == 0){
                    cache.remove(Pair(uuid,id))
                    InventoryMenu.userMenu(p,id,uuid)
                    return
                }

                val value = item.type == Material.RED_STAINED_GLASS_PANE

                when(slot){

                    13 ->{
                        cacheData.allowAll = value
                        User.setPermission(uuid,id, User.Permission.ALL,value)
                    }

                    22 ->{
                        cacheData.allowBlock = value
                        User.setPermission(uuid,id, User.Permission.BLOCK,value)
                    }

                    31 ->{
                        cacheData.allowInv = value
                        User.setPermission(uuid,id, User.Permission.INVENTORY,value)
                    }

                    40 ->{
                        cacheData.allowDoor = value
                        User.setPermission(uuid,id, User.Permission.DOOR,value)
                    }

                }

                InventoryMenu.setPermission(p,id,uuid)
                cache[Pair(uuid,id)] = cacheData

            }

        }
    }


    @EventHandler
    fun closeEvent(e:InventoryCloseEvent){

        val p = e.player
        if (p !is Player)return
        CustomInventory.close(p,false)
    }


}