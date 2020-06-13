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
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.city
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.region
import red.man10.realestate.Plugin.Companion.user
import red.man10.realestate.Utility
import red.man10.realestate.Utility.Companion.sendHoverText
import red.man10.realestate.Utility.Companion.sendMessage

class Event(private val pl :Plugin) :Listener{

    @EventHandler
    fun playerJoin(e:PlayerJoinEvent){
        Plugin.es.execute { Plugin.user.load(e.player) }
    }

    /**
     * 看板のアップデート
     */
    fun updateSign(sign: Sign, id:Int){

        val data = region.get(id)?:return

        sign.setLine(0,"§eID:$id")
        sign.setLine(1,data.name)
        sign.setLine(2,"§d§l${region.getOwner(data)}")
        sign.setLine(3,"§b§l${data.status}")

        sign.update()

    }

    /**
     * 範囲指定(pos1)
     */
    @EventHandler
    fun setFirstPosition(e: PlayerInteractEvent){
        val p = e.player

        if (!p.hasPermission("mre.op"/*仮パーミッション*/))return

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
            lore[1] = ("§aServer:§f"+p.server.name)
            lore[2] = ("§aWorld:§f"+p.world.name)
            lore[3] = ("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }else{
            lore.add("§aOwner:§f${p.name}")
            lore.add("§aServer:§f"+p.server.name)
            lore.add("§aWorld:§f"+p.world.name)
            lore.add("§aStart:§fX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")
        }

        sendMessage(p,"§e§lSet Start:§f§lX:${loc.blockX},Y:${loc.blockY},Z:${loc.blockZ}")

        pl.wandStartLocation = loc.clone()

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

        pl.wandEndLocation = loc.clone()

        wand.lore = lore

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

            val data = region.get(id)?:return

            e.setLine(0,"§eID:$id")
            e.setLine(1,data.name)
            e.setLine(2,"§d§l${region.getOwner(data)}")
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

        val data = region.get(id)?:return

        val p = e.player

        //左クリックでいいね
        if (e.action == Action.LEFT_CLICK_BLOCK && !p.isSneaking){
            e.isCancelled = true
            p.performCommand("mre good $id")
            return
        }

        sendMessage(p,"§a§l==========${data.name}§a§lの情報==========")

        sendMessage(p,"§a土地名:${data.name}")
        sendMessage(p,"§a現在のステータス:${data.status}")
        sendMessage(p,"§a現在のオーナー:${region.getOwner(data)}")
        sendMessage(p,"§a値段:${String.format("%,.1f",data.price)}")

        sendMessage(p,"§a§l==========================================")

        sendHoverText(p,"§d§lいいねする！＝＞[いいね！]","§d§lいいね！","mre good $id")

        if (data.status == "OnSale"){
            sendHoverText(p,"§a§l土地の購入など＝＞[購入について]","","mre buycheck $id")
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

        if (!canBreak(p,e.block.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを破壊する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun blockPlaceEvent(e: BlockPlaceEvent){
        val p = e.player

        if (!canBreak(p,e.block.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを設置する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun shootBowEvent(e: EntityShootBowEvent){

        val p = e.entity

        if (p !is Player)return

        if (!disableWorld.contains(p.world.name)){
            e.isCancelled = true
            return
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun blockTouchEvent(e:PlayerInteractEvent){
        if (e.action != Action.RIGHT_CLICK_BLOCK)return
        if (!e.hasBlock())return

        val p = e.player

        if (e.hasBlock()&&e.clickedBlock!!.state is Sign){
            e.isCancelled = true
            return
        }

        if (!canBreak(p,e.clickedBlock!!.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun signEvent(e:SignChangeEvent){
        val p = e.player

        if (!canBreak(p,e.block.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません")
            e.isCancelled = true
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun breakEntity(e: HangingBreakByEntityEvent){
        val p = e.remover?:return

        if (p !is Player)return

        if (!canBreak(p,e.entity.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません")
            e.isCancelled = true
        }

    }


    //ブロック破壊処理
    fun canBreak(p:Player,loc: Location,e:Any):Boolean{

        if (p.hasPermission("mre.op"))return true

        if (disableWorld.contains(loc.world.name)){
            return true
        }

        val cityID = city.where(loc)

        if (cityID == -1){
            if (disableWorld.contains(loc.world.name)){
                return true
            }
        }

        for (id in city.get(cityID)!!.regionList){

            val rg = region.get(id)?:continue

            if (Utility.isWithinRange(loc,rg.startPosition,rg.endPosition,rg.world)){

                if (rg.status == "Lock")return false
                if (rg.ownerUUID == p.uniqueId)return true

                val data = user.get(p,id)?:return false

                if (data.status == "Lock")return false
                if (data.allowAll)return true

                //ブロックの設置、破壊　
                if ((e is BlockBreakEvent || e is BlockPlaceEvent) && data.allowBlock)return true
                if ((e is SignChangeEvent || e is HangingBreakByEntityEvent) && data.allowBlock)return true

                //ブロックの右クリック
                if (e is PlayerInteractEvent){
                    if (data.allowInv && invList.contains(e.clickedBlock!!.type))return true
                    if (data.allowDoor && !invList.contains(e.clickedBlock!!.type))return true

                }

                return false
            }
        }

        return false
    }
}