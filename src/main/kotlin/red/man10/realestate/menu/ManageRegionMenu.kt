package red.man10.realestate.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import red.man10.realestate.region.City
import red.man10.realestate.region.Region
import red.man10.realestate.util.MenuFramework
import red.man10.realestate.util.Utility
import red.man10.realestate.util.Utility.format

class ManageRegionMenu(p:Player,val page:Int = 0) : MenuFramework(p, LARGE_CHEST_SIZE,"土地を管理する") {

    override fun init() {

        var inc = 0

        while (menu.getItem(44) == null){

            val index = inc + page*45
            inc ++
            val list = Region.regionData.filterValues { rg -> rg.ownerUUID == p.uniqueId }.values.toList()
            if (list.size <= index)break

            val button = Button(Material.PAPER)
            val rg = list[index]
            button.title("§e§lID:${rg.id}")
            button.lore(mutableListOf("§7§lステータス:${Region.formatStatus(rg.status)}",
                "§8§l価格:${format(rg.price)}円",
                "§7§l税額:${if (rg.data.tax!=0.0) format(rg.data.tax) else format(City.where(rg.teleport)?.getTax(rg.id)?:0.0)}円"))
            button.setClickAction{
                //TODO:詳細メニュー
            }
            addButton(button)
        }

        //Back
        val back = Button(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
        back.title("")
        arrayOf(45,46,47,48,49,50,51,52,53).forEach { setButton(back,it) }

        //previous
        if (page!=0){
            val previous = Button(Material.RED_STAINED_GLASS_PANE)
            previous.title("前のページへ")
            previous.setClickAction{ ManageRegionMenu(p,page-1) }
            arrayOf(45,46,47).forEach { setButton(previous,it) }

        }

        //next
        if (inc>=44){
            val next = Button(Material.RED_STAINED_GLASS_PANE)
            next.title("次のページへ")
            next.setClickAction{ ManageRegionMenu(p,page+1) }
            arrayOf(51,52,53).forEach { setButton(next,it) }
        }

    }

}