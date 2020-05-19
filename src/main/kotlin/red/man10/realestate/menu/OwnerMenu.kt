package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Constants
import red.man10.realestate.Constants.Companion.mysqlQueue
import red.man10.realestate.Constants.Companion.ownerData
import red.man10.realestate.Constants.Companion.prefix
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.Constants.Companion.sendMessage
import red.man10.realestate.Constants.Companion.sendSuggest
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.regionUserDatabase
import red.man10.realestate.menu.InventoryMenu.Companion.IS
import red.man10.realestate.menu.InventoryMenu.Companion.getId
import red.man10.realestate.region.RegionUserDatabase
import java.util.*
import kotlin.collections.HashMap

class OwnerMenu(val pl : Plugin) : Listener{

    val regionCustomMenu = "${prefix}§a§l土地の設定"
    val ownerMenu = "${prefix}§a§lオーナーメニュー"
    val customRegionData = "${prefix}§a§l土地の詳細設定"
    val customUserMenu = "${prefix}§a§l住人の設定"
    val customUserData = "${prefix}§a§l住人の詳細設定"
    val changeStatus = "${prefix}§a§lステータスの変更"
    val changeRent = "${prefix}§a§l賃料設定"
    val changeRentSpan = "${prefix}§a§lスパン設定"
    val perm = "$prefix§3§l権限設定"

    var mysql : MySQLManager? = null

    val map = HashMap<Pair<UUID,Int>,PermList>()


    var loadItem : ItemStack

    init {
        mysql = MySQLManager(pl,"mreOwnerMenu")
        loadItem = IS(pl,Material.CLOCK,"§e§l現在データの読み込み中です....", mutableListOf(),"loading")
    }

