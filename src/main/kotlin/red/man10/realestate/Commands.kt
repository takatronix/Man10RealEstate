package red.man10.realestate

import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.maxBalance
import red.man10.realestate.Plugin.Companion.numbers
import red.man10.realestate.Plugin.Companion.ownerData
import red.man10.realestate.Plugin.Companion.regionData
import red.man10.realestate.Plugin.Companion.regionUserData
import red.man10.realestate.Plugin.Companion.sendHoverText
import red.man10.realestate.Plugin.Companion.sendMessage
import red.man10.realestate.Plugin.Companion.regionDatabase
import red.man10.realestate.Plugin.Companion.regionUserDatabase
import red.man10.realestate.menu.InventoryMenu
import red.man10.realestate.region.ProtectRegionEvent
import red.man10.realestate.region.RegionDatabase
import java.util.*

class Commands (private val pl :Plugin):CommandExecutor{

    val USER = "mre.user"
    val GUEST = "mre.guest"
    val OP = "mre.op"
    val RENT = "mre.rent"


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label == "mre"){

            if (args.isEmpty()){
                if (!sender.hasPermission(USER))return true
                pl.invmain.openMainMenu(sender)
                return true
            }

            val cmd = args[0]

            //onSaleの土地を購入する
            if (cmd == "buy"){
                if (!sender.hasPermission(USER))return true

                Thread(Runnable {
                    regionDatabase.buy(args[1].toInt(),sender)
                }).start()
                return true
            }

            if (cmd == "buycheck"){

                if (!sender.hasPermission(USER))return true

                val data = regionData[args[1].toInt()]?:return true

                sendMessage(sender,"§3§l料金：${data.price} 名前：${data.name}" +
                        " §a§l現在のオーナー名：${regionDatabase.getOwner(data)}")
                sendMessage(sender,"§e§l本当に購入しますか？(購入しない場合は無視してください)")
                sendHoverText(sender,"§a§l[購入する]","§6§l${data.price}","mre buy ${args[1]}")

                return true
            }

            //いいね
            if (cmd == "good" && args.size == 2){

                if (!sender.hasPermission(GUEST))return true

                if (!NumberUtils.isNumber(args[1])){
                    sendMessage(sender,"§3§l数字のIDを入力してください")
                    return true
                }

                regionUserDatabase.setLike(sender,args[1].toInt())
                return true
            }

            //共同者を追加する ex)/mre adduser [id] [user]
            //def: type:2 status:Share
            if (cmd == "adduser" && args.size == 3){

                if (!sender.hasPermission(USER))

                if (!NumberUtils.isNumber(args[1]))return false
                val id = args[1].toInt()

                val data = regionData[id]?:return false

                if (!hasRegionAdmin(sender,id)){ return true }

                if (sender.name == args[2]){
                    sendMessage(sender,"§3§lあなたは既に住人です")
                    return true
                }

                val p = Bukkit.getPlayer(args[2])?:return false

                if (regionUserData[p] != null && regionUserData[p]!![id] != null){
                    sendMessage(sender,"§3§lこのユーザーは既に住人です")
                    return true
                }

                sendMessage(sender,"§a§l現在承諾待ちです....")

                val number = Random().nextInt()

                numbers.add(number)

                sendMessage(p,"§a§l居住者追加を求められています！")

                sendMessage(p,"§a§l=================土地の情報==================")
                sendMessage(p,"§a§lオーナー：${sender.name}")
                sendMessage(p,"§a§l土地の名前：${data.name}")
                sendMessage(p,"§a§l土地のステータス：${data.status}")
                sendMessage(p,"§a§l===========================================")

                sendMessage(p,"§e§l承諾する場合は下のチャット分をクリック、しない場合はこの文を無視してください")

                sendHoverText(p,"§e§l[住人追加に承諾する]","","mre acceptuser $id ${sender.name} $number")

                return true

            }

            //居住者を削除
            if (cmd == "removeuser"){

                if (!sender.hasPermission(USER))return true

                val id = args[1].toInt()

                if (!hasRegionAdmin(sender,id)){ return true }

                regionUserDatabase.removeUserData(id,Bukkit.getPlayer(args[2])?:return false)
                sendMessage(sender,"§e§l削除完了！")
                return true
            }


            //mre acceptuser id owner
            if (cmd == "acceptuser"){

                if (!sender.hasPermission(GUEST))return true

                if (!numbers.contains(args[3].toInt()))return true

                numbers.remove(args[3].toInt())

                regionUserDatabase.createUserData(args[1].toInt(),sender)

                sendMessage(sender,"§a§l登録完了！住人になりました！")

                sendMessage(Bukkit.getPlayer(args[2])!!,"§a§l${sender.name}が住人の追加に承諾しました")

                return true
            }

            //mre accept id owner
            if (cmd == "acceptrent"){

                if (!sender.hasPermission(GUEST))return true

                if (!numbers.contains(args[3].toInt()))return true

                numbers.remove(args[3].toInt())


                if (regionUserData[sender]!![args[1].toInt()] == null){
                    return true
                }

                regionUserDatabase.setRent(sender,args[1].toInt())

                sendMessage(sender,"§a§l設定追加完了！、あなたはこれから${args[1]}のオーナーに賃料を支払うことになります！")

                sendMessage(Bukkit.getPlayer(args[2])!!,"§a§l${sender.name}が賃料の支払いに承諾しました")

                return true
            }

            //スパン /mre span id span
            if (cmd == "span" && args.size == 3){

                if (!sender.hasPermission(RENT))return true

                val id = args[1].toInt()
                val span = args[2].toInt()

                if (!hasRegionAdmin(sender,id))return false

                regionDatabase.setSpan(id,span)


                return true
            }


            //権限設定 [id] [user] [permname] [true or false]
            if (cmd == "setperm"){

                if (!sender.hasPermission(USER))return true

                val p = Bukkit.getPlayer(args[2])?:return true
                val id = args[1].toInt()

                val pd = (regionUserData[p]?:return true)[id]?:return true

                when(args[3]) {
                    "all" -> pd.allowAll = args[4].toBoolean()
                    "block" -> pd.allowBlock = args[4].toBoolean()
                    "inv" -> pd.allowInv = args[4].toBoolean()
                    "door" -> pd.allowDoor = args[4].toBoolean()
                    else ->return true
                }

                if (pd.allowAll){
                    ownerData[p]!!.add(id)
                }

                regionUserDatabase.saveMap(p,pd,id)
                regionUserDatabase.saveUserData(p,args[1].toInt())

                sendMessage(sender,"§e§l設定完了！")

            }

            //指定地点をテレポート地点にする
            if (cmd == "settp" && args.size == 2){
                if (!sender.hasPermission(USER))return true

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                val loc = sender.location

                regionDatabase.setRegionTeleport(args[1].toInt(), mutableListOf(
                        loc.x,
                        loc.y,
                        loc.z,
                        loc.yaw.toDouble(),
                        loc.pitch.toDouble()
                ))

                sendMessage(sender,"§e§l登録完了！")
                return true
            }

            //owner変更
            if (cmd == "changeowner"){

                if (!sender.hasPermission(USER))return true

                if (!NumberUtils.isNumber(args[1]))return false

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                val p = Bukkit.getPlayer(args[2])

                if (p == null){
                    sendMessage(sender,"§3§lオンラインのユーザーを入力してください")
                    return true
                }

                regionDatabase.setRegionOwner(args[1].toInt(),p)

                sendMessage(sender,"§e§l${args[1]}のオーナーを${args[2]}に変更しました")
                return true
            }

            //賃料 /mre rent id p rent
            if (cmd == "changerent" && args.size == 4){

                if (!sender.hasPermission(RENT))return true

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                if (!NumberUtils.isNumber(args[1])||!NumberUtils.isNumber(args[3])){
                    sendMessage(sender,"§3§lパラメータの入力方法が違います")
                    return true
                }

                val id = args[1].toInt()
                val p = Bukkit.getPlayer(args[2])?:return true
                val rent = args[3].toDouble()

                if (rent< 0.0 || rent> maxBalance){
                    return true
                }

                regionUserDatabase.setRentPrice(p,id,rent)

                sendMessage(sender,"§a§l設定完了！")
                sendMessage(p,"§a§lID:$id　の賃料が変更されました！！ 賃料:$rent")

                return true
            }


            if (cmd == "changestatus" && args.size == 3){

                if (!sender.hasPermission(USER))return true

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                if (sender.hasPermission("mre.op") && args[2]=="Lock"){
                    sendMessage(sender,"§3§lリージョンのロックは運営しか出来ません！")
                    return true
                }

                regionDatabase.setRegionStatus(args[1].toInt(),args[2])

                sendMessage(sender,"§e§l${args[1]}のステータスを${args[2]}に変更しました")
                return true
            }

            if (cmd == "changeprice" && args.size == 3){

                if (!sender.hasPermission(USER))return true

                if (!NumberUtils.isNumber(args[1]))return true

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                if (!NumberUtils.isNumber(args[2])){
                    return true
                }

                val price = args[2].toDouble()

                if (price<0.0 || price> maxBalance)return true

                regionDatabase.setPrice(args[1].toInt(),price)

                sendMessage(sender,"§e§l${args[1]}の金額を${args[2]}に変更しました")
                return true
            }

            return true
        }

        if (label == "mreop"){

            if (args.isEmpty()){
                help(sender,true)
                return true
            }

            if (!sender.hasPermission(OP))return true

            val cmd = args[0]

            if (cmd == "create" && args.size == 3){

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

                if (!NumberUtils.isNumber(args[2])){
                    sendMessage(sender,"§3§lパラメータの入力方法が違います！")
                    sendMessage(sender,"§3§l/mreop create [リージョンの名前] [初期の値段]")
                    return true
                }

                sendMessage(sender,"§a§l現在登録中です・・・")

                val data = RegionDatabase.RegionData()

                data.name = args[1]
                data.status = "OnSale"
                data.price = args[2].toDouble()

                data.owner_uuid = null
                data.server = lore[1].replace("§aServer:§f","")
                data.world = lore[2].replace("§aWorld:§f","")

                val c1 = lore[3].replace("§aStart:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.startPosition = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                val c2 = lore[4].replace("§aEnd:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.endPosition = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())


                data.teleport = mutableListOf(
                        sender.location.x,
                        sender.location.y,
                        sender.location.z,
                        sender.location.yaw.toDouble(),
                        sender.location.pitch.toDouble()
                )

                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {

                    val id = regionDatabase.registerRegion(data)

                    if (id == -1){
                        sendMessage(sender,"§3§l登録失敗！")
                        return@Runnable
                    }

                    sendMessage(sender,"§a§l登録完了！")
                    sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")

                })

                return true
            }

            //リージョンの削除
            if (cmd == "delete" && args.size==2){

                if (!NumberUtils.isNumber(args[1])){
                    sendMessage(sender,"§3§l数字を入力してください")
                    return true
                }

                val id = args[1].toInt()

                if (regionData[id] == null){
                    sendMessage(sender,"§3§l存在しないリージョンです！")
                    return true
                }

                regionDatabase.deleteRegion(args[1].toInt())

                sendMessage(sender,"§a§l削除完了")

                return true
            }

            //範囲指定ワンド取得
            if (cmd == "wand"){
                val wand = ItemStack(Material.STICK)
                val meta = wand.itemMeta
                meta.setDisplayName(WAND_NAME)
                wand.itemMeta = meta
                sender.inventory.addItem(wand)
                return true
            }


            //リージョンデータのリロード
            if (cmd == "reload"){
                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
                    regionDatabase.loadRegion()

                    for (p in Bukkit.getOnlinePlayers()){
                        regionUserDatabase.loadUserData(p)
                    }

                    pl.reloadConfig()

                    disableWorld = pl.config.getStringList("disableWorld")
                    maxBalance = pl.config.getDouble("maxBalance",100000000.0)

                    sendMessage(sender,"§e§lリロード完了")

                })
            }

            if (cmd == "disableWorld"){

                if (args[2].isBlank()){
                    sendMessage(sender,"§3§l保護を外すワールドを指定してください")
                    return true
                }

                if (args[1] == "add"){
                    disableWorld.add(args[2])

                    Thread(Runnable {
                        pl.config.set("disableWorld", disableWorld)
                        pl.saveConfig()
                        sendMessage(sender,"追加完了！")
                    }).start()
                }
                if (args[1] == "remove"){
                    disableWorld.remove(args[2])
                    Thread(Runnable {
                        pl.config.set("disableWorld", disableWorld)
                        pl.saveConfig()
                        sendMessage(sender,"削除完了！")
                    }).start()
                }

            }

            if (cmd == "where"){
                val loc = sender.location

                Thread(Runnable {
                    sendMessage(sender,"§e§l=====================================")

                    for (region in regionData){

                        val data = region.value

                        if (ProtectRegionEvent.isWithinRange(loc,data.startPosition,data.endPosition,data.world)){
                            sendMessage(sender,"§e§lRegionID:${region.key}")
                        }
                    }

                    sendMessage(sender,"§e§l=====================================")
                }).start()
            }

            ///////////////////////
            //リージョンの再指定
            //////////////////////
            if (cmd == "setregion"){

                val id = args[1].toInt()

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

                val data = regionData[id]?:return true

                sendMessage(sender,"§a§l現在登録中です・・・")

                data.server = lore[1].replace("§aServer:§f","")
                data.world = lore[2].replace("§aWorld:§f","")

                val c1 = lore[3].replace("§aStart:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.startPosition = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                val c2 = lore[4].replace("§aEnd:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.endPosition = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())

                regionData[id] = data

                regionDatabase.saveRegion(id)

                sendMessage(sender,"§a§l範囲の再指定が完了しました！")

                return true

            }

            if (cmd == "list"){

                val page = if (args.size == 2)args[1].toInt() else 0

                val regionList = regionData.keys.toList()

                for (i in (page*10) .. (page*10)+10){

                    if (regionList.size <=i){
                        break
                    }

                    val id = regionList[i]
                    val rg = regionData[id]!!

                    sendHoverText(sender,"§e§lID:$id ${rg.name} [Teleport]","§e§l[Teleport]","mre tp $id")

                }

                if (page!=0){
                    sendHoverText(sender,"§e§l[戻る]","戻る","mreop list ${page -1}")
                }
                if (regionList.size >(page*10)+10){
                    sendHoverText(sender,"§e§l[進む]","戻る","mreop list ${page +1}")
                }

                return true
            }

            //新規都市を作成、/mreop createcity <cityname> <tax>
            if (cmd == "createcity"){

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

                if (!NumberUtils.isNumber(args[2])){
                    sendMessage(sender,"§3§lパラメータの入力方法が違います！")
                    sendMessage(sender,"§3§l/mreop createcity [都市の名前] [初期の値段]")
                    return true
                }

                sendMessage(sender,"§a§l現在登録中です・・・")

                val data = RegionDatabase.RegionData()

                data.name = args[1]
                data.status = "City"
                data.price = args[2].toDouble()

                data.owner_uuid = null
                data.server = lore[1].replace("§aServer:§f","")
                data.world = lore[2].replace("§aWorld:§f","")

                val c1 = lore[3].replace("§aStart:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.startPosition = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                val c2 = lore[4].replace("§aEnd:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.endPosition = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())


                data.teleport = mutableListOf(
                        sender.location.x,
                        sender.location.y,
                        sender.location.z,
                        sender.location.yaw.toDouble(),
                        sender.location.pitch.toDouble()
                )

                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {

                    val id = regionDatabase.registerRegion(data)

                    if (id == -1){
                        sendMessage(sender,"§3§l登録失敗！")
                        return@Runnable
                    }

                    sendMessage(sender,"§a§l登録完了！")
                    //sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")

                })

                return true

            }

            //tp
            if (cmd == "tp" && args.size == 2){

                if (!sender.hasPermission(USER))return true

                val data = regionData[args[1].toInt()]?:return false

                sender.teleport(Location(
                        Bukkit.getWorld(data.world),
                        data.teleport[0],
                        data.teleport[1],
                        data.teleport[2],
                        data.teleport[3].toFloat(),
                        data.teleport[4].toFloat()
                ))
                sendMessage(sender,"§a§lテレポートしました！")
                return true
            }

        }

        return false
    }

    fun help(p:Player,op:Boolean){

        if (!op){
//            sendMessage(p,"§e§l/mre good <id> : 指定idに評価(いいね！)します")
//            sendMessage(p,"§e§l/mre buy <id> : 指定idが販売中なら購入します")
//            sendMessage(p,"§e§l/mre adduser <id> <user> : リージョンにユーザーを追加します")
//            sendMessage(p,"§e§l/mre removeuser <id> <user> : リージョンのユーザーを削除します")
//            sendMessage(p,"§e§l/mre tp <id> : 指定したidにテレポートします")
//            sendMessage(p,"§e§l/mre rent <id> <rent> : リージョンの賃料を設定します")
//            sendMessage(p,"§e§l/mre span <id> <span> : 賃料を支払うスパンを設定します 0:月 1:週 2:日")
//            sendMessage(p,"§e§l/mre settp <id> : 現在地点をテレポート地点に設定します")
//            sendMessage(p,"§e§l/mre changestatus <id> <status> : 指定idのステータスを変更します")
//            sendMessage(p,"§e§l/mre changeprice <id> <price> : 指定idの金額を変更します")
//            sendMessage(p,"§e§l/mre changeowner <id> <owner> : 指定idのオーナーを変更します")
        }else{
            if (!p.hasPermission("mre.op"/*仮パーミッション*/))return
            sendMessage(p,"§e§l==============================================")
            sendMessage(p,"§e§l/mreop wand : 範囲指定用のワンドを取得")
            sendMessage(p,"§e§l/mreop good <id> : 指定idに評価(いいね！)します")
            sendMessage(p,"§e§l/mreop create <リージョン名> <値段> : 新規リージョンを作成します")
            sendMessage(p,"§e§l範囲指定済みの${WAND_NAME}§e§lを持ってコマンドを実行してください")
            sendMessage(p,"§e§l/mreop delete <id> : 指定idのリージョンを削除します")
            sendMessage(p,"§e§l/mreop list : リージョンID:リージョン名 のリストを表示します")
            sendMessage(p,"§e§l/mreop reload : 再読み込みをします")
            sendMessage(p,"§e§l/mreop where : 現在地点がどのリージョンが確認します")
            sendMessage(p,"§e§l/mreop setregion <id> : 指定idのリージョンを再指定します")
            sendMessage(p,"§e§l/mreop disableWorld <add/remove> <world> : 指定ワールドの保護を外します")
            sendMessage(p,"§e§l/mreop createcity <都市の名前> <固定資産税> : 新規都市を作成します")
        }
    }

    //指定リージョンの管理者かどうか
    fun hasRegionAdmin(p:Player,id:Int):Boolean{
        if (p.hasPermission("mre.op"))return true

        val data = regionData[id]?:return false

        if (data.status == "Lock")return false

        if (data.owner_uuid == p.uniqueId)return true

        val userdata = (regionUserData[p]?:return false)[id]?:return false
        if (userdata.allowAll && userdata.status == "Share")return true

        return false
    }
}