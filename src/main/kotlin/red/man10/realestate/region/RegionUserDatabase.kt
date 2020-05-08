package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.realestate.Constants.Companion.isLiked
import red.man10.realestate.Constants.Companion.mysqlQueue
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import java.util.*

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

        data.statsu = status

        regionUserData[Pair(user,regionId)] = data
    }

    ///////////////////////
    //ユーザーデータを削除
    ///////////////////////
    fun removeUserData(regionId: Int,user: Player){
        regionUserData.remove(Pair(user,regionId))
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

            val key = Pair(p,rs1.getInt("region_id"))

            val d = regionData[key.second]?:continue

            val data = RegionUserData()

            data.paid = rs1.getDate("paid_date")
//            data.deposit = rs1.getDouble("deposit")
            data.statsu = rs1.getString("deposit")

            data.allowAll = rs1.getInt("allow_all")==1
            data.allowBlock = rs1.getInt("allow_block")==1
            data.allowInv = rs1.getInt("allow_inv")==1
            data.allowDoor = rs1.getInt("allow_door")==1

            regionUserData[key] = data

            if (data.statsu=="Lock"){
                pl.sendMessage(p,"§4§lLockされたリージョン:${d.name}")
            }
        }

        rs1.close()
        mysql.close()

        //いいねデータ
        val rs2 = mysql.query("SELECT * FROM `liked_index` WHERE `uuid`='${p.uniqueId}';")?:return

        while (rs2.next()){

            val isLike = rs2.getInt("is_like")
            val id = rs2.getInt("region_id")

            isLiked[Pair(p,id)] = isLike == 1
        }

        rs2.close()
        mysql.close()

    }

    ///////////////////////////////
    //ユーザーデータの保存
    ///////////////////////////////
    fun saveUserData(p:Player,id:Int){

        val key = Pair(p,id)

        val data = regionUserData[key]?:return

        val sql = "UPDATE `region_user` " +
                "SET " +
                "`status`='${data.statsu}'," +
//                "`deposit`='${data.deposit}'," +
                "`paid_date`='${data.paid}'," +
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
    fun setLike(p:Player,id:Int):Boolean{

        val key = Pair(p,id)
        var isLike = isLiked[key]

        if (isLike == null){
            isLike = true
            mysqlQueue.add("INSERT INTO `liked_index` (`region_id`, `player`, `uuid`, `score`) VALUES ('$id', '${p.name}', '${p.uniqueId}', '0');")
        }else{
            isLike = !isLike
        }

        isLiked[key] = isLike

        mysqlQueue.add("UPDATE `liked_index` SET `is_like`='${if (isLike){ 1 }else{ 0 }}' WHERE `uuid`='${p.uniqueId}' AND `region_id`=$id;")

        return isLike

    }
    //////////////////////////
    //支払い処理
    //////////////////////////
//    fun addDeposit(id: Int,p:Player,price:Double):Boolean{
//
//        val pd = regionUserData[Pair(p,id)]?:return false
//
//        if (!pd.isRent)return false
//
//        if (pl.vault.getBalance(p.uniqueId) < price)return false
//
//        pl.vault.withdraw(p.uniqueId,price)
//        pd.deposit += price
//
//        pd.statsu = "Share"
//
//        saveUserData(p,id)
//
//        return true
//    }

    class RegionUserData{

//        var deposit : Double = 0.0  //この金がなくなったら支払えなくなる
        var paid = Date()  //最後に支払った日
        var statsu = ""
        var isRent = false //賃貸の場合true

        var allowAll = false
        var allowBlock = false
        var allowInv = false
        var allowDoor = false
    }

}