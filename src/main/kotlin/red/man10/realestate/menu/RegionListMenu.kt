package red.man10.realestate.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import red.man10.realestate.region.City
import red.man10.realestate.region.Region
import red.man10.realestate.util.MenuFramework
import red.man10.realestate.util.Utility.format

class RegionListMenu(p:Player, val page:Int = 0) : MenuFramework(p, LARGE_CHEST_SIZE,"土地を管理する") {

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
            button.lore(mutableListOf(
                "§e§l左クリック:土地の詳細設定",
                "§b§l右クリック:住民の設定",
                "§7ステータス:${Region.formatStatus(rg.status)}",
                "§7価格:${format(rg.price)}円",
                "§7税額:${format(City.getTax(rg.id))}円"))
            button.setClickAction{
                if (it.isLeftClick){
                    ManageRegionMenu(p,rg.id).open()
                    return@setClickAction
                }
                if (it.isRightClick){
                    UserListMenu(p,rg.id).open()
                    return@setClickAction
                }
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
            previous.setClickAction{ RegionListMenu(p,page-1) }
            arrayOf(45,46,47).forEach { setButton(previous,it) }

        }

        //next
        if (inc>=44){
            val next = Button(Material.RED_STAINED_GLASS_PANE)
            next.title("次のページへ")
            next.setClickAction{ RegionListMenu(p,page+1) }
            arrayOf(51,52,53).forEach { setButton(next,it) }
        }

    }

}