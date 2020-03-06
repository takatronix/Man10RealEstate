package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin

class RegionDatabase(private val pl: Plugin) {


    //TODO:Man10CommonLibsを参照できるようにする(できない原因不明)

    /**
     * リージョンの登録
     *
     * @param owner リージョンのオーナー
     * @param name リージョン名
     * @param status ステータス(Free,Protected,Lock)
     *
     */
    fun registerRegion(owner:Player,name:String,status:String,server:String,world:String
                       ,start:Triple<Double,Double,Double>,end:Triple<Double,Double,Double>){

        val sql = "INSERT INTO `region` " +
                "(`server`, `world`, `owner_uuid`, `owner_name`, `x`, `y`, `z`, `pitch`, `yaw`, `name`, `status`," +
                "`sx`, `sy`, `sz`, `ex`, `ey`, `ez`) " +
                "VALUES ('${server}', '$world'," +
                " '${owner.uniqueId}', '${owner.name}','$name', '$status', " +
                "`${start.first}`, `${start.second}`, `${start.third}`, " +
                "`${end.first}`, `${end.second}`, `${end.third}`);"

        val mysql = MySQLManager(pl,"man10estate")

        mysql.execute(sql)

        val data = RegionData()

        data.name = name
        data.owner = owner
        data.startCoordinate = start
        data.endCoordinate = end
        data.status = status

        pl.regionData[pl.regionData.size] = data

    }

    /**
     * リージョンの座標を変更
     * @param id リージョンid
     * @param start Triple<sx,sy,sz>
     * @param end Triple<ex,ey,ez>
     */
    fun setRegionCoordinate(id:Int,start:Triple<Double,Double,Double>,end:Triple<Double,Double,Double>){

        val data = pl.regionData[id]?:return

        data.startCoordinate = start
        data.endCoordinate = end

        pl.regionData[id] = data

        val sql = "UPDATE `region` SET `sx`='${start.first}', `sy`='${start.second}', `sz`='${start.third}'," +
                " `ex`='${end.first}', `ey`='${end.second}', `ez`='${end.third}' WHERE  `id`='$id';"


        val mysql = MySQLManager(pl,"man10estate")

        mysql.execute(sql)
    }

    /**
     * テレポート地点の変更
     * @param id リージョンid
     * @param tp List<x,y,z,pitch,yay>
     *
     */
    fun setRegionTeleport(id:Int,tp:List<String>){

        val data = pl.regionData[id]?:return
        data.teleport = mutableListOf(tp[0].toDouble(),tp[1].toDouble(),tp[2].toDouble(),tp[3].toDouble(),tp[4].toDouble())
        pl.regionData[id] = data

        val sql = "UPDATE `region` SET `x`=${tp[0]},`y`=${tp[1]},`z`=${tp[2]}," +
                "`pitch`=${tp[3]},`yaw`=${tp[4]} WHERE `id`='$id';"

        val mysql = MySQLManager(pl,"man10estate")

        mysql.execute(sql)

    }

    //リージョンの値段を変更
    fun setPrice(id:Int,price:Double){

        val data = pl.regionData[id]?:return
        data.price = price
        pl.regionData[id] = data

        val sql =  "UPDATE `region` SET `price`='$price' WHERE  `id`='$id';"

        val mysql = MySQLManager(pl,"man10estate")

        mysql.execute(sql)

    }

    //ステータスの変更
    fun setRegionStatus(id:Int,status: String){

        val data = pl.regionData[id]?:return
        data.status = status
        pl.regionData[id] = data

        val sql =  "UPDATE `region` SET `status`='$status' WHERE  `id`='$id';"

        val mysql = MySQLManager(pl,"man10estate")

        mysql.execute(sql)

    }

    //オーナーの変更
    fun setRegionOwner(id:Int,owner: Player){

        val data = pl.regionData[id]?:return
        data.owner = owner
        pl.regionData[id] = data

        val sql = "UPDATE `region` SET `owner_uuid`='${owner.uniqueId}', `owner_name='${owner.name}' WHERE `id`='$id';"

        val mysql = MySQLManager(pl,"man10estate")

        mysql.execute(sql)

    }

    //リージョンの削除
    fun deleteRegion(id:Int){
        pl.regionData.remove(id)

        val sql = "DELETE FROM `region` WHERE  `id`=$id;"

        val mysql = MySQLManager(pl,"man10estate")

        mysql.execute(sql)

    }

    ////////////////////////////////////
    //鯖起動時にリージョンのデータを読み込む
    ////////////////////////////////////
    fun loadRegion(){

        pl.regionData.clear()

        val sql = "SELECT * FROM `region`;"

        val mysql = MySQLManager(pl,"man10estate")

        val rs = mysql.query(sql)?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = RegionData()

            data.name = rs.getString("name")
            data.world = rs.getString("world")
            data.server = rs.getString("server")
            data.owner = Bukkit.getPlayer(rs.getString("owner_uuid"))
            data.status = rs.getString("status")
            data.price = rs.getDouble("price")

            data.teleport = mutableListOf(
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getDouble("pitch"),
                    rs.getDouble("yay")
            )
            data.startCoordinate = Triple(
                    rs.getDouble("sx"),
                    rs.getDouble("sy"),
                    rs.getDouble("sz")
            )
            data.endCoordinate = Triple(
                    rs.getDouble("ex"),
                    rs.getDouble("ey"),
                    rs.getDouble("ez")
            )

            pl.regionData[id] = data
        }
        rs.close()
        mysql.close()
    }


    ///////////////////////////////
    //リージョンのデータをまとめたclass
    ///////////////////////////////
    class RegionData{

        var name = "RegionName"
        var owner : Player? = null
        var status = "OnSale"

        var world = "world"
        var server = "server"

        var startCoordinate: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        var endCoordinate: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        var teleport = mutableListOf<Double>()

        var price : Double = 0.0

    }
}