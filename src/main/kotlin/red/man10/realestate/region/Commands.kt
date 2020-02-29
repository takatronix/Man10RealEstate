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
            return true
        }

        if (label == "mreop" && args.size == 3){

            if (!sender.hasPermission("mre.op"))return true

            if (cmd == "create"){

                val name = args[1]
                val status = args[2]

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

                val ownerName = lore[0].replace("§aOwner:§f","")
                val server = lore[1].replace("§aServer:§f","")
                val world = lore[2].replace("§aWorld:§f","")

                val c1 = lore[3].replace("§aStart:§fX:","")
                        .replace("Y","").replace("Z","").split(",")
                val start = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                val c2 = lore[3].replace("§aEnd:§fX:","")
                        .replace("Y","").replace("Z","").split(",")
                val end = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())

                val db = RegionDatabase(pl)

                Thread(Runnable {
                    //リージョンをDBに登録
                    db.registerRegion(Bukkit.getPlayer(ownerName)!!,name,status,server,world, start, end)
                }).start()

                pl.sendMessage(sender,"§a§l登録完了！")

                return true
            }

            //TODO:ページ切り替えをして見れるようにする
            if (cmd == "list"){
                for (i in 0 until pl.regionData.size){
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


        if (!p.hasPermission("mre.op"/*仮パーミッション*/))return
        pl.sendMessage(p,"§e§l/mreop create <リージョン名> <初期ステータス> : 新規リージョンを作成します")
        pl.sendMessage(p,"§e§l範囲指定済みの${Constants.WAND_NAME}§d§lを持ってコマンドを実行してください")
        pl.sendMessage(p,"§e§l/mreop list : リージョンID:リージョン名 のリストを表示します")
        pl.sendMessage(p,"§e§l/mreop reloadregion : リージョンデータの再読み込みをします")
    }


}