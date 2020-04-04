package red.man10.realestate.region

import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import java.util.*

class RegionUserDatabase (private val pl:Plugin){

    val mysql = MySQLManager(pl,"mreUserData")

    //////////////////////////////////
    //ユーザーデータを新規作成
    //////////////////////////////////
    fun createUserData(regionId:Int,user:Player,type:Int,status: String){

        val data = RegionUserData()

        val sql = "INSERT INTO `region_user` " +
                "(`region_id`," +
                " `type`," +
                " `uuid`," +
                " `player`," +
                " `created_time`," +
                " `status`)" +
                " VALUES " +
                "('$regionId'," +
                " '$type'," +
                " '${user.uniqueId}'," +
                " '${user.name}'," +
                " now()," +
                " '$status');"

        pl.mysqlQueue.add(sql)

        data.type = type
        data.statsu = status

        pl.regionUserData[Pair(user,regionId)] = data
    }

    ///////////////////////
    //ユーザーデータを削除
    ///////////////////////
    fun removeUserData(regionId: Int,user: Player){
        pl.mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`='$regionId' AND `uuid`='${user.uniqueId}';")
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

            val data = RegionUserData()

            data.paid = rs1.getDate("paid_date")
            data.deposit = rs1.getDouble("deposit")
            data.statsu = rs1.getString("deposit")
            data.type = rs1.getInt("type")

            pl.regionUserData[key] = data

        }

        rs1.close()
        mysql.close()

        //いいねデータ
        val rs2 = mysql.query("SELECT * FROM `liked_index` WHERE `uuid`='${p.uniqueId}';")?:return

        while (rs2.next()){

            val isLike = rs2.getInt("is_like")
            val id = rs2.getInt("region_id")

            if (isLike == 1){
                pl.isLiked[Pair(p,id)] = true
            }else if (isLike == 0){
                pl.isLiked[Pair(p,id)] = false
            }
        }

        rs2.close()
        mysql.close()

    }

    ///////////////////////////////
    //ユーザーデータの保存
    ///////////////////////////////
    fun saveUserData(p:Player,id:Int){

        val key = Pair(p,id)

        val data = pl.regionUserData[key]?:return

        val sql = "UPDATE `region_user` " +
                "SET `type`='${data.type}', `status`='${data.statsu}'," +
                " `deposit`='${data.deposit}', `paid_date`='${data.paid}'" +
                " WHERE `uuid`='${p.uniqueId}' AND `region_id`='$id';"
        pl.mysqlQueue.add(sql)

    }

    ///////////////////////////////
    //利益を取り出す
    ///////////////////////////////
    fun takeProfit(p:Player){

        pl.vault.deposit(p.uniqueId,getProfit(p))

         val sql = "UPDATE `user_index` SET `received`='1' WHERE `uuid`='${p.uniqueId}';"

        pl.mysqlQueue.add(sql)
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

    ///////////////////////////////
    //いいね、いいね解除する
    ///////////////////////////////
    fun setLike(p:Player,id:Int):Boolean{

        val key = Pair(p,id)
        var isLiked = pl.isLiked[key]

        if (isLiked == null){
            isLiked = true
            pl.mysqlQueue.add("INSERT INTO `liked_index` (`region_id`, `player`, `uuid`, `score`) VALUES ('0', 'aaa', 'aaa', '0');")
        }else{
            isLiked != isLiked
        }

        pl.isLiked[key] = isLiked

        pl.mysqlQueue.add("UPDATE `liked_index` SET `is_like`='${if (isLiked){ 1 }else{ 0 }}';")

        return isLiked

    }
    //////////////////////////
    //支払い処理
    //////////////////////////
    fun addDeposit(id: Int,p:Player,price:Double):Boolean{

        val pd = pl.regionUserData[Pair(p,id)]?:return false

        if (!pd.isRent)return false

        if (pl.vault.getBalance(p.uniqueId) < price)return false

        pl.vault.withdraw(p.uniqueId,price)
        pd.deposit += price

        pd.statsu = "Share"

        saveUserData(p,id)

        return true
    }

    class RegionUserData{

        var deposit : Double = 0.0
        var paid = Date()
        var statsu = ""
        var type = 0
        var isRent = false
    }

}