package red.man10.realestate.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import red.man10.realestate.util.MenuFramework

class MainMenu(p: Player) : MenuFramework(p,9, "メインメニュー") {

    override fun init() {
        val manageButton = Button(Material.PAPER)
        manageButton.title("§f§l自分の土地を管理する")
        manageButton.setClickAction{
            RegionListMenu(p).open()
        }
        setButton(manageButton,1)

        val bookmarkButton = Button(Material.NETHER_STAR)
        bookmarkButton.title("§f§lブックマークした土地にとぶ")
        bookmarkButton.setClickAction{
            BookmarkMenu(p).open()
        }
        setButton(bookmarkButton,4)

        val taxButton = Button(Material.NETHERITE_INGOT)
        taxButton.title("§e§l税金情報などを確認する")
        taxButton.cmd(2)
        taxButton.setClickAction{
            p.closeInventory()
            p.performCommand("mre balance")
        }
        setButton(taxButton,7)
    }

}