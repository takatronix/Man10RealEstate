package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Constants
import red.man10.realestate.Constants.Companion.ownerData
import red.man10.realestate.Constants.Companion.prefix
import red.man10.realestate.Constants.Companion.sendMessage
import red.man10.realestate.Constants.Companion.sendSuggest
import red.man10.realestate.Plugin
import red.man10.realestate.menu.InventoryMenu.Companion.IS
import red.man10.realestate.menu.InventoryMenu.Companion.getId

class OwnerMenu(val pl : Plugin) : Listener{

    val regionCustomMenu = "${prefix}§a§l土地の設定"
    val ownerMenu = "${prefix}§a§lオーナーメニュー"
    val customRegionData = "${prefix}§a§l土地の詳細設定"
    val customUserMenu = "${prefix}§a§l住人の設定"
    val customUserData = "${prefix}§a§l住人の設定"
    val changeStatus = "${prefix}§a§lステータスの変更"
    val changeRent = "${prefix}§a§l賃料設定"
    val changeRentSpan = "${prefix}§a§lスパン設定"


    //リージョンの管理メニュ
    fun regionCustomMenu(p: Player, id:Int){

        val data = Constants.regionData[id]

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

        inv.setItem(0,IS(pl,Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"$id"))
        inv.setItem(10,IS(pl,Material.COMPASS,"§e§lステータス", mutableListOf(),"$id"))
        inv.setItem(13,IS(pl,Material.EMERALD,"§e§l料金設定", mutableListOf(),"$id"))
        inv.setItem(16,IS(pl,Material.ENDER_PEARL,"§a§lテレポート設定", mutableListOf(),"$id"))
        inv.setItem(38,IS(pl,Material.CLOCK,"§b§l賃貸設定", mutableListOf(),"$id"))
        inv.setItem(42,IS(pl,Material.PLAYER_HEAD,"§3§lオーナーの変更", mutableListOf(),"$id"))

        p.openInventory(inv)

    }

    fun changeStatus(p:Player,id:Int){
        val inv = Bukkit.createInventory(null,9,changeStatus)

        inv.setItem(1, IS(pl,Material.EMERALD,"§a§l販売中", mutableListOf(),"$id"))
        inv.setItem(4, IS(pl,Material.GREEN_WOOL,"§a§lフリー", mutableListOf(),"$id"))
        inv.setItem(7, IS(pl,Material.OAK_DOOR,"§a§l保護", mutableListOf(),"$id"))

        p.openInventory(inv)
    }

    fun changeRentSetting(p:Player, id:Int){

        val inv = Bukkit.createInventory(null,9,changeRent)

        inv.setItem(2, IS(pl,Material.EMERALD,"§e§l賃料の変更", mutableListOf(),"$id"))
        inv.setItem(7, IS(pl,Material.CLOCK,"§e§lスパンの変更", mutableListOf(),"$id"))

        p.openInventory(inv)
    }

    fun changeSpan(p:Player,id: Int){
        val inv = Bukkit.createInventory(null,9,changeRentSpan)

        inv.setItem(1, IS(pl,Material.EMERALD,"§a§l一日ごと", mutableListOf(),"$id"))
        inv.setItem(4, IS(pl,Material.GREEN_WOOL,"§a§l一週間ごと", mutableListOf(),"$id"))
        inv.setItem(7, IS(pl,Material.OAK_DOOR,"§a§l一ヶ月ごと", mutableListOf(),"$id"))

        p.openInventory(inv)
    }


    fun customUserMenu(p:Player,id:Int){

        val inv = Bukkit.createInventory(null,54,customUserMenu)

    }

    fun customUserData(p:Player,id:Int,user:String){

        val inv = Bukkit.createInventory(null,54,customUserData)


    }



    ////////////////////////
    //リージョンを持っているユーザーがリージョンを管理するメニュー
    ////////////////////////
    fun openOwnerSetting(p:Player, page:Int){

        val inv = Bukkit.createInventory(null,54,ownerMenu)

        val list = ownerData[p]?:return

        for (i in page*45 .. (page*45)+44){

            if (list.size <=i)break

            val d = Constants.regionData[list[i]]?:continue

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
                13->customUserMenu(p, getId(item,pl).toInt())
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
                7->changeSpan(p,getId(item, pl).toInt())
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

        if (name == customUserMenu){

        }

        if (name == customUserData){

        }

    }

}