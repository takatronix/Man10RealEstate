package red.man10.realestate.storage

import com.google.gson.Gson
import org.bukkit.*
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
import red.man10.realestate.Plugin.Companion.votingDiamond
import red.man10.realestate.Utility
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.storage.upgrade.PasswordUpgrade

object RemoteController : Listener{

    val controllerName = "§b特殊樽アクセス端末"
    val customModel = 370

    val key = "Ver.1.0"

    private val pageMap = HashMap<Player,Pair<Int,ItemStack>>()
    private val checkingMap = HashMap<Player,String>()

    private val gson = Gson()

    val password = PasswordUpgrade()

    fun getController():ItemStack{

        val controller = ItemStack(Material.IRON_HOE)

        controller.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        controller.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
        controller.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)

        val meta = controller.itemMeta
        meta.setCustomModelData(customModel)
        meta.setDisplayName(controllerName)
        meta.isUnbreakable = true

        meta.lore = mutableListOf("§f[1]ボタンで前のページ [2]ボタンで次のページ")

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

    fun editLocation(controller:ItemStack,loc:Location,p:Player):Int{

        var ret = 0

        if (!isController(controller))return ret

        val jsonLoc = Utility.locationToJson(loc)

        val list = getStringLocationList(controller).toMutableList()


        if (list.contains(jsonLoc)){

            ret = 4

            //確認画面を出す
            if (checkingRemove(p,jsonLoc)){
                list.remove(jsonLoc)
                ret = 1
            }

        }else{

            var removed = false

            for (item in p.inventory){
                if (item == null|| item.type == Material.AIR)continue

                if (item.isSimilar(votingDiamond)){
                    removed = true
                    item.amount = item.amount -1
                    break
                }
            }

            ret = if (removed){
                list.add(jsonLoc)
                2
            }else{ 3 }

        }


        setStringLocationList(controller,list)

        return ret
    }

    fun checkingRemove(p:Player,loc:String):Boolean{

        if (checkingMap[p]==null || checkingMap[p] != loc){
            checkingMap[p] = loc
            return false
        }
        checkingMap.remove(p)
        return true
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

        if (Barrel.isOpen(loc)){
            sendMessage(p, "§c§l現在他のプレイヤーが開いています！")
            return
        }

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

        //コントローラーはさわれないようにする
        if (e.currentItem?.isSimilar(controller) == true)e.isCancelled = true
        if (e.hotbarButton >= 0){ e.isCancelled = true}

        when(e.hotbarButton){
            0 ->{//ページ戻る
                if ((page-1)<0)return

                val list = getStringLocationList(controller)

                val block = Utility.jsonToLocation(list[page]).block
                Barrel.setStorageItem(e.inventory,block)

                if (Barrel.isOpen(Utility.jsonToLocation(list[page-1]).block.location)){
                    sendMessage(p, "§c§l現在他のプレイヤーが開いています！")
                    return
                }

                Barrel.removeMap(block.location)

                p.playSound(p.location, Sound.UI_BUTTON_CLICK,0.3F,1.0F)

                openInventory(controller,p, (page-1))

            }

            1 ->{//ページ進む

                if (getStringLocationList(controller).size==(page+1))return

                val list = getStringLocationList(controller)

                val block = Utility.jsonToLocation(list[page]).block
                Barrel.setStorageItem(e.inventory,block)

                if (Barrel.isOpen(Utility.jsonToLocation(list[page+1]).block.location)){
                    sendMessage(p, "§c§l現在他のプレイヤーが開いています！")
                    return
                }

                Barrel.removeMap(block.location)

                p.playSound(p.location, Sound.UI_BUTTON_CLICK,0.3F,1.0F)

                openInventory(controller,p, (page+1))

            }

            2 ->{//現在のページ確認
                sendMessage(p,"現在のページ:$page")
                return
            }

            3 ->{

            }

            8 ->{//デバッグメニュー
                sendMessage(p, "pages:${getStringLocationList(controller).size}")
//                sendMessage(p,"")
            }
        }

    }

    @EventHandler
    fun openController(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_AIR)return

        val item = e.item?:return

        if (!isController(item))return

        val p = e.player

        //パスワードの確認処理追加
        if (password.hasPassword(item)){
            password.openCheckingPassword(p,item)
            return
        }

        openInventory(item,p,0)

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

        Barrel.removeMap(block.location)
        pageMap.remove(p)

    }


}