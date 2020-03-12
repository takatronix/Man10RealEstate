package red.man10.realestate.region

import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import java.sql.ClientInfoStatus
import java.util.*

@Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
class RegionUserDatabase (private val pl:Plugin){


    fun addUserData(regionId:Int,user:Player,type:Int,status: String){

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

    fun changeDeposit(id:Int,user:Player,price:Double){

        val data = pl.regionUserData[Pair(user,id)]?:return

        data.deposit = price

        pl.regionUserData[Pair(user,id)] = data

        MySQLManager(pl,"region_user").execute(
              "UPDATE `region_user` " +
                    "SET `deposit`='$price' " +
                    "WHERE  `region_id`='$id' AND `uuid`='${user.uniqueId}';")

    }

    fun removeUser(id:Int,user:Player){
        pl.regionUserData.remove(Pair(user,id))

        MySQLManager(pl,"region_user").execute("DELETE FROM `region_user` WHERE  `region_id`=${id} AND `uuid`='${user.uniqueId}';")

    }




    class RegionUserData{

        var deposit : Double = 0.0
        var paid = Date()
        var statsu = ""
        var type = 0

    }

}