package red.man10.realestate.menu

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Utility.format
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.menu.CustomInventory.IS
import red.man10.realestate.menu.CustomInventory.InventoryID.*
import red.man10.realestate.menu.CustomInventory.setData
import red.man10.realestate.region.Region
import red.man10.realestate.region.Region.formatStatus
import red.man10.realestate.region.User
import java.util.*
import kotlin.collections.HashMap

object InventoryMenu {

    private val loadItem: ItemStack = IS(Material.CLOCK, "§e§l現在データの読み込み中です.....")
    private val back = IS(Material.RED_STAINED_GLASS_PANE, "§c§l戻る")
    val cache = HashMap<Pair<UUID, Int>, User.UserData>()

    init {
        setData(back, "type", "back")
    }

    /**
     * メインメニュー
     */
    fun mainMenu(p: Player) {

        val inv = CustomInventory.createInventory(9, "§a§lメインメニュー")

        inv.setItem(1, IS(Material.PAPER, "§f§l自分が管理できる土地の管理をする"))
        inv.setItem(4, IS(Material.NETHER_STAR, "§f§lいいねした土地をみる"))
        inv.setItem(7, IS(Material.NETHERITE_INGOT, "§e§l支払う税金を見る",1))

        p.openInventory(inv)
        CustomInventory.open(p, MAIN_MENU)

    }

    /**
     * いいねのリスト
     */
    fun openBookmark(p: Player, page: Int) {

        val inv = CustomInventory.createInventory(54, "§a§lいいねしたリスト")

        val list = User.likeData[p] ?: return

        for (i in page * 45..(page + 1) * 45) {

            if (list.size <= i) break

            val id = list[i]

            val rg = Region.get(id) ?: continue

            val icon = IS(Material.PAPER, rg.name, mutableListOf(
                    "§7§lID:$id",
                    "§f§lオーナー:${Region.getOwner(rg)}",
                    "§7§lステータス:${formatStatus(rg.status)}",
                    "§f§lクリックでテレポート",

            ))
            setData(icon, "id", id.toString())

            inv.addItem(icon)

        }

        for (i in 45..53) {
            inv.setItem(i, back)
        }

        if (inv.getItem(44) != null) {

            val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§6§l次のページ")
            setData(next, "type", "next")
            setData(next, "page", "$page")

            for (i in 51..53) {
                inv.setItem(i, next)
            }

        }

        if (page != 0) {
            val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§6§l前のページ")
            setData(next, "type", "previous")
            setData(next, "page", "$page")

            for (i in 45..47) {
                inv.setItem(i, next)
            }

        }

        p.openInventory(inv)
        CustomInventory.open(p, BOOKMARK)

    }

    /**
     * 権限を持っているリージョンのリストを表示
     */
    fun openRegionList(p: Player, page: Int) {

        val inv = CustomInventory.createInventory(54, "§a§l土地のリスト")

        val list = User.ownerList[p]

        if (list.isNullOrEmpty()) {
            sendMessage(p, "§c§lあなたは自分の土地を持っていません")
            CustomInventory.close(p)
            return
        }

        for (i in page * 45..(page + 1) * 45) {

            if (list.size <= i) break

            val rg = Region.get(list[i]) ?: continue

            val icon = IS(Material.PAPER, rg.name, mutableListOf(
                    "§e§lID:${list[i]}",
                    "§a§lステータス:${formatStatus(rg.status)}"
            ))

            setData(icon, "id", "${list[i]}")

            inv.addItem(icon)

        }

        for (i in 45..53) {
            inv.setItem(i, back)
        }

        if (inv.getItem(44) != null) {

            val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§6§l次のページ")
            setData(next, "type", "next")
            setData(next, "page", "$page")

            for (i in 51..53) {
                inv.setItem(i, next)
            }

        }

        if (page != 0) {
            val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§6§l前のページ")
            setData(next, "type", "previous")
            setData(next, "page", "$page")

            for (i in 45..47) {
                inv.setItem(i, next)
            }

        }

        p.openInventory(inv)
        CustomInventory.open(p, REGION_LIST)

    }

