package red.man10.realestate

import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.bank
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Plugin.Companion.prefix
import red.man10.realestate.Plugin.Companion.vault
import red.man10.realestate.Utility.format
import red.man10.realestate.Utility.sendClickMessage
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.menu.InventoryMenu
import red.man10.realestate.region.City
import red.man10.realestate.region.Region
import red.man10.realestate.region.Region.formatStatus
import red.man10.realestate.region.Region.getUsers
import red.man10.realestate.region.User
import java.util.*


class AddUserData{

    var id = 0
    var rent = 0.0
    lateinit var owner : Player

}

object Command:CommandExecutor {

    private const val USER = "mre.user"
    private const val GUEST = "mre.guest"
    const val OP = "mre.op"

    private val userMap = HashMap<Player,AddUserData>()
    // buycheck -> buyコマンドへの確認キー playerUUID, pair<landId, keyUUID>
    val buyConfirmationKey = HashMap<UUID, Pair<Int, UUID>>()
    val ownerConfirmation = HashMap<UUID,Int>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label == "mre"){

            if (args.isEmpty()){

                if (!hasPermission(sender,GUEST))return false

                InventoryMenu.mainMenu(sender)
                return true
            }

            when(args[0]){

                "buy" ->{

                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3)return false

                    val id = args[1].toIntOrNull()?:return false

                    //購入確認キー確認
                    val confirmationData = buyConfirmationKey[sender.uniqueId]
                    if(confirmationData == null || confirmationData.first != id || !confirmationData.second.toString().equals(args[2])){
                        sendMessage(sender,"§4§l購入確認をしていません！")
                        return false
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                        Region.buy(sender,id)
                        buyConfirmationKey.remove(sender.uniqueId) //購入確認キー消去
                    })
                    return true
                }

                "buycheck" ->{

                    if (!hasPermission(sender,USER))return false

                    if (args.size != 2)return false

                    val id = args[1].toIntOrNull()?:return false

                    val data = Region.get(id)?:return false

                    if (data.status != "OnSale"){
                        sendMessage(sender,"§c§lこの土地は販売されていません！")
                        return false
                    }

                    // 購入確認キーを生成
                    val confirmationKey = UUID.randomUUID()
                    buyConfirmationKey[sender.uniqueId] = Pair(id, confirmationKey)


                    sendMessage(sender,"§e§l値段：${format(data.price)}")
                    sendMessage(sender,"§e§lID：${id}")
                    sendMessage(sender,"§a§l現在のオーナー：${Region.getOwner(data)}")
                    sendMessage(sender,"§e§l本当に購入しますか？(購入しない場合は無視してください)")

                    sendClickMessage(sender,"§a§l[購入する]","mre buy $id $confirmationKey","§6§l電子マネー${format(data.price)}円")

                    return true
                }

                "good" ->{

                    if (args.size !=2)return false

                    if (!hasPermission(sender,GUEST))return false

                    User.changeLike(sender,args[1].toIntOrNull()?:return true)

                    return true
                }

                "adduser" ->{

                    if (!hasPermission(sender,USER))return false

                    if (args.size < 3){
                        sendMessage(sender,"§c§l/mre adduser <ID> <ユーザー名> <賃料(支払う場合のみ)>")
                        return false
                    }

                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id)){ return false }

                    val data = Region.get(id)

                    if (data == null){
                        sendMessage(sender,"§c§l存在しない土地です！")
                        return false
                    }

                    val rent = if (args.size == 4) args[3].toDoubleOrNull()?:0.0 else 0.0

                    val spanDisplay = when(data.span){
                        0 -> "一ヶ月ごと"
                        1 -> "一週間ごと"
                        2 -> "毎日"
                        else -> "不明"
                    }

                    val user = Bukkit.getPlayer(args[2])

                    if (user == null){
                        sendMessage(sender,"§c§lユーザーがオフラインの可能性があります！")
                        return false
                    }

                    if (user.uniqueId == data.ownerUUID){
                        return false
                    }

                    if (User.get(user,id) != null){
                        sendMessage(sender,"§c§lこのユーザーは既に住人です！")
                        return false
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {

                        val city = City.get(City.where(data.teleport)!!)!!

                        if (city.maxUser<= getUsers(id)){
                            sendMessage(sender,"§c§l土地に住まわせることのできる住人の上限に達しています")
                            return@Runnable
                        }
                        if (!City.setLiveScore(id,user)){
                            sendMessage(sender,"ユーザーのスコアが足りません！")
                            return@Runnable
                        }

                        sendMessage(user,"§a§l=================土地の情報==================")
                        sendMessage(user,"§a§lオーナー：${sender.name}")
                        sendMessage(user,"§a§l土地のID：$id")
                        if (rent>0.0){
                            sendMessage(user,"§a§l賃料：$rent スパン:${spanDisplay}")
                            sendMessage(user,"§a§l住人になる場合、初回賃料を銀行から引き出されます！")
                        }
                        sendMessage(user,"§a§l===========================================")

                        val addData = AddUserData()
                        addData.id = id
                        addData.rent = rent
                        addData.owner = sender
                        userMap[user] = addData

                        sendClickMessage(user,"§e§l住人になる場合は§nここを§e§lクリック！","mre acceptuser","§eこの土地の住人になります")

                        sendMessage(sender,"§a§l現在承諾待ちです....")
                        return@Runnable

                    })

                    return true

                }

                "acceptuser"->{

                    if (!hasPermission(sender,GUEST))return false

                    if (!userMap.keys.contains(sender))return false

                    val data = userMap[sender]?:return false

                    userMap.remove(sender)

                    if (data.rent > 0.0){
                        if (!bank.withdraw(sender.uniqueId,data.rent,"Man10RealEstate Rent")){
                            sendMessage(sender,"§c§l銀行にお金がないので初回賃料を支払うことができませんでした！")
                            sendMessage(data.owner,"§c§l住人予定のプレイヤーが賃料を支払えませんでした")
                            return false
                        }
                        bank.deposit(data.owner.uniqueId,data.rent,"Man10RealEstate RentProfit")
                    }

                    User.create(sender,data.id,data.rent)

                    sendMessage(sender,"§a§lあなたは住人になりました！")

                    sendMessage(data.owner,"§a§l${sender.name}が住人になりました！")

                    return true

                }

                "removeuser" ->{

                    if (args.size != 3)return false

                    if (!hasPermission(sender,USER))return false

                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§l住人がオフラインなので退去させられません！")
                        return false
                    }

                    User.remove(p,id)

                    sendMessage(sender,"§a§l退去できました！")
                    return true

                }

                "span" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size!=3)return false

                    val id = args[1].toIntOrNull()?:return false
                    val span = args[2].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    Region.setSpan(id,span)

                }

                "setowner" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3)return false

                    val id = args[1].toIntOrNull()?:return false

                    if ((sender.uniqueId != Region.get(id)!!.ownerUUID) && !sender.hasPermission(OP))return false


                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§lオンラインのユーザーを入力してください")
                        return true
                    }

