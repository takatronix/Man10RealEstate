package red.man10.realestate.region

import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
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
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Command
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Plugin.Companion.serverName
import red.man10.realestate.Utility
import red.man10.realestate.Utility.format
import red.man10.realestate.Utility.sendClickMessage
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.region.User.Permission.*

object Event :Listener{

    var containerAmount = 24

    @EventHandler
    fun playerJoin(e:PlayerJoinEvent){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            Thread.sleep(5000)
            User.load(e.player)
        })
    }

    /**
     * 看板のアップデート
     */
    private fun updateSign(sign: Sign, id:Int){

        val data = Region.get(id)?:return

        sign.line(0, text("§eID:$id"))
        sign.line(1, text(data.name))
        sign.line(2, text("§d§l${Region.getOwner(data)}"))
        sign.line(3, text("§b§l${Region.formatStatus(data.status)}"))

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
                sendMessage(p,"§3§l入力方法：”mre:<id>”")
                return
            }

            val data = Region.get(id)?:return

            e.line(0, text("§eID:$id"))
            e.line(1, text(data.name))
            e.line(2, text("§d§l${Region.getOwner(data)}"))
            e.line(3, text("§b§l${Region.formatStatus(data.status)}"))

            sendMessage(p,"§a§l作成完了！ id:$id name:${data.name}")
        }
    }

    //看板クリック
    @EventHandler
    fun signClickEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK && e.action !=Action.LEFT_CLICK_BLOCK)return

        val b= e.clickedBlock?:return
        val sign : Sign

        try{
            sign = b.state as Sign
        }catch (e:Exception){
            return
        }

        val lines = sign.lines

        val id: Int

        try {
            id = lines[0].replace("§eID:","").toInt()
        }catch (e:Exception){
            return
        }

        val data = Region.get(id)?:return

        val p = e.player

        //左クリックでいいね
        if (e.action == Action.LEFT_CLICK_BLOCK && !p.isSneaking){
            e.isCancelled = true
            p.performCommand("mre good $id")
            return
        }

        sendMessage(p,"§a==========${data.name}§a§lの情報==========")
        sendMessage(p,"§aID:$id")
        sendMessage(p,"§aステータス:${Region.formatStatus(data.status)}")
        sendMessage(p,"§aオーナー:${Region.getOwner(data)}")
        sendMessage(p,"§a値段:${format(data.price)}")
        sendMessage(p,"§a==========================================")

        sendClickMessage(p,"§d§lいいねする！＝＞[いいね！]","mre good $id")

        if (data.status == "OnSale"){
            sendClickMessage(p,"§a§l§n[土地を買う！] §e§l値段:${format(data.price)}","mre buycheck $id")
        }

        updateSign(sign,id)
    }

    //////////////////////////////////////////////////////////////////////////
    //保護処理
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
            Material.TRAPPED_CHEST,
            Material.FURNACE
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

        if (e.hasBlock()&&e.clickedBlock!!.state is Sign){ return }

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

    fun hasPermission(p:Player, loc: Location, perm:User.Permission):Boolean{

        if (p.hasPermission(Command.OP))return true

        if (disableWorld.contains(loc.world.name)){
            return true
        }

        if (City.where(loc) == null)return false

        for (id in Region.regionData.keys){

            val rg = Region.get(id)?:continue

            if (Utility.isWithinRange(loc,rg.startPosition,rg.endPosition,rg.world,rg.server)){

                if (rg.status == "Lock")return false
                if (rg.ownerUUID == p.uniqueId)return true
                if (rg.status == "Danger")return true

                val data = User.get(p,id)?:return false

                if (perm != BLOCK &&rg.status == "Free")return true

                if (data.status == "Lock")return false
                if (data.allowAll)return true

                when(perm){

                    BLOCK ->{
                        if (data.allowBlock)return true
                    }
                    INVENTORY ->{
                        if (data.allowInv)return true
                    }
                    DOOR ->{
                        if (data.allowDoor)return true
                    }
                    else->return false

                }

                return false
            }
        }

        return false
    }


}