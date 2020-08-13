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
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Plugin
import red.man10.realestate.storage.Barrel.Companion.title

class BarrelEvent:Listener {

    val blockMap = HashMap<Player,Block>()
    val isOpen = mutableListOf<Triple<Int,Int,Int>>()

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

        val loc = block.location

        if (isOpen.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))){
            p.sendMessage("§c§l現在他のプレイヤーが開いています！")
            return
        }

        isOpen.add(Triple(loc.blockX,loc.blockY,loc.blockZ))

        Plugin.barrel.openStorage(barrelState,p)

        blockMap[p] = block


        Bukkit.getLogger().info("open barrel")

    }

    @EventHandler
    fun closeInventory(e:InventoryCloseEvent){

        if (e.view.title != title)return

        Bukkit.getLogger().info("close barrel")

        val p = e.player

        Plugin.barrel.setStorageItem(e.inventory,blockMap[p]?:return)

        val loc = blockMap[p]!!.location

        isOpen.remove(Triple(loc.blockX,loc.blockY,loc.blockZ))
        blockMap.remove(p)
    }

}