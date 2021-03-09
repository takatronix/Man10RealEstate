package red.man10.realestate.region

import org.bukkit.Location
import org.bukkit.Material
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
import red.man10.realestate.Command
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.es
import red.man10.realestate.Plugin.Companion.serverName
import red.man10.realestate.Utility
import red.man10.realestate.Utility.sendHoverText
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.region.User.Permission.*

object Event :Listener{

    @EventHandler
    fun playerJoin(e:PlayerJoinEvent){
        es.execute {
            Thread.sleep(5000)
            User.load(e.player)
        }
    }

    /**
     * 看板のアップデート
     */
    fun updateSign(sign: Sign, id:Int){

        val data = Region.get(id)?:return

        sign.setLine(0,"§eID:$id")
        sign.setLine(1,data.name)
        sign.setLine(2,"§d§l${Region.getOwner(data)}")
        sign.setLine(3,"§b§l${data.status}")

        sign.update()

    }

    /**
     * 範囲指定(pos1)
     */
    @EventHandler
    fun setFirstPosition(e: PlayerInteractEvent){
        val p = e.player

        if (!p.hasPermission(Command.OP))return

        val wand = e.item?:return
        if (wand.type != Material.STICK)return
        if (!wand.hasItemMeta())return
        if(wand.itemMeta.displayName != WAND_NAME) return

        val lore = wand.lore?: mutableListOf()

        val loc = when(e.action){
            Action.LEFT_CLICK_AIR -> p.location
            Action.LEFT_CLICK_BLOCK -> e.clickedBlock!!.location
            else ->return
        }

        if (lore.size>=5){
            lore[0] = ("§aOwner:§f${p.name}")
            lore[1] = ("§aServer:§f"+ serverName)
            lore[2] = ("§aWorld:§f"+p.world.name)
            lore[3] = ("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }else{
            lore.add("§aOwner:§f${p.name}")
            lore.add("§aServer:§f"+p.server.name)
            lore.add("§aWorld:§f"+p.world.name)
            lore.add("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }

        sendMessage(p,"§e§lSet Start:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")

        wand.lore = lore

        e.isCancelled = true

    }

    /**
     * 範囲指定(pos2)
     */
    @EventHandler
    fun setSecondPosition(e:PlayerInteractEvent){

        val p = e.player

        if (!p.hasPermission("mre.op"/*仮パーミッション*/))return

        val wand = e.item?:return
        if (wand.type != Material.STICK)return
        if (!wand.hasItemMeta())return
        if(wand.itemMeta.displayName != WAND_NAME) return

        val lore = wand.lore?: mutableListOf("","","","","")

        val loc = when(e.action){
            Action.RIGHT_CLICK_AIR -> p.location
            Action.RIGHT_CLICK_BLOCK -> e.clickedBlock!!.location
            else ->return
        }

        if (lore.size == 5){
            lore[4] = "§aEnd:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}"
        }else{
            lore.add("§aEnd:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }
        sendMessage(p,"§e§lSet End:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")

        wand.lore = lore

        e.isCancelled = true

    }

    //リージョンの看板作成
    @EventHandler
    fun signChangeEvent(e: SignChangeEvent){
        val lines = e.lines
        val p = e.player

        //間版でカラコを使うためのおまけ機能
        if (p.hasPermission("mre.sign_color")){
            e.setLine(0,e.lines[0].replace("&","§"))
            e.setLine(1,e.lines[1].replace("&","§"))
            e.setLine(2,e.lines[2].replace("&","§"))
            e.setLine(3,e.lines[3].replace("&","§"))
        }

        if (lines[0].indexOf("mre:") == 0){

            val id : Int

            try {
                id = lines[0].replace("mre:","").toInt()
            }catch (e:Exception){
                sendMessage(p,"§3§l入力方法：”mre:<id>”")
                return
            }

            val data = Region.get(id)?:return

            e.setLine(0,"§eID:$id")
            e.setLine(1,data.name)
            e.setLine(2,"§d§l${Region.getOwner(data)}")
            e.setLine(3,"§b§l${data.status}")

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

        sendMessage(p,"§a土地名:${data.name}")
        sendMessage(p,"§aID:$id")
        sendMessage(p,"§aステータス:${data.status}")
        sendMessage(p,"§aオーナー:${Region.getOwner(data)}")
        sendMessage(p,"§a値段:${String.format("%,.1f",data.price)}")

        sendMessage(p,"§a==========================================")

        sendHoverText(p,"§d§lいいねする！＝＞[いいね！]","§d§lいいね！","mre good $id")

        if (data.status == "OnSale"){
            sendHoverText(p,"§a§l土地の購入など＝＞[購入について]","§a値段:${String.format("%,.1f",data.price)}","mre buycheck $id")
        }

        updateSign(sign,id)
    }

    //////////////////////////////////////////////////////////////////////////
    //保護処理
    ///////////////////////////////////////////////////////////////////////////

    val invList = mutableListOf(
            Material.CHEST,
            Material.ENDER_CHEST,
            Material.HOPPER,
            Material.TRAPPED_CHEST,
            Material.DISPENSER,
            Material.DROPPER,
            Material.FURNACE,
            Material.BARREL,
            Material.SHULKER_BOX)


    @EventHandler(priority = EventPriority.HIGHEST)
    fun blockBreakEvent(e: BlockBreakEvent){

        val p = e.player

        if (!hasPermission(p,e.block.location, BLOCK)){
            sendMessage(p,"§cあなたにはこの場所でブロックを破壊する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun blockPlaceEvent(e: BlockPlaceEvent){
        val p = e.player

        if (!hasPermission(p,e.block.location,BLOCK)){
            sendMessage(p,"§cあなたにはこの場所でブロックを設置する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerBucketEmpty(e: PlayerBucketEmptyEvent) {
        val p = e.player
        val bucket = e.bucket

        if (!hasPermission(p, p.location, BLOCK)) {

            if (bucket.toString().contains("WATER") || bucket.toString().contains("LAVA")) {
                sendMessage(p,"§cあなたにはこの場所でブロックを設置する権限がありません！")
                e.isCancelled = true
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun interactEvent(e:PlayerInteractEvent){
        if (e.action != Action.RIGHT_CLICK_BLOCK && e.action != Action.PHYSICAL)return
        if (!e.hasBlock())return

        val p = e.player

        if (e.hasBlock()&&e.clickedBlock!!.state is Sign){
            return
        }

        if (!hasPermission(p,e.clickedBlock!!.location, DOOR)){
            sendMessage(p,"§cあなたにはこの場所でブロックを触る権限がありません！")
            e.isCancelled = true
            return
        }

        if (invList.contains(e.clickedBlock!!.type)){
            if (!hasPermission(p,e.clickedBlock!!.location, INVENTORY)){
                sendMessage(p,"§cあなたにはこの場所でブロックを触る権限がありません！")
                e.isCancelled = true
                return
            }
        }else{
            if (!hasPermission(p,e.clickedBlock!!.location,DOOR)){
                sendMessage(p,"§cあなたにはこの場所でブロックを触る権限がありません！")
                e.isCancelled = true
                return
            }

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun signEvent(e:SignChangeEvent){
        val p = e.player

        if (!hasPermission(p,e.block.location,BLOCK)){
            sendMessage(p,"§cあなたにはこの場所で看板を設置する権限がありません")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun breakEntity(e: HangingBreakByEntityEvent){
        val p = e.remover?:return

        if (p !is Player)return

        if (!hasPermission(p,e.entity.location,BLOCK)){
            sendMessage(p,"§cあなたにはこの場所でブロックを触る権限がありません")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun interact(e:PlayerInteractEntityEvent){

        val p = e.player

        if (!hasPermission(p, e.rightClicked.location,DOOR)){
            sendMessage(p,"§cあなたにはこの場所でブロックを触る権限がありません")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun breakEntity(e:EntityDamageByEntityEvent){

        val p = e.damager

        if (p !is Player)return

        if (!hasPermission(p, e.entity.location,DOOR)){
            sendMessage(p,"§cあなたにはこの場所でブロックを触る権限がありません")
            e.isCancelled = true
        }

    }

    fun hasPermission(p:Player, loc: Location, perm:User.Permission):Boolean{

        if (p.hasPermission(Command.OP))return true

        if (disableWorld.contains(loc.world.name)){
            return true
        }

        val cityID = City.where(loc)

        if (cityID == -1)return false

        for (id in City.get(cityID)!!.regionList){

            val rg = Region.get(id)?:continue

            if (Utility.isWithinRange(loc,rg.startPosition,rg.endPosition,rg.world,rg.server)){

                if (rg.status == "Lock")return false
                if (rg.status == "Danger")return true
                if (rg.ownerUUID == p.uniqueId)return true

                val data = User.get(p,id)?:return false

                if (data.status == "Lock")return false
                if (data.allowAll)return true

                if (perm != BLOCK &&rg.status == "Free")return true

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