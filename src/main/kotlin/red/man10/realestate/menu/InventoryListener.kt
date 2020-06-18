package red.man10.realestate.menu

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import red.man10.realestate.Plugin.Companion.customInventory
import red.man10.realestate.menu.CustomInventory.Companion.InventoryID.*
import java.util.*

class InventoryListener : Listener{

    val invMenu = InventoryMenu()

    @EventHandler
    fun clickEvent(e:InventoryClickEvent){

        val p = e.whoClicked

        if (p !is Player)return

        val inv = e.inventory
        val slot = e.slot
        val item = e.currentItem?:return

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

                if (slot == 0) invMenu.openRegionList(p,0)

                val id = customInventory.getData(item,"id").toInt()

                when(slot){
                    11 -> invMenu.regionSetting(p,id)
                    13 -> invMenu.userList(p,id,0)
                    15 -> {
                        customInventory.close(p)
                        p.performCommand("")
                        //TODO: コマンドを設定
                    }
                }

            }

            REGION_SETTING ->{

                val id = customInventory.getData(item,"id").toInt()

                when(slot){

                    0->invMenu.regionMenu(p,id)
                    10->invMenu.statusMenu(p,id)
                    13->{
                        //TODO:料金設定のコマンド表示(サジェスト)
                    }
                    16->{}
                    38->invMenu.spanMenu(p,id)
                    42->{
                        //TODO:オーナー変更コマンド表示
                    }
                }

            }

            REGION_STATUS ->{

                val id = customInventory.getData(item,"id").toInt()

                when(slot){
                    //TODO: コマンド
                    0->invMenu.regionSetting(p,id)

                }

            }

            REGION_SPAN ->{

                val id = customInventory.getData(item,"id").toInt()

                when(slot){
                    0 ->invMenu.regionSetting(p,id)
                    //TODO:コマンド

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
                }

                val uuid = UUID.fromString(customInventory.getData(item,"uuid"))

                when(slot){

                    0->invMenu.userList(p,id,0)

                    10->invMenu.setPermission(p,id,uuid)
                    //TODO:設定

                }

            }

            USER_PERMISSION->{
                val id = customInventory.getData(item,"id").toInt()
                val uuid = UUID.fromString(customInventory.getData(item,"uuid"))

                when(slot){

                    0->invMenu.userMenu(p,id,uuid)

                    //TODO:コマンド

                }

            }

        }
    }


    @EventHandler
    fun closeEvent(e:InventoryCloseEvent){

        val p = e.player
        if (p !is Player)return
        customInventory.close(p)
    }


}