package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.MySQLManager.Companion.mysqlQueue
import red.man10.realestate.Plugin.Companion.bank
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Utility.sendMessage
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

object User{

    val userData = ConcurrentHashMap<Player,HashMap<Int,UserData>>()
    val likeData = ConcurrentHashMap<Player,MutableList<Int>>()

//    val ownerList = ConcurrentHashMap<Player,MutableList<Int>>()

    fun set(p:Player,id:Int,data:UserData){
        (userData[p]?: HashMap())[id] = data
        save(p,id)
    }

    fun get(p:Player,id:Int): UserData? {
        val data = userData[p]?: HashMap()
        return data[id]
    }

    @Synchronized
    fun get(uuid: UUID,id:Int):UserData?{
        val p = Bukkit.getOfflinePlayer(uuid)
        if (p.isOnline){
            return get(p.player!!,id)
        }

        val mysql = MySQLManager(plugin,"Man10RealEstate")

        val rs = mysql.query("SELECT * FROM `region_user` WHERE `uuid`='${p.uniqueId}' AND region_id=$id;")?:return null

        val data = UserData()

        while (rs.next()){


            data.status = rs.getString("status")

            data.allowAll = rs.getInt("allow_all")==1
            data.allowBlock = rs.getInt("allow_block")==1
            data.allowInv = rs.getInt("allow_inv")==1
            data.allowDoor = rs.getInt("allow_door")==1

            data.isRent = rs.getInt("is_rent")==1

        }

        rs.close()
        mysql.close()

        return data
    }

    /**
     * 住人データを削除
     */
    fun remove(p:Player,id:Int){
        if (!userData[p].isNullOrEmpty()){
            userData[p]!!.remove(id)
        }

        mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`='$id' AND `uuid`='${p.uniqueId}';")

    }

    fun remove(uuid:UUID,id:Int){

        mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`='$id' AND `uuid`='${uuid}';")

    }

