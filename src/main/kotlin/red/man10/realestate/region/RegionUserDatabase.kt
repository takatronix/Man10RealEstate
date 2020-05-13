package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.realestate.Constants.Companion.isLike
import red.man10.realestate.Constants.Companion.mysqlQueue
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import java.util.*
import kotlin.collections.HashMap

class RegionUserDatabase (private val pl:Plugin){

    val mysql = MySQLManager(pl,"mreUserData")

    //////////////////////////////////
    //ユーザーデータを新規作成
    //////////////////////////////////
    fun createUserData(regionId:Int,user:Player,status: String){

        val data = RegionUserData()

        val sql = "INSERT INTO `region_user` " +
                "(`region_id`," +
                " `uuid`," +
                " `player`," +
                " `created_time`," +
                " `status`)" +
                " VALUES " +
                "('$regionId'," +
                " '${user.uniqueId}'," +
                " '${user.name}'," +
                " now()," +
                " '$status');"

        mysqlQueue.add(sql)

        data.status = status

        saveMap(user,data,regionId)
    }

    ///////////////////////
    //ユーザーデータを削除
    ///////////////////////
    fun removeUserData(regionId: Int,user: Player){
        regionUserData[user]!!.remove(regionId)
        mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`='$regionId' AND `uuid`='${user.uniqueId}';")
    }


    /////////////////////////////
    //ユーザーデータを読み込み
    /////////////////////////////
    @Synchronized
    fun loadUserData(p:Player){

        //リージョンデータ
        val rs1 = mysql.query("SELECT * FROM `region_user` WHERE `uuid`='${p.uniqueId}';")?:return

        while (rs1.next()){

            val id = rs1.getInt("region_id")

            val d = regionData[id]?:continue

            val data = RegionUserData()

            data.paid = rs1.getDate("paid_date")
            data.status = rs1.getString("status")

            data.allowAll = rs1.getInt("allow_all")==1
            data.allowBlock = rs1.getInt("allow_block")==1
            data.allowInv = rs1.getInt("allow_inv")==1
            data.allowDoor = rs1.getInt("allow_door")==1

            saveMap(p,data,id)

            if (data.status=="Lock"){
                pl.sendMessage(p,"§4§lLockされたリージョン:${d.name}")
            }
        }

        rs1.close()
        mysql.close()

        //いいねデータ
        val rs2 = mysql.query("SELECT * FROM `liked_index` WHERE `uuid`='${p.uniqueId}';")?:return

        val list = mutableListOf<Int>()

        while (rs2.next()){

            val isLike = rs2.getInt("is_like")
            val id = rs2.getInt("region_id")

            if (isLike == 1){
                list.add(id)
            }
        }

        isLike[p] = list

        rs2.close()
        mysql.close()

        if (getProfit(p) > 0){
            p.performCommand("mre bal")
        }

    }

    ///////////////////////////////
    //ユーザーデータの保存
    ///////////////////////////////
    fun saveUserData(p:Player,id:Int){

        val data = regionUserData[p]!![id]?:return

        val sql = "UPDATE `region_user` " +
                "SET " +
                "`status`='${data.status}'," +
                "`paid_date`='${data.paid}'," +
                "`is_rent`='${data.isRent}`,"
                "`allow_all`='${if (data.allowAll){1}else{0}}'," +
                "`allow_block`='${if (data.allowBlock){1}else{0}}'," +
                "`allow_inv`='${if (data.allowInv){1}else{0}}'," +
                "`allow_door`='${if (data.allowDoor){1}else{0}}'" +
                " WHERE `uuid`='${p.uniqueId}' AND `region_id`='$id';"
        mysqlQueue.add(sql)

    }

    ///////////////////////////////
    //利益を取り出す
    ///////////////////////////////
    fun takeProfit(p:Player){

        pl.vault.deposit(p.uniqueId,getProfit(p))

        val sql = "UPDATE `user_index` SET `received`='1' WHERE `uuid`='${p.uniqueId}';"

        mysqlQueue.add(sql)
    }

    /////////////////////////////
    //利益の取得
    /////////////////////////////
    fun getProfit(p:Player):Double{

        val mysql = MySQLManager(pl,"mreGetProfit")
        var profit = 0.0
        val rs = mysql.query("SELECT `profit` FROM `user_index` " +
                "WHERE `uuid`='${p.uniqueId}' AND `received`='0';")?:return 0.0

        while (rs.next()){
            profit += rs.getDouble("profit")
        }
        return profit
    }

    //////////////////////////////
    //利益の追加
    //////////////////////////////
    fun addProfit(uuid:UUID,amount:Double){
        val p = Bukkit.getOfflinePlayer(uuid)

        if (p.isOnline && p.player != null){
            pl.sendMessage(p.player!!,"§e§l入金情報:$${amount}")
        }
        mysqlQueue.add("INSERT INTO user_index (uuid, player, profit, type, received, date) " +
                "VALUES ('$uuid', '${p.name}', '$amount', DEFAULT, DEFAULT, DEFAULT)")
    }

    ///////////////////////////////
    //いいね、いいね解除する
    ///////////////////////////////
    fun setLike(p:Player,id:Int){

        val data = regionData[id]?:return

        if (data.owner_uuid == p.uniqueId){
            pl.sendMessage(p,"§3§lあなたはオーナーなのでいいね出来ません！")
            return
        }

        val list = isLike[p]?: mutableListOf()

        if (list.isEmpty()){
            mysqlQueue.add("INSERT INTO `liked_index` (`region_id`, `player`, `uuid`, `score`) VALUES ('$id', '${p.name}', '${p.uniqueId}', '0');")

            list.add(id)
            isLike[p] = list
            pl.sendMessage(p,"§a§lいいねしました！")
            return
        }

        if (list.contains(id)){
            list.remove(id)
            pl.sendMessage(p,"§a§aいいね解除しました！")
        }else{
            list.add(id)
            pl.sendMessage(p,"§a§lいいねしました！")
        }

        isLike[p] = list

        mysqlQueue.add("UPDATE `liked_index` SET `is_like`='${if (list.contains(id)){ 1 }else{ 0 }}' WHERE `uuid`='${p.uniqueId}' AND `region_id`=$id;")


    }

    fun saveMap(p:Player,data:RegionUserData,id:Int){

        val map = regionUserData[p]?: HashMap()
        map[id] = data
        regionUserData[p] = map

    }

    class RegionUserData{

//        var deposit : Double = 0.0  //この金がなくなったら支払えなくなる
        var paid = Date()  //最後に支払った日
        var status = ""
        var isRent = false //賃貸の場合true

        var allowAll = false
        var allowBlock = false
        var allowInv = false
        var allowDoor = false
    }

}