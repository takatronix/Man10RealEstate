package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.man10score.ScoreDatabase
import red.man10.realestate.Plugin
import red.man10.realestate.util.MySQLManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//居住者に関するクラス
class User(val uuid: UUID,val regionId:Int) {

    companion object{
        val userMap = ConcurrentHashMap<Pair<UUID,Int>,User>()

        fun get(p:Player,id:Int):User?{
            return userMap[Pair(p.uniqueId,id)]
        }

        fun fromRegion(rg:Int): List<User> {
            return userMap.filterKeys { it.second == rg }.values.toList()
        }

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
                    user.paid = rs.getTimestamp("paid_date").toLocalDateTime()
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

        //指定リージョンの全ユーザーを削除
        fun asyncDeleteAllRegionUser(regionId: Int){
            MySQLManager.mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`=$regionId;")
            fromRegion(regionId).forEach { userMap.remove(Pair(it.uuid,it.regionId)) }
        }

        fun asyncLoginProcess(p:Player){
            Plugin.async.execute {
                val data = userMap.filterValues { it.uuid == p.uniqueId }
                val score = ScoreDatabase.getScore(p.uniqueId)

                data.forEach {
                    val id = it.key.second
                    val city = City.where(Region.regionData[id]!!.teleport)!!
                    if (city.liveScore>score){ it.value.asyncDelete() }
                }
            }
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
        //こっちは新規作成
        if (!userMap.containsKey(Pair(uuid,regionId))){
            val name = Bukkit.getOfflinePlayer(uuid).name
            MySQLManager.mysqlQueue.add("INSERT INTO region_user " +
                    "(region_id, player, uuid, created_time, status, is_rent, paid_date, rent) " +
                    "VALUES ($regionId, '${name}', '${uuid}', now(), 'Share', ${if (rentAmount>0) 1 else 0}, now(), ${rentAmount});")
            userMap[Pair(uuid,regionId)] = this
            return
        }

        //存在する場合
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
                " WHERE `uuid`='${uuid}' AND `region_id`='$regionId';")
    }

    fun asyncDelete(){
        MySQLManager.mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`=$regionId AND `uuid`='${uuid}';")
        userMap.remove(Pair(uuid,regionId))
    }

    fun payRent(){
        if (!isRent)return

        if (!Plugin.bank.withdraw(uuid,rentAmount,"Man10RealEstate Rent","賃料の支払い")){
            status = "Lock"
        }else{
            paid = LocalDateTime.now()
            val region = Region.regionData[regionId]
            if (region?.ownerUUID != null){
                Plugin.bank.deposit(region.ownerUUID!!,rentAmount,"Man10RealEstate Rent","賃料の支払い")
            }
        }

        asyncSave()
    }

    fun hasPermission(permission: Permission):Boolean{
        val region=Region.regionData[regionId]?:return false
        if (region.status == Region.Status.LOCK)return false
        if (region.ownerUUID == uuid)return true
        if (region.status == Region.Status.DANGER)return true

        if (permission != Permission.BLOCK &&region.status == Region.Status.FREE)return true

        if (status == "Lock")return false
        if (allowAll)return true

        when(permission){

            Permission.BLOCK ->{ if (allowBlock)return true }
            Permission.INVENTORY ->{ if (allowInv)return true }
            Permission.DOOR ->{ if (allowDoor)return true }
            else->return false

        }

        return false
    }

    enum class Permission{
        ALL,
        BLOCK,
        DOOR,
        INVENTORY
    }

}