    /**
     * リージョンの設定メニュ
     */
    fun regionMenu(p: Player, id: Int) {

        val data = Region.get(id) ?: return

        val inv = CustomInventory.createInventory(27, "§a§l土地の設定")

        inv.setItem(0, back)
        inv.setItem(11, IS(Material.PAPER, "§f§l土地の詳細設定", mutableListOf(
                "§f§l現在の設定",
                "§7§lステータス:${formatStatus(data.status)}",
                "§f§l値段:${format(data.price)}",
                "§7§l支払いスパン:${
                    when (data.span) {
                        0 -> "一ヶ月ごと"
                        1 -> "一週間ごと"
                        else -> "一日ごと"
                    }
                }"
        ), id)
        )

        inv.setItem(13, IS(Material.PLAYER_HEAD, "§b§l住人を管理する", mutableListOf(), id))
        inv.setItem(15, IS(Material.EMERALD_BLOCK, "§a§l住人を追加する", mutableListOf(), id))

        p.openInventory(inv)
        CustomInventory.open(p, REGION_MENU)

    }

    /**
     * 土地の詳細設定
     */
    fun regionSetting(p: Player, id: Int) {

        val inv = CustomInventory.createInventory(54, "§6§l土地の詳細設定")

        val rg = Region.get(id) ?: return

        val backBtn = back.clone()
        setData(backBtn, "id", "$id")

        inv.setItem(0, backBtn)

        inv.setItem(10, IS(Material.COMPASS, "§e§lステータス", mutableListOf("§a現在のステータス：${formatStatus(rg.status)}"), id))
        inv.setItem(13, IS(Material.EMERALD, "§e§l料金設定",
                mutableListOf("§e現在の料金：${format(rg.price)}"), id)
        )

        inv.setItem(16, IS(Material.ENDER_PEARL, "§a§lテレポート設定", mutableListOf("§a現在位置をテレポート地点にします"), id))

        inv.setItem(38, IS(Material.CLOCK, "§b§l賃貸スパン設定",
                mutableListOf("§a現在設定されているスパン：${
                    when (rg.span) {
                        0 -> "一ヶ月ごと"
                        1 -> "一週間ごと"
                        else -> "一日ごと"
                    }
                }"), id)
        )

        inv.setItem(42, IS(Material.PLAYER_HEAD, "§3§lオーナーの変更", mutableListOf(), id))

        p.openInventory(inv)
        CustomInventory.open(p, REGION_SETTING)

    }

    /**
     * ステータスの変更
     */
    fun statusMenu(p: Player, id: Int) {

        val inv = CustomInventory.createInventory(9, "§a§lステータスの変更")

        val backBtn = back.clone()
        setData(backBtn, "id", "$id")

        inv.setItem(0, backBtn)

        inv.setItem(1, IS(Material.RED_STAINED_GLASS_PANE, "§c§l無法地帯", mutableListOf("§f§l保護を外します"), id))
        inv.setItem(3, IS(Material.LIME_WOOL, "§a§lフリー", mutableListOf("§f§lブロックの設置破壊以外できる"), id))
        inv.setItem(5, IS(Material.EMERALD, "§e§l販売中", mutableListOf(), id))
        inv.setItem(7, IS(Material.IRON_DOOR, "§c§l保護", mutableListOf(), id))

        p.openInventory(inv)
        CustomInventory.open(p, REGION_STATUS)
    }

    /**
     * スパンの変更
     */
    fun spanMenu(p: Player, id: Int) {

        val inv = CustomInventory.createInventory(9, "§a§lスパンの変更")

        val backBtn = back.clone()
        setData(backBtn, "id", "$id")

        inv.setItem(0, backBtn)

        inv.setItem(1, IS(Material.ENDER_PEARL, "§c§l一日ごと", mutableListOf(), id))
        inv.setItem(4, IS(Material.ENDER_PEARL, "§b§l一週間ごと", mutableListOf(), id))
        inv.setItem(7, IS(Material.ENDER_PEARL, "§a§l一ヶ月ごと", mutableListOf(), id))

        p.openInventory(inv)
        CustomInventory.open(p, REGION_SPAN)

    }


    /**
     * 住人のリストを表示
     */
    fun userList(p: Player, id: Int, page: Int) {

        val inv = CustomInventory.createInventory(54, "§a§l住人のリスト")

        inv.setItem(22, loadItem)

        Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
            val list = User.loadUsers(id, page)

            if (list == null) {
                sendMessage(p, "§c§lこの土地には住人がいないようです")
                CustomInventory.close(p)
                return@Runnable
            }

            inv.remove(loadItem)

            for (d in list) {


                val user = Bukkit.getOfflinePlayer(UUID.fromString(d.first))
                val userData = d.second

                if (p.uniqueId == user.uniqueId) continue

                val head = ItemStack(Material.PLAYER_HEAD)
                val meta = head.itemMeta as SkullMeta

                if (user.isOnline) {
                    meta.owningPlayer = user
                }

                meta.displayName(Component.text("§f§l${user.name}"))
                meta.lore = mutableListOf(if (user.isOnline) { "§aオンライン" } else { "§4§lオフライン" },
                    "§7§lステータス:${if (userData.status=="Share") "§a§l共有されています" else "§c§lロックされています"}",
                    "§f§l賃料:${format(userData.rent)}"
                )

                head.itemMeta = meta
                setData(head, "id", "$id")
                setData(head, "uuid", "${user.uniqueId}")

                inv.addItem(head)

            }

            //////////////////戻る進む、バックボタン
            val backBtn = back.clone()
            setData(backBtn, "id", "$id")

            for (i in 45..53) {
                inv.setItem(i, backBtn)
            }

            if (inv.getItem(44) != null) {

                val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§6§l次のページ")
                setData(next, "type", "next")
                setData(next, "page", "$page")
                setData(next, "id", "$id")

                for (i in 51..53) {
                    inv.setItem(i, next)
                }

            }

            if (page != 0) {
                val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§6§l前のページ")
                setData(next, "type", "previous")
                setData(next, "page", "$page")
                setData(next, "id", "$id")

                for (i in 45..47) {
                    inv.setItem(i, next)
                }
            }

        })

        p.openInventory(inv)
        CustomInventory.open(p, USER_LIST)

    }

    fun userMenu(p: Player, id: Int, uuid: UUID) {

        val inv = CustomInventory.createInventory(27, "§6§l${Bukkit.getOfflinePlayer(uuid).name}§a§lの設定")

        val backBtn = back.clone()
        setData(backBtn, "id", "$id")

        inv.setItem(0, backBtn)

        inv.setItem(11, IS(Material.RED_STAINED_GLASS_PANE, "§3§l権限設定", mutableListOf(), uuid, id))
        inv.setItem(13, IS(Material.EMERALD, "§a§l賃料を設定する", mutableListOf(), uuid, id))
        inv.setItem(15, IS(Material.REDSTONE_BLOCK, "§4§l住人を退去させる", mutableListOf(), uuid, id))

        p.openInventory(inv)
        CustomInventory.open(p, USER_MENU)

    }

    fun setPermission(p: Player, id: Int, uuid: UUID) {

        val inv = CustomInventory.createInventory(54, "§a§l権限の詳細設定")

        inv.setItem(22, loadItem)

        Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
            val data = cache[Pair(uuid, id)] ?: User.get(uuid, id)!!

            val backBtn = back.clone()
            setData(backBtn, "id", "$id")
            setData(backBtn, "uuid", "$uuid")

            inv.setItem(0, backBtn)

            inv.setItem(13, IS(if (data.allowAll) {
                Material.LIME_STAINED_GLASS_PANE
            } else {
                Material.RED_STAINED_GLASS_PANE
            }, "§3§l§n管理権限", mutableListOf(), uuid, id))

            inv.setItem(22, IS(if (data.allowBlock) {
                Material.LIME_STAINED_GLASS_PANE
            } else {
                Material.RED_STAINED_GLASS_PANE
            }, "§3ブロックの設置と破壊", mutableListOf(), uuid, id))

            inv.setItem(31, IS(if (data.allowInv) {
                Material.LIME_STAINED_GLASS_PANE
            } else {
                Material.RED_STAINED_GLASS_PANE
            }, "§3チェストなどを開く", mutableListOf(), uuid, id))

            inv.setItem(40, IS(if (data.allowDoor) {
                Material.LIME_STAINED_GLASS_PANE
            } else {
                Material.RED_STAINED_GLASS_PANE
            }, "§3ドアなどの右クリック", mutableListOf(), uuid, id))

            if (cache[Pair(uuid, id)] == null) {
                cache[Pair(uuid, id)] = data
            }
        })

        p.openInventory(inv)
        CustomInventory.open(p, USER_PERMISSION)
    }
}

