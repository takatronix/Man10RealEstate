package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.realestate.region.User
import red.man10.realestate.util.MenuFramework

class ManageUserMenu(p:Player,val user:User) : MenuFramework(p, CHEST_SIZE,"${Bukkit.getOfflinePlayer(user.uuid).name}の管理"){

    override fun init() {

    }
}