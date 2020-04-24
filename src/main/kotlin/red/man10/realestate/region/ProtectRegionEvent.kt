package red.man10.realestate.region

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.Plugin

// test TODO: delete later
class ProtectRegionEvent(private val pl:Plugin):Listener{

    @EventHandler
    fun blockBreakEvent(e:BlockBreakEvent){

        val p = e.player

        if (!canBreak(p,e.block.location,0)){
            pl.sendMessage(p,"§4§lあなたにはこの場所でブロックを破壊する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun blockPlaceEvent(e:BlockPlaceEvent){
        val p = e.player

        if (!canBreak(p,e.block.location,1)){
            pl.sendMessage(p,"§4§lあなたにはこの場所でブロックを設置する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun blockTouchEvent(e:PlayerInteractEvent){
        if (e.action != Action.RIGHT_CLICK_BLOCK)return
        if (!e.hasBlock())return

        val p = e.player

        if (!canBreak(p,e.clickedBlock!!.location,2)){
            pl.sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません！")
            e.isCancelled = true
        }
    }

    fun canBreak(p:Player,loc: org.bukkit.Location,eventType:Int):Boolean{

        //adminなどはプロテクト無視
        if (p.hasPermission("mre.op"))return true

        for (id in regionData){

            val data = id.value

            if (isWithinRange(loc,data.startCoordinate,data.endCoordinate)){
                if (data.status == "Lock")return false
                if (data.owner_uuid == p.uniqueId)return true

                val pd = regionUserData[Pair(p,id.key)]?:return false

                if (pd.statsu == "Lock")return false
                if (pd.type == 0)return true

                //0:break 1:put 2:interact
                when(eventType){
                    0 -> {
                        if (pd.type == 1)return true
                        if (pd.type == 2)return true
                    }
                    1 ->{
                        if (pd.type == 1)return true
                        if (pd.type == 2)return true
                    }
                    2 ->{
                        if (pd.type == 1)return true
                        if (pd.type == 2)return true
                        if (pd.type == 3)return true
                    }
                }
                return false
            }

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