    //リージョンの管理メニュ
    fun regionCustomMenu(p: Player, id:Int){

        val data = regionData[id]

        if (data == null){
            sendMessage(p,"§e§l存在しない土地です")
            p.closeInventory()
            return
        }

        if (data.owner_uuid != p.uniqueId){
            sendMessage(p,"§e§lあなたはこの土地のオーナーではありません")
            p.closeInventory()
            return
        }

        val inv = Bukkit.createInventory(null,27,regionCustomMenu)

        inv.setItem(0,IS(pl,Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"back"))
        inv.setItem(11,IS(pl,Material.PAPER,"§f§l土地の詳細設定", mutableListOf(),"$id"))
        inv.setItem(13,IS(pl,Material.PLAYER_HEAD,"§f§l住人の管理", mutableListOf(),"$id"))
        inv.setItem(15,IS(pl,Material.PAPER,"§f§l住人の追加", mutableListOf(),"$id"))

        p.openInventory(inv)

    }
    ///////////////////////
    //リージョンの詳細設定
    ///////////////////////
    fun customRegionData(p: Player, id:Int){

        val inv = Bukkit.createInventory(null,54,customRegionData)

        val data = regionData[id]?:return

        inv.setItem(0,IS(pl,Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"$id"))
        inv.setItem(10,IS(pl,Material.COMPASS,"§e§lステータス", mutableListOf("§a現在のステータス：${data.status}"),"$id"))
        inv.setItem(13,IS(pl,Material.EMERALD,"§e§l料金設定",
                mutableListOf("§e現在の料金：${String.format("%,.1f",data.price)}"),"$id"))

        inv.setItem(16,IS(pl,Material.ENDER_PEARL,"§a§lテレポート設定", mutableListOf(),"$id"))

        inv.setItem(38,IS(pl,Material.CLOCK,"§b§l賃貸設定",
                mutableListOf("§e現在の賃料：${String.format("%,.1f",data.rent)}"
                        ,"§aスパン：${when (data.span) {
                            0 -> "一ヶ月ごと"
                            1 -> "一週間ごと"
                            else -> "一日ごと"
                        }}"),"$id"))

        inv.setItem(42,IS(pl,Material.PLAYER_HEAD,"§3§lオーナーの変更", mutableListOf(),"$id"))

        inv.setItem(8, IS(pl,Material.GREEN_STAINED_GLASS_PANE,"§a§l自分の土地にテレポートする", mutableListOf(),"$id"))

        p.openInventory(inv)

    }

    //リージョンのステータスの変更
    fun changeStatus(p:Player,id:Int){
        val inv = Bukkit.createInventory(null,9,changeStatus)

        inv.setItem(1, IS(pl,Material.EMERALD,"§a§l販売中", mutableListOf(),"$id"))
        inv.setItem(4, IS(pl,Material.GREEN_WOOL,"§a§lフリー", mutableListOf(),"$id"))
        inv.setItem(7, IS(pl,Material.OAK_DOOR,"§a§l保護", mutableListOf(),"$id"))

        p.openInventory(inv)
    }

    //賃貸設定
    fun changeRentSetting(p:Player, id:Int){

        val inv = Bukkit.createInventory(null,9,changeRent)

        inv.setItem(2, IS(pl,Material.EMERALD,"§e§l賃料の変更", mutableListOf(),"$id"))
        inv.setItem(6, IS(pl,Material.CLOCK,"§e§lスパンの変更", mutableListOf(),"$id"))

        p.openInventory(inv)
    }

    //賃料の徴収スパンの設定
    fun changeSpan(p:Player,id: Int){
        val inv = Bukkit.createInventory(null,9,changeRentSpan)

        inv.setItem(1, IS(pl,Material.EMERALD,"§a§l一日ごと", mutableListOf(),"$id"))
        inv.setItem(4, IS(pl,Material.GREEN_WOOL,"§a§l一週間ごと", mutableListOf(),"$id"))
        inv.setItem(7, IS(pl,Material.OAK_DOOR,"§a§l一ヶ月ごと", mutableListOf(),"$id"))

        p.openInventory(inv)
    }

    //リージョンに登録されたユーザーのデータを取得
    fun customUserMenu(p:Player,id:Int,page:Int){

        val inv = Bukkit.createInventory(null,54, customUserMenu)

        inv.setItem(22, loadItem)

        Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {

            val list = loadUsersList(id,page)

            if (list.isNullOrEmpty()){

                sendMessage(p,"§3§lこの土地には住人はいないようです")

                p.closeInventory()
                return@Runnable
            }

            inv.remove(loadItem)

            for (uuid in list){
                val head = ItemStack(Material.PLAYER_HEAD)
                val meta = head.itemMeta as SkullMeta

                val user =  Bukkit.getOfflinePlayer(UUID.fromString(uuid))

                meta.owningPlayer = user
                meta.setDisplayName("§6§l${user.name}")
                meta.lore = mutableListOf(if (user.isOnline){"§aOnline"}else{"§4§lOffline"})

                meta.persistentDataContainer.set(NamespacedKey(pl,"id"), PersistentDataType.STRING,"$uuid,$id")

                head.itemMeta = meta

                inv.addItem(head)
            }


            val back = IS(pl,Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"back")

            for (i in 45..53){
                inv.setItem(i,back)
            }

            if (inv.getItem(44) != null){

                val next = IS(pl,Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l次のページ", mutableListOf(),"next")

                val meta = next.itemMeta
                //クリックしたら開くページを保存
                meta.persistentDataContainer.set(NamespacedKey(pl,"page"), PersistentDataType.INTEGER,page-1)
                next.itemMeta = meta

                inv.setItem(51,next)
                inv.setItem(52,next)
                inv.setItem(53,next)

            }

            if (page!=0){
                val previous = IS(pl,Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l前のページ", mutableListOf(),"previous")

                val meta = previous.itemMeta
                //クリックしたら開くページを保存
                meta.persistentDataContainer.set(NamespacedKey(pl,"page"), PersistentDataType.INTEGER,page-1)
                previous.itemMeta = meta


                inv.setItem(45,previous)
                inv.setItem(46,previous)
                inv.setItem(47,previous)

            }

        })

        p.openInventory(inv)

    }

    //ユーザーの設定
    fun customUserData(p:Player,id:Int,uuid:UUID){

        val inv = Bukkit.createInventory(null,9,customUserData)

        inv.setItem(1, IS(pl,Material.RED_STAINED_GLASS_PANE,"§3§l権限設定", mutableListOf(),"$uuid,$id"))
        inv.setItem(4, IS(pl,Material.COMPASS,"§a§l賃料を徴収する", mutableListOf(),"$uuid,$id"))
        inv.setItem(7,IS(pl,Material.REDSTONE_BLOCK,"§4§l住人を退去", mutableListOf(),"$uuid,$id"))

        p.openInventory(inv)
    }

    //権限設定
    fun customPerm(p:Player,id:Int,uuid: UUID) {

        var permData:PermList

        val inv = Bukkit.createInventory(null,54,perm)

        inv.setItem(22, loadItem)

        Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
            permData = getPerms(id,uuid)

            inv.setItem(13,IS(pl,
                    if (permData.allowAll){Material.LIME_STAINED_GLASS_PANE }
                    else{Material.RED_STAINED_GLASS_PANE},"§3§l全権限", mutableListOf(),"$uuid,$id"))
            inv.setItem(22, IS(pl,
                    if (permData.allowBlock){Material.LIME_STAINED_GLASS_PANE }
                    else{Material.RED_STAINED_GLASS_PANE},"§3§lブロックの設置、破壊権限", mutableListOf(),"$uuid,$id"))
            inv.setItem(31, IS(pl,
                    if (permData.allowDoor){Material.LIME_STAINED_GLASS_PANE }
                    else{Material.RED_STAINED_GLASS_PANE},"§3§lドアなど、右クリックで触るものに対する権限", mutableListOf(),"$uuid,$id"))
            inv.setItem(40, IS(pl,
                    if (permData.allowInv){Material.LIME_STAINED_GLASS_PANE }
                    else{Material.RED_STAINED_GLASS_PANE},"§3§lインベントリを開く権限", mutableListOf(),"$uuid,$id"))

            inv.setItem(0,IS(pl,Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"$uuid,$id"))

            inv.setItem(8,IS(pl,Material.YELLOW_STAINED_GLASS_PANE,"§3§lセーブ", mutableListOf(),"$uuid,$id"))

        })

        p.openInventory(inv)
    }

