package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import red.man10.realestate.region.Region
import red.man10.realestate.util.MenuFramework
import red.man10.realestate.util.Utility

class UserListMenu(p:Player,private val id:Int) : MenuFramework(p, LARGE_CHEST_SIZE,"ID:${id}の住人の管理") {

    override fun init() {

//        val prepareButton = Button(Material.BARRIER)
//        prepareButton.title("§c§l準備中")
//        addButton(prepareButton)

        val rg = Region.regionData[id]

        if (rg == null){
            Utility.sendMessage(p,"存在しない土地です")
            p.closeInventory()
            return
        }
        val userList = rg.getUser()

        for (user in userList){
            val button = Button(Material.PLAYER_HEAD)
            val offP = Bukkit.getOfflinePlayer(user.uuid)
            if (offP.isOnline){
                val icon = ItemStack(Material.PLAYER_HEAD)
                val meta = icon.itemMeta as SkullMeta
                meta.setOwningPlayer(offP)
                icon.itemMeta = meta
                button.setIcon(icon)
            }

            button.title("§e§l${offP.name}")
            button.setClickAction{
                ManageUserMenu(p,user).open()
                return@setClickAction
            }
            addButton(button)
        }

    }

}