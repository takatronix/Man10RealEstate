package red.man10.realestate.region

import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import java.sql.ClientInfoStatus
import java.sql.ResultSet
import java.util.*

class RegionUserDatabase (private val pl:Plugin){


    fun createUserData(regionId:Int,user:Player,type:Int,status: String){

        val data = RegionUserData()

        val sql = "INSERT INTO `region_user` " +
                "(`region_id`," +
                " `type`," +
                " `uuid`," +
                " `name`," +
                " `created_time`," +
                " `status`)" +
                " VALUES " +
                "('$regionId'," +
                " '$type'," +
                " '${user.uniqueId}'," +
                " '${user.name}'," +
                " now()," +
                " '$status');"

        val mysql = MySQLManager(pl,"region_user")
        mysql.execute(sql)

        data.type = type
        data.statsu = status

        pl.regionUserData[Pair(user,regionId)] = data
    }

    //////////////////////////
    //
    //////////////////////////
    fun changeDeposit(id:Int,user:Player,price:Double){

        val data = pl.regionUserData[Pair(user,id)]?:return

        data.deposit = price

        pl.regionUserData[Pair(user,id)] = data

        MySQLManager(pl,"region_user").execute(
              "UPDATE `region_user` " +
                    "SET `deposit`='$price'," +
                      "`paid_date`=now() " +
                    "WHERE  `region_id`='$id' AND `uuid`='${user.uniqueId}';")

        data.paid = Date()

        pl.regionUserData[Pair(user,id)] = data
    }

    //////////////////////////
    //共同者データを削除する
    //////////////////////////
    fun removeUser(id:Int,user:Player){
        pl.regionUserData.remove(Pair(user,id))

        MySQLManager(pl,"region_user").execute("DELETE FROM `region_user` WHERE  `region_id`=${id} AND `uuid`='${user.uniqueId}';")

    }

    //////////////////////////////////////////
    //指定プレイヤーに利益を追加する
    //////////////////////////////////////////
    fun addProfit(user:Player,price: Double,type:String){

        MySQLManager(pl,"userindex").execute("INSERT INTO `user_index` " +
                "(`uuid`, `player`, `profit`, `type`, `date`)" +
                " VALUES ('${user.uniqueId}', '${user.name}','$price','$type', now());")

    }

    ///////////////////////////
    //利益の額を取得する
    ///////////////////////////
    fun getProfit(user:Player,type: String):Double{

        var profit = 0.0

        val mysql = MySQLManager(pl,"userindex")

        val rs : ResultSet

        rs = if (type == "all"){
            mysql.query("SELECT `profit` WHERE `uuid`='${user.uniqueId}' and `received`='0';")?:return 0.0
        }else{
            //家賃:house 投げ銭:donation 土地販売:land
            mysql.query("SELECT `profit` WHERE `uuid`='${user.uniqueId}' and `received`='0' and `type`='$type';")?:return 0.0
        }

        while (rs.next()){
            profit+=rs.getDouble("profit")
        }

        return profit
    }

    ///////////////////////////////////////
    //溜まっているお金を取り出す
    ///////////////////////////////////////
    fun takeProfit(user:Player){
        val profit = getProfit(user,"all")

        MySQLManager(pl,"userindex").execute("UPDATE `user_index` SET `received`='1'" +
                " WHERE `received`=0 and `uuid`='${user.uniqueId}';")

        pl.vault.deposit(user.uniqueId,profit)

    }

    class RegionUserData{

        var deposit : Double = 0.0
        var paid = Date()
        var statsu = ""
        var type = 0

    }

}