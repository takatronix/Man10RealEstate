package red.man10.realestate.menu

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import red.man10.realestate.Plugin.Companion.customInventory
import red.man10.realestate.Plugin.Companion.region
import red.man10.realestate.Plugin.Companion.user
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.menu.CustomInventory.Companion.InventoryID.*
import red.man10.realestate.region.User
import java.util.*
import kotlin.collections.HashMap

class InventoryMenu {

    val loadItem : ItemStack = customInventory.IS(Material.CLOCK,"§e§l現在データの読み込み中です.....")
    val back = customInventory.IS(Material.RED_STAINED_GLASS_PANE,"§c§l戻る")

    init {
        customInventory.setData(back,"type","back")
    }

    /**
     * メインメニュー
     */
    fun mainMenu(p:Player){

        val inventory = customInventory.createInventory(9,"§a§lメインメニュー")

        inventory.setItem(1,customInventory.IS(Material.PAPER,"§f§l自分が管理できる土地の管理をする"))
        inventory.setItem(4,customInventory.IS(Material.NETHER_STAR,"§f§lいいねした土地を確認する"))
        inventory.setItem(7,customInventory.IS(Material.STONE,""))

        p.openInventory(inventory)
        customInventory.open(p, MAIN_MENU)

    }

    /**
     * いいねのリスト
     */
    fun openBookmark(p:Player,page:Int){

        val inventory = customInventory.createInventory(54,"§a§lいいねしたリスト")

        val list = user.likeData[p]?:return

        for (i in page*45 .. (page+1)*45){

            if (list.size <=i)break

            val id = list[i]

            val rg = region.get(id)?:continue

            val icon = customInventory.IS(Material.PAPER,rg.name, mutableListOf(
                    "§e§lID:$id",
                    "§b§lOWNER:${region.getOwner(rg)}",
                    "§a§lStatus:${rg.status}",
                    "§fX:${rg.teleport.blockX}",
                    "§fY:${rg.teleport.blockY}",
                    "§fZ:${rg.teleport.blockZ}"
            ))
            customInventory.setData(icon,"id",id.toString())

            inventory.addItem(icon)

        }

        for (i in 45..53){
            inventory.setItem(i,back)
        }

        if (inventory.getItem(44)!=null){

            val next = customInventory.IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l次のページ")
            customInventory.setData(next,"type","next")
            customInventory.setData(next,"page","$page")

            for (i in 51..53){
                inventory.setItem(i,next)
            }

        }

        if (page!=0){
            val next = customInventory.IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l前のページ")
            customInventory.setData(next,"type","previous")
            customInventory.setData(next,"page","$page")

            for (i in 45..47){
                inventory.setItem(i,next)
            }

        }

        p.openInventory(inventory)
        customInventory.open(p,BOOKMARK)

    }

    /**
     * 権限を持っているリージョンのリストを表示
     */
    fun openRegionList(p:Player,page:Int){

        val inventory = customInventory.createInventory(54,"§a§l土地のリスト")

        val list = user.ownerList[p]

        if (list.isNullOrEmpty()){
            sendMessage(p,"§c§lあなたは自分の土地を持っていません")
            customInventory.close(p)
            return
        }

        for (i in page*45..(page+1)*45){

            if (list.size<=i)break

            val rg = region.get(list[i])?:continue

            val icon = customInventory.IS(Material.PAPER,rg.name, mutableListOf(
                    "§e§lID:${list[i]}",
                    "§a§lStatus:${rg.status}"
            ))

            customInventory.setData(icon,"id","${list[i]}")

            inventory.addItem(icon)

        }

        for (i in 45..53){
            inventory.setItem(i,back)
        }

        if (inventory.getItem(44)!=null){

            val next = customInventory.IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l次のページ")
            customInventory.setData(next,"type","next")
            customInventory.setData(next,"page","$page")

            for (i in 51..53){
                inventory.setItem(i,next)
            }

        }

        if (page!=0){
            val next = customInventory.IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l前のページ")
            customInventory.setData(next,"type","previous")
            customInventory.setData(next,"page","$page")

            for (i in 45..47){
                inventory.setItem(i,next)
            }

        }

        p.openInventory(inventory)
        customInventory.open(p,REGION_LIST)

    }

