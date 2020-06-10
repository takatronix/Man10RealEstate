package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.mysqlQueue
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

class Region(private val pl:Plugin) {

    //idとリージョンデータの辞書
    val regionData = ConcurrentHashMap<Int,RegionData>()

    fun get(id:Int):RegionData?{
        return regionData[id]
    }

    fun map(): Map<Int, RegionData> {
        return regionData.toMap()
    }

    fun set(id: Int,region: RegionData){
        regionData[id] = region
        save(id,region)
    }

    fun delete(id:Int){
        regionData.remove(id)

        mysqlQueue.add("DELETE FROM `region` WHERE  `id`=$id;")
        mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`=$id;")
    }


    /**
     * create new region
     *
     * @param pos1 start position
     * @param pos2 end position
     * @param name region name
     * @param price region price
     * @param tp teleportation position (x,y,z,pitch,yaw
     *
     * @return region id
     */
    fun create(pos1:Triple<Double,Double,Double>,pos2:Triple<Double,Double,Double>,name:String,price:Double,tp:Location):Int{

        val query = "INSERT INTO region " +
                "(server, world, name, status, price, " +
                "x, y, z, pitch, yaw, " +
                "sx, sy, sz, ex, ey, ez) " +
                "VALUES(" +
                "'${Bukkit.getServer().name}', " +
                "'${tp.world}', " +
                "'$name', " +
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

        val mysql = MySQLManager(pl,"Man10RealEstate CreateRegion")

        mysql.execute(query)

        val rs = mysql.query("SELECT t.* FROM region t ORDER BY id DESC LIMIT 1;")?:return -1
        rs.next()
        val id = rs.getInt("id")

        rs.close()
        mysql.close()

        val data = RegionData()

        data.name = name

        data.startPosition = pos1
        data.endPosition = pos2
        data.teleport = tp

        data.world = tp.world.name
        data.server = Bukkit.getServer().name

        data.price = price

        set(id,data)

        return id

    }

    /**
     * set teleport location
     */
    fun setTeleport(id:Int,tp:Location){
        val data = get(id)?:return
        data.teleport = tp
        set(id,data)
    }

    /**
     * set status
     */
    fun setStatus(id:Int,status:String){
        val data = get(id)?:return
        data.status = status
        set(id,data)
    }

    /**
     * set owner
     */
    fun setOwner(id:Int,p:Player){
        val data = get(id)?:return
        data.ownerUUID = p.uniqueId
        set(id,data)
    }

    /**
     * set price
     */
    fun setPrice(id: Int,price:Double){
        val data = get(id)?:return
        data.price = price
        set(id,data)
    }

    /**
     * set span
     * 0:month 1:week 2:day
     */
    fun setSpan(id:Int,span:Int){
        val data = get(id)?:return
        data.span = span
        set(id,data)
    }





    /**
     * リージョンのデータをdbに保存する
     */
    fun save(id:Int,data:RegionData){

        mysqlQueue.add("UPDATE region t SET " +
                "t.owner_uuid = '${data.ownerUUID}', " +
                "t.owner_name = '${if (data.ownerUUID == null)null
                else{Bukkit.getOfflinePlayer(data.ownerUUID!!).name}}', " +
                "t.x = ${data.teleport.x}," +
                " t.y = ${data.teleport.y}, " +
                "t.z = ${data.teleport.z}, " +
                "t.pitch = ${data.teleport.pitch}, " +
                "t.yaw = ${data.teleport.yaw}, " +
                "t.sx = ${data.startPosition.first}, " +
                "t.sy = ${data.startPosition.second}, " +
                "t.sz = ${data.startPosition.third}, " +
                "t.ex = ${data.endPosition.first}, " +
                "t.ey = ${data.endPosition.second}, " +
                "t.ez = ${data.endPosition.third}, " +
                "t.status = '${data.status}', " +
                "t.price = ${data.price}, " +
                "t.profit = 0, " +
                "t.span = ${data.span} " +
                "WHERE t.id = $id")

    }

    /**
     * 全リージョンデータを読み込み
     */
    fun load(){
        regionData.clear()

        val sql = MySQLManager(pl,"Man10RealEstate Loading")

        val rs = sql.query("SELECT * FROM region;")?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = RegionData()

            data.name = rs.getString("name")
            data.world = rs.getString("world")
            data.server = rs.getString("server")
            if (rs.getString("owner_uuid") == null || rs.getString("owner_uuid") == "null"){
                data.ownerUUID = null
            }else{
                data.ownerUUID = UUID.fromString(rs.getString("owner_uuid"))
            }
            data.status = rs.getString("status")
            data.price = rs.getDouble("price")

            data.span = rs.getInt("span")

            data.startPosition = Triple(
                    rs.getDouble("sx"),
                    rs.getDouble("sy"),
                    rs.getDouble("sz")
            )
            data.endPosition = Triple(
                    rs.getDouble("ex"),
                    rs.getDouble("ey"),
                    rs.getDouble("ez")
            )

            data.teleport = Location(
                Bukkit.getWorld(data.world),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch")
            )

            regionData[id] = data

        }

        rs.close()
        sql.close()
    }


    class RegionData{

        var name = "RegionName"
        var ownerUUID : UUID? = null
        var status = "OnSale"

        var world = "builder"
        var server = "server"

        var startPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        var endPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        lateinit var teleport : Location

        var price : Double = 0.0

        var span = 0 //0:month 1:week 2:day

    }

}