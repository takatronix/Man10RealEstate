package red.man10.realestate.region

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

        val regionData = ConcurrentHashMap<Int, RegionOld.RegionData>()

        fun formatStatus(status:String):String{
            return when(status){
                "Protected" -> "保護されています"
                "OnSale" -> "販売中"
                "Lock" -> "ロック"
                "Free" -> "フリー"
                else -> status
            }
        }

        fun asyncLoad(){

            Plugin.async.execute {
                regionData.clear()

                val sql = MySQLManager(Plugin.plugin,"Man10RealEstate Loading")

                val rs = sql.query("SELECT * FROM region WHERE server='${Plugin.serverName}';")?:return@execute

                while (rs.next()){

                    val id = rs.getInt("id")

                    val data = RegionOld.RegionData()

                    data.name = rs.getString("name")
                    data.world = rs.getString("world")
                    data.server = Plugin.serverName
                    if (rs.getString("owner_uuid") == null || rs.getString("owner_uuid") == "null"){
                        data.ownerUUID = null
                    }else{
                        data.ownerUUID = UUID.fromString(rs.getString("owner_uuid"))
                    }
                    data.status = rs.getString("status")
                    data.price = rs.getDouble("price")

                    data.span = rs.getInt("span")

                    data.startPosition = Triple(
                        rs.getInt("sx"),
                        rs.getInt("sy"),
                        rs.getInt("sz")
                    )
                    data.endPosition = Triple(
                        rs.getInt("ex"),
                        rs.getInt("ey"),
                        rs.getInt("ez")
                    )

                    data.teleport = Location(
                        Bukkit.getWorld(data.world),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                    )

                    data.isRemitTax = rs.getInt("remit_tax") == 1

                    RegionOld.regionData[id] = data

                    if (Bukkit.getWorld(data.world) == null){
                        RegionOld.delete(id)
                        Bukkit.getLogger().info("id:${id}は存在しない土地だったので、削除しました!")
                    }
                }
                rs.close()
                sql.close()
            }
        }

        fun create(pos1:Triple<Int,Int,Int>,pos2:Triple<Int,Int,Int>,name:String,price:Double,tp:Location):Int{

            val query = "INSERT INTO region " +
                    "(server, world, name, status, price, " +
                    "x, y, z, pitch, yaw, " +
                    "sx, sy, sz, ex, ey, ez) " +
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
                    "${pos2.third}); "

            val mysql = MySQLManager(Plugin.plugin,"Man10RealEstate CreateRegion")

            mysql.execute(query)

            val rs = mysql.query("SELECT t.* FROM region t ORDER BY id DESC LIMIT 1;")?:return -1
            rs.next()
            val id = rs.getInt("id")

            rs.close()
            mysql.close()

            val data = RegionOld.RegionData()

            data.name = name

            data.startPosition = pos1
            data.endPosition = pos2
            data.teleport = tp

            data.world = tp.world.name
            data.server = Plugin.serverName

            data.price = price

            RegionOld.set(id, data)

            return id

        }
    }

    var name = "RegionName"
    var ownerUUID : UUID? = null
    var status = "OnSale" //Danger,Free,OnSale,Protected

    var world = "builder"
    var server = "server"

    var startPosition: Triple<Int,Int,Int> = Triple(0,0,0)
    var endPosition: Triple<Int,Int,Int> = Triple(0,0,0)
    lateinit var teleport : Location

    var price : Double = 0.0

    var span = 0 //0:month 1:week 2:day

    var isRemitTax = false

    fun asyncSave(id:Int,data: RegionOld.RegionData){

        MySQLManager.mysqlQueue.add("UPDATE region SET " +
                "owner_uuid = '${data.ownerUUID}', " +
                "owner_name = '${if (data.ownerUUID == null)null
                else{Bukkit.getOfflinePlayer(data.ownerUUID!!).name}}', " +
                "x = ${data.teleport.x}," +
                "y = ${data.teleport.y}, " +
                "z = ${data.teleport.z}, " +
                "pitch = ${data.teleport.pitch}, " +
                "yaw = ${data.teleport.yaw}, " +
                "sx = ${data.startPosition.first}, " +
                "sy = ${data.startPosition.second}, " +
                "sz = ${data.startPosition.third}, " +
                "ex = ${data.endPosition.first}, " +
                "ey = ${data.endPosition.second}, " +
                "ez = ${data.endPosition.third}, " +
                "status = '${MySQLManager.escapeStringForMySQL(data.status)}', " +
                "price = ${data.price}, " +
                "profit = 0, " +
                "span = ${data.span}," +
                "remit_tax = ${if (data.isRemitTax) 1 else 0} " +
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

    fun init(){
        val city = City.where(teleport)?:return
        ownerUUID = null
        price = city.defaultPrice
        status = "OnSale"
        //TODO:ユーザーデータ削除する
    }
}