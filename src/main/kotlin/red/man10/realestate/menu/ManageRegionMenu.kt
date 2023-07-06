package red.man10.realestate.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import red.man10.realestate.Plugin
import red.man10.realestate.region.Region
import red.man10.realestate.util.MenuFramework
import red.man10.realestate.util.Utility
import red.man10.realestate.util.Utility.sendMessage

class ManageRegionMenu(p:Player,val id:Int) : MenuFramework(p, CHEST_SIZE,"ID:${id}の管理") {

    override fun init() {
        val rg = Region.regionData[id]

        if (rg == null){
            val closeButton = Button(Material.BARRIER)
            setButton(closeButton,13)
            sendMessage(p,"§c§l存在しない土地です")
            return
        }

        val statusButton = Button(Material.COMPASS)
        statusButton.title("§e土地のステータスを設定する")
        statusButton.lore(mutableListOf("§f現在のステータス:${Region.formatStatus(rg.status)}"))
        statusButton.setClickAction{
            if (rg.status == "Protected"){
                rg.status = "OnSale"
                rg.asyncSave()
                sendMessage(p,"§e土地のステータスを販売中に変更しました")
                ManageRegionMenu(p,id).open()
                return@setClickAction
            }
            if (rg.status == "OnSale"){
                rg.status = "Protected"
                rg.asyncSave()
                sendMessage(p,"§e土地のステータスを保護に変更しました")
                ManageRegionMenu(p,id).open()
                return@setClickAction
            }
        }
        setButton(statusButton,2)

        val priceButton = Button(Material.DIAMOND)
        priceButton.cmd(1)
        priceButton.title("§e土地の値段を決める(コマンドを使います)")
        priceButton.lore(mutableListOf("§e現在の値段:${Utility.format(rg.price)}"))
        priceButton.setClickAction{
            p.sendMessage(text(Plugin.prefix).append(text("§b§l§n[ここをクリック]")
                .clickEvent(ClickEvent.suggestCommand("/mre setprice $id "))))
            p.closeInventory()
        }
        setButton(priceButton,4)

        val teleportButton = Button(Material.ENDER_PEARL)
        teleportButton.title("§a§l現在地点をテレポートポイントにする")
        teleportButton.setClickAction{
            rg.teleport = p.location
            rg.asyncSave()
            p.closeInventory()
        }
        setButton(teleportButton,6)

        val spanButton = Button(Material.CLOCK)
        spanButton.title("§a賃料のスパンを設定する")
        spanButton.lore(mutableListOf("§f現在のスパン:${Region.formatSpan(rg.span)}"))
        spanButton.setClickAction{
            RegionSpanMenu(p,id).open()
        }
        setButton(spanButton,20)

        val ownerButton = Button(Material.PLAYER_HEAD)
        ownerButton.title("§7土地の持ち主を変える(コマンドを使います)")
        ownerButton.setClickAction{
            p.sendMessage(text(Plugin.prefix).append(text("§b§l§n[ここをクリック]")
                .clickEvent(ClickEvent.suggestCommand("/mre setowner $id "))))
            p.closeInventory()
        }
        setButton(ownerButton,22)

        val initButton = Button(Material.BARRIER)
        initButton.title("§c§l土地を手放す(一月分の税金が必要)")
        ownerButton.setClickAction{
            p.performCommand("/mre confirminit $id")
            p.closeInventory()
        }
        setButton(initButton,24)
    }
}