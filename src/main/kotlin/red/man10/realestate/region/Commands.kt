package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Constants
import red.man10.realestate.Plugin

class Commands (private val pl :Plugin):CommandExecutor{
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (args.isEmpty()){
            help(sender)
            return true
        }

        val cmd = args[0]

        if (label == "mre"){
            //範囲指定ワンド取得
            if (cmd == "wand"){
                val wand = ItemStack(Material.STICK)
                val meta = wand.itemMeta
                meta.setDisplayName(Constants.WAND_NAME)
                wand.itemMeta = meta
                sender.inventory.addItem(wand)
                return true
            }

            if (cmd == "buy"){

                Bukkit.getScheduler().runTask(pl, Runnable {
                    RegionDatabase(pl).buy(args[1].toInt(),sender)
                })

            }

            //いいね
            if (cmd == "good" && args.size == 2){

            }
            return true
        }

        if (label == "mreop"){

            if (!sender.hasPermission("mre.op"))return true

            if (cmd == "setteleport" && args.size == 2){

                Thread(Runnable {

                    val loc = sender.location

                    RegionDatabase(pl).setRegionTeleport(args[1].toInt(), mutableListOf(
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
                    RegionDatabase(pl).setRegionStatus(args[1].toInt(),args[2])
                }).start()
                pl.sendMessage(sender,"§e§l${args[1]}のステータスを${args[2]}に変更しました")
                return true
            }

            if (cmd == "changeprice" && args.size == 3){
                Thread(Runnable {
                    RegionDatabase(pl).setPrice(args[1].toInt(),args[2].toDouble())
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

                data.owner = Bukkit.getPlayer(lore[0].replace("§aOwner:§f",""))
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


                val id = pl.regionData.size+1

                Thread(Runnable {
                    //リージョンをDBに登録
                    RegionDatabase(pl).registerRegion(data,id)
                }).start()

                pl.sendMessage(sender,"§a§l登録完了！")
                pl.sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")

                return true
            }

            //リージョンの削除
            if (cmd == "delete" && args.size==2){
                Thread(Runnable {
                    RegionDatabase(pl).deleteRegion(args[1].toInt())
                }).start()

                return true
            }

            //TODO:ページ切り替えをして見れるようにする
            if (cmd == "list"){
                for (i in 1 .. pl.regionData.size){
                    pl.sendMessage(sender,"$i : §b§l${pl.regionData[i]!!.name}")
                }
                return true
            }

            //リージョンデータのリロード
            if (cmd == "reloadregion"){
                Thread(Runnable {
                    RegionDatabase(pl).loadRegion()
                }).start()
            }
        }


        return false
    }

    fun help(p:Player){
        pl.sendMessage(p,"§e§l/mre wand : 範囲指定用のワンドを取得")
        pl.sendMessage(p,"§e§l/mre good <id> : 指定idに評価(いいね！)します")
        pl.sendMessage(p,"§e§l/mre buy <id> : 指定idが販売中なら購入します")

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