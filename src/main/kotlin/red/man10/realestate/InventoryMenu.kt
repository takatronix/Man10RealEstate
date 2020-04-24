package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Constants.Companion.regionData

class InventoryMenu(private val pl:Plugin) : Listener {

    val ownerMenu = "${pl.prefix}§a§lオーナーメニュー"
    val mainMenu = "${pl.prefix}§a§lメインメニュー"
    val bookmark = "${pl.prefix}§a§lいいねしたリスト"

    ////////////////////////
    //リージョンを持っているユーザーがリージョンを管理するメニュー
    ////////////////////////
    fun openOwnerSetting(p:Player,first:Int){

        val inv = Bukkit.createInventory(null,54,ownerMenu)

        for (i in first .. first+44){

            val d = regionData[i]?:continue

            if (d.owner_uuid != p.uniqueId)continue

            val icon = ItemStack(Material.PAPER)
            val meta = icon.itemMeta
            meta.setDisplayName(d.name)
            meta.lore = mutableListOf(
                    "§e§lID:${i}",
                    "§a§lStatus:${d.status}"
            )
            meta.persistentDataContainer.set(NamespacedKey(pl,"id"), PersistentDataType.INTEGER,i)
            icon.itemMeta = meta
            inv.addItem(icon)
        }

        val back = IS(Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"back")
        for (i in 45..53){
            inv.setItem(i,back)
        }

        if (inv.getItem(44) != null){

            val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l次のページ", mutableListOf(),"next")
            inv.setItem(51,next)
            inv.setItem(52,next)
            inv.setItem(53,next)

        }

        p.openInventory(inv)
    }

    /////////////////////
    //メインメニュー
    /////////////////////
    fun openMainMenu(p:Player){
        val inv = Bukkit.createInventory(null,9,mainMenu)

        inv.setItem(1,IS(Material.PAPER,"§f§l自分がオーナーのリージョンを管理する", mutableListOf(),"manage"))
        inv.setItem(4,IS(Material.NETHER_STAR,"§f§lいいねしたリージョンを確認する", mutableListOf(),"bookmark"))
        inv.setItem(7,IS(Material.PAPER,"", mutableListOf(),"unnamed"))

        p.openInventory(inv)
    }

    //////////////////////////
    //いいねしたリージョンの確認
    //////////////////////////
    fun openBookMark(p:Player,first: Int){

        val inv = Bukkit.createInventory(null,54,bookmark)

        for (i in first .. first+44){

            if (!(pl.isLiked[Pair(p,i)]?:continue))continue

            val d = regionData[i]?:continue

            val icon = IS(Material.PAPER,d.name,mutableListOf(
                    "§e§lID:${i}",
                    "§b§lOwner:${Bukkit.getOfflinePlayer(d.owner_uuid).name}",
                    "§a§lStatus:${d.status}"
            ),i.toString())

            inv.addItem(icon)
        }

        val back = IS(Material.RED_STAINED_GLASS_PANE,"§3§l戻る", mutableListOf(),"back")
        for (i in 45..53){
            inv.setItem(i,back)
        }

        if (inv.getItem(44) != null){

            val next = IS(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§6§l次のページ", mutableListOf(),"next")
            inv.setItem(51,next)
            inv.setItem(52,next)
            inv.setItem(53,next)

        }

        p.openInventory(inv)

    }

    /**
     *
     * @param mateirial アイテムのタイプ
     * @param name 表示名
     * @param lore ロール
     * @param id 識別名
     *
     */
    fun IS(mateirial:Material,name:String,lore:MutableList<String>,id:String):ItemStack{

        val item = ItemStack(mateirial)
        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(pl,"id"), PersistentDataType.STRING,id)
        meta.setDisplayName(name)
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    ////////////////////
    //nbttagを取得
    /////////////////////
    fun getId(item:ItemStack):String{
        return item.itemMeta!!.persistentDataContainer[NamespacedKey(pl,"id"), PersistentDataType.STRING]?:""
    }


    @EventHandler
    fun invEvent(e:InventoryClickEvent){

        val name = e.view.title
        val item = e.currentItem?:return
        val p = e.whoClicked as Player

        //メインメニュ
        if (name == mainMenu){
            e.isCancelled = true
            when(getId(item)){
                "manage"->openOwnerSetting(p,1)
                "bookmark"->openBookMark(p,1)
            }
        }

        //オーナーメニュ
        if (name == ownerMenu){
            e.isCancelled = true
            if (e.slot >=45){
                when(getId(item)){

                    "back"->openMainMenu(p)
                    "next"->openOwnerSetting(p,getId(e.inventory.getItem(44)!!).toInt())
                    "previous"->openOwnerSetting(p,1)//TODO:うまく戻る方法を考える
                }
            }
        }

        //いいね
        if (name == bookmark){
            e.isCancelled = true
            if (e.slot >=45){
                when(getId(item)){

                    "back"->openMainMenu(p)
                    "next"->openBookMark(p,getId(e.inventory.getItem(44)!!).toInt())
                    "previous"->openBookMark(p,1)//TODO:うまく戻る方法を考える
                }
            }
        }


    }

}