    /**
     * 指定idの全住人データを削除
     */
    fun removeAll(id:Int){

        for (data in userData.values){
            data.remove(id)
        }

        mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`='$id';")
    }

    /**
     * 新規住人データを作成
     */
    fun create(p:Player,id:Int,rent: Double){

        mysqlQueue.add("INSERT INTO region_user " +
                "(region_id, player, uuid, created_time, status, is_rent, paid_date, rent) " +
                "VALUES ($id, '${p.name}', '${p.uniqueId}', now(), 'Share', ${if (rent>0) 1 else 0}, now(), ${rent});")


        val data = UserData()

        data.rent = rent
        data.isRent = rent>0
//        data.paid = Date()

        set(p,id,data)

    }


    /**
     * 指定プレイヤーの住人データを読み込む
     */
    @Synchronized
    fun load(p:Player){

        if (!userData[p].isNullOrEmpty()){ userData[p]!!.clear() }

        val mysql = MySQLManager(plugin,"Man10RealEstate")

        val rs1 = mysql.query("SELECT * FROM `region_user` WHERE `uuid`='${p.uniqueId}';")?:return

        val map = HashMap<Int,UserData>()

//        val ownerList = mutableListOf<Int>()

        while (rs1.next()){

            val id = rs1.getInt("region_id")

            if (Region.get(id) ==null) {
                remove(p, id)
                continue
            }

            val data = UserData()

            data.status = rs1.getString("status")

            data.allowAll = rs1.getInt("allow_all")==1
            data.allowBlock = rs1.getInt("allow_block")==1
            data.allowInv = rs1.getInt("allow_inv")==1
            data.allowDoor = rs1.getInt("allow_door")==1

            data.isRent = rs1.getInt("is_rent")==1

            map[id] = data

            if (data.status == "Lock"){ sendMessage(p,"§c§lID:${id}の賃料が支払われていませんでした！") }

//            if (data.allowAll){ ownerList.add(id) }

        }

        userData[p] = map

        rs1.close()
        mysql.close()

        val rs2 = mysql.query("SELECT * FROM `liked_index` WHERE `uuid`='${p.uniqueId}' AND is_like=1;")?:return

        val likeList = mutableListOf<Int>()

        while (rs2.next()){ likeList.add(rs2.getInt("region_id")) }

        likeData[p] = likeList

        rs2.close()
        mysql.close()

//        //オーナーリストに追加
//        for (rg in Region.regionData){
//            if (rg.value.ownerUUID == p.uniqueId)ownerList.add(rg.key)
//        }
//        this.ownerList[p] = ownerList

    }

    /**
     * 住人のリストを返す(inventory用)
     */
    @Synchronized//uuid,data
    fun loadUsers(id:Int): MutableList<Pair<UUID,UserData>>? {

        val mysql = MySQLManager(plugin,"Man10RealEstate")

        val rs = mysql.query("SELECT * FROM `region_user` WHERE `region_id`='$id';")?:return null

        val list = mutableListOf<Pair<UUID,UserData>>()

        while (rs.next()){

            val data = UserData()

            data.status = rs.getString("status")

            data.rent = rs.getDouble("rent")

            data.allowAll = rs.getInt("allow_all")==1
            data.allowBlock = rs.getInt("allow_block")==1
            data.allowInv = rs.getInt("allow_inv")==1
            data.allowDoor = rs.getInt("allow_door")==1

            list.add(Pair(UUID.fromString(rs.getString("uuid")),data))
        }

        rs.close()
        mysql.close()

        return list
    }


    /**
     * ユーザーデータの保存
     */
    private fun save(p:Player,id:Int){

        val data = get(p,id)?:return

        mysqlQueue.add("UPDATE `region_user` " +
                "SET " +
                "`status`='${data.status}'," +
//                "`paid_date`='${Timestamp(data.paid.time)}'," +
                "`is_rent`='${if (data.isRent){1}else{0}}',"+
                "`allow_all`='${if (data.allowAll){1}else{0}}'," +
                "`allow_block`='${if (data.allowBlock){1}else{0}}'," +
                "`allow_inv`='${if (data.allowInv){1}else{0}}'," +
                "`allow_door`='${if (data.allowDoor){1}else{0}}'," +
                "`rent`='${data.rent}'" +
                " WHERE `uuid`='${p.uniqueId}' AND `region_id`='$id';"
        )
    }

    /**
     * @return いいねしてるかどうか
     */
    private fun isLiked(p:Player, id:Int):Boolean{

        val data = likeData[p]

        if (data.isNullOrEmpty())return false

        if (data.contains(id))return true

        return false
    }

    /**
     * いいね、いいねの解除(トグル式)
     */
    fun changeLike(p:Player, id:Int){
//
//        val rg = Region.get(id)?:return
//
//        if (rg.ownerUUID == p.uniqueId){
//            sendMessage(p,"§3§lあなたはオーナーなのでいいね出来ません！")
//            return
//        }

        if (isLiked(p,id)){
            likeData[p]!!.remove(id)

            mysqlQueue.add("DELETE FROM `liked_index` WHERE `uuid`='${p.uniqueId}' AND `region_id`=$id;")
            sendMessage(p,"§a§aいいね解除しました！")
            return
        }

        likeData[p]!!.add(id)
        mysqlQueue.add("INSERT INTO `liked_index` (`region_id`, `player`, `uuid`, `score`) VALUES ('$id', '${p.name}', '${p.uniqueId}', '0');")
        sendMessage(p,"§a§lいいねしました！")
    }


    /**
     * 賃料の変更
     */
    fun setRentPrice(p:Player,id:Int,rent:Double){

        val data = get(p,id)?:return

        if (rent == 0.0){
            isChargeRent(p,id,false)
        }else{
            isChargeRent(p,id,true)
        }

        data.rent = rent
        set(p,id,data)
    }

    /**
     * 賃料を徴収するかどうか
     */
    private fun isChargeRent(p:Player, id:Int, isRent:Boolean){

        val data = get(p,id)?:return
        data.isRent = isRent
        set(p,id,data)

    }

    fun setPermission(uuid:UUID, id:Int, perm: Permission, value:Boolean){

        val p = Bukkit.getOfflinePlayer(uuid)

        if (p.isOnline){

            val player = p.player!!

            val data = get(player,id)?:return

            when(perm){
                Permission.ALL-> data.allowAll = value
                Permission.BLOCK -> data.allowBlock = value
                Permission.DOOR -> data.allowDoor = value
                Permission.INVENTORY -> data.allowInv = value
            }

            set(player,id,data)

            return
        }

        //オフラインだった場合
        when(perm){
            Permission.ALL -> mysqlQueue.add("UPDATE region_user SET allow_all='${if (value){1}else{0}}' WHERE uuid='${uuid}' AND region_id=$id;")
            Permission.BLOCK -> mysqlQueue.add("UPDATE region_user SET allow_block='${if (value){1}else{0}}' WHERE uuid='${uuid}' AND region_id=$id;")
            Permission.DOOR -> mysqlQueue.add("UPDATE region_user SET allow_door='${if (value){1}else{0}}' WHERE uuid='${uuid}' AND region_id=$id;")
            Permission.INVENTORY -> mysqlQueue.add("UPDATE region_user SET allow_inv='${if (value){1}else{0}}' WHERE uuid='${uuid}' AND region_id=$id;")
        }
    }

    /**
     * 指定リージョンの賃料を支払う
     * (タイムスタンプも押される)
     *
     * @param p 賃料を支払うプレイヤー
     * @param id リージョンのid
     * @param rent 賃料
     *
     * @return 賃料の支払いが成功したらtrue
     */
    private fun payingRent(p:UUID,id:Int,rent:Double):Boolean{

        val rg = Region.get(id)?:return false
        val owner = rg.ownerUUID

        if (!bank.withdraw(p,rent,"Man10RealEstate Rent")){
            mysqlQueue.add("UPDATE `region_user` SET status='Lock' WHERE uuid='$p' AND region_id=$id;")
            return false
        }

        if (owner !=null){ bank.deposit(owner,rent,"Man10RealEstate RentProfit") }

        mysqlQueue.add("UPDATE `region_user` SET paid_date=now(), status='Share' WHERE uuid='$p' AND region_id=$id;")

        return true
    }

    fun rent(){

        val mysql = MySQLManager(plugin,"Man10RealEstate")

        val rs = mysql.query("SELECT * FROM region_user WHERE rent>0.0;")?:return

        while (rs.next()){

            val id = rs.getInt("region_id")
            val uuid = UUID.fromString(rs.getString("uuid"))

            val rent = rs.getDouble("rent")

            val different = (Date().time - rs.getDate("paid_date").time)/1000/3600/24

            val rg = Region.get(id)?:continue

            if (rg.span == 0 && different < 30)continue
            if (rg.span == 1 && different < 7)continue
            if (rg.span == 2 && different < 1)continue

            payingRent(uuid,id,rent)
        }

        rs.close()
        mysql.close()

    }

    fun tax(){
        Bukkit.getLogger().info("税金の徴収開始")
        for (rg in Region.regionData){
            val uuid = rg.value.ownerUUID?:continue
            City.payingTax(uuid,rg.key)
        }
        Bukkit.getLogger().info("税金の徴収完了！")

    }


    class UserData{
        var status = "Share"
        var isRent = false //賃貸の場合true

        var allowAll = false
        var allowBlock = false
        var allowInv = false
        var allowDoor = false

        var rent = 0.0

    }

    enum class Permission{
        ALL,
        BLOCK,
        INVENTORY,
        DOOR
    }

}