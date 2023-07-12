package red.man10.realestate.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import red.man10.realestate.util.MenuFramework

class UserListMenu(p:Player,id:Int) : MenuFramework(p, LARGE_CHEST_SIZE,"ID:${id}の住人の管理") {

    override fun init() {

        val prepareButton = Button(Material.BARRIER)
        prepareButton.title("§c§l準備中")
        addButton(prepareButton)

    }

}