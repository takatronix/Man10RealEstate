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

            if (!isWithinRange(location,data.startCoordinate,data.endCoordinate))continue

            if (data.status == "Lock")break

            if (data.status == "OnSale" && data.owner != p)break

            if (data.status == "Protected"){
                if (pl.regionUserData[Pair(p,region)] == null&& p != data.owner)break

                val ud = pl.regionUserData[Pair(p,region)]?:break

                if (ud.statsu == "Lock")break

            }
            return true
        }

        return false
    }

    /////////////////////////////////////
    //2点で指定した立方体の座標の範囲内かどうか
    /////////////////////////////////////
    fun isWithinRange(loc: org.bukkit.Location, start:Triple<Double,Double,Double>, end:Triple<Double,Double,Double>):Boolean{

        val x = loc.blockX
        val y = loc.blockY
        val z = loc.blockZ

        if (x < start.first || x > end.first)return false
        if (y < start.second|| y > end.second)return false
        if (z < start.third || z > end.third)return false

        return true

    }

}