//                    Region.setOwner(id,p)


                    sendMessage(sender,"現在承認待ち・・・")

                    sendMessage(p,"§a§l土地のオーナー変更の依頼が来ています")
                    sendMessage(p,"§a§l現在のオーナー:${sender.name}")
                    sendMessage(p,"§a§lID:${id}")
//                    sendMessage(p,"§a§l都市名:${city.name}")
//                    sendMessage(p,"§a§l税額:${City.getTax(id)}円")
                    p.sendMessage(text(prefix).append(text("§b§l§n[受け入れる]")
                        .clickEvent(ClickEvent.runCommand("/mre acceptowner"))).append(text(" §c§l[断る]")
                            .clickEvent(ClickEvent.runCommand("/mre denyowner"))))

                    ownerConfirmation[p.uniqueId] = id

                    return true

                }

                "acceptowner" ->{
                    if (!hasPermission(sender,USER))return false

                    val id = ownerConfirmation[sender.uniqueId]

                    if (id==null){
                        sendMessage(sender,"§a§l承認待ちの依頼はありません")
                        return true
                    }

                    val rg = Region.get(id)?:return true
                    val owner = Bukkit.getOfflinePlayer(rg.ownerUUID!!)
                    owner.player?.let { sendMessage(it,"§a§l承認されました") }
                    sendMessage(sender,"§a§l${id}の土地のオーナーになりました")

                    ownerConfirmation.remove(sender.uniqueId)
                    Region.setOwner(id,sender)
                }

                "denyowner" ->{
                    if (!hasPermission(sender,USER))return false

                    val id = ownerConfirmation[sender.uniqueId]

                    if (id==null){
                        sendMessage(sender,"§a§l承認待ちの依頼はありません")
                        return true
                    }

                    val rg = Region.get(id)?:return true
                    val owner = Bukkit.getOfflinePlayer(rg.ownerUUID!!)
                    owner.player?.let { sendMessage(it,"§c§l承認がキャンセルされました") }
                    sendMessage(sender,"§a§l${id}のオーナー譲渡を断りました")

                    ownerConfirmation.remove(sender.uniqueId)

                }

                "init" -> {
                    if (!hasPermission(sender, USER))return false

                    if (args.size < 2)return false

                    val id = args[1].toIntOrNull()

                    if (id == null){
                        sendMessage(sender,"§c§l数字を入力してください")
                        return true
                    }
                    val rg = Region.regionData[id]

                    if (rg==null){
                        sendMessage(sender,"§c§l指定したIDの土地は存在しません")
                        return true
                    }
                    val city = City.get(City.where(rg.teleport)!!)!!

                    if (rg.ownerUUID != sender.uniqueId){
                        sendMessage(sender,"§c§l持ち主以外は使用できません")
                        return false
                    }

//                    val tax = City.getTax(city.name,id)
//
//                    if (!vault.withdraw(sender.uniqueId,tax)){
//                        sendMessage(sender,"§c§l所持金が足りません！(必要額:${format(tax)}円)")
//                        return false
//                    }

                    Region.initRegion(id,city.defaultPrice)

                    sendMessage(sender,"§c§l手放しました")
                }

                "settp" ->{
                    if (!sender.hasPermission(USER))return true

                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val loc = sender.location

                    val data = Region.get(id)?:return false

                    if (!Utility.isWithinRange(loc,data.startPosition,data.endPosition,data.world,data.server)){
                        sendMessage(sender,"§c土地の外にテレポートポイントを登録することはできません")
                        return true
                    }

                    Region.setTeleport(args[1].toInt(), loc)

                    sendMessage(sender,"§e§l登録完了！")
                    return true
                }

                "setrent" ->{// mre setrent id p amount

                    if (!hasPermission(sender,USER))return false

                    if (args.size != 4)return false

                    val id = args[1].toIntOrNull()?:return false
                    val rent = args[3].toDoubleOrNull()

                    if (!hasRegionPermission(sender,id))return false

                    if (rent == null || rent< 0.0){
                        sendMessage(sender,"金額の設定に問題があります！")
                        return true
                    }

                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§l住人がオンラインのときのみ、賃料を変更できます")
                        return false
                    }

                    User.setRentPrice(p,id,rent)

                    sendMessage(sender,"§a§l設定完了！")
                    sendMessage(p,"§a§lID:${id}の賃料が変更されました！賃料:$rent")

                    return true

                }

                "setstatus" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3)return false

                    val id = args[1].toIntOrNull()?:return false
                    val status = args[2]

                    if (!hasRegionPermission(sender,id))return false

                    if (!hasPermission(sender,OP) && status=="Lock"){ return true }

                    Region.setStatus(id,status)

                    sendMessage(sender,"§a§l${id}の土地の状態を${formatStatus(status)}に変更しました")

                    return true

                }

                "setprice" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3)return false

                    if (!NumberUtils.isNumber(args[2]))return false

                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val price = args[2].toDoubleOrNull()

                    if (price==null || price <0.0){
                        sendMessage(sender,"§c§l金額の設定に問題があります！")
                        return false
                    }

                    Region.setPrice(id,price)

                    sendMessage(sender,"§a§l${id}の金額を${args[2]}に変更しました")

                }

                "tp" ->{
                    if (!hasPermission(sender, USER))return false

                    if (args.size < 2)return false

                    val id = args[1].toIntOrNull()

                    if (id == null){
                        sendMessage(sender,"§c§l数字を入力してください")
                        return true
                    }

                    val data = Region.get(id)

                    if (data==null){
                        sendMessage(sender,"§c§l指定したIDの土地は存在しません")
                        return true
                    }

                    sender.teleport(data.teleport)

                    return true

                }

                "balance" ->{

                    if (!hasPermission(sender, USER))return false

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                        Region.showTaxAndRent(sender)
                    })

                }

                else ->{
                    sendMessage(sender,"§c§l不明なコマンドです！")
                    return false

                }
            }
        }

        if (label == "mreop"){

            if (!hasPermission(sender,OP))return false

            if (args.isEmpty()){

                sendMessage(sender,"""
                    §e§l/mreop wand : 範囲指定用のワンドを取得
                    §e§l/mreop create <rg/city> <リージョン名/都市名> <値段/税額> : 新規リージョンを作成します
                    §e§l範囲指定済みの${WAND_NAME}§e§lを持ってコマンドを実行してください
                    §e§l/mreop delete <rg/city> <id> : 指定idのリージョンを削除します
                    §e§l/mreop reload : 再読み込みをします
                    §e§l/mreop where : 現在地点がどのリージョンが確認します
                    §e§l/mreop reset <rg/city> <id> : 指定idのリージョンを再指定します
                    §e§l/mreop disableWorld <add/remove> <world> : 指定ワールドの保護を外します
                    §e§l/mreop tax <id> <tax>: 指定都市の税額を変更します
                    §e§l/mreop buyscore <id> <score>: 指定都市の買うのに必要なスコアを変更します
                    §e§l/mreop livescore <id> <score>: 指定都市の住むのに必要なスコアを変更します
                    §e§l/mreop tp <id> : リソース無しでテレポートする
                    §e§l/mreop init <id> <price> : 指定リージョンを初期化する
                    §e§l/mreop starttax : 手動で税金を徴収する
                    §e§l/mreop search : 指定ユーザーの持っている土地を確認する"
                    §e§l/mreop maxuser <id>: 都市の住める上限を設定する
                    §e§l/mreop calctax <id> : 指定都市で徴収できる税額を計算する
                """.trimIndent())

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

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {

                        val meta = wand.itemMeta

                        val c1 = meta.persistentDataContainer[NamespacedKey.fromString("first")!!,
                                PersistentDataType.STRING]?.split(";")?:return@Runnable
                        val startPosition = Triple(c1[0].toInt(),c1[1].toInt(),c1[2].toInt())

                        val c2 = meta.persistentDataContainer[NamespacedKey.fromString("second")!!,
                                PersistentDataType.STRING]?.split(";")?:return@Runnable

                        val endPosition = Triple(c2[0].toInt(),c2[1].toInt(),c2[2].toInt())

                        var id = -1

                        if (args[1] == "city"){

                            val ret = City.create(startPosition,endPosition,args[2],amount,sender.location)

                            sendMessage(sender,"§a§lcode:${ret} 登録処理終了")

                            return@Runnable

                        }else if (args[1] == "rg"){
                            id = Region.create(startPosition,endPosition,args[2],amount,sender.location)
                        }

                        if (id == -1){
                            sendMessage(sender,"§c§l登録失敗！")
                            return@Runnable
                        }

                        sendMessage(sender,"§a§l登録完了！")

                        if (args[1] == "rg"){
                            sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")
                        }


                    })
                }

                "delete" ->{

                    if (args.size != 3)return false


                    val isRg = args[1] == "rg"

                    if (isRg){

                        if (!NumberUtils.isNumber(args[2])){
                            sendMessage(sender,"§c§l数字を入力してください")
                            return true
                        }

                        val id = args[2].toInt()

                        if (Region.get(id) == null){
                            sendMessage(sender,"§c§l存在しない土地です！")
                            return true

                        }

                        Region.delete(id)
                        sendMessage(sender,"§a§l削除完了！")

                        return true

                    }

                    val id = args[2]

                    if (City.get(id) == null){
                        sendMessage(sender,"§c§l存在しない都市です！")
                        return true

                    }
                    City.delete(id)
                    sendMessage(sender,"§a§l削除完了！")

                }

                "wand" ->{
                    val wand = ItemStack(Material.STICK)
                    val meta = wand.itemMeta
                    meta.displayName(text(WAND_NAME))
                    wand.itemMeta = meta
                    sender.inventory.addItem(wand)
                    return true

                }

                "reload" ->{

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                        Region.load()
                        City.load()

                        for (p in Bukkit.getOnlinePlayers()){
                            User.load(p)
                        }

                        plugin.loadConfig()

                        sendMessage(sender,"§e§lリロード完了")

                    })

                }

                "disableWorld" ->{

                    if (args.size != 3)return true

                    if (args[2].isBlank()){
                        sendMessage(sender,"§3§l保護を外すワールドを指定してください")
                        return true
                    }

                    if (args[1] == "add"){
                        disableWorld.add(args[2])

                        Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                            plugin.config.set("disableWorld", disableWorld)
                            plugin.saveConfig()
                            sendMessage(sender,"追加完了！")
                        })
                    }
                    if (args[1] == "remove"){
                        disableWorld.remove(args[2])

                        Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                            plugin.config.set("disableWorld", disableWorld)
                            plugin.saveConfig()
                            sendMessage(sender,"削除完了！")

                        })
                    }
                }

                "where" ->{

                    val loc = sender.location

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                        sendMessage(sender, "§e§l=====================================")

                        for (rg in Region.regionData) {

                            val data = rg.value

                            if(Utility.isWithinRange(loc, data.startPosition, data.endPosition, data.world,rg.value.server)) {
                                sendMessage(sender, "§e§lRegionID:${rg.key}")
                                sendMessage(sender, "§7Name:${rg.value.name}")
                                sendMessage(sender, "§8Price:${rg.value.price}")
                                sendMessage(sender, "§7Owner:${Region.getOwner(rg.value)}")
                                sendMessage(sender,"§8Tax:${City.getTax(City.whereRegion(rg.key),rg.key)}")

                            }
                        }

                        for (c in City.cityData){

                            val data = c.value

                            if(Utility.isWithinRange(loc, data.getStart(), data.getEnd(), data.world,data.server)) {
                                sendMessage(sender, "§e§lCityID:${c.key}")
                                sendMessage(sender, "§7Name:${c.value.name}")
                                sendMessage(sender, "§8Tax:${c.value.tax}")
                                sendMessage(sender, "§7MaxUser:${c.value.maxUser}")
                            }

                        }

                        sendMessage(sender, "§e§l=====================================")

                    })
                }

                //都市の範囲の再設定
                "reset" ->{//mreop reset city id

                    if (args.size != 3)return false

                    val wand = sender.inventory.itemInMainHand

                    if (!wand.hasItemMeta() || wand.itemMeta.displayName != WAND_NAME){
                        sendMessage(sender,"${WAND_NAME}§e§lを持ってください！")
                        return true
                    }

                    val lore = wand.lore

                    if (lore == null){
                        sendMessage(sender,"§e§fの範囲指定ができていません！")
                        return true
                    }

                    if (!NumberUtils.isNumber(args[2]))return true

                    val isRg = args[1] == "rg"

                    val meta = wand.itemMeta

                    val c1 = meta.persistentDataContainer[NamespacedKey.fromString("first")!!,
                            PersistentDataType.STRING]?.split(";")?:return true
                    val startPosition = Triple(c1[0].toInt(),c1[1].toInt(),c1[2].toInt())

                    val c2 = meta.persistentDataContainer[NamespacedKey.fromString("second")!!,
                            PersistentDataType.STRING]?.split(";")?:return true

                    val endPosition = Triple(c2[0].toInt(),c2[1].toInt(),c2[2].toInt())

                    if (isRg){

                        val id = args[2].toInt()

                        val data = Region.get(id)

                        if (data == null){
                            sendMessage(sender,"§c§l存在しない土地です！")
                            return true
                        }

                        data.startPosition = startPosition
                        data.endPosition = endPosition

                        Region.set(id,data)

                        sendMessage(sender,"§a§l再設定完了！")
                        return true
                    }

                    val id = args[2]

                    val data = City.get(id)

                    if (data == null){
                        sendMessage(sender,"§c§l存在しない土地です！")
                        return true
                    }

                    data.setStart(startPosition)
                    data.setEnd(endPosition)

                    City.set(id, data)

                    sendMessage(sender,"§a§l再設定完了！")
                }

                "tax" ->{

                    if (args.size != 3)return false
                    if (!NumberUtils.isNumber(args[2]))return false

                    val id = args[1]
                    val tax= args[2].toDouble()

                    City.setTax(id,tax)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true
                }

                "tp" ->{

                    val id = args[1].toInt()

                    val data = Region.get(id)?:return true

                    sender.teleport(data.teleport)

                    return true

                }

                "init" ->{

                    val id = args[1].toInt()

                    val price =  args[2].toDouble()

                    Region.initRegion(id,price)

                    sendMessage(sender,"§a§l初期化完了")

                    return true
                }

                "starttax" ->{

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                        sender.sendMessage("税金の徴収開始")
                        User.tax()
                        sender.sendMessage("税金の徴収完了")

                    })

                }

                "search" ->{

                    val uuid = Bukkit.getPlayer(args[1])?.uniqueId

                    if (uuid==null){

                        Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                            val mysql = MySQLManager(plugin,"mre")

                            val rs = mysql.query("select id from region where owner_name = '${args[1]}';")?:return@Runnable

                            while (rs.next()){
                                val id = rs.getInt("id")
                                sendClickMessage(sender,"§e§lID:$id","mreop tp $id","飛ぶ")
                            }

                            rs.close()
                            mysql.close()

                        })

                        return true
                    }
                    for (rg in Region.regionData.filter { it.value.ownerUUID == uuid }.keys){
                        sendClickMessage(sender,"§e§lID:${rg}","mreop tp $rg","飛ぶ")
                    }

                }

                "maxuser" ->{

                    if (args.size != 3)return false
                    if (!NumberUtils.isNumber(args[2]))return false

                    val id = args[1]
                    val amount= args[2].toInt()

                    City.setMaxUser(id,amount)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true


                }

                "calctax" ->{//mreop calctax <id>

                    if(args.size != 2)return false

                    val cityID = args[1]

                    var tax = 0.0

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,Runnable {
                        for (rg in Region.regionData){

                            if (City.whereRegion(rg.key) !=cityID)continue

                            if (rg.value.ownerUUID == null)continue

                            tax += City.getTax(cityID,rg.key)

                        }

                        sendMessage(sender,"ID:$cityID の回収可能税額は、$tax です。")

                    })

                    return true

                }

                "remit" ->{//mreop remit <id>

                    val id = args[1].toIntOrNull()?:return false

                    val rg = Region.get(id)?:return false
                    rg.isRemitTax = !rg.isRemitTax

                    if (rg.isRemitTax){
                        sendMessage(sender,"§a§l$id の税金を免除するようにしました")
                    }else{
                        sendMessage(sender,"§a§l$id の税金を免除を解除しました")
                    }

                    Region.set(id,rg)

                    return true
                }

                "buyscore" ->{
                    if (args.size != 3)return false
                    if (!NumberUtils.isNumber(args[2]))return false

                    val id = args[1]
                    val score= args[2].toInt()

                    City.setBuyScore(id,score)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true

                }

                "livescore" ->{
                    if (args.size != 3)return false

                    val id = args[1]
                    val score= args[2].toIntOrNull()?:return true

                    City.setLiveScore(id,score)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true

                }

                "defaultPrice" ->{//mreop defaultPrice id amount
                    if (args.size != 3)return false

                    val id = args[1]
                    val amount= args[2].toDoubleOrNull()?:return true

                    City.setDefaultPrice(id,amount)

                    sendMessage(sender,"§a§l設定完了！")

                    return true
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

    private fun hasPermission(p:Player, permission:String):Boolean{

        if (p.hasPermission(permission))return true

        sendMessage(p,"§c§lあなたはこのコマンドを使うことができません！")
        return false

    }


    /**
     * 指定リージョンの編集権限を持っているかどうか
     */
    private fun hasRegionPermission(p:Player,id:Int):Boolean{

        if (p.hasPermission(OP))return true

        val data = Region.get(id)?:return false

        if (data.status == "Lock")return false

        if (data.ownerUUID == p.uniqueId)return true

        val userData = User.get(p,id)?:return false

        if (userData.allowAll && userData.status == "Share")return true

        return false

    }
}