    /**
     * リージョンの設定メニュ
     */
    fun regionMenu(p:Player,id:Int){

        val data = region.get(id)?:return

        val inventory = customInventory.createInventory(27,"§a§l土地の設定")

        inventory.setItem(0,back)
        inventory.setItem(11, customInventory.IS(Material.PAPER,"§f§l土地の詳細設定", mutableListOf(
                "§f§l現在の設定",
                "§7§lStatus:${data.status}",
                "§8§lPrice:${data.price}",
                "§7§lSpan:${when (data.span) {
                    0 -> "一ヶ月ごと"
                    1 -> "一週間ごと"
                    else -> "一日ごと"}}"
        ),id))

        inventory.setItem(13, customInventory.IS(Material.PLAYER_HEAD,"§b§l住人の管理", mutableListOf(),id))
        inventory.setItem(15, customInventory.IS(Material.EMERALD_BLOCK,"§a§l住人の追加", mutableListOf(),id))

        p.openInventory(inventory)
        customInventory.open(p,REGION_MENU)

    }

    /**
     * 土地の詳細設定
     */
    fun regionSetting(p:Player,id:Int){

        val inventory = customInventory.createInventory(54,"§6§l土地の詳細設定")

        val rg = region.get(id)?:return

        val backBtn = back.clone()
        customInventory.setData(backBtn,"id","$id")

        inventory.setItem(0,backBtn)

        inventory.setItem(10,customInventory.IS(Material.COMPASS,"§e§lステータス", mutableListOf("§a現在のステータス：${rg.status}"),id))
        inventory.setItem(13,customInventory.IS(Material.EMERALD,"§e§l料金設定",
                mutableListOf("§e現在の料金：${String.format("%,.1f",rg.price)}"),id))

        inventory.setItem(16,customInventory.IS(Material.ENDER_PEARL,"§a§lテレポート設定", mutableListOf("§c§l現在テレポートは使用できません"),id))

        inventory.setItem(38,customInventory.IS(Material.CLOCK,"§b§l賃貸スパン設定",
                mutableListOf("§a現在設定されているスパン：${when (rg.span) {
                    0 -> "一ヶ月ごと"
                    1 -> "一週間ごと"
                    else -> "一日ごと"
                }}"),id))

        inventory.setItem(42, customInventory.IS(Material.PLAYER_HEAD,"§3§lオーナーの変更", mutableListOf(),id))

        p.openInventory(inventory)
        customInventory.open(p,REGION_SETTING)

    }

    /**
     * ステータスの変更
     */
    fun statusMenu(p:Player,id:Int){

        val inventory = customInventory.createInventory(9,"§a§lステータスの変更")

        val backBtn = back.clone()
        customInventory.setData(backBtn,"id","$id")

        inventory.setItem(0,backBtn)

        inventory.setItem(1, customInventory.IS(Material.RED_STAINED_GLASS_PANE,"§c§l無法地帯", mutableListOf("§f§l保護を外します"),id))
        inventory.setItem(3, customInventory.IS(Material.LIME_WOOL,"§a§lフリー", mutableListOf("§f§lブロックの設置破壊以外できる"),id))
        inventory.setItem(5, customInventory.IS(Material.EMERALD,"§e§l販売中", mutableListOf(),id))
        inventory.setItem(7, customInventory.IS(Material.IRON_DOOR,"§c§l保護", mutableListOf(),id))

        p.openInventory(inventory)
        customInventory.open(p,REGION_STATUS)
    }

    /**
     * スパンの変更
     */
    fun spanMenu(p:Player,id:Int){

        val inventory = customInventory.createInventory(9,"§a§lスパンの変更")

        val backBtn = back.clone()
        customInventory.setData(backBtn,"id","$id")

        inventory.setItem(0,backBtn)

        inventory.setItem(1, customInventory.IS(Material.ENDER_PEARL,"§c§l一日ごと", mutableListOf(),id))
        inventory.setItem(4, customInventory.IS(Material.ENDER_PEARL,"§b§l一週間ごと", mutableListOf(),id))
        inventory.setItem(7, customInventory.IS(Material.ENDER_PEARL,"§a§l一ヶ月ごと", mutableListOf(),id))

        p.openInventory(inventory)
        customInventory.open(p,REGION_SPAN)

    }


    /**
     * 住人のリストを表示
     */
    fun userList(p:Player,id:Int,page:Int){

        val inventory = customInventory.createInventory(54,"§a§l住人のリスト")

        inventory.setItem(22,loadItem)

        GlobalScope.launch {
            val list = user.loadUsers(id,page)

            if (list == null){
                sendMessage(p,"§c§lこの土地には住人がいないようです")
                customInventory.close(p)
                return@launch
            }

            inventory.remove(loadItem)

            for (d in list){


                val user = Bukkit.getOfflinePlayer(UUID.fromString(d.first))
                val userData = d.second

                if (p.uniqueId == user.uniqueId)continue

                val head = ItemStack(Material.PLAYER_HEAD)
                val meta = head.itemMeta as SkullMeta

                if (user.isOnline){
                    meta.owningPlayer = user
                }

                meta.setDisplayName("§6§l${user.name}")
                meta.lore = mutableListOf(
                        if (user.isOnline){"§aOnline"}else{"§4§lOffline"},
                        "§7§lステータス:${userData.status}",
                        "§8§l賃料:${userData.rent}"
                )

                head.itemMeta = meta
                customInventory.setData(head,"id","$id")
                customInventory.setData(head,"uuid","${user.uniqueId}")

                inventory.addItem(head)

            }

            //////////////////戻る進む、バックボタン
            val backBtn = back.clone()
            customInventory.setData(backBtn,"id","$id")

            for (i in 45..53){
                inventory.setItem(i,backBtn)
            }

            if (inventory.getItem(44)!=null){

                val next = customInventory.IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l次のページ")
                customInventory.setData(next,"type","next")
                customInventory.setData(next,"page","$page")
                customInventory.setData(next,"id","$id")

                for (i in 51..53){
                    inventory.setItem(i,next)
                }

            }

            if (page!=0){
                val next = customInventory.IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l前のページ")
                customInventory.setData(next,"type","previous")
                customInventory.setData(next,"page","$page")
                customInventory.setData(next,"id","$id")

                for (i in 45..47){
                    inventory.setItem(i,next)
                }
            }

        }

        p.openInventory(inventory)
        customInventory.open(p,USER_LIST)

    }

    fun userMenu(p:Player,id: Int,uuid: UUID){

        val inventory = customInventory.createInventory(27,"§6§l${Bukkit.getOfflinePlayer(uuid).name}§a§lの設定")

        val backBtn = back.clone()
        customInventory.setData(backBtn,"id","$id")

        inventory.setItem(0,backBtn)

        inventory.setItem(11, customInventory.IS(Material.RED_STAINED_GLASS_PANE,"§3§l権限設定", mutableListOf(),uuid,id))
        inventory.setItem(13, customInventory.IS(Material.EMERALD,"§a§l賃料を設定する", mutableListOf(),uuid,id))
        inventory.setItem(15,customInventory.IS(Material.REDSTONE_BLOCK,"§4§l住人を退去させる", mutableListOf(),uuid,id))

        p.openInventory(inventory)
        customInventory.open(p,USER_MENU)

    }

    fun setPermission(p:Player,id: Int,uuid:UUID) {

        val inventory = customInventory.createInventory(54,"§a§l権限の詳細設定")

        inventory.setItem(22,loadItem)

        GlobalScope.launch {
            val data = cache[Pair(uuid,id)]?:user.get(uuid,id)!!

            val backBtn = back.clone()
            customInventory.setData(backBtn,"id","$id")
            customInventory.setData(backBtn,"uuid","$uuid")

            inventory.setItem(0,backBtn)

            inventory.setItem(13, customInventory.IS(if (data.allowAll){Material.LIME_STAINED_GLASS_PANE }
            else{Material.RED_STAINED_GLASS_PANE},"§3§l全権限", mutableListOf(),uuid,id))

            inventory.setItem(22, customInventory.IS(if (data.allowBlock){Material.LIME_STAINED_GLASS_PANE }
            else{Material.RED_STAINED_GLASS_PANE},"§3§lブロックの設置、破壊", mutableListOf(),uuid,id))

            inventory.setItem(31, customInventory.IS(if (data.allowInv){Material.LIME_STAINED_GLASS_PANE }
            else{Material.RED_STAINED_GLASS_PANE},"§3§lチェストなどのインベントリを開く", mutableListOf(),uuid,id))

            inventory.setItem(40, customInventory.IS(if (data.allowDoor){Material.LIME_STAINED_GLASS_PANE }
            else{Material.RED_STAINED_GLASS_PANE},"§3§lドアなどの右クリック、左クリック(看板を除く)", mutableListOf(),uuid,id))

            if (cache[Pair(uuid,id)] == null){
                cache[Pair(uuid,id)] = data
            }

        }

        p.openInventory(inventory)
        customInventory.open(p,USER_PERMISSION)
    }

    companion object{
        val cache = HashMap<Pair<UUID,Int>,User.UserData>()
    }




}