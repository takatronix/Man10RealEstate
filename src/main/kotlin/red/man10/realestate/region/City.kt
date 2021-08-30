package red.man10.realestate.region

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.man10score.ScoreDatabase
import red.man10.realestate.Plugin.Companion.bank
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Plugin.Companion.serverName
import red.man10.realestate.Utility
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object City {

    val cityData = ConcurrentHashMap<String,CityData>()
    private val gson = Gson()
//    val mapper = ObjectMapper()

    fun get(id:String):CityData?{
        return cityData[id]
    }

    fun set(id:String,data:CityData){
        cityData[id] = data
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            save(id,data)
        })
    }

    fun delete(id: String){

        val data = get(id)?:return
        data.isLoad = false
        set(id,data)
        cityData.remove(id)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val file = File("${plugin.dataFolder.name}/$id.json")
                if (file.exists())file.delete()
            }catch (e:Exception){}
        })
    }


    /**
     * 新規都市作成
     */
    fun create(pos1:Triple<Double,Double,Double>,pos2:Triple<Double,Double,Double>,name:String,tax:Double,tp:Location):Boolean{

        val data = CityData()

        data.server = serverName
        data.world = tp.world.name

        data.setStart(pos1)
        data.setEnd(pos2)
//        data.teleport = tp

        data.tax = tax

        data.name = name

        val jsonStr = gson.toJson(data)
        try {

            val file = File("${plugin.dataFolder}",File.separator+"/$name.json")

            if (file.exists()){
                Bukkit.getLogger().info("すでにファイル")
                return false
            }

            file.createNewFile()

            val writer = FileWriter(file)

            writer.write(jsonStr)
            writer.close()

            cityData[name] = data

        }catch (e:IOException){
            Bukkit.getLogger().info(e.message)
            return false
        }

        return true
    }

    /**
     * 読み込み
     */
    fun load(){
        cityData.clear()

        val files = File(plugin.dataFolder,File.separator).listFiles()?.toMutableList()?:return

        for (file in files){

            if (!file.path.endsWith(".json") || file.isDirectory){
                Bukkit.getLogger().info("${file.name} はJsonファイルではありません")
                continue
            }

            try {

                val name = file.name.replace(".json","")

                val reader = FileReader(file)
                val jsonStr = reader.readText()

                reader.close()

                val data = gson.fromJson(jsonStr,CityData::class.java)
//                val data = mapper.readValue(jsonStr,CityData::class.java)


                if (!data.isLoad){
                    Bukkit.getLogger().info("unload city : $name")
                    continue
                }
                Bukkit.getLogger().info("load city : $name")

                cityData[name] = data

            }catch (e:IOException){
                Bukkit.getLogger().info(e.message)
            }catch (e:java.lang.Exception){
                Bukkit.getLogger().info("Error")
            }

        }

    }


    /**
     * 現在地点がどの都市か
     *
     * @return 指定地点の都市id(存在しなかったら null　を返す)
     */
    fun where(loc:Location):String?{

        for (city in cityData){
            if (Utility.isWithinRange(loc,city.value.getStart(),city.value.getEnd(),city.value.world,city.value.server)){
                return city.key
            }
        }

        return null
    }


    /**
     * 税金の変更
     */
    fun setTax(id:String, tax:Double){
        val data = get(id)?:return
        data.tax = tax
        set(id,data)
    }


    fun liveScore(regionId:Int, p:Player):Boolean{

        val data = Region.get(regionId)?:return false

        val id = where(data.teleport) ?: return false

        val city = get(id)?:return false

        return city.liveScore<=ScoreDatabase.getScore(p.uniqueId)
    }

    fun buyScore(regionId:Int,p:Player):Boolean{

        val data = Region.get(regionId)?:return false

        val id = where(data.teleport)?:return false

        val city = get(id)?:return false

        return city.buyScore<=ScoreDatabase.getScore(p.uniqueId)
    }

    /**
     * 住むのに必要なスコアの変更
     */
    fun setLiveScore(id:String, score:Int){
        val data = get(id)?:return
        data.liveScore = score
        set(id,data)
    }

    /**
     * 買うのに必要なスコアの変更
     */
    fun setBuyScore(id:String, score:Int){
        val data = get(id)?:return
        data.buyScore = score
        set(id,data)
    }
    /**
     * 現在のデータを保存する
     */
    private fun save(id:String,data:CityData){

        try {
            val file = File("${plugin.dataFolder}/${id}.json")

            if (file.exists()){
                file.delete()
            }else{
                Bukkit.getLogger().info("存在しない都市")
                return
            }

            file.createNewFile()
//            val jsonStr = mapper.writeValueAsString(data)
            val jsonStr = gson.toJson(data)
            val writer = FileWriter(file)

            writer.write(jsonStr)
            writer.close()
        }catch (e:IOException){
            Bukkit.getLogger().info(e.message)
            Bukkit.getLogger().info(e.stackTraceToString())
        }
    }

    /**
     * 税金を支払う
     * @param p プレイヤーのuuid
     * @param id リージョンのid
     */
    fun payingTax(p:UUID,id:Int):Boolean{

        val rg = Region.get(id)?:return false
        val cityID = where(rg.teleport)?:return false

        if (rg.isRemitTax)return false

        val city = get(cityID)?:return false

        if (city.tax == 0.0)return false

        //支払えなかった場合(リージョンのオーナーがAdminに、住人は全退去)
        if (!bank.withdraw(p,getTax(cityID,id),"Man10RealEstate Tax")){

            if (city.defaultPrice == 0.0){
                Region.setStatus(id,"Lock")

            }else{
                Region.initRegion(id,city.defaultPrice)
            }

//            Region.initRegion(id,10000000.0)

            return false

        }
        return true
    }


    /**
     * 土地の税金を計算する
     */
    fun getTax(cityID:String,rgID:Int):Double{

        val city = get(cityID)?:return 0.0
        val rg = Region.get(rgID)?:return 0.0

        if (rg.isRemitTax)return 0.0

        val width = rg.startPosition.first.coerceAtLeast(rg.endPosition.first) - rg.startPosition.first.coerceAtMost(rg.endPosition.first)
        val height = rg.startPosition.third.coerceAtLeast(rg.endPosition.third) - rg.startPosition.third.coerceAtMost(rg.endPosition.third)

        return width * height * city.tax

    }


    fun setMaxUser(cityID: String,value:Int){
        val data = get(cityID)?:return
        data.maxUser = value
        set(cityID,data)
    }

    //リージョンのidから都市を返す
    fun whereRegion(id:Int): String {
        return where(Region.get(id)!!.teleport)?:"無名の都市"
    }

    class CityData{

//        var regionList = mutableListOf<Int>()

        var tax = 0.0

        var world = "builder"
        var server = "server"

        var name = "CityName"

        var maxUser = 100

        var buyScore = 0
        var liveScore = 0

        var defaultPrice = 0.0

//        var startPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)
//        var endPosition: Triple<Double,Double,Double> = Triple(0.0,0.0,0.0)

        var startX = 0.0
        var startY = 0.0
        var startZ = 0.0

        var endX = 0.0
        var endY = 0.0
        var endZ = 0.0

//        lateinit var teleport : Location

        var isLoad = true

        fun getStart(): Triple<Double, Double, Double> {
            return Triple(startX,startY,startZ)
        }
        fun getEnd(): Triple<Double, Double, Double> {
            return Triple(endX,endY,endZ)
        }
        fun setStart(triple: Triple<Double,Double,Double>){
            startX = triple.first
            startY = triple.second
            startZ = triple.third
        }
        fun setEnd(triple: Triple<Double,Double,Double>){
            endX = triple.first
            endY = triple.second
            endZ = triple.third
        }
    }

}