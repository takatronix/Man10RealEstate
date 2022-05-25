package red.man10.realestate.region

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.MySQLManager
import red.man10.realestate.MySQLManager.Companion.escapeStringForMySQL
import red.man10.realestate.MySQLManager.Companion.mysqlQueue
import red.man10.realestate.Plugin.Companion.bank
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Plugin.Companion.serverName
import red.man10.realestate.Plugin.Companion.vault
import red.man10.realestate.Utility.format
import red.man10.realestate.Utility.sendMessage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

object Region {

    //idとリージョンデータの辞書
    val regionData = ConcurrentHashMap<Int,RegionData>()

    fun get(id:Int):RegionData?{
        return regionData[id]
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
    fun create(pos1:Triple<Int,Int,Int>,pos2:Triple<Int,Int,Int>,name:String,price:Double,tp:Location):Int{

        val query = "INSERT INTO region " +
                "(server, world, name, status, price, " +
                "x, y, z, pitch, yaw, " +
                "sx, sy, sz, ex, ey, ez) " +
                "VALUES(" +
                "'$serverName', " +
                "'${tp.world.name}', " +
                "'${escapeStringForMySQL(name)}', " +
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

//        if (data.ownerUUID !=null){
//            val old = Bukkit.getPlayer(data.ownerUUID!!)
//
//            if (old !=null){
//                val list = User.ownerList[old]!!
//                list.remove(id)
//                User.ownerList[old] = list
//            }
//        }

        if (p != null){
            data.ownerUUID = p.uniqueId
//            val list = User.ownerList[p]?: mutableListOf()
//            list.add(id)
//            User.ownerList[p] = list
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
    private fun save(id:Int,data:RegionData){

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
                "status = '${escapeStringForMySQL(data.status)}', " +
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

            regionData[id] = data

            if (Bukkit.getWorld(data.world) == null){
                delete(id)
                Bukkit.getLogger().info("id:${id}は存在しない土地だったので、削除しました!")
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

        if (data.status != "OnSale"){
            sendMessage(p,"§4§lこの土地は販売されていません！")
            return
        }

        if (p.uniqueId == data.ownerUUID){
            sendMessage(p,"§c§lあなたはこの土地のオーナーです！")
            return
        }

        if (!City.setBuyScore(id,p)){
            sendMessage(p,"§c§lあなたにはこの土地を買うためのスコアが足りません！")
            return
        }

        if (vault.getBalance(p.uniqueId) < data.price){
            sendMessage(p,"§c§l電子マネーが足りません！")
            return
        }

        vault.withdraw(p.uniqueId,data.price)

        if (data.ownerUUID != null){
            bank.deposit(data.ownerUUID!!,data.price,"Man10RealEstate RegionProfit")
        }

        setOwner(id,p)
        setStatus(id,"Protected")

        sendMessage(p,"§a§l土地の購入成功！")

    }

    /**
     * オーナー名を取得
     */
    fun getOwner(data:RegionData):String{

        val uuid = data.ownerUUID

        return if (uuid == null){
            "サーバー運営"
        }else{
            Bukkit.getOfflinePlayer(uuid).name?:"不明なユーザー"
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

    fun showTaxAndRent(p:Player){
        val db = MySQLManager(plugin,"realestate")

        val rs1 = db.query("select id from region where owner_uuid='${p.name}' or owner_name='${p.name}';")?:return

        var total = 0
        var totalArea = 0
        var totalTax = 0.0

        while (rs1.next()){

            val id = rs1.getInt("id")

            val rg = get(id)!!

            val width = rg.startPosition.first.coerceAtLeast(rg.endPosition.first) - rg.startPosition.first.coerceAtMost(rg.endPosition.first)
            val height = rg.startPosition.third.coerceAtLeast(rg.endPosition.third) - rg.startPosition.third.coerceAtMost(rg.endPosition.third)

            totalArea += (width*height)
            totalTax += City.getTax(City.whereRegion(id),id)
            total++

        }

        rs1.close()
        db.close()

        if (total!=0){
            sendMessage(p,"§e§l土地の数:${total}")
            sendMessage(p,"§e§l土地の総面積:${totalArea}ブロック")
            sendMessage(p,"§e§l支払う税金:${format(totalTax)}")
        }

        val rs2 = db.query("select * from region_user where uuid='${p.uniqueId}' and is_rent=1 and rent>0;")?:return

        while (rs2.next()){

            val id = rs2.getInt("id")
            val rent = rs2.getDouble("rent")
            val paid = Calendar.getInstance()
            paid.time = rs2.getDate("paid_date")

            val rg = get(id)?:continue

            when(rg.span){
                0 ->{paid.add(Calendar.DAY_OF_YEAR,30)}
                1 ->{paid.add(Calendar.DAY_OF_YEAR,7)}
                2 ->{paid.add(Calendar.DAY_OF_YEAR,1)}
            }

            sendMessage(p,"§e§l=====================================================")

            sendMessage(p,"§e§lID:${id} 支払う賃料:${format(rent)} 支払日:${SimpleDateFormat("yyyy-MM-dd").format(paid.time)}")

        }

        rs2.close()
        db.close()

    }

    fun formatStatus(status:String):String{
        return when(status){
            "Protected" -> "保護されています"
            "OnSale" -> "販売中"
            "Lock" -> "ロック"
            "Free" -> "フリー"
            else -> status
        }
    }

    class RegionData{

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

}