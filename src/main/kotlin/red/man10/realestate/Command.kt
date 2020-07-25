package red.man10.realestate

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.city
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.es
import red.man10.realestate.Plugin.Companion.maxBalance
import red.man10.realestate.Plugin.Companion.numbers
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Plugin.Companion.region
import red.man10.realestate.Plugin.Companion.user
import red.man10.realestate.Utility.Companion.sendHoverText
import red.man10.realestate.Utility.Companion.sendMessage
import red.man10.realestate.menu.InventoryMenu
import java.util.*

class Command:CommandExecutor {

    val USER = "mre.user"
    val GUEST = "mre.guest"
    val OP = "mre.op"


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label == "mre"){

            if (args.isNullOrEmpty()){

                if (!hasPerm(sender,GUEST))return false

                InventoryMenu().mainMenu(sender)
                return true

            }
            //TODO:Intを少数にしたときの処理修正

            when(args[0]){

                "buy" ->{

                    if (!hasPerm(sender,USER))return false

                    if (args.size != 2 || !NumberUtils.isNumber(args[1]))return false

                    val data = region.get(args[1].toInt())?:return false

                    if (data.status != "OnSale"){
                        sendMessage(sender,"§4§lこの土地は販売されていません！")
                        return false
                    }

                    region.buy(sender,args[1].toInt())

                    return true
                }

                "buycheck" ->{

                    if (!hasPerm(sender,USER))return false

                    if (args.size != 2 || !NumberUtils.isNumber(args[1]))return false

                    val data = region.get(args[1].toInt())?:return false

                    if (data.status != "OnSale"){
                        sendMessage(sender,"§4§lこの土地は販売されていません！")
                        return false
                    }

                    sendMessage(sender,"§3§l料金：${data.price} 名前：${data.name}" +
                            " §a§l現在のオーナー名：${region.getOwner(data)}")
                    sendMessage(sender,"§e§l本当に購入しますか？(購入しない場合は無視してください)")
                    sendHoverText(sender,"§a§l[購入する]","§6§l${data.price}","mre buy ${args[1]}")

                    return true
                }

                "good" ->{

                    if (args.size !=2 || !NumberUtils.isNumber(args[1]))return false

                    if (!hasPerm(sender,GUEST))return false

                    user.setLike(sender,args[1].toInt())

                    return true
                }

                "adduser" ->{

                    if (!hasPerm(sender,USER))return false

                    if (args.size != 3){
                        sendMessage(sender,"§c§l入力方法に問題があります！")
                        return false
                    }
                    val id = parse(args[1])?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val data = region.get(id)

                    if (data == null){
                        sendMessage(sender,"§c§l存在しない土地です！")
                        return false
                    }

                    val p = Bukkit.getPlayer(args[2])

                    if (p == null ){
                        sendMessage(sender,"§c§lユーザーがオフラインの可能性があります！")
                        return false
                    }

                    if (user.get(p,id) != null){
                        sendMessage(sender,"§c§lこのユーザーは既に住人です！")
                        return false
                    }

                    val number = Random().nextInt()

                    numbers.add(number)


                    sendMessage(p,"§a§l=================土地の情報==================")
                    sendMessage(p,"§a§lオーナー：${sender.name}")
                    sendMessage(p,"§a§l土地の名前：${data.name}")
                    sendMessage(p,"§a§l土地のステータス：${data.status}")
                    sendMessage(p,"§a§l===========================================")

                    sendMessage(p,"§e§l承諾する場合は下のチャット文をクリック、しない場合はこの文を無視してください")

                    sendHoverText(p,"§e§l[住人追加に承諾する]","§a§l承諾する","mre acceptuser $id ${sender.name} $number")

                    sendMessage(sender,"§a§l現在承諾待ちです....")
                    return true

                }

                "acceptuser"->{

                    if (!hasPerm(sender,GUEST))return false

                    if (args.size != 4)return false

                    val num = args[3].toInt()

                    if (!numbers.contains(num))return false

                    numbers.remove(num)

                    user.create(sender,args[1].toInt())

                    sendMessage(sender,"§a§l登録完了！あなたは住人になりました！")

                    sendMessage(Bukkit.getPlayer(args[2])!!,"§a§l${sender.name}が住人の追加に承諾しました！")

                    return true

                }

                "removeuser" ->{

                    if (args.size != 3)return false

                    if (!hasPerm(sender,USER))return false

                    val id = parse(args[1])?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§l住人がオフラインなので削除できません！")
                        return false
                    }

                    user.remove(p,id)

                    sendMessage(sender,"§a§l削除完了！")
                    return true

                }

                "span" ->{
                    if (!hasPerm(sender,USER))return false

                    if (args.size!=3)return false

                    val id = args[1].toInt()
                    val span = args[2].toInt()

                    if (!hasRegionPermission(sender,id))return false

                    region.setSpan(id,span)

                }

                "setowner" ->{
                    if (!hasPerm(sender,USER))return false

                    if (args.size != 3)return false

                    val id = parse(args[1])?:return false

                    if (sender.uniqueId != region.get(id)!!.ownerUUID)return false


                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§3§lオンラインのユーザーを入力してください")
                        return true
                    }

                    region.setOwner(id,p)

                    sendMessage(sender,"§e§l${args[1]}のオーナーを${args[2]}に変更しました")

                    return true

                }

                "setrent" ->{// mre setrent id p amount

                    if (!hasPerm(sender,USER))return false

                    if (args.size != 4)return false

                    if (!NumberUtils.isNumber(args[3]))return false

                    val id = parse(args[1])?:return false
                    val rent = args[3].toDouble()

                    if (!hasRegionPermission(sender,id))return false


                    if (rent< 0.0 || rent> maxBalance){
                        return true
                    }

                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§l住人がオンラインのときのみ、賃料を変更できます")
                        return false
                    }

                    user.setRentPrice(p,id,rent)

                    sendMessage(sender,"§a§l設定完了！")
                    sendMessage(p,"§a§lID:$id　の賃料が変更されました！！ 賃料:$rent")

                    return true

                }

                "setstatus" ->{
                    if (!hasPerm(sender,USER))return false

                    if (args.size != 3)return false

                    val id = parse(args[1])?:return false

                    if (!hasRegionPermission(sender,id))return false

                    if (!hasPerm(sender,OP) && args[2]=="Lock"){ return true }

                    region.setStatus(id,args[2])

                    sendMessage(sender,"§a§l${args[1]}のステータスを${args[2]}に変更しました")

                    return true

                }

                "setprice" ->{
                    if (!hasPerm(sender,USER))return false

                    if (args.size != 3)return false

                    if (!NumberUtils.isNumber(args[2]))return false

                    val id = parse(args[1])?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val price = args[2].toDouble()

                    if (price <0.0 || price> maxBalance)return false

                    region.setPrice(id,price)

                    sendMessage(sender,"§a§l${args[1]}の金額を${args[2]}に変更しました")

                }

                else ->{
                    sendMessage(sender,"§c§l不明なコマンドです！")

                    return false

                }
            }


        }

        if (label == "mreop"){

            if (!hasPerm(sender,OP))return false

            if (args.isEmpty()){
                sendMessage(sender,"§e§l==============================================")
                sendMessage(sender,"§e§l/mreop wand : 範囲指定用のワンドを取得")
                sendMessage(sender,"§e§l/mreop create <rg/city> <リージョン名/都市名> <値段/税額> : 新規リージョンを作成します")
                sendMessage(sender,"§e§l範囲指定済みの${WAND_NAME}§e§lを持ってコマンドを実行してください")
                sendMessage(sender,"§e§l/mreop delete <rg/city> <id> : 指定idのリージョンを削除します")
                sendMessage(sender,"§e§l/mreop reload : 再読み込みをします")
                sendMessage(sender,"§e§l/mreop where : 現在地点がどのリージョンが確認します")
                sendMessage(sender,"§e§l/mreop reset <rg/city> <id> : 指定idのリージョンを再指定します")
                sendMessage(sender,"§e§l/mreop disableWorld <add/remove> <world> : 指定ワールドの保護を外します")
                sendMessage(sender,"§e§l/mreop tax <id> <tax>: 指定都市の税額を変更します")

                return true
            }

            when(args[0]){

                //mreop create city <name> <tax>
                //mreop create rg <name> <tax>
                "create" ->{

                    if (args.size != 4)return false

                    val wand = sender.inventory.itemInMainHand

                    if (!wand.hasItemMeta() || wand.itemMeta.displayName != WAND_NAME){
                        sendMessage(sender,"${WAND_NAME}§e§lを持ってください！")
                        return true
                    }

                    val lore = wand.lore

                    if (lore == null || wand.lore!!.size != 5){
                        sendMessage(sender,"§e§fの範囲指定ができていません！")
                        return true
                    }

                    if (!NumberUtils.isNumber(args[3])){
                        sendMessage(sender,"§3§lパラメータの入力方法が違います！")
                        sendMessage(sender,"§3§l/mreop create city [都市の名前] [税額]")
                        sendMessage(sender,"§3§l/mreop create rg [リージョンの名前] [初期の値段]")
                        return true
                    }

                    val amount = args[3].toDouble()

                    sendMessage(sender,"§a§l現在登録中です・・・")

                    GlobalScope.launch {
                        val c1 = lore[3].replace("§aStart:§fX:","")
                                .replace("Y","").replace("Z","")
                                .replace(":","").split(",")

                        val startPosition = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                        val c2 = lore[4].replace("§aEnd:§fX:","")
                                .replace("Y","").replace("Z","")
                                .replace(":","").split(",")

                        val endPosition = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())

                        var id = -1

                        if (args[1] == "city"){

                            id = city.create(startPosition,endPosition,args[2],amount,sender.location)

                        }else if (args[1] == "rg"){
                            id = region.create(startPosition,endPosition,args[2],amount,sender.location)
                        }

                        if (id == -1){
                            sendMessage(sender,"§c§l登録失敗！")
                            return@launch
                        }

                        sendMessage(sender,"§a§l登録完了！")

                        if (args[1] == "rg"){
                            sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")
                        }


                    }
                }

                "delete" ->{

                    if (args.size != 3)return false

                    if (!NumberUtils.isNumber(args[2])){
                        sendMessage(sender,"§c§l数字を入力してください")
                        return true
                    }

                    val id = args[2].toInt()

                    val isRg = args[1] == "rg"

                    if (isRg){
                        if (region.get(id) == null){
                            sendMessage(sender,"§c§l存在しない土地です！")
                            return true

                        }

                        region.delete(id)
                        sendMessage(sender,"§a§l削除完了！")

                        return true

                    }

                    if (city.get(id) == null){
                        sendMessage(sender,"§c§l存在しない都市です！")
                        return true

                    }
                    city.delete(id)
                    sendMessage(sender,"§a§l削除完了！")

                }

                "wand" ->{
                    val wand = ItemStack(Material.STICK)
                    val meta = wand.itemMeta
                    meta.setDisplayName(WAND_NAME)
                    wand.itemMeta = meta
                    sender.inventory.addItem(wand)
                    return true

                }

                "reload" ->{

                    es.execute {
                        region.load()
                        city.load()

                        for (p in Bukkit.getOnlinePlayers()){
                            user.load(p)
                        }

                        plugin.reloadConfig()

                        disableWorld = plugin.config.getStringList("disableWorld")
                        maxBalance = plugin.config.getDouble("maxBalance",100000000.0)

                        sendMessage(sender,"§e§lリロード完了")

                    }

                }

                "disableWorld" ->{

                    if (args.size != 3)return true

                    if (args[2].isBlank()){
                        sendMessage(sender,"§3§l保護を外すワールドを指定してください")
                        return true
                    }

                    if (args[1] == "add"){
                        disableWorld.add(args[2])

                        GlobalScope.launch {
                            plugin.config.set("disableWorld", disableWorld)
                            plugin.saveConfig()
                            sendMessage(sender,"追加完了！")

                        }
                    }
                    if (args[1] == "remove"){
                        disableWorld.remove(args[2])
                        GlobalScope.launch {
                            plugin.config.set("disableWorld", disableWorld)
                            plugin.saveConfig()
                            sendMessage(sender,"削除完了！")
                        }
                    }
                }

                "where" ->{

                    val loc = sender.location

                    GlobalScope.launch {
                        sendMessage(sender, "§e§l=====================================")

                        for (rg in region.map()) {

                            val data = rg.value

                            if(Utility.isWithinRange(loc, data.startPosition, data.endPosition, data.world)) {
                                sendMessage(sender, "§e§lRegionID:${rg.key}")
                                sendMessage(sender, "§7Name:${rg.value.name}")
                                sendMessage(sender, "§8Price:${rg.value.price}")
                                sendMessage(sender, "§7Owner:${region.getOwner(rg.value)}")

                            }
                        }

                        for (c in city.map()){

                            val data = c.value

                            if(Utility.isWithinRange(loc, data.startPosition, data.endPosition, data.world)) {
                                sendMessage(sender, "§e§lCityID:${c.key}")
                                sendMessage(sender, "§7Name:${c.value.name}")
                                sendMessage(sender, "§8Tax:${c.value.tax}")
                            }

                        }

                        sendMessage(sender, "§e§l=====================================")

                    }
                }

                "reset" ->{//mreop reset city id

                    if (args.size != 3)return false

                    val wand = sender.inventory.itemInMainHand

                    if (!wand.hasItemMeta() || wand.itemMeta.displayName != WAND_NAME){
                        sendMessage(sender,"${WAND_NAME}§e§lを持ってください！")
                        return true
                    }

                    val lore = wand.lore

                    if (lore == null || wand.lore!!.size != 5){
                        sendMessage(sender,"§e§fの範囲指定ができていません！")
                        return true
                    }

                    if (!NumberUtils.isNumber(args[2]))return true

                    val id = args[2].toInt()

                    val isRg = args[1] == "rg"

                    val c1 = lore[3].replace("§aStart:§fX:","")
                            .replace("Y","").replace("Z","")
                            .replace(":","").split(",")

                    val startPosition = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                    val c2 = lore[4].replace("§aEnd:§fX:","")
                            .replace("Y","").replace("Z","")
                            .replace(":","").split(",")

                    val endPosition = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())


                    if (isRg){
                        val data = region.get(id)

                        if (data == null){
                            sendMessage(sender,"§c§l存在しない土地です！")
                            return true
                        }

                        data.startPosition = startPosition
                        data.endPosition = endPosition

                        region.set(id,data)

                        sendMessage(sender,"§a§l再設定完了！")
                        return true
                    }

                    val data = city.get(id)

                    if (data == null){
                        sendMessage(sender,"§c§l存在しない土地です！")
                        return true
                    }

                    data.startPosition = startPosition
                    data.endPosition = endPosition

                    city.set(id, data)

                    sendMessage(sender,"§a§l再設定完了！")
                }

                "tax" ->{

                    if (args.size != 3)return false
                    if (!NumberUtils.isNumber(args[1]) || !NumberUtils.isNumber(args[2]))return false

                    val id = args[1].toInt()
                    val tax= args[2].toDouble()

                    val data = city.get(id)?:return true
                    data.tax = tax
                    city.set(id,data)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true
                }

                "tp" ->{

                    val id = args[1].toInt()

                    val data = region.get(id)?:return true

                    sender.teleport(data.teleport)

                    return true

                }

                "init" ->{

                    val id = args[1].toInt()

                    region.initRegion(id)

                }

                else ->{

                    sendMessage(sender,"§c§l不明なコマンドです！")

                    return false

                }

            }

            return false
        }



        return true
    }

    fun hasPerm(p:Player,permission:String):Boolean{

        if (p.hasPermission(permission))return true

        sendMessage(p,"§c§lYou do not have permission!")
        return false

    }

    fun parse(str:String):Int?{

        if (!NumberUtils.isNumber(str) || !NumberUtils.isDigits(str))return null

        return str.toInt()
    }

    fun hasRegionPermission(p:Player,id:Int):Boolean{

        if (p.hasPermission(OP))return true

        val data = region.get(id)?:return false

        if (data.status == "Lock")return false

        if (data.ownerUUID == p.uniqueId)return true

        val userData = user.get(p,id)?:return false

        if (userData.allowAll && userData.status == "Share")return true

        return false

    }
}