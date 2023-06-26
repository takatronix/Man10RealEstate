package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.Location
import red.man10.realestate.Plugin
import red.man10.realestate.util.MySQLManager
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




}