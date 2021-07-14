package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.MySQLManager.Companion.mysqlQueue
import red.man10.realestate.Plugin.Companion.bank
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Plugin.Companion.serverName
import red.man10.realestate.Plugin.Companion.vault
import red.man10.realestate.Utility.sendMessage
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Region {

    //idとリージョンデータの辞書
    private val regionData = ConcurrentHashMap<Int,RegionData>()

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

        User.removeAll(id)
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
                "'$serverName', " +
                "'${tp.world.name}', " +
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

        val mysql = MySQLManager(plugin,"Man10RealEstate CreateRegion")

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
        data.server = serverName

        data.price = price

        set(id,data)

        val cID = City.where(data.teleport)
        if (cID != -1){
            City.updateRegion(cID)
        }

        return id

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
    fun setOwner(id:Int, p: Player?){
        val data = get(id)?:return

        if (data.ownerUUID !=null){
            val old = Bukkit.getPlayer(data.ownerUUID!!)

            if (old !=null){
                val list = User.ownerList[old]!!
                list.remove(id)
                User.ownerList[old] = list
            }
        }

        if (p != null){
            data.ownerUUID = p.uniqueId
            val list = User.ownerList[p]?: mutableListOf()
            list.add(id)
            User.ownerList[p] = list
        }else{
            data.ownerUUID = p
        }
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
     *
     */
//    fun where(loc:Location): Int {
//        for (rg in regionData){
//            if (Utility.isWithinRange(loc,rg.value.startPosition,rg.value.endPosition,rg.value.world)){
//                return rg.key
//            }
//        }
//        return -1
//    }

    fun setTeleport(id:Int, tp:Location){

        val data = regionData[id]?:return

        if (data.teleport.world.name != tp.world.name)return

        data.teleport = tp.clone()
        regionData[id] = data

        save(id,data)

    }

    /**
     * リージョンのデータをdbに保存する
     */
    fun save(id:Int,data:RegionData){

        mysqlQueue.add("UPDATE region SET " +
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
                "status = '${data.status}', " +
                "price = ${data.price}, " +
                "profit = 0, " +
                "span = ${data.span}," +
                "remit_tax = ${if (data.isRemitTax) 1 else 0} " +
                "WHERE id = $id")

    }

    /**
     * 全リージョンデータを読み込み
     */
    fun load(){
        regionData.clear()

        val sql = MySQLManager(plugin,"Man10RealEstate Loading")

        val rs = sql.query("SELECT * FROM region WHERE server='$serverName';")?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = RegionData()

            data.name = rs.getString("name")
            data.world = rs.getString("world")
            data.server = serverName
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

            data.isRemitTax = rs.getInt("remit_tax") == 1

            regionData[id] = data

            if (Bukkit.getWorld(data.world) == null){
                delete(id)
                Bukkit.getLogger().info("存在しない土地だったので、削除しました!")
            }

        }

        rs.close()
        sql.close()
    }

    @Synchronized
    fun buy(p:Player,id:Int){

        val data = get(id)

        if (data == null){
            sendMessage(p,"§c§l存在しない土地です！")
            return
        }

        if (p.uniqueId == data.ownerUUID){
            sendMessage(p,"§c§lあなたはこの土地のオーナーです！")
            return
        }

        if (vault.getBalance(p.uniqueId) < data.price){
            sendMessage(p,"§c§l所持金が足りません！")
            return
        }

        if (!City.canLive(id,p,true)){
            sendMessage(p,"あなたにはこの土地を買うためのスコアが足りません！")
            return
        }

        vault.withdraw(p.uniqueId,data.price)

        if (data.ownerUUID != null){
            bank.deposit(data.ownerUUID!!,data.price,"Man10RealEstate RegionProfit")
        }

        setOwner(id,p)
        setStatus(id,"Protected")

        sendMessage(p,"§a§l土地の購入成功！土地の保護がされました！")

    }

    /**
     * オーナー名を取得
     */
    fun getOwner(data:RegionData):String{

        val uuid = data.ownerUUID

        return if (uuid == null){
            "Admin"
        }else{
            Bukkit.getOfflinePlayer(uuid).name!!
        }
    }

    fun initRegion(id: Int, price: Double){

        setOwner(id,null)
        setPrice(id,price)
        setStatus(id,"OnSale")
        User.removeAll(id)

    }

    //住人の数を数える
    fun getUsers(id:Int):Int{
        val mysql = MySQLManager(plugin,"mre")

        val rs = mysql.query("select COUNT(region_id) from region_user where region_id=$id;")?:return 0
        rs.next()
        val users = rs.getInt(1)
        rs.close()
        mysql.close()
        return users
    }

    class RegionData{

        var name = "RegionName"
        var ownerUUID : UUID? = null
        var status = "OnSale" //Danger,Free,OnSale,Protected

        var world = "builder"
        var server = "server"

        var startPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        var endPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
        lateinit var teleport : Location

        var price : Double = 0.0

        var span = 0 //0:month 1:week 2:day

        var isRemitTax = false
    }

}