    @Synchronized
    fun loadUsersList(id:Int, page: Int): MutableList<String>? {

        val rs = mysql!!.query("SELECT `uuid` FROM `region_user` WHERE `region_id`='$id' LIMIT ${page*45}, ${(page+1)*45};")?:return null

        val list = mutableListOf<String>()

        while (rs.next()){
            list.add(rs.getString("uuid"))
        }

        rs.close()
        mysql!!.close()

        return list
    }

    @Synchronized
    fun getPerms(id:Int,uuid:UUID): PermList {

        val p = Bukkit.getOfflinePlayer(uuid)
        val data = PermList()

        if (map[Pair(uuid,id)]!=null){
            return map[Pair(uuid,id)]!!
        }

        if (p.isOnline){
            val pd = regionUserData[p]!![id]!!

            data.allowAll = pd.allowAll
            data.allowBlock = pd.allowBlock
            data.allowDoor = pd.allowDoor
            data.allowInv = pd.allowInv
            return data
        }

        val rs = mysql!!.query("SELECT * FROM `region_user` WHERE `region_id`=$id AND `uuid`='$uuid';")!!

        rs.next()

        data.allowAll = rs.getInt("allow_all")==1
        data.allowBlock = rs.getInt("allow_block")==1
        data.allowInv = rs.getInt("allow_inv")==1
        data.allowDoor = rs.getInt("allow_door")==1

        rs.close()
        mysql!!.close()

        map[Pair(uuid,id)] = data

        return data
    }

    fun savePerm(data:PermList,uuid: UUID,id: Int){

        val sql = "UPDATE `region_user` " +
                "SET " +
                "`allow_all`='${if (data.allowAll){1}else{0}}'," +
                "`allow_block`='${if (data.allowBlock){1}else{0}}'," +
                "`allow_inv`='${if (data.allowInv){1}else{0}}'," +
                "`allow_door`='${if (data.allowDoor){1}else{0}}'" +
                " WHERE `uuid`='${uuid}' AND `region_id`='$id';"
        mysqlQueue.add(sql)

    }

