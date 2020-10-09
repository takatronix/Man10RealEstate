package red.man10.realestate.storage

import org.bukkit.Material
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Plugin.Companion.barrel
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.storage.Barrel.Companion.title

object BarrelEvent:Listener {

    val blockMap = HashMap<Player,Block>()
    val isOpen = mutableListOf<Triple<Int,Int,Int>>()

    @EventHandler
    fun setBarrelEvent(e:BlockPlaceEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        barrel.addPermission(e.player,barrelState)
    }

    @EventHandler
    fun openBarrelEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val block = e.clickedBlock?:return

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        if((barrelState.customName?:return) != title)return

        val p = e.player

        if (p.isSneaking){
            if (e.hasItem() &&e.item!!.type == Material.PAPER){

                if (!barrel.hasPermission(p,barrelState))return

                barrel.addPermission(p,barrelState,e.item!!)

                sendMessage(p,"§e§l権限の設定に成功しました！")
            }

            if (!e.hasItem()){
                e.isCancelled = true
            }

            return
        }

        e.isCancelled = true

        if (!barrel.hasPermission(p,barrelState)){
            sendMessage(p,"§c§lあなたはこの樽を開く権限がありません！")
            return
        }

        val loc = block.location

        if (isOpen.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))){
            sendMessage(p,"§c§l現在他のプレイヤーが開いています！")
            return
        }

        isOpen.add(Triple(loc.blockX,loc.blockY,loc.blockZ))

        barrel.openStorage(barrelState,p)

        blockMap[p] = block

    }

    @EventHandler
    fun clickEvent(e:InventoryClickEvent){
        if (e.inventory != e.whoClicked.inventory){ return}

        if (e.cursor?.type?:return == Material.WRITTEN_BOOK){
            e.isCancelled = true
        }
    }

    @EventHandler
    fun closeInventory(e:InventoryCloseEvent){

        if (e.view.title != title)return

        val p = e.player

        barrel.setStorageItem(e.inventory,blockMap[p]?:return)

        val loc = blockMap[p]!!.location

        isOpen.remove(Triple(loc.blockX,loc.blockY,loc.blockZ))
        blockMap.remove(p)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun breakBarrel(e:BlockBreakEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val state = block.state
        if (state !is Barrel)return

        if ((state.customName?:return) != title)return

        val loc = block.location

        val p = e.player

        if (!barrel.hasPermission(p,state)){
            sendMessage(p,"§c§lあなたはこの樽を壊す権限がありません")
            e.isCancelled = true
            return
        }

        if (isOpen.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))){
            sendMessage(p,"§c§l現在他のプレイヤーが開いています！")
            e.isCancelled = true
            return
        }

        if(barrel.hasItem(state)){
            sendMessage(p,"§c§l中にアイテムが入っています！")
            e.isCancelled = true
            return
        }

        //barrel.dropStorage(state)

    }

}