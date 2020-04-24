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
import red.man10.realestate.region.RegionDatabase
import red.man10.realestate.region.RegionUserDatabase

class Commands (private val pl :Plugin):CommandExecutor{

    val db = RegionDatabase(pl)
    val pdb = RegionUserDatabase(pl)
    val inventory = InventoryMenu(pl)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label == "mre"){

            if (args.isEmpty()){
                help(sender,false)
                return true
            }

            val cmd = args[0]

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

                Bukkit.getScheduler().runTask(pl, Runnable {
                    RegionDatabase(pl).buy(args[1].toInt(),sender)
                })
                return true
            }

            //いいね
            if (cmd == "good" && args.size == 2){

                val isLike = pdb.setLike(sender,args[1].toInt())

                if (isLike){
                    pl.sendMessage(sender,"§a§aいいねしました！")
                }else{
                    pl.sendMessage(sender,"§a§aいいね解除しました！")
                }
                return true

            }

            //取り出し
            if (cmd == "withdraw"){
                Bukkit.getScheduler().runTask(pl, Runnable {
                    pdb.takeProfit(sender)
                })
                return true
            }

            //利益を表示
            if (cmd == "bal"){
                Bukkit.getScheduler().runTask(pl, Runnable {

                    val profit = pdb.getProfit(sender)

                    pl.sendMessage(sender,"§l§kXX§r§e§l利益の合計：$profit§e§l§kXX")

                    if (profit >0){
                        //TODO:利益があればチャットクリックで受け取れるようにする
                    }
                })
                return true
            }

            //共同者を追加する ex)/mre adduser [id] [user] [type] [status]
            //def: type:2 status:Share
            if (cmd == "adduser" && args.size == 5){

                val id = args[1].toInt()

                if (!hasRegionAdmin(sender,id)){ return true }

                pdb.createUserData(id,Bukkit.getPlayer(args[2])?:return false,args[3].toInt(),args[4])
                pl.sendMessage(sender,"§e§l${args[2]}§a§lを共同者に追加しました！")
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

            //賃料支払い
            if (cmd == "payrent"){
                if (pdb.addDeposit(args[1].toInt(),sender,args[2].toDouble())){
                    pl.sendMessage(sender,"§e§l支払い完了！$${args[2]} 支払いました！")
                    return true
                }
                pl.sendMessage(sender,"§3§l支払いできませんでした")
                return true
            }

            //設置画面を開く
            if(cmd == "setting"){

                inventory.openOwnerSetting(sender,1)

                return true
            }

            //メニューを開く
            if (cmd == "menu"){

                inventory.openMainMenu(sender)

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

            //指定地点をテレポート地点にする
            if (cmd == "setteleport" && args.size == 2){

                Thread(Runnable {

                    val loc = sender.location

                    db.setRegionTeleport(args[1].toInt(), mutableListOf(
                            loc.x,
                            loc.y,
                            loc.z,
                            loc.pitch.toDouble(),
                            loc.yaw.toDouble()
                    ))

                }).start()

                pl.sendMessage(sender,"§e§l登録完了！")
                return true
            }

            if (cmd == "changestatus" && args.size == 3){
                Thread(Runnable {
                    db.setRegionStatus(args[1].toInt(),args[2])
                }).start()
                pl.sendMessage(sender,"§e§l${args[1]}のステータスを${args[2]}に変更しました")
                return true
            }

            if (cmd == "changeprice" && args.size == 3){
                Thread(Runnable {
                    db.setPrice(args[1].toInt(),args[2].toDouble())
                }).start()
                pl.sendMessage(sender,"§e§l${args[1]}の金額を${args[2]}に変更しました")
                return true
            }

            if (cmd == "changeowner"){
                Thread(Runnable {
                    RegionDatabase(pl).setRegionOwner(args[1].toInt(),Bukkit.getPlayer(args[1])!!)
                }).start()
                pl.sendMessage(sender,"§e§l${args[1]}のオーナーを${args[2]}に変更しました")
                return true
            }

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
                        sender.location.pitch.toDouble(),
                        sender.location.yaw.toDouble()
                )


                val id = regionData.size+1

                Thread(Runnable {
                    //リージョンをDBに登録
                    db.registerRegion(data,id)
                }).start()

                pl.sendMessage(sender,"§a§l登録完了！")
                pl.sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")

                return true
            }

            //リージョンの削除
            if (cmd == "delete" && args.size==2){
                Thread(Runnable {
                    db.deleteRegion(args[1].toInt())
                }).start()

                return true
            }

            //TODO:ページ切り替えをして見れるようにする
            if (cmd == "list"){
                for (i in 1 .. regionData.size){
                    pl.sendMessage(sender,"$i : §b§l${regionData[i]!!.name}")
                }
                return true
            }

            //リージョンデータのリロード
            if (cmd == "reloadregion"){
                Thread(Runnable {
                    db.loadRegion()
                }).start()
            }
        }

        return false
    }

    fun help(p:Player,op:Boolean){

        if (!op){
            pl.sendMessage(p,"§e§l/mre wand : 範囲指定用のワンドを取得")
            pl.sendMessage(p,"§e§l/mre good <id> : 指定idに評価(いいね！)します")
            pl.sendMessage(p,"§e§l/mre buy <id> : 指定idが販売中なら購入します")
            pl.sendMessage(p,"§e§l/mre adduser <id> <user> <type> <status> : リージョンにユーザーを追加します")
            pl.sendMessage(p,"§e§l/mre removeuser <id> <user> : リージョンのユーザーを削除します")
            pl.sendMessage(p,"§e§l/mre tp <id> : 指定したidにテレポートします")
            pl.sendMessage(p,"§e§l/mre rent <id> <rent> : リージョンの賃料を設定します")
            pl.sendMessage(p,"§e§l/mre span <id> <span> : 賃料を支払うスパンを設定します 0:月 1:週 2:日")
            pl.sendMessage(p,"§e§l/mre setting : 自分のリージョンの管理をします")
            pl.sendMessage(p,"§e§l/mre menu : メニューを開きます")
        }else{
            if (!p.hasPermission("mre.op"/*仮パーミッション*/))return
            pl.sendMessage(p,"§e§l==============================================")
            pl.sendMessage(p,"§e§l/mreop good <id> : 指定idに評価(いいね！)します")
            pl.sendMessage(p,"§e§l/mreop setteleport <id> : 現在地点をテレポート地点に設定します")
            pl.sendMessage(p,"§e§l/mreop changestatus <id> <status> : 指定idのステータスを変更します")
            pl.sendMessage(p,"§e§l/mreop changeprice <id> <price> : 指定idの金額を変更します")
            pl.sendMessage(p,"§e§l/mreop changeowner <id> <owner> : 指定idのオーナーを変更します")
            pl.sendMessage(p,"§e§l/mreop create <リージョン名> <初期ステータス> : 新規リージョンを作成します")
            pl.sendMessage(p,"§e§l範囲指定済みの${Constants.WAND_NAME}§e§lを持ってコマンドを実行してください")
            pl.sendMessage(p,"§e§l/mreop delete <id> : 指定idのリージョンを削除します")
            pl.sendMessage(p,"§e§l/mreop list : リージョンID:リージョン名 のリストを表示します")
            pl.sendMessage(p,"§e§l/mreop reloadregion : リージョンデータの再読み込みをします")
        }
    }

    //指定リージョンの管理者かどうか
    fun hasRegionAdmin(p:Player,id:Int):Boolean{
        if (p.hasPermission("mre.op"))return true

        val data = regionData[id]?:return false
        if (data.owner_uuid == p.uniqueId)return true

        val userdata = regionUserData[Pair(p,id)]?:return false
        if (userdata.type == 0 && userdata.statsu == "Share")return true

        return false
    }
}