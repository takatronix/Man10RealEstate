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
import red.man10.realestate.Plugin.Companion.regionData
import red.man10.realestate.Plugin.Companion.regionUserData
import red.man10.realestate.Plugin.Companion.sendHoverText
import red.man10.realestate.Plugin.Companion.sendMessage
import red.man10.realestate.Plugin.Companion.regionDatabase
import red.man10.realestate.Plugin.Companion.regionUserDatabase
import red.man10.realestate.menu.InventoryMenu
import red.man10.realestate.region.ProtectRegionEvent
import red.man10.realestate.region.RegionDatabase

class Commands (private val pl :Plugin):CommandExecutor{

    val inventory = InventoryMenu(pl)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label == "mre"){

            if (!sender.hasPermission("mre.user")){
                return false
            }

            if (args.isEmpty()){
                inventory.openMainMenu(sender)
                return true
            }

            val cmd = args[0]

            if (cmd == "help"){
                help(sender,false)
            }

            //onSaleの土地を購入する
            if (cmd == "buy"){

                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
                    regionDatabase.buy(args[1].toInt(),sender)
                })
                return true
            }

            if (cmd == "buycheck"){

                val data = regionData[args[1].toInt()]?:return true

                sendMessage(sender,"§3§l料金：${data.price} 名前：${data.name}" +
                        " §a§l現在のオーナー名：${regionDatabase.getOwner(data)}")
                sendMessage(sender,"§e§l本当に購入しますか？(購入しない場合は無視してください)")
                sendHoverText(sender,"§a§l[購入する]","§6§l${data.price}","mre buy ${args[1]}")

                return true
            }

            //いいね
            if (cmd == "good" && args.size == 2){

                if (!NumberUtils.isNumber(args[1])){
                    sendMessage(sender,"§3§l数字のIDを入力してください")
                    return true
                }

                regionUserDatabase.setLike(sender,args[1].toInt())
                return true
            }

            //取り出し
            if (cmd == "withdraw"){
                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
                    regionUserDatabase.takeProfit(sender)
                })
                return true
            }

            //利益を表示
            if (cmd == "bal"){
                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {

                    val profit = regionUserDatabase.getProfit(sender)

                    sendMessage(sender,"§l§kXX§r§e§l利益の合計：${String.format("%,.1f",profit)}§e§l§kXX")

                    if (profit >0){
                        sendHoverText(sender,"§e§l§n受け取る","§b§l§io§n${String.format("%,.1f",profit)}","mre withdraw")
                    }
                })
                return true
            }

            //共同者を追加する ex)/mre adduser [id] [user]
            //def: type:2 status:Share
            if (cmd == "adduser" && args.size == 3){

                if (!NumberUtils.isNumber(args[1]))return false
                val id = args[1].toInt()

                val data = regionData[id]?:return false

                if (!hasRegionAdmin(sender,id)){ return true }

                if (sender.name == args[2]){
                    sendMessage(sender,"§3§lあなたは既に住人です")
                    return true
                }

                val p = Bukkit.getPlayer(args[2])?:return false

                regionUserDatabase.createUserData(id,p)
                sendMessage(sender,"§e§l${args[2]}§a§lを居住者に追加しました！")

                sendMessage(p,"§e§lあなたは居住者に追加されました")
                sendMessage(p,"§a§l=================土地の情報==================")
                sendMessage(p,"§a§lオーナー：${regionDatabase.getOwner(data)}")
                sendMessage(p,"§a§l土地の名前：${data.name}")
                sendMessage(p,"§a§l土地のステータス：${data.status}")
                sendMessage(p,"§a§l===========================================")
                return true

            }

            //共同者を削除
            if (cmd == "removeuser" && args.size == 3){

                val id = args[1].toInt()

                if (!hasRegionAdmin(sender,id)){ return true }

                regionUserDatabase.removeUserData(id,Bukkit.getPlayer(args[2])?:return false)
                sendMessage(sender,"§e§l削除完了！")
                return true
            }

            //tp
            if (cmd == "tp" && args.size == 2){
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

            //賃料 /mre rent id rent
            if (cmd == "rent" && args.size == 3){

                if (!NumberUtils.isNumber(args[1])||!NumberUtils.isNumber(args[2])){
                    sendMessage(sender,"§3§lパラメータの入力方法が違います")
                    return true
                }

                val id = args[1].toInt()
                val rent = args[2].toDouble()

                if (rent< 0.0 || rent> maxBalance){
                    return true
                }

                if (!hasRegionAdmin(sender,id))return false

                regionDatabase.setRent(id,rent)
                sendMessage(sender,"§a§l設定完了！")

                return true
            }

            //mre accept id owner
            if (cmd == "accept"){

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
                val id = args[1].toInt()
                val span = args[2].toInt()

                if (!hasRegionAdmin(sender,id))return false

                regionDatabase.setSpan(id,span)


                return true
            }


            //権限設定 [id] [user] [permname] [true or false]
            if (cmd == "setperm"){

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

                regionUserDatabase.saveMap(p,pd,id)
                regionUserDatabase.saveUserData(p,args[1].toInt())

                sendMessage(sender,"§e§l設定完了！")

            }

            //指定地点をテレポート地点にする
            if (cmd == "settp" && args.size == 2){

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

            if (cmd == "changestatus" && args.size == 3){

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

            if (!sender.hasPermission("mre.op"))return true

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

            if (cmd == "list"){

//                if (args.size == 2){
//                    for (i in args[1].toInt() .. args[1].toInt()+15){
//                        if (i >= regionData.size)break
//                        sendMessage(sender,"$i : §b§l${regionData[i]!!.name}")
//                    }
//
//                    sendHoverText(sender,"§e§l[NEXT]","","mre list ${args[1].toInt()+16}")
//                    sendHoverText(sender,"§e§l[Previous]","","mre list ${args[1].toInt()-16}")
//
//                }else{
//                    for (i in 1 .. 16){
//                        if (i >= regionData.size)break
//                        if (regionData[i] == null)continue
//                        sendMessage(sender,"$i : §b§l${regionData[i]!!.name}")
//                    }
//                    sendHoverText(sender,"§e§l[NEXT]","","mre list ${17}")
//                }

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