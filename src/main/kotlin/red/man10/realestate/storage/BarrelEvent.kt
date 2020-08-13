package red.man10.realestate.storage

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Plugin
import red.man10.realestate.storage.Barrel.Companion.title

class BarrelEvent:Listener {

    val blockMap = HashMap<Player,Block>()
    val isOpen = mutableListOf<Triple<Int,Int,Int>>()

    @EventHandler
    fun setBarrelEvent(e:BlockPlaceEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        Plugin.barrel.addPermission(e.player,barrelState)
    }

    @EventHandler
    fun openBarrelEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val block = e.clickedBlock?:return

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        if((barrelState.customName?:return) != title)return

        e.isCancelled = true

        val p = e.player

        if (!Plugin.barrel.hasPermission(p,barrelState)){
            p.sendMessage("§c§lあなたはこの樽を開く権限がありません！")
            return
        }

        val loc = block.location

        if (isOpen.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))){
            p.sendMessage("§c§l現在他のプレイヤーが開いています！")
            return
        }

        isOpen.add(Triple(loc.blockX,loc.blockY,loc.blockZ))

        Plugin.barrel.openStorage(barrelState,p)

        blockMap[p] = block

    }

    @EventHandler
    fun closeInventory(e:InventoryCloseEvent){

        if (e.view.title != title)return


        val p = e.player

        Plugin.barrel.setStorageItem(e.inventory,blockMap[p]?:return)

        val loc = blockMap[p]!!.location

        isOpen.remove(Triple(loc.blockX,loc.blockY,loc.blockZ))
        blockMap.remove(p)
    }

    @EventHandler
    fun breakBarrel(e:BlockBreakEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val state = block.state
        if (state !is Barrel)return

        if ((state.customName?:return) != title)return

        val loc = block.location

        val p = e.player

        if (isOpen.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))){
            p.sendMessage("§c§l現在他のプレイヤーが開いています！")
            e.isCancelled = true
            return
        }

        Plugin.barrel.dropStorage(state)

    }

}