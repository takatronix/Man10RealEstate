package red.man10.realestate.protect

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Plugin

class ProtectRegionEvent(private val pl:Plugin):Listener{

    @EventHandler
    fun blockBreakEvent(e:BlockBreakEvent){

        val regionList = pl.worldRegion[e.player.world.name]?:return

        //adminなどはプロテクト無視
        if (e.player.hasPermission("mre.op"))return

        val p = e.player

        for (region in regionList){

            val data = pl.regionData[region]?:continue

            if (!pl.isWithinRange(e.block.location,data.startCoordinate,data.endCoordinate))continue

            if (data.status == "OnSale" && data.owner == p)break

            if (data.status == "Protected" &&(data.member.contains(p) || data.owner == p))break

            if (data.status == "Lock")break

            return
        }

        pl.sendMessage(p,"§4§lあなたにはこの場所のブロックを壊す権限がありません！")
        e.isCancelled = true
    }

    @EventHandler
    fun blockPlaceEvent(e:BlockPlaceEvent){
        val regionList = pl.worldRegion[e.player.world.name]?:return

        //adminなどはプロテクト無視
        if (e.player.hasPermission("mre.op"))return

        val p = e.player

        for (region in regionList){

            val data = pl.regionData[region]?:continue

            if (!pl.isWithinRange(e.block.location,data.startCoordinate,data.endCoordinate))continue

            if (data.status == "OnSale" && data.owner == p)break

            if (data.status == "Protected" &&(data.member.contains(p) || data.owner == p))break

            if (data.status == "Lock")break

            return
        }

        pl.sendMessage(p,"§4§lあなたにはこの場所にブロックを設置する権限がありません！")
        e.isCancelled = true

    }

    @EventHandler
    fun blockTouchEvent(e:PlayerInteractEvent){
        if (e.action != Action.RIGHT_CLICK_BLOCK)return
        if (!e.hasBlock())return

        val regionList = pl.worldRegion[e.player.world.name]?:return

        //adminなどはプロテクト無視
        if (e.player.hasPermission("mre.op"))return

        val p = e.player

        for (region in regionList){

            val data = pl.regionData[region]?:continue

            if (!pl.isWithinRange(e.clickedBlock!!.location,data.startCoordinate,data.endCoordinate))continue

            if (data.status == "OnSale" && data.owner == p)break

            if (data.status == "Protected" &&(data.member.contains(p) || data.owner == p))break

            if (data.status == "Lock")break

            return
        }

        pl.sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません！")
        e.isCancelled = true

    }
}