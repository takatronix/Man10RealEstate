package red.man10.realestate

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.Constants.Companion.sendHoverText
import red.man10.realestate.menu.InventoryMenu
import red.man10.realestate.region.RegionDatabase
import red.man10.realestate.region.RegionUserDatabase

class Commands (private val pl :Plugin):CommandExecutor{

    val db = RegionDatabase(pl)
    val pdb = RegionUserDatabase(pl)
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

            //範囲指定ワンド取得
            if (cmd == "wand"){
                val wand = ItemStack(Material.STICK)
                val meta = wand.itemMeta
                meta.setDisplayName(Constants.WAND_NAME)
                wand.itemMeta = meta
                sender.inventory.addItem(wand)
                return true
            }

            //onSaleの土地を購入する
            if (cmd == "buy"){

                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
                    db.buy(args[1].toInt(),sender)
                })
                return true
            }

            if (cmd == "buycheck"){

                val data = regionData[args[1].toInt()]?:return true

                pl.sendMessage(sender,"§3§l料金：${data.price} 名前：${data.name}" +
                        " §a§l現在のオーナー名：${Bukkit.getOfflinePlayer(data.owner_uuid).name}")
                pl.sendMessage(sender,"§e§l本当に購入しますか？(購入しない場合は無視してください)")
                sendHoverText(sender,"§a§l[購入する]","§6§l${data.price}","mre buy ${args[1]}")

                return true
            }

            //いいね
            if (cmd == "good" && args.size == 2){

                pdb.setLike(sender,args[1].toInt())
                return true
            }

            //取り出し
            if (cmd == "withdraw"){
                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
                    pdb.takeProfit(sender)
                })
                return true
            }

            //利益を表示
            if (cmd == "bal"){
                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {

                    val profit = pdb.getProfit(sender)

                    pl.sendMessage(sender,"§l§kXX§r§e§l利益の合計：$profit§e§l§kXX")

                    if (profit >0){
                        sendHoverText(sender,"§e§l§n受け取る","§b§l§io§n$profit","mre withdraw")
                    }
                })
                return true
            }

            //共同者を追加する ex)/mre adduser [id] [user] [status]
            //def: type:2 status:Share
            if (cmd == "adduser" && args.size == 4){

                val id = args[1].toInt()

                val data = regionData[id]?:return true

                if (!hasRegionAdmin(sender,id)){ return true }

                val p = Bukkit.getPlayer(args[2])?:return false

                pdb.createUserData(id,p,args[4])
                pl.sendMessage(sender,"§e§l${args[2]}§a§lを居住者に追加しました！")

                pl.sendMessage(p,"§e§lあなたは居住者に追加されました")
                pl.sendMessage(p,"§a§l=================土地の情報==================")
                pl.sendMessage(p,"§a§lオーナー：${Bukkit.getPlayer(data.owner_uuid)!!.name}")
                pl.sendMessage(p,"§a§l土地の名前：${data.name}")
                pl.sendMessage(p,"§a§l土地のステータス：${data.status}")
                pl.sendMessage(p,"§a§l===========================================")
                return true

            }

            //共同者を削除
            if (cmd == "removeuser" && args.size == 3){

                val id = args[1].toInt()

                if (!hasRegionAdmin(sender,id)){ return true }

                pdb.removeUserData(id,Bukkit.getPlayer(args[2])?:return false)
                pl.sendMessage(sender,"§e§l削除完了！")
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
                pl.sendMessage(sender,"§a§lテレポートしました！")
                return true
            }

            //賃料 /mre rent id rent
            if (cmd == "rent" && args.size == 3){
                val id = args[1].toInt()
                val rent = args[2].toDouble()

                if (!hasRegionAdmin(sender,id))return false

                db.setRent(id,rent)
                return true
            }

            //スパン /mre span id span
            if (cmd == "span" && args.size == 4){
                val id = args[1].toInt()
                val span = args[2].toInt()

                if (!hasRegionAdmin(sender,id))return false

                db.setSpan(id,span)
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

                pdb.saveMap(p,pd,id)
                pdb.saveUserData(p,args[1].toInt())

                pl.sendMessage(sender,"§e§l設定完了！")

            }

            //指定地点をテレポート地点にする
            if (cmd == "settp" && args.size == 2){

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                val loc = sender.location

                db.setRegionTeleport(args[1].toInt(), mutableListOf(
                        loc.x,
                        loc.y,
                        loc.z,
                        loc.yaw.toDouble(),
                        loc.pitch.toDouble()
                ))

                pl.sendMessage(sender,"§e§l登録完了！")
                return true
            }

            //owner変更
            if (cmd == "changeowner"){

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                RegionDatabase(pl).setRegionOwner(args[1].toInt(),Bukkit.getPlayer(args[1])!!)

                pl.sendMessage(sender,"§e§l${args[1]}のオーナーを${args[2]}に変更しました")
                return true
            }

            if (cmd == "changestatus" && args.size == 3){

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                if (sender.hasPermission("mre.op") && args[2]=="Lock"){
                    pl.sendMessage(sender,"§3§lリージョンのロックは運営しか出来ません！")
                    return true
                }

                pl.sendMessage(sender,"§e§l${args[1]}のステータスを${args[2]}に変更しました")
                return true
            }

            if (cmd == "changeprice" && args.size == 3){

                if (!hasRegionAdmin(sender,args[1].toInt()))return false

                db.setPrice(args[1].toInt(),args[2].toDouble())

                pl.sendMessage(sender,"§e§l${args[1]}の金額を${args[2]}に変更しました")
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

                if (!wand.hasItemMeta() || wand.itemMeta.displayName != Constants.WAND_NAME){
                    pl.sendMessage(sender,"${Constants.WAND_NAME}§e§lを持ってください！")
                    return true
                }

                val lore = wand.lore

                if (lore == null || wand.lore!!.size != 5){
                    pl.sendMessage(sender,"§e§fの範囲指定ができていません！")
                    return true
                }

                val data = RegionDatabase.RegionData()

                data.name = args[1]
                data.status = args[2]

                data.owner_uuid = Bukkit.getPlayer(lore[0].replace("§aOwner:§f",""))!!.uniqueId
                data.server = lore[1].replace("§aServer:§f","")
                data.world = lore[2].replace("§aWorld:§f","")

                val c1 = lore[3].replace("§aStart:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.startCoordinate = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                val c2 = lore[4].replace("§aEnd:§fX:","")
                        .replace("Y","").replace("Z","")
                        .replace(":","").split(",")

                data.endCoordinate = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())


                data.teleport = mutableListOf(
                        sender.location.x,
                        sender.location.y,
                        sender.location.z,
                        sender.location.yaw.toDouble(),
                        sender.location.pitch.toDouble()
                )

                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {

                    pl.sendMessage(sender,"§a§l現在登録中です・・・")

                    val mysql = MySQLManager(pl,"mre")

                    val rs = mysql.query("SELECT t.*\n" +
                            "                 FROM region t\n" +
                            "                 ORDER BY id DESC\n" +
                            "                 LIMIT 501")

                    val id : Int

                    if (rs == null){
                        id = 1
                    }else{
                        rs.next()
                        id = rs.getInt("id")+1
                        rs.close()
                    }
                    mysql.close()

                    db.registerRegion(data,id)

                    pl.sendMessage(sender,"§a§l登録完了！")
                    pl.sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")

                })

                return true
            }

            //リージョンの削除
            if (cmd == "delete" && args.size==2){
                Thread(Runnable {
                    db.deleteRegion(args[1].toInt())
                }).start()

                return true
            }

            if (cmd == "list"){

                if (args.size == 2){
                    for (i in args[1].toInt() .. args[1].toInt()+15){
                        if (i >= regionData.size)break
                        pl.sendMessage(sender,"$i : §b§l${regionData[i]!!.name}")
                    }

                    sendHoverText(sender,"§e§l[NEXT]","","mre list ${args[1].toInt()+16}")
                    sendHoverText(sender,"§e§l[Previous]","","mre list ${args[1].toInt()-16}")

                }else{
                    for (i in 1 .. 16){
                        if (i >= regionData.size)break
                        if (regionData[i] == null)continue
                        pl.sendMessage(sender,"$i : §b§l${regionData[i]!!.name}")
                    }
                    sendHoverText(sender,"§e§l[NEXT]","","mre list ${17}")
                }

                return true
            }

            //リージョンデータのリロード
            if (cmd == "reload"){
                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
                    db.loadRegion()

                    for (p in Bukkit.getOnlinePlayers()){
                        pdb.loadUserData(p)
                    }

                    pl.sendMessage(sender,"§e§lリロード完了")

                })
            }

            if (cmd == "debug"){
                pl.debugMode = !pl.debugMode
                pl.sendMessage(sender,pl.debugMode.toString())
            }

            if (cmd == "rentTimer"){
                Bukkit.getScheduler().runTaskAsynchronously(pl, Runnable {
                    pl.rentTimer()
                })
            }
        }

        return false
    }

    fun help(p:Player,op:Boolean){

        if (!op){
            pl.sendMessage(p,"§e§l/mre wand : 範囲指定用のワンドを取得")
            pl.sendMessage(p,"§e§l/mre good <id> : 指定idに評価(いいね！)します")
            pl.sendMessage(p,"§e§l/mre buy <id> : 指定idが販売中なら購入します")
            pl.sendMessage(p,"§e§l/mre adduser <id> <user> <status> : リージョンにユーザーを追加します")
            pl.sendMessage(p,"§e§l/mre removeuser <id> <user> : リージョンのユーザーを削除します")
            pl.sendMessage(p,"§e§l/mre tp <id> : 指定したidにテレポートします")
            pl.sendMessage(p,"§e§l/mre rent <id> <rent> : リージョンの賃料を設定します")
            pl.sendMessage(p,"§e§l/mre span <id> <span> : 賃料を支払うスパンを設定します 0:月 1:週 2:日")
            pl.sendMessage(p,"§e§l/mre settp <id> : 現在地点をテレポート地点に設定します")
            pl.sendMessage(p,"§e§l/mre changestatus <id> <status> : 指定idのステータスを変更します")
            pl.sendMessage(p,"§e§l/mre changeprice <id> <price> : 指定idの金額を変更します")
            pl.sendMessage(p,"§e§l/mre changeowner <id> <owner> : 指定idのオーナーを変更します")
        }else{
            if (!p.hasPermission("mre.op"/*仮パーミッション*/))return
            pl.sendMessage(p,"§e§l==============================================")
            pl.sendMessage(p,"§e§l/mreop good <id> : 指定idに評価(いいね！)します")
            pl.sendMessage(p,"§e§l/mreop create <リージョン名> <初期ステータス> : 新規リージョンを作成します")
            pl.sendMessage(p,"§e§l範囲指定済みの${Constants.WAND_NAME}§e§lを持ってコマンドを実行してください")
            pl.sendMessage(p,"§e§l/mreop delete <id> : 指定idのリージョンを削除します")
            pl.sendMessage(p,"§e§l/mreop list : リージョンID:リージョン名 のリストを表示します")
            pl.sendMessage(p,"§e§l/mreop reload : 再読み込みをします")
        }
    }

    //指定リージョンの管理者かどうか
    fun hasRegionAdmin(p:Player,id:Int):Boolean{
        if (p.hasPermission("mre.op"))return true

        val data = regionData[id]?:return false

        if (data.status == "Lock")

        if (data.owner_uuid == p.uniqueId)return true

        val userdata = (regionUserData[p]?:return false)[id]?:return false
        if (userdata.allowAll && userdata.status == "Share")return true

        return false
    }
}