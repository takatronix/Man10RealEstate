package red.man10.realestate.region

import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.material.Colorable
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Command
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.serverName
import red.man10.realestate.region.User.Permission.*
import red.man10.realestate.util.Utility
import red.man10.realestate.util.Utility.format
import red.man10.realestate.util.Utility.sendClickMessage
import red.man10.realestate.util.Utility.sendMessage
import tororo1066.itemframeprotector.api.event.IFPAddEvent
import tororo1066.itemframeprotector.api.event.IFPCause
import tororo1066.itemframeprotector.api.event.IFPInteractEvent
import tororo1066.itemframeprotector.api.event.IFPRemoveEvent

object Event :Listener{

    var maxContainers = 24

    /**
     * 看板のアップデート
     */
    private fun updateSign(sign: Sign, id:Int){

        val rg = Region.regionData[id]?:return

        sign.line(0, text("§eID:$id"))
        sign.line(1, text(rg.name))
        sign.line(2, text("§d§l${rg.ownerName}"))
        sign.line(3, text("§b§l${Region.formatStatus(rg.status)}"))

        sign.update()

    }

    private fun setFirstPosition(p:Player,loc: Location,wand:ItemStack){

        val meta = wand.itemMeta

        val lore = meta.lore()?: mutableListOf()

        meta.persistentDataContainer.set(NamespacedKey.fromString("first")!!, PersistentDataType.STRING,"${loc.blockX};${loc.blockY};${loc.blockZ}")


        if (lore.size>=5){
            lore[0] = text("§aOwner:§f${p.name}")
            lore[1] = text("§aServer:§f$serverName")
            lore[2] = text("§aWorld:§f${p.world.name}")
            lore[3] = text("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }else{
            lore.add(text("§aOwner:§f${p.name}"))
            lore.add(text("§aServer:§f"+p.server.name))
            lore.add(text("§aWorld:§f"+p.world.name))
            lore.add(text("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}"))
        }

        meta.lore(lore)

        wand.itemMeta = meta
        sendMessage(p,"§e§lSet Start:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
    }

    private fun setSecondPosition(p:Player,loc: Location,wand:ItemStack){

        val meta = wand.itemMeta

        val lore = meta.lore()?: mutableListOf()

        meta.persistentDataContainer.set(NamespacedKey.fromString("second")!!, PersistentDataType.STRING,"${loc.blockX};${loc.blockY};${loc.blockZ}")

        if (lore.size == 5){
            lore[4] = text("§aEnd:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }else{
            lore.add(text("§aEnd:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}"))
        }

        meta.lore(lore)

        wand.itemMeta = meta
        sendMessage(p,"§e§lSet End:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")

    }

    @EventHandler
    fun joinEvent(e:PlayerJoinEvent){
        val p = e.player
        Bookmark.asyncLoadBookmark(p)
        Region.asyncLoginProcess(p)
        User.asyncLoginProcess(p)
    }

    /**
     * 範囲指定
     */
    @EventHandler
    fun setPosition(e: PlayerInteractEvent){
        val p = e.player

        if (!p.hasPermission(Command.OP))return

        val isFirst = when(e.action){
            Action.RIGHT_CLICK_BLOCK,Action.RIGHT_CLICK_AIR ->false
            Action.LEFT_CLICK_AIR,Action.LEFT_CLICK_BLOCK->true
            else -> return
        }

        val wand = e.item?:return
        if (wand.type != Material.STICK)return
        if (!wand.hasItemMeta())return
        if(wand.itemMeta.displayName != WAND_NAME) return

        val loc = when(e.action){
            Action.LEFT_CLICK_AIR,Action.RIGHT_CLICK_AIR -> p.location
            Action.LEFT_CLICK_BLOCK,Action.RIGHT_CLICK_BLOCK -> e.clickedBlock!!.location
            else ->return
        }

        if (isFirst){
            setFirstPosition(p,loc, wand)
        }else{
            setSecondPosition(p,loc,wand)
        }

        e.isCancelled = true

    }

    //リージョンの看板作成
    @EventHandler
    fun signChangeEvent(e: SignChangeEvent){
        val lines = e.lines
        val p = e.player

        if (lines[0].indexOf("mre:") == 0){

            val id : Int

            try {
                id = lines[0].replace("mre:","").toInt()
            }catch (e:Exception){
                sendMessage(p,"§3§l入力方法：\"mre:<id>\"")
                return
            }

            val rg = Region.regionData[id]?:return
            if (!Utility.isWithinRange(e.block.location ,rg.startPosition,rg.endPosition,rg.world,rg.server) && !hasPermission(e.player, e.block.location, BLOCK)){
                sendMessage(e.player,"§c土地の外に看板を設置することはできません")
                return
            }

            e.line(0, text("§eID:$id"))
            e.line(1, text(rg.name))
            e.line(2, text("§d§l${rg.ownerName}"))
            e.line(3, text("§b§l${Region.formatStatus(rg.status)}"))

            sendMessage(p,"§a§l作成完了！ id:$id name:${rg.name}")
        }
    }

    //看板クリック
    @EventHandler
    fun signClickEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK && e.action !=Action.LEFT_CLICK_BLOCK)return

        val b= e.clickedBlock?:return
        val sign = b.state

        if (sign !is Sign)return

        val lines = sign.lines

        val id = lines[0].replace("§eID:","").toIntOrNull()?:return

        val rg = Region.regionData[id]?:return

        val p = e.player

        //左クリックでブックマーク
        if (e.action == Action.LEFT_CLICK_BLOCK && !p.isSneaking){
            e.isCancelled = true
            p.performCommand("mre bookmark $id")
            return
        }

        sendMessage(p,"§a==========${rg.name}§a§lの情報==========")
        sendMessage(p,"§aID:$id")
        sendMessage(p,"§aステータス:${Region.formatStatus(rg.status)}")
        sendMessage(p,"§aオーナー:${rg.ownerName}")
        sendMessage(p,"§a値段:${format(rg.price)}")
        sendMessage(p,"§a==========================================")

        sendClickMessage(p,"§d§lブックマークする！＝＞[ブックマーク！]","mre bookmark $id","ブックマークをすると、/mreメニューから テレポートをすることができます")

        if (rg.status == "OnSale"){
            sendClickMessage(p,"§a§l§n[土地を買う！]","mre buyconfirm $id","§e§l値段:${format(rg.price)}")
        }

        updateSign(sign,id)
    }

    //////////////////////////////////////////////////////////////////////////
    //保護処理 イベント
    ///////////////////////////////////////////////////////////////////////////

    private val invList = listOf(
            Material.CHEST,
            Material.ENDER_CHEST,
            Material.HOPPER,
            Material.TRAPPED_CHEST,
            Material.DISPENSER,
            Material.DROPPER,
            Material.FURNACE,
            Material.BARREL,
            Material.SHULKER_BOX)

    private val containerList = listOf(
            Material.CHEST,
            Material.HOPPER,
            Material.TRAPPED_CHEST
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun blockBreakEvent(e: BlockBreakEvent){

        val p = e.player

        if (!hasPermission(p,e.block.location, BLOCK)){
            sendMessage(p,"§cこのブロックは壊すことができません！")
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun blockPlaceEvent(e: BlockPlaceEvent){
        val p = e.player

        val block = e.block

        if (!hasPermission(p,block.location,BLOCK)){
            sendMessage(p,"§cここにブロックを置くことはできません！")
            e.isCancelled = true
            return
        }

        if (block.type==Material.CHEST){ sendMessage(p,"§c§lチェストより樽の使用をおすすめします！") }

        if (containerList.contains(block.type) && countContainer(block)> maxContainers){
            sendMessage(p,"§cこのチャンクには、これ以上このブロックは置けません！")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerBucketEmpty(e: PlayerBucketEmptyEvent) {
        val p = e.player

        if (!hasPermission(p, p.location, BLOCK)) {
            sendMessage(p,"§cここに水などを置くことはできません！")
            e.isCancelled = true

        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun interactEvent(e:PlayerInteractEvent){
        if (e.action == Action.RIGHT_CLICK_AIR && e.action == Action.LEFT_CLICK_AIR)return
        if (!e.hasBlock())return

        val p = e.player

        if (e.hasBlock()&&e.clickedBlock!!.state is Sign){

            if (!e.hasItem()){return}

            val dye = e.item!!.itemMeta

            if (dye !is Colorable && e.item!!.type != Material.GLOW_INK_SAC)return
        }

        if (!hasPermission(p,e.clickedBlock!!.location, DOOR)){
            sendMessage(p,"§cこのブロックを触ることはできません！")
            e.isCancelled = true
            return
        }

        if (invList.contains(e.clickedBlock!!.type)){
            if (!hasPermission(p,e.clickedBlock!!.location, INVENTORY)){
                sendMessage(p,"§cこのブロックを触ることはできません！")
                e.isCancelled = true
                return
            }
        }else{
            if (!hasPermission(p,e.clickedBlock!!.location,DOOR)){
                sendMessage(p,"§cこのブロックを触ることはできません！")
                e.isCancelled = true
                return
            }

        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun signEvent(e:SignChangeEvent){
        val p = e.player

        if (!hasPermission(p,e.block.location,BLOCK)){
            sendMessage(p,"§cここに看板を置くことができません！")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun breakEntity(e: HangingBreakByEntityEvent){
        val p = e.remover?:return

        if (p !is Player)return

        if (!hasPermission(p,e.entity.location,BLOCK)){
            sendMessage(p,"§cこのブロックを触ることはできません！")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun interact(e:PlayerInteractEntityEvent){

        val p = e.player

        if (!hasPermission(p, e.rightClicked.location,DOOR)){
            sendMessage(p,"§cこのブロックを触ることはできません！")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun breakEntity(e:EntityDamageByEntityEvent){

        val p = e.damager

        if (p !is Player)return

        if (!hasPermission(p, e.entity.location,DOOR)){
            sendMessage(p,"§cこのブロックを触ることはできません！")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun armorStand(e:PlayerArmorStandManipulateEvent){
        val p = e.player

        if (!hasPermission(p, e.rightClicked.location,BLOCK)){
            sendMessage(p,"§cこのアーマースタンドを触ることはできません！")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun itemFrameInteractEvent(e:IFPInteractEvent){
        val p = e.entity
        if (p !is Player)return
        if (e.ifpCause == IFPCause.OP_STAFF)return
        if (!hasPermission(p,e.data.loc,INVENTORY)){
            sendMessage(p,"§cこの額縁を触ることはできません！")
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun itemFrameRemoveEvent(e:IFPRemoveEvent){
        val p = e.remover
        if (p !is Player)return
        if (e.ifpCause == IFPCause.OP_STAFF)return
        if (!hasPermission(p,e.data.loc,BLOCK)){
            sendMessage(p,"§cこの額縁を触ることはできません！")
            e.isCancelled = true
        }
    }

    private fun hasPermission(p:Player, loc: Location, perm:User.Permission):Boolean{

        if (p.hasPermission(Command.OP))return true

        if (disableWorld.contains(loc.world.name)){ return true }

        if (City.where(loc) == null)return false

        Region.regionData.forEach{ entry ->
            val rg = entry.value
            val id = entry.key

            if (Utility.isWithinRange(loc,rg.startPosition,rg.endPosition,rg.world,rg.server)){

                if (rg.status == "Lock")return false
                if (rg.ownerUUID == p.uniqueId)return true
                if (rg.status == "Danger")return true

                if (perm != BLOCK &&rg.status == "Free")return true

                val data = User.get(p,id) ?:return false

                if (data.status == "Lock")return false
                if (data.allowAll)return true

                when(perm){

                    BLOCK ->{ if (data.allowBlock)return true }
                    INVENTORY ->{ if (data.allowInv)return true }
                    DOOR ->{ if (data.allowDoor)return true }
                    else->return false

                }

                return false
            }
        }

        return false
    }

    private fun countContainer(block: Block): Int {

        val te = block.chunk.tileEntities

        var count = 0

        for (entity in te){
            if (containerList.contains(entity.block.type))count++
        }

        return count
    }


}