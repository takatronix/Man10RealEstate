package red.man10.realestate.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.likedRegion
import red.man10.realestate.Plugin.Companion.prefix
import red.man10.realestate.Plugin.Companion.regionData
import red.man10.realestate.Plugin.Companion.regionDatabase

class InventoryMenu(private val pl: Plugin) : Listener {

    val mainMenu = "${prefix}§a§lメインメニュー"
    val bookmark = "${prefix}§a§lいいねしたリスト"


    companion object{
        fun IS(pl:Plugin,mateirial: Material, name:String, lore:MutableList<String>, id:String): ItemStack {

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
        fun getId(item:ItemStack,pl:Plugin):String{
            return item.itemMeta!!.persistentDataContainer[NamespacedKey(pl,"id"), PersistentDataType.STRING]?:""
        }

    }

    /////////////////////
    //メインメニュー
    /////////////////////
    fun openMainMenu(p:Player){
        val inv = Bukkit.createInventory(null,9,mainMenu)

        inv.setItem(1,IS(pl,Material.PAPER,"§f§l自分がオーナーの土地を管理する", mutableListOf(),"manage"))
        inv.setItem(4,IS(pl,Material.NETHER_STAR,"§f§lいいねした土地を確認する", mutableListOf(),"bookmark"))
        inv.setItem(7,IS(pl,Material.PAPER,"", mutableListOf(),"unnamed"))

        p.openInventory(inv)
    }

    //////////////////////////
    //いいねした土地の確認
    //////////////////////////
    fun openBookMark(p:Player, page: Int){

        val inv = Bukkit.createInventory(null,54,bookmark)

        val list = likedRegion[p]!!

        for (i in page*45 .. (page+1)*45){

            if (list.size <=i)break

            val d = regionData[list[i]]?:continue

            val icon = IS(pl,Material.PAPER,d.name,mutableListOf(
                    "§e§lID:${i}",
                    "§b§lOwner:${regionDatabase.getOwner(d)}",
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
            meta.persistentDataContainer.set(NamespacedKey(pl,"page"), PersistentDataType.INTEGER,page+1)
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
    fun invEvent(e:InventoryClickEvent){

        val name = e.view.title
        val item = e.currentItem?:return
        val p = e.whoClicked as Player

        if (!item.hasItemMeta())return

        //メインメニュ
        if (name == mainMenu){
            e.isCancelled = true
            when(getId(item,pl)){
                "manage"->pl.ownerInv.openOwnerSetting(p,0)
                "bookmark"->openBookMark(p,0)
                else ->{}
            }
        }

        //いいね
        if (name == bookmark){
            e.isCancelled = true
            when(getId(item,pl)){

                "back"->openMainMenu(p)
                "next"->openBookMark(p,item.itemMeta.persistentDataContainer[NamespacedKey(pl,"page"), PersistentDataType.INTEGER]!!)
                "previous"->openBookMark(p,item.itemMeta.persistentDataContainer[NamespacedKey(pl,"page"), PersistentDataType.INTEGER]!!)
                else ->{
                    p.performCommand("mre tp ${getId(item,pl).toInt()}")
                }
            }
            return
        }
    }
}