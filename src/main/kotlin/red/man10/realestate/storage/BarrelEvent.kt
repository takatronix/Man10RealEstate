package red.man10.realestate.storage

import org.bukkit.Material
import org.bukkit.block.Barrel
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Plugin
import red.man10.realestate.storage.Barrel.Companion.title

class BarrelEvent:Listener {

    @EventHandler
    fun openBarrelEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val block = e.clickedBlock?:return

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        if((barrelState.customName?:return) != title)return

        e.isCancelled = true

        Plugin.barrel.openStorage(barrelState,e.player)

    }

    @EventHandler
    fun closeInventory(e:InventoryCloseEvent){

        if (e.view.title != title)return

        Plugin.barrel.setStorageItem(e.inventory)

    }



}