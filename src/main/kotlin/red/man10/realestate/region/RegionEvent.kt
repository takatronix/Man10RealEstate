package red.man10.realestate.region

import org.apache.commons.lang.Validate
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Constants
import red.man10.realestate.Plugin
import java.util.*


class RegionEvent (private val pl :Plugin) : Listener{

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




}