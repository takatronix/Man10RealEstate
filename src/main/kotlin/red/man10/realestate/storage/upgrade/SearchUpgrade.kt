package red.man10.realestate.storage.upgrade

import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class SearchUpgrade : Upgrade() ,Listener{

    override val upgradeName: String = "search"


    fun getUpgrade(): ItemStack {
        return super.getUpgrade("§eSearchApp",mutableListOf("検索したアイテムが格納されている","特殊樽を表示します"))
    }



}