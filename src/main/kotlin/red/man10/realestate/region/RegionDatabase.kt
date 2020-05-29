package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.realestate.Plugin.Companion.mysqlQueue
import red.man10.realestate.Plugin.Companion.ownerData
import red.man10.realestate.Plugin.Companion.regionData
import red.man10.realestate.Plugin.Companion.sendMessage
import red.man10.realestate.Plugin.Companion.worldRegion
import red.man10.realestate.MySQLManager
import red.man10.realestate.Plugin
import java.util.*

class RegionDatabase(private val pl: Plugin) {


    /////////////////////////////////
    //リージョンデータを新規で登録
    /////////////////////////////////
    fun registerRegion(data:RegionData):Int{

        val sql = "INSERT INTO `region` " +
                "(`server`, `world`, `owner_uuid`, `owner_name`, `name`, `status`, `price`, " +
                "`x`, `y`, `z`, `pitch`, `yaw`, " +
                "`sx`, `sy`, `sz`, `ex`, `ey`, `ez`) " +
                "VALUES (" +
                "'${data.server}', " +
                "'${data.world}', " +
                "'${null}', " +
                "'${null}' ," +
                "'${data.name}', " +
                "'${data.status}', " +
                "'${data.price}', " +
                "'${data.teleport[0]}', "+
                "'${data.teleport[1]}', "+
                "'${data.teleport[2]}', "+
                "'${data.teleport[3]}', "+
                "'${data.teleport[4]}', "+
                "'${data.startPosition.first}', " +
                "'${data.startPosition.second}', " +
                "'${data.startPosition.third}', " +
                "'${data.endPosition.first}', " +
                "'${data.endPosition.second}', " +
                "'${data.endPosition.third}');"

        val mysql = MySQLManager(pl,"registerRegion")

        mysql.execute(sql)

        val rs = mysql.query("SELECT t.* FROM region t ORDER BY id DESC LIMIT 501")?:return -1
        rs.next()

        val id = rs.getInt(1)

        rs.close()
        mysql.close()

        regionData[id] = data

        val list = worldRegion[data.world]?: mutableListOf()
        list.add(id)
        worldRegion[data.world] = list

        return id
    }


    /**
     * テレポート地点の変更
     * @param id リージョンid
     * @param tp List<x,y,z,pitch,yaw>
     *
     */
    fun setRegionTeleport(id:Int,tp:MutableList<Double>){

        val data = regionData[id]?:return
        data.teleport = tp
        regionData[id] = data

        saveRegion(id)

    }

    //リージョンの値段を変更
    fun setPrice(id:Int,price:Double){

        val data = regionData[id]?:return
        data.price = price
        regionData[id] = data

        saveRegion(id)

    }

    //ステータスの変更
    fun setRegionStatus(id:Int,status: String){

        val data = regionData[id]?:return
        data.status = status
        regionData[id] = data

        saveRegion(id)
    }

    //オーナーの変更
    fun setRegionOwner(id:Int,owner: Player){

        val data = regionData[id]?:return

        //旧オーナーがいた場合、リストから削除
        if (data.owner_uuid!=null){

            val p = Bukkit.getOfflinePlayer(data.owner_uuid!!)
            if (p.isOnline){
                val list = ownerData[p.player!!]!!
                list.remove(id)
                ownerData[p.player!!] = list
            }
        }

        //新オーナー追加
        val list = ownerData[owner]?: mutableListOf()
        list.add(id)
        ownerData[owner] = list

        //リージョンのデータに追加
        data.owner_uuid = owner.uniqueId
        regionData[id] = data

        saveRegion(id)

    }

    //スパンの変更
    fun setSpan(id:Int,span:Int){
        val data = regionData[id]?:return
        data.span = span
        regionData[id] = data

        saveRegion(id)
    }

