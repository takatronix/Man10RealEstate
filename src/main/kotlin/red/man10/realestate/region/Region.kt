package red.man10.realestate.region

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.man10score.ScoreDatabase
import red.man10.realestate.Plugin
import red.man10.realestate.util.MySQLManager
import red.man10.realestate.util.Utility
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Region {

    companion object{

        val regionData = ConcurrentHashMap<Int, Region>()
        val gson = Gson()

        fun formatStatus(status:String):String{
            return when(status){
                "Protected" -> "保護されています"
                "OnSale" -> "販売中"
                "Lock" -> "ロック"
                "Free" -> "フリー"
                else -> status
            }
        }

        fun formatSpan(span:Int):String{
            return when(span){
                0 -> "1ヶ月"
                1 -> "1週間"
                2 -> "毎日"
                else -> "不明"
            }
        }

        fun asyncLoad(){

            Plugin.async.execute {
                regionData.clear()

                val sql = MySQLManager(Plugin.plugin,"Man10RealEstate Loading")

                val rs = sql.query("SELECT * FROM region WHERE server='${Plugin.serverName}';")?:return@execute

                while (rs.next()){

                    val id = rs.getInt("id")

                    val rg = Region()

                    rg.id = id
                    rg.name = rs.getString("name")
                    rg.world = rs.getString("world")
                    rg.server = Plugin.serverName
                    if (rs.getString("owner_uuid") == null || rs.getString("owner_uuid") == "null"){
                        rg.ownerUUID = null
                    }else{
                        rg.ownerUUID = UUID.fromString(rs.getString("owner_uuid"))
                    }
                    rg.status = rs.getString("status")
                    rg.taxStatus = rs.getString("tax_status")
                    rg.price = rs.getDouble("price")

                    rg.span = rs.getInt("span")

                    rg.startPosition = Triple(
                        rs.getInt("sx"),
                        rs.getInt("sy"),
                        rs.getInt("sz")
                    )
                    rg.endPosition = Triple(
                        rs.getInt("ex"),
                        rs.getInt("ey"),
                        rs.getInt("ez")
                    )

                    rg.teleport = Location(
                        Bukkit.getWorld(rg.world),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                    )

                    rg.data = gson.fromJson(rs.getString("data"),RegionData::class.java)

                    regionData[id] = rg

                    if (Bukkit.getWorld(rg.world) == null){
                        rg.asyncDelete()
                        Bukkit.getLogger().warning("id:${id}は存在しない土地だったので、削除しました!")
                    }
                }
                rs.close()
                sql.close()
            }
        }

        fun create(pos1:Triple<Int,Int,Int>,pos2:Triple<Int,Int,Int>,name:String,price:Double,tp:Location):Int{

            val data = RegionData(false,0.0,0.0)

            val query = "INSERT INTO region " +
                    "(server, world, name, status, price, " +
                    "x, y, z, pitch, yaw, " +
                    "sx, sy, sz, ex, ey, ez, data) " +
                    "VALUES(" +
                    "'${Plugin.serverName}', " +
                    "'${tp.world.name}', " +
                    "'${MySQLManager.escapeStringForMySQL(name)}', " +
                    "'OnSale', " +
                    "$price, " +
                    "${tp.x}, " +
                    "${tp.y}, " +
                    "${tp.z}, " +
                    "${tp.pitch}, " +
                    "${tp.yaw}, " +
                    "${pos1.first}, " +
                    "${pos1.second}, " +
                    "${pos1.third}, " +
                    "${pos2.first}, " +
                    "${pos2.second}, " +
                    "${pos2.third}, " +
                    "${gson.toJson(data)}); "

            val mysql = MySQLManager(Plugin.plugin,"Man10RealEstate CreateRegion")

            mysql.execute(query)

            val rs = mysql.query("SELECT t.* FROM region t ORDER BY id DESC LIMIT 1;")?:return -1
            rs.next()
            val id = rs.getInt("id")

            rs.close()
            mysql.close()

            val rg = Region()

            rg.name = name

            rg.startPosition = pos1
            rg.endPosition = pos2
            rg.teleport = tp

            rg.world = tp.world.name
            rg.server = Plugin.serverName

            rg.price = price

            rg.data = data

            regionData[id] = rg

            return id

        }
    }

    var id = 0
    var name = "RegionName"
    var ownerUUID : UUID? = null
    var ownerName : String? = if (ownerUUID == null)null else Bukkit.getOfflinePlayer(ownerUUID!!).name
    var status = "OnSale" //Danger,Free,OnSale,Protected
    var taxStatus = "SUCCESS" //SUCCESS,WARN

    var world = "builder"
    var server = "server"

    var startPosition: Triple<Int,Int,Int> = Triple(0,0,0)
    var endPosition: Triple<Int,Int,Int> = Triple(0,0,0)
    lateinit var teleport : Location

    var price : Double = 0.0
    var span = 0 //0:month 1:week 2:day

    lateinit var data : RegionData


    fun asyncSave(){

        MySQLManager.mysqlQueue.add("UPDATE region SET " +
                "owner_uuid = '${ownerUUID}', " +
                "owner_name = '${if (ownerUUID == null)null
                else{Bukkit.getOfflinePlayer(ownerUUID!!).name}}', " +
                "x = ${teleport.x}," +
                "y = ${teleport.y}, " +
                "z = ${teleport.z}, " +
                "pitch = ${teleport.pitch}, " +
                "yaw = ${teleport.yaw}, " +
                "sx = ${startPosition.first}, " +
                "sy = ${startPosition.second}, " +
                "sz = ${startPosition.third}, " +
                "ex = ${endPosition.first}, " +
                "ey = ${endPosition.second}, " +
                "ez = ${endPosition.third}, " +
                "status = '${status}', " +
                "tax_status = '${taxStatus}'," +
                "price = ${price}, " +
                "profit = 0, " +
                "span = ${span}," +
                "data = ${gson.toJson(data)} " +
                "WHERE id = $id")

    }

    @Synchronized
    fun buy(p: Player){

        val city = City.where(teleport)
        val score = ScoreDatabase.getScore(p.uniqueId)

        if (city == null){
            Utility.sendMessage(p,"§c§l都市の外に土地があります。運営に報告してください")
            return
        }

        if (status != "OnSale"){
            Utility.sendMessage(p, "§c§lこの土地は販売されていません！")
            return
        }

        if (p.uniqueId == ownerUUID){
            Utility.sendMessage(p, "§c§lあなたはこの土地のオーナーです！")
            return
        }

        if (city.buyScore > score){
            Utility.sendMessage(p, "§c§lあなたにはこの土地を買うためのスコアが足りません！")
            return
        }

        if (!Plugin.vault.withdraw(p.uniqueId,price)){
            Utility.sendMessage(p, "§c§l電子マネーが足りません！")
            return
        }

        if (ownerUUID != null){
            Plugin.bank.deposit(ownerUUID!!,price,"Man10RealEstate RegionProfit","土地の売上")
        }

        ownerUUID = p.uniqueId
        status = "Protected"

        Utility.sendMessage(p, "§a§l土地の購入成功！")
    }

    fun init(status: String = "OnSale"){
        val city = City.where(teleport)?:return
        ownerUUID = null
        price = city.defaultPrice
        this.status = status
        User.asyncDeleteFromRegion(id)
    }

    fun asyncDelete(){
        MySQLManager.mysqlQueue.add("DELETE FROM `region` WHERE  `id`=$id;")
        User.asyncDeleteFromRegion(id)
    }

    //賃料の支払い
    fun payRent(){
        User.userMap.filterKeys { pair -> pair.second == id }.values.forEach { it.payRent() }
    }

    data class RegionData(
        var denyTeleport : Boolean,
        var defaultPrice : Double,
        var tax : Double
    )
}