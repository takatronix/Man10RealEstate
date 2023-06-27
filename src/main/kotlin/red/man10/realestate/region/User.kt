package red.man10.realestate.region

import org.bukkit.Bukkit
import red.man10.realestate.Plugin
import red.man10.realestate.util.MySQLManager
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//居住者に関するクラス
class User(val uuid: UUID,val id:Int) {

    companion object{
        val userMap = ConcurrentHashMap<Pair<UUID,Int>,User>()

        fun asyncLoad(){
            userMap.clear()
            Plugin.async.execute {
                val sql = MySQLManager(Plugin.plugin,"Man10RealEstate")
                val rs = sql.query("SELECT * FROM `region_user`")?:return@execute
                while (rs.next()){
                    val id = rs.getInt("region_id")
                    val uuid = UUID.fromString(rs.getString("uuid"))
                    val user = User(uuid, id)

                    user.status = rs.getString("status")
                    user.paid = LocalDateTime.ofInstant(rs.getDate("paid_date").toInstant(), ZoneId.systemDefault())
                    user.isRent = rs.getInt("is_rent") == 1
                    user.rentAmount = rs.getDouble("rent")

                    user.allowAll = rs.getInt("allow_all") == 1
                    user.allowBlock = rs.getInt("allow_block") == 1
                    user.allowDoor = rs.getInt("allow_door") == 1
                    user.allowInv = rs.getInt("allow_inv") == 1

                    userMap[Pair(uuid,id)] = user
                }

                rs.close()
                sql.close()
            }
        }

        fun asyncDeleteFromRegion(id: Int){
            MySQLManager.mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`=$id;")
        }

    }

    var status = "Share"
    var isRent = false
    var paid = LocalDateTime.now()

    var allowAll = false
    var allowBlock = false
    var allowInv = false
    var allowDoor = false

    var rentAmount = 0.0

    //辞書にデータがなかったら新規情報として保存
    fun asyncSave(){
        if (!userMap.containsKey(Pair(uuid,id))){
            val name = Bukkit.getOfflinePlayer(uuid).name
            MySQLManager.mysqlQueue.add("INSERT INTO region_user " +
                    "(region_id, player, uuid, created_time, status, is_rent, paid_date, rent) " +
                    "VALUES ($id, '${name}', '${uuid}', now(), 'Share', ${if (rentAmount>0) 1 else 0}, now(), ${rentAmount});")
            return
        }

        MySQLManager.mysqlQueue.add("UPDATE `region_user` " +
                "SET " +
                "`status`='${status}'," +
                "`paid_date`='${paid.format(DateTimeFormatter.ISO_LOCAL_DATE)}'," +
                "`is_rent`='${if (isRent){1}else{0}}',"+
                "`allow_all`='${if (allowAll){1}else{0}}'," +
                "`allow_block`='${if (allowBlock){1}else{0}}'," +
                "`allow_inv`='${if (allowInv){1}else{0}}'," +
                "`allow_door`='${if (allowDoor){1}else{0}}'," +
                "`rent`='${rentAmount}'" +
                " WHERE `uuid`='${uuid}' AND `region_id`='$id';")
    }

    fun asyncDelete(){
        MySQLManager.mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`=$id AND `uuid`='${uuid}';")
    }

    fun payRent(){
        if (!isRent)return

        if (!Plugin.bank.withdraw(uuid,rentAmount,"Man10RealEstate Rent","賃料の支払い")){
            status = "Lock"
        }else{
            paid = LocalDateTime.now()
        }

        asyncSave()
    }

}