    //土地の購入
    fun buy(id: Int,user:Player){

        val data = regionData[id]?:when{
            else -> {
                sendMessage(user,"§3§l購入失敗！ 存在しないidです！")
                return
            }
        }

        if (user.uniqueId == data.owner_uuid){
            sendMessage(user,"§3§lあなたはこのリージョンのオーナーです！")
            return
        }

        if (data.status != "OnSale"){
            sendMessage(user,"§3§lこのリージョンは販売中ではありません")
            return
        }

        if (pl.vault.getBalance(user.uniqueId) < data.price){
            sendMessage(user,"§3§l所持金が足りません！")
            return
        }

        //旧オーナーに所持金を追加
        pl.vault.withdraw(user.uniqueId,data.price)

        if (data.owner_uuid != null){
            Plugin.offlineBank.deposit(data.owner_uuid!!,data.price,"RealEstate RegionProfit")
        }

        setRegionOwner(id,user)

        setRegionStatus(id,"Protected")

        sendMessage(user,"§a§l購入完了、土地の保護がされました！")
    }

    //リージョンの削除
    fun deleteRegion(id:Int){
        regionData.remove(id)

        mysqlQueue.add("DELETE FROM `region` WHERE  `id`=$id;")
        mysqlQueue.add("DELETE FROM `region_user` WHERE `region_id`=$id;")
    }

    ////////////////////////////////////
    //鯖起動時にリージョンのデータを読み込む
    ////////////////////////////////////
    fun loadRegion(){

        regionData.clear()
        worldRegion.clear()

        val sql = "SELECT * FROM `region`;"

        val mysql = MySQLManager(pl,"man10estate")

        val rs = mysql.query(sql)?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = RegionData()

            data.name = rs.getString("name")
            data.world = rs.getString("world")
            data.server = rs.getString("server")
            if (rs.getString("owner_uuid") == null || rs.getString("owner_uuid") == "null"){
                data.owner_uuid = null
            }else{
                data.owner_uuid = UUID.fromString(rs.getString("owner_uuid"))
            }
            data.status = rs.getString("status")
            data.price = rs.getDouble("price")

            data.span = rs.getInt("span")

            data.teleport = mutableListOf(
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getDouble("pitch"),
                    rs.getDouble("yaw")
            )
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

            regionData[id] = data
            val list = worldRegion[data.world]?: mutableListOf()
            list.add(id)
            worldRegion[data.world] = list
        }
        rs.close()
        mysql.close()
    }

    ////////////////////////////////////////
    //メソッドを呼び出した時点でのリージョンのデータを保存
    ////////////////////////////////////////
    fun saveRegion(id:Int):Boolean{

        val data = regionData[id]?:return false

        mysqlQueue.add("UPDATE region t SET " +
                "t.owner_uuid = '${data.owner_uuid}', " +
                "t.owner_name = '${if (data.owner_uuid == null)null
                else{Bukkit.getOfflinePlayer(data.owner_uuid!!).name}}', " +
                "t.x = ${data.teleport[0]}," +
                " t.y = ${data.teleport[1]}, " +
                "t.z = ${data.teleport[2]}, " +
                "t.pitch = ${data.teleport[3]}, " +
                "t.yaw = ${data.teleport[4]}, " +
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

        return true
    }

    //オーナーを取得
    fun getOwner(data:RegionData):String{

        val uuid = data.owner_uuid

        return if (uuid == null){
            "Admin"
        }else{
            Bukkit.getOfflinePlayer(uuid).name!!
        }
    }

    ///////////////////////////////
    //リージョンのデータをまとめたclass
    ///////////////////////////////
    class RegionData{

        var name = "RegionName"
        var owner_uuid : UUID? = UUID.randomUUID()
        var status = "OnSale"

        var world = "builder"
        var server = "server"

        var startPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        var endPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        var teleport = mutableListOf<Double>()

        var price : Double = 0.0

        var rent : Double = 0.0
        var span = 0 //0:month 1:week 2:day

    }
}