package red.man10.realestate.region

import org.bukkit.*
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.regionData
import red.man10.realestate.Plugin.Companion.sendHoverText
import red.man10.realestate.Plugin.Companion.sendMessage
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.regionDatabase
import red.man10.realestate.Plugin.Companion.regionUserDatabase
import java.lang.Exception


class RegionEvent (private val pl : Plugin) : Listener{


    /////////////////////////
    //リージョンの座標指定
    //"wand"と書いた木の棒で設定(変更する可能性あり)
    /////////////////////////
    @EventHandler
    fun setFirstPosition(e:PlayerInteractEvent){
        val p = e.player

        if (!p.hasPermission("mre.op"/*仮パーミッション*/))return

        val wand = e.item?:return
        if (wand.type != Material.STICK)return
        if (!wand.hasItemMeta())return
        if(wand.itemMeta.displayName != WAND_NAME) return

        val lore = wand.lore?: mutableListOf()

        val loc = when(e.action){
            Action.LEFT_CLICK_AIR -> p.location
            Action.LEFT_CLICK_BLOCK -> e.clickedBlock!!.location
            else ->return
        }

        if (lore.size>=5){
            lore[0] = ("§aOwner:§f${p.name}")
            lore[1] = ("§aServer:§f"+p.server.name)
            lore[2] = ("§aWorld:§f"+p.world.name)
            lore[3] = ("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }else{
            lore.add("§aOwner:§f${p.name}")
            lore.add("§aServer:§f"+p.server.name)
            lore.add("§aWorld:§f"+p.world.name)
            lore.add("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }

        sendMessage(p,"§e§lSet Start:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")

        pl.wandStartLocation = loc.clone()

        wand.lore = lore

        e.isCancelled = true

    }

    //////////////////////////////
    //範囲指定(Pos2)
    ///////////////////////////////
    @EventHandler
    fun setSecondPosition(e:PlayerInteractEvent){

        val p = e.player

        if (!p.hasPermission("mre.op"/*仮パーミッション*/))return

        val wand = e.item?:return
        if (wand.type != Material.STICK)return
        if (!wand.hasItemMeta())return
        if(wand.itemMeta.displayName != WAND_NAME) return

        val lore = wand.lore?: mutableListOf("","","","","")

        val loc = when(e.action){
            Action.RIGHT_CLICK_AIR -> p.location
            Action.RIGHT_CLICK_BLOCK -> e.clickedBlock!!.location
            else ->return
        }

        if (lore.size == 5){
            lore[4] = "§aEnd:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}"
        }else{
            lore.add("§aEnd:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }
        sendMessage(p,"§e§lSet End:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        //      TODO:二人同時に編集できないのをいつか直す
        pl.wandEndLocation = loc.clone()

        wand.lore = lore

        e.isCancelled = true
        return


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
                sendMessage(p,"§3§l入力方法：”mre:<id>”")
                return
            }

            val data = regionData[id]?:return

            e.setLine(0,"§eID:$id")
            e.setLine(1,data.name)
            e.setLine(2,"§d§l${regionDatabase.getOwner(data)}")
            e.setLine(3,"§b§l${data.status}")

            sendMessage(p,"§a§l作成完了！ id:$id name:${data.name}")
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
            return
        }

        val lines = sign.lines

        val id: Int

        try {
            id = lines[0].replace("§eID:","").toInt()
        }catch (e:Exception){
            return
        }

        val data = regionData[id]?:return

        val p = e.player

        sendMessage(p,"§a§l==========${data.name}§a§lの情報==========")

        sendMessage(p,"§a土地名:${data.name}")
        sendMessage(p,"§a現在のステータス:${data.status}")
        sendMessage(p,"§a現在のオーナー:${regionDatabase.getOwner(data)}")
        sendMessage(p,"§a値段:${String.format("%,.1f",data.price)}")

        sendMessage(p,"§a§l==========================================")

        sendHoverText(p,"§d§lいいねする！＝＞[いいね！]","§d§lいいね！","mre good $id")
        sendHoverText(p,"§a§l土地の購入など＝＞[購入について]","","mre buycheck $id")

        refreshSign(sign,id)
    }

    fun refreshSign(sign: Sign, id:Int){

        val data = regionData[id]?:return

        sign.setLine(0,"§eID:$id")
        sign.setLine(1,data.name)
        sign.setLine(2,"§d§l${regionDatabase.getOwner(data)}")
        sign.setLine(3,"§b§l${data.status}")

        sign.update()

    }

    @EventHandler
    fun loginEvent(e:PlayerJoinEvent){
        Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
            regionUserDatabase.loadUserData(e.player)
        })
    }

}