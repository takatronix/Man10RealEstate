package red.man10.realestate.protect

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Plugin
import javax.xml.stream.Location

class ProtectRegionEvent(private val pl:Plugin):Listener{

    @EventHandler
    fun blockBreakEvent(e:BlockBreakEvent){

        val regionList = pl.worldRegion[e.player.world.name]?:return

        //adminなどはプロテクト無視
        if (e.player.hasPermission("mre.op"))return

        val p = e.player

        if (!canBreak(p,regionList,e.block.location)){
            pl.sendMessage(p,"§4§lあなたにはこの場所でブロックを破壊する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun blockPlaceEvent(e:BlockPlaceEvent){
        val regionList = pl.worldRegion[e.player.world.name]?:return

        //adminなどはプロテクト無視
        if (e.player.hasPermission("mre.op"))return

        val p = e.player

        if (!canBreak(p,regionList,e.block.location)){
            pl.sendMessage(p,"§4§lあなたにはこの場所でブロックを設置する権限がありません！")
            e.isCancelled = true
        }

    }

    @EventHandler
    fun blockTouchEvent(e:PlayerInteractEvent){
        if (e.action != Action.RIGHT_CLICK_BLOCK)return
        if (!e.hasBlock())return

        val regionList = pl.worldRegion[e.player.world.name]?:return

        //adminなどはプロテクト無視
        if (e.player.hasPermission("mre.op"))return

        val p = e.player

        if (!canBreak(p,regionList,e.clickedBlock!!.location)){
            pl.sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません！")
            e.isCancelled = true
        }

    }

    fun canBreak(p:Player,regionList:MutableList<Int>,location: org.bukkit.Location):Boolean{
        for (region in regionList){

            val data = pl.regionData[region]?:continue

                if (isWithinRange(location,data.startCoordinate,data.endCoordinate)){

                if (data.owner == p && data.status != "Lock")break

                    if (p == data.owner)break

                    val ud = pl.regionUserData[Pair(p,region)]?:return false

                    if (ud.statsu != "Lock")break
                }

                return false
            }
        return true
    }

    ////////////////////////////////////////////////////////////
    //立体の対角線の頂点から、指定座標が立体の中にあるかどうか判定するメソッド
    ////////////////////////////////////////////////////////////
    fun isWithinRange(loc: org.bukkit.Location, start:Triple<Double,Double,Double>, end:Triple<Double,Double,Double>):Boolean{

        val x = loc.blockX
        val y = loc.blockY
        val z = loc.blockZ

        if (x < start.first.coerceAtMost(end.first) || x > start.first.coerceAtLeast(end.first))return false
        if (y < start.second.coerceAtMost(end.second) || y > start.second.coerceAtLeast(end.second))return false
        if (z < start.third.coerceAtMost(end.third) || z > start.third.coerceAtLeast(end.third))return false

        return true

    }

}