package red.man10.realestate.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import red.man10.realestate.Plugin
import red.man10.realestate.region.Region
import red.man10.realestate.region.User
import red.man10.realestate.util.MenuFramework
import red.man10.realestate.util.Utility.sendMessage

class ManageUserMenu(p:Player,val user:User) : MenuFramework(p, 9,"${Bukkit.getOfflinePlayer(user.uuid).name}の管理"){

    override fun init() {
        val rg = Region.regionData[user.regionId]
        val name = Bukkit.getOfflinePlayer(user.uuid)

        if (rg == null){
            val closeButton = Button(Material.BARRIER)
            setButton(closeButton,4)
            sendMessage(p, "§c§l存在しない土地です")
            return
        }

        //賃料ボタン
        val rentButton = Button(Material.DIAMOND)
        rentButton.cmd(1)

        rentButton.title("§e§l賃料を設定する(コマンドを使います)")
        rentButton.lore(mutableListOf("§f賃料を設定します","§c金額を0にすると賃料が発生しなくなります"))
        rentButton.setClickAction{
            p.sendMessage(
                Component.text(Plugin.prefix).append(
                    Component.text("§b§l§n[ここをクリック]")
                .clickEvent(ClickEvent.suggestCommand("/mre setrent ${user.regionId} $name "))))
            p.closeInventory()
        }
        setButton(rentButton,1)

        //権限設定
        val permissionButton = Button(Material.EMERALD_BLOCK)
        permissionButton.title("§f§l権限の設定")
        permissionButton.lore(mutableListOf(
            "§a§l現在の設定",
            "§7§l全権限:${if (user.allowAll) "§a§lo" else "§c§lx"}",
            "§7§lブロック:${if (user.allowBlock) "§a§lo" else "§c§lx"}",
            "§7§lチェストや樽:${if (user.allowInv) "§a§lo" else "§c§lx"}",
            "§7§lドア:${if (user.allowDoor) "§a§lo" else "§c§lx"}"
        ))
        permissionButton.setClickAction{
            PermissionMenu(p,user).open()
        }
        setButton(permissionButton,4)

        //退去
        val deleteButton = Button(Material.BARRIER)
        deleteButton.title("§c§l退去させる")
        deleteButton.setClickAction{
            user.asyncDelete()
            sendMessage(p, "§a§l退去できました！")
            p.closeInventory()
        }
        setButton(deleteButton,7)

    }
}

class PermissionMenu(p:Player,val user: User) : MenuFramework(p, 9,"${Bukkit.getOfflinePlayer(user.uuid).name}の権限"){

    override fun init() {

        val allButton = Button(if (user.allowAll) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK)
        allButton.title("§e§l全権限(付与注意!)")
        allButton.setClickAction{
            user.allowAll = !user.allowAll
            user.asyncSave()
            PermissionMenu(p,user).open()
        }
        setButton(allButton,1)

        val blockButton = Button(if (user.allowBlock) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK)
        blockButton.title("§e§lブロックの設置破壊")
        blockButton.setClickAction{
            user.allowBlock = !user.allowBlock
            user.asyncSave()
            PermissionMenu(p,user).open()
        }
        setButton(blockButton,3)

        val invButton = Button(if (user.allowInv) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK)
        invButton.title("§e§l樽やかまどなどのインベントリ")
        invButton.setClickAction{
            user.allowInv = !user.allowInv
            user.asyncSave()
            PermissionMenu(p,user).open()
        }
        setButton(invButton,5)

        val doorButton = Button(if (user.allowDoor) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK)
        doorButton.title("§e§lドアの開閉")
        doorButton.setClickAction{
            user.allowDoor = !user.allowDoor
            user.asyncSave()
            PermissionMenu(p,user).open()
        }
        setButton(doorButton,7)

    }
}