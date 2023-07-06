package red.man10.realestate.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import red.man10.realestate.region.Region
import red.man10.realestate.util.MenuFramework
import red.man10.realestate.util.Utility.sendMessage

class RegionSpanMenu(p:Player,val id:Int) : MenuFramework(p,9,"スパンの変更"){

    override fun init() {
        val rg = Region.regionData[id]
        if (rg == null){
            val closeButton = Button(Material.BARRIER)
            setButton(closeButton,4)
            sendMessage(p, "§c§l存在しない土地です")
            return
        }

        val daily = Button(Material.QUARTZ)
        daily.cmd(68)
        daily.title("§a毎日支払い")
        setButton(daily,2)
        daily.setClickAction{
            p.performCommand("/mre span $id 2")
            p.closeInventory()
        }

        val weekly = Button(Material.QUARTZ)
        weekly.cmd(87)
        weekly.title("§a毎週支払い")
        setButton(weekly,4)
        weekly.setClickAction{
            p.performCommand("/mre span $id 1")
            p.closeInventory()
        }

        val monthly = Button(Material.QUARTZ)
        monthly.cmd(77)
        monthly.title("§a毎月支払い")
        setButton(monthly,6)
        monthly.setClickAction{
            p.performCommand("/mre span $id 0")
            p.closeInventory()
        }
    }
}