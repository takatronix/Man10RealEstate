package red.man10.realestate.region

import org.apache.commons.lang.Validate
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Utility
import org.bukkit.block.data.type.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Constants
import red.man10.realestate.Plugin
import java.lang.Exception
import java.util.*


class RegionEvent (private val pl :Plugin) : Listener{

    val utility = red.man10.realestate.Utility()

    /////////////////////////
    //リージョンの座標指定
    //"wand"と書いた木の棒で設定(変更する可能性あり)
    /////////////////////////
    @EventHandler
    fun setCoordinateEvent(e:PlayerInteractEvent){
        val p = e.player

        if (!p.hasPermission("mre.wand"/*仮パーミッション*/))return

        if (e.action != Action.LEFT_CLICK_BLOCK)return

        val wand = e.item?:return
        if (wand.type != Material.STICK)return
        if (!wand.hasItemMeta())return
        if(wand.itemMeta.displayName != Constants.WAND_NAME)
            return

        val lore = wand.lore?: mutableListOf()
        val loc = e.clickedBlock!!.location

        if (lore.size>=5){ lore.clear() }

        if (lore.isEmpty()){
            lore.add("§aOwner:§f${p.name}")
            lore.add("§aServer:§f"+p.server.name)
            lore.add("§aWorld:§f"+p.world.name)
            lore.add("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
            pl.sendMessage(p,"§e§lSet Start:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")

            //      TODO:二人同時に編集できないのをいつか直す
            pl.wandStartLocation = loc.clone()
        }else{


            lore.add("§aEnd:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
            pl.sendMessage(p,"§e§lSet End:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
            //      TODO:二人同時に編集できないのをいつか直す
            pl.wandEndLocation = loc.clone()

        }





        wand.lore = lore

        e.isCancelled = true
    }

    /////////////////////
    //看板設置イベント
    /////////////////////
    @EventHandler
    fun signChangeEvent(e:SignChangeEvent){
        val lines = e.lines
        val p = e.player

        if (lines[0].indexOf("mre:") == 0){

            val id : Int

            try {
                id = lines[0].replace("mre:","").toInt()
            }catch (e:Exception){
                println(e)
                pl.sendMessage(p,"§3§l入力方法：”mre:<id>”")
                return
            }

            val data = pl.regionData[id]?:return

            e.setLine(0,"§eID:$id")
            e.setLine(1,data.name)
            e.setLine(2,"§d§l${data.owner!!.name}")
            e.setLine(3,"§b§l${data.status}")

            pl.sendMessage(p,"§a§l作成完了！ id:$id name:${data.name}")
        }
    }

    ////////////////////////////
    //看板クリックイベント
    ////////////////////////////
    @EventHandler
    fun signClickEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val b= e.clickedBlock?:return
        val sign : org.bukkit.block.Sign

        try{
            sign = b.state as org.bukkit.block.Sign
        }catch (e:Exception){
            println(e.message)
            return
        }

        val lines = sign.lines

        val id = lines[0].replace("§eID:","").toInt()

        val data = pl.regionData[id]?:return

        val p = e.player

        p.sendMessage("§a§l==========${data.name}§a§lの情報==========")

        p.sendMessage("§a土地名:${data.name}")
        p.sendMessage("§a現在のステータス:${data.status}")
        p.sendMessage("§a現在のオーナー:${data.owner!!.name}")
        p.sendMessage("§a値段:${data.price}")

        p.sendMessage("§a§l==========================================")

        utility.sendHoverText(p,"§d§lいいねする！＝＞","§d§l[いいね！]","")
        utility.sendHoverText(p,"土地の購入など＝＞","[購入について]","")


    }



}