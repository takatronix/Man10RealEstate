package red.man10.realestate.storage.upgrade

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Utility
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.storage.Barrel
import red.man10.realestate.storage.RemoteController

class SearchUpgrade : Upgrade() ,Listener{

    override val upgradeName: String = "search"

    private val searchUser = HashMap<Player,ItemStack>()


    fun getUpgrade(): ItemStack {
        return super.getUpgrade("§eSearchApp",mutableListOf("検索したアイテムが格納されている","特殊樽を表示します"))
    }

    private fun searchItems(p:Player, controller:ItemStack, keyword:String){

        val jsonList = RemoteController.getStringLocationList(controller)

        val list = mutableListOf<String>()

        for (str in jsonList){

            val loc = Utility.jsonToLocation(str)

            val block = loc.block

            if (block.type != Material.BARREL)continue

            val barrelState = block.state
            if (barrelState !is org.bukkit.block.Barrel)continue

            if(!Barrel.isSpecialBarrel(barrelState))continue

            val inv = Barrel.getStorage(barrelState)?:continue

            for (item in inv){

                if (item == null || item.type == Material.AIR)continue

                if (item.i18NDisplayName?.equals(keyword) == true){
                    list.add(Utility.locationToJson(loc))
                    break
                }

                val meta = item.itemMeta?:continue

                if (meta.displayName == keyword){
                    list.add(Utility.locationToJson(loc))
                    break
                }

                if (meta.lore?.contains(keyword) == true){
                    list.add(Utility.locationToJson(loc))
                    break
                }
            }
        }

        if (list.isEmpty()){
            sendMessage(p,"検索結果:0")
            return
        }

        RemoteController.openInventory(controller,p,0,list)

    }

    fun startSearch(p:Player,controller: ItemStack){

        p.closeInventory()
        sendMessage(p,"チャット欄にキーワードを入れて検索をしてください")
        searchUser[p] = controller
    }

    @EventHandler
    fun chatEvent(e:AsyncPlayerChatEvent){

        val p = e.player

        if (!searchUser.containsKey(p))return

        val item = searchUser[p]!!

        e.isCancelled = true

        if (e.message == "cancel"){
            searchUser.remove(p)
            sendMessage(p,"検索をキャンセルしました")
            return
        }

        val keyword = e.message

        Bukkit.getScheduler().runTask(plugin, Runnable {
            searchItems(p,item,keyword)
            searchUser.remove(p)

        })


    }


}