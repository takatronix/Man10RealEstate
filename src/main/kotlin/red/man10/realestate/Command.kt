package red.man10.realestate

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import red.man10.realestate.Plugin.Companion.maxBalance
import red.man10.realestate.Plugin.Companion.numbers
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

            when(args[0]){

                "buy" ->{

                    if (!hasPerm(sender,USER))return false

                    if (args.size != 2 || !NumberUtils.isNumber(args[1]))return false

                    region.buy(sender,args[1].toInt())

                    return true
                }

                "buycheck" ->{

                    if (!hasPerm(sender,USER))return false

                    if (args.size != 2 || !NumberUtils.isNumber(args[1]))return false

                    val data = region.get(args[1].toInt())?:return false

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

                    if (args.size == 3|| !NumberUtils.isNumber(args[1])){
                        sendMessage(sender,"§c§l入力方法に問題があります！")
                        return false
                    }
                    val id = args[1].toInt()

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

                    number.and(number)


                    sendMessage(p,"§a§l=================土地の情報==================")
                    sendMessage(p,"§a§lオーナー：${sender.name}")
                    sendMessage(p,"§a§l土地の名前：${data.name}")
                    sendMessage(p,"§a§l土地のステータス：${data.status}")
                    sendMessage(p,"§a§l===========================================")

                    sendMessage(p,"§e§l承諾する場合は下のチャット文をクリック、しない場合はこの文を無視してください")

                    sendHoverText(p,"§e§l[住人追加に承諾する]","","mre acceptuser $id ${sender.name} $number")

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

                    if (args.size != 3 || !NumberUtils.isNumber(args[1]))return false

                    if (!hasPerm(sender,USER))return false

                    val id = args[1].toInt()

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

                    if (!NumberUtils.isNumber(args[1]))return false

                    val id = args[1].toInt()

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

                    if (!NumberUtils.isNumber(args[1]) || !NumberUtils.isNumber(args[3]))return false

                    val id = args[1].toInt()
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

                    if (!NumberUtils.isNumber(args[1]))return false

                    val id = args[1].toInt()

                    if (!hasRegionPermission(sender,id))return false

                    if (!hasPerm(sender,OP) && args[2]=="Lock"){ return true }

                    region.setStatus(id,args[2])

                    sendMessage(sender,"§a§l${args[1]}のステータスを${args[2]}に変更しました")

                    return true

                }

                "setprice" ->{
                    if (!hasPerm(sender,USER))return false

                    if (args.size != 3)return false

                    if (!NumberUtils.isNumber(args[1]) || !NumberUtils.isNumber(args[2]))return false

                    val id = args[1].toInt()

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




            sendMessage(sender,"§c§l不明なコマンドです！")

            return false
        }



        return true
    }

    fun hasPerm(p:Player,permission:String):Boolean{

        if (p.hasPermission(permission))return true

        sendMessage(p,"§c§lYou do not have permission!")
        return false

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