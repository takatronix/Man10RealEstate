package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.Location
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import red.man10.realestate.Plugin.Companion.mysqlQueue
import red.man10.realestate.Plugin.Companion.offlineBank
import red.man10.realestate.Plugin.Companion.region
import red.man10.realestate.Plugin.Companion.user
import red.man10.realestate.Utility
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class City(private val pl:Plugin) {

    val cityData = ConcurrentHashMap<Int,CityData>()

    fun get(id:Int):CityData?{
        return cityData[id]
    }

    fun set(id:Int,data:CityData){
        cityData[id] = data
        save(id,data)
    }

    fun map():ConcurrentHashMap<Int,CityData>{
        return cityData
    }

    fun delete(id: Int){

        cityData.remove(id)

        mysqlQueue.add("DELETE FROM `city` WHERE  `id`=$id;")

    }


    /**
     * 新規都市作成
     */
    fun create(pos1:Triple<Double,Double,Double>,pos2:Triple<Double,Double,Double>,name:String,tax:Double,tp:Location):Int{

        val sql = "INSERT INTO city " +
                "(name, server, world, x, y, z, pitch, yaw, sx, sy, sz, ex, ey, ez, tax) " +
                "VALUE(" +
                "'$name', " +
                "'${Bukkit.getServer().name}', " +
                "'${tp.world.name}', " +
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
                "$tax);"

        val mysql = MySQLManager(pl,"Man10RealEstate City")

        mysql.execute(sql)

        val rs = mysql.query("SELECT t.* FROM city t ORDER BY id DESC LIMIT 1;")?:return -1
        rs.next()
        val id = rs.getInt("id")

        rs.close()
        mysql.close()

        val data = CityData()

        data.server = Bukkit.getServer().name
        data.world = tp.world.name

        data.startPosition = pos1
        data.endPosition = pos2
        data.teleport = tp

        data.tax = tax

        data.name = name


        cityData[id] = data

        updateRegion(id)

        return id

    }

    /**
     * 読み込み
     */
    fun load(){
        cityData.clear()

        val sql = MySQLManager(pl,"Man10RealEstate Loading")

        val rs = sql.query("SELECT * FROM city;")?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = CityData()

            data.name = rs.getString("name")
            data.world = rs.getString("world")
            data.server = rs.getString("server")

            data.tax = rs.getDouble("tax")

            data.maxUser = rs.getInt("max_user")

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

            cityData[id] = data

            updateRegion(id)
        }

        rs.close()
        sql.close()

    }

    /**
     * 都市内のリージョンを探す
     */
    fun updateRegion(id:Int){

        val data = get(id)?:return

        val pos1 = data.startPosition
        val pos2 = data.endPosition
        val world = data.world

        val list = mutableListOf<Int>()

        for (rg in region.map()){
            if (Utility.isWithinRange(rg.value.teleport,pos1,pos2,world)){
                list.add(rg.key)
            }
        }

        data.regionList = list

        cityData[id] = data

    }

    /**
     * 現在地点がどの都市か
     *
     * @return 指定地点の都市id(存在しなかったら -1　を返す)
     */
    fun where(loc:Location):Int{
        for (city in cityData){
            if (Utility.isWithinRange(loc,city.value.startPosition,city.value.endPosition,city.value.world)){
                return city.key
            }
        }
        return  -1
    }


    /**
     * 税金の変更
     */
    fun setTax(id:Int, tax:Double){
        val data = get(id)?:return
        data.tax = tax
        set(id,data)
    }

    /**
     * 現在のデータを保存する
     */
    fun save(id:Int,data:CityData){

        mysqlQueue.add("UPDATE city t SET " +
                "t.name = '${data.name}', " +
                "t.server = '${data.server}', " +
                "t.world = '${data.world}', " +
                "t.x = ${data.teleport.x}, " +
                "t.y = ${data.teleport.y}, " +
                "t.z = ${data.teleport.z}, " +
                "t.pitch = ${data.teleport.pitch}, " +
                "t.yaw = ${data.teleport.yaw}, " +
                "t.sx = ${data.startPosition.first}, " +
                "t.sy = ${data.startPosition.second}, " +
                "t.sz = ${data.startPosition.third}, " +
                "t.ex = ${data.endPosition.first}, " +
                "t.ey = ${data.endPosition.second}, " +
                "t.ez = ${data.endPosition.third}, " +
                "t.tax = ${data.tax}," +
                "t.max_user = ${data.maxUser} WHERE t.id = $id")

    }

    /**
     * 税金を支払う
     * @param p プレイヤーのuuid
     * @param id リージョンのid
     */
    fun payingTax(p:UUID,id:Int):Boolean{

        val rg = region.get(id)?:return false
        val cityID = where(rg.teleport)

        if (cityID == -1)return false

        val city = get(cityID)?:return false

        if (city.tax == 0.0)return false

        Bukkit.getLogger().info("${Bukkit.getOfflinePlayer(p).name} bal:${offlineBank.getBalance(p)}")

        //支払えなかった場合(リージョンのオーナーがAdminに、住人は全退去)
        if (!offlineBank.withdraw(p,getTax(cityID,id),"Man10RealEstate Tax")){

            region.initRegion(id)

            return false

        }

        Bukkit.getLogger().info("$id ${Bukkit.getOfflinePlayer(p).name} tax:${city.tax}")
        return true
    }


    /**
     * 土地の税金を計算する
     */
    fun getTax(cityID:Int,rgID:Int):Double{

        val city = get(cityID)?:return 0.0
        val rg = region.get(rgID)?:return 0.0

        val width = rg.startPosition.first.coerceAtLeast(rg.endPosition.first) - rg.startPosition.first.coerceAtMost(rg.endPosition.first)
        val height = rg.startPosition.third.coerceAtLeast(rg.endPosition.third) - rg.startPosition.third.coerceAtMost(rg.endPosition.third)

        return width * height * city.tax

    }

    fun getMaxUser(cityID: Int):Int{
        return get(cityID)?.maxUser?:0
    }

    fun setMaxUser(cityID: Int,value:Int){
        val data = get(cityID)?:return
        data.maxUser = value
        set(cityID,data)
    }

    class CityData{

        var regionList = mutableListOf<Int>()

        var tax = 0.0

        var world = "builder"
        var server = "server"

        var name = "CityName"

        var maxUser = 100

        var startPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        var endPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        lateinit var teleport : Location

    }

}