    ////////////////////////
    //リージョンを持っているユーザーがリージョンを管理するメニュー
    ////////////////////////
    fun openOwnerSetting(p:Player, page:Int){

        val inv = Bukkit.createInventory(null,54,ownerMenu)

        val list = ownerData[p]?:return

        for (i in page*45 .. (page+1)*45){

            if (list.size <=i)break

            val d = regionData[list[i]]?:continue

            val icon = IS(pl,Material.PAPER,d.name,mutableListOf(
                    "§e§lID:${list[i]}",
                    "§a§lStatus:${d.status}"
            ),list[i].toString())

            inv.addItem(icon)

        }

        val back = IS(pl,Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"back")

        for (i in 45..53){
            inv.setItem(i,back)
        }

        if (inv.getItem(44) != null){

            val next = IS(pl,Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l次のページ", mutableListOf(),"next")

            val meta = next.itemMeta
            //クリックしたら開くページを保存
            meta.persistentDataContainer.set(NamespacedKey(pl,"page"), PersistentDataType.INTEGER,page-1)
            next.itemMeta = meta

            inv.setItem(51,next)
            inv.setItem(52,next)
            inv.setItem(53,next)

        }

        if (page!=0){
            val previous = IS(pl,Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l前のページ", mutableListOf(),"previous")

            val meta = previous.itemMeta
            //クリックしたら開くページを保存
            meta.persistentDataContainer.set(NamespacedKey(pl,"page"), PersistentDataType.INTEGER,page-1)
            previous.itemMeta = meta


            inv.setItem(45,previous)
            inv.setItem(46,previous)
            inv.setItem(47,previous)

        }

        p.openInventory(inv)
    }




    @EventHandler
    fun invEvent(e: InventoryClickEvent){

        val name = e.view.title
        val item = e.currentItem?:return
        val p = e.whoClicked as Player

        if (!item.hasItemMeta())return

        if (getId(item,pl) == "loading"){
            e.isCancelled = true
            return
        }

        //オーナーメニュ
        if (name == ownerMenu){
            e.isCancelled = true
            when(getId(item,pl)){

                "back"->pl.invmain.openMainMenu(p)
                "next"->openOwnerSetting(p,item.itemMeta.persistentDataContainer[NamespacedKey(pl,"page"), PersistentDataType.INTEGER]!!)
                "previous"->openOwnerSetting(p,item.itemMeta.persistentDataContainer[NamespacedKey(pl,"page"), PersistentDataType.INTEGER]!!)
                else ->{
                    regionCustomMenu(p, getId(item,pl).toInt())
                }
            }
        }

        //リージョンの管理メニュー
        if (name == regionCustomMenu){
            e.isCancelled = true
            when(e.slot){
                0->openOwnerSetting(p,0)
                11->customRegionData(p, getId(item,pl).toInt())
                13->customUserMenu(p, getId(item,pl).toInt(),0)
                15-> {
                    p.closeInventory()
                    sendSuggest(p,"§e§lユーザー名を入力してください！","/mre adduser ${getId(item,pl)} ")
                }
            }
        }

        //リージョン設定
        if (name == customRegionData){
            e.isCancelled = true
            when(e.slot){
                0->regionCustomMenu(p, getId(item,pl).toInt())                              //オーナーメニュに戻る
                10->changeStatus(p, getId(item,pl).toInt())                                 //ステータスの変更
                13->{
                    p.closeInventory()
                    sendSuggest(p,"§e§l販売する料金を入力してください！","/mre changeprice ${getId(item, pl)} ")
                } // 料金変更
                16->p.performCommand("mre settp ${getId(item, pl)}")              //テレポート地点変更
                38->changeRentSetting(p, getId(item,pl).toInt())                              //賃料設定
                42->{
                    p.closeInventory()
                    sendSuggest(p,"§e§l変更するオーナーの名前を入力してください！","/mre changeowner ${getId(item, pl)} ")
                } //オーナー変更

                8->{
                    p.closeInventory()
                    p.performCommand("mre tp ${getId(item,pl)}")
                }
            }
        }

        //ステータス
        if (name == changeStatus){
            e.isCancelled = true
            when(e.slot){
                1->p.performCommand("mre changestatus ${getId(item,pl)} OnSale")
                4->p.performCommand("mre changestatus ${getId(item,pl)} Free")
                7->p.performCommand("mre changestatus ${getId(item,pl)} Protected")
            }
        }

        //賃料の設定
        if (name == changeRent){
            e.isCancelled = true
            when(e.slot){
                2-> {
                    p.closeInventory()
                    sendSuggest(p,"§e§l賃料を入力してください！","/mre rent ${getId(item, pl)} ")
                }
                6->changeSpan(p,getId(item, pl).toInt())
            }

        }

        //スパンの変更
        if (name == changeRentSpan){
            e.isCancelled = true
            when(e.slot){
                1->p.performCommand("mre span ${getId(item,pl)} 2")
                4->p.performCommand("mre span ${getId(item,pl)} 1")
                7->p.performCommand("mre span ${getId(item,pl)} 0")
            }

        }

        //ユーザーリストの表示
        if (name  == customUserMenu){

            e.isCancelled = true

            val id = getId(e.inventory.getItem(0)!!,pl).split(",")[1].toInt()

            when(getId(item,pl)){

                "back"->regionCustomMenu(p,id)
                "next"->customUserMenu(p,id,item.itemMeta.persistentDataContainer[NamespacedKey(pl,"page"), PersistentDataType.INTEGER]!!)
                "previous"->customUserMenu(p,id,item.itemMeta.persistentDataContainer[NamespacedKey(pl,"page"), PersistentDataType.INTEGER]!!)
                else ->{
                    val uuid = UUID.fromString(getId(item,pl).split(",")[0])

                    customUserData(p,id, uuid)
                }
            }
        }

        //ユーザーの設定
        if (name == customUserData){

            e.isCancelled = true
            val uuid = UUID.fromString(getId(item,pl).split(",")[0])
            val id = getId(item,pl).split(",")[1].toInt()

            when(e.slot){
                1->customPerm(p,id,uuid)
                4->{
                    val user = Bukkit.getOfflinePlayer(uuid)
                    if (user.isOnline && user.player !=null){
                        p.closeInventory()
                        regionUserDatabase.setRent(user.player!!,id)
                        sendMessage(p,"§a§l登録成功！、試験実装のため不具合があるかもしれません！")
                        return
                    }
                    sendMessage(p,"§3§lユーザーがオフラインです")
                    return
                }
                7->{
                    if (Bukkit.getOfflinePlayer(uuid).isOnline){
                        regionUserDatabase.removeUserData(id,Bukkit.getOfflinePlayer(uuid).player!!)
                    }else{
                        regionUserDatabase.removeUserData(id,uuid)
                    }
                    p.closeInventory()
                    sendMessage(p,"削除しました")
                }
            }
        }

        //権限設定
        if (name == perm){
            e.isCancelled = true

            val uuid = UUID.fromString(getId(item,pl).split(",")[0])
            val id = getId(item,pl).split(",")[1].toInt()
            val data = map[Pair(uuid,id)]?:PermList()

            when(e.slot){

                0->{
                    customUserData(p,id,uuid)
                    return
                }
                13->data.allowAll = item.type == Material.RED_STAINED_GLASS_PANE
                22->data.allowBlock = item.type == Material.RED_STAINED_GLASS_PANE
                31->data.allowDoor = item.type == Material.RED_STAINED_GLASS_PANE
                40->data.allowInv = item.type == Material.RED_STAINED_GLASS_PANE
                8->{
                    val user = Bukkit.getOfflinePlayer(uuid)
                    if (user.isOnline){
                        val d = regionUserData[user]!![id]!!

                        d.allowAll      = data.allowAll
                        d.allowInv      = data.allowInv
                        d.allowDoor     = data.allowDoor
                        d.allowBlock    = data.allowBlock

                    }
                    savePerm(data, uuid, id)
                    sendMessage(p,"§e§l保存しました！")
                    p.closeInventory()
                    return
                }
            }

            map[Pair(uuid,id)] = data
            customPerm(p,id,uuid)
        }

    }

    class PermList{
        var allowAll = false
        var allowBlock = false
        var allowInv = false
        var allowDoor = false

    }

}