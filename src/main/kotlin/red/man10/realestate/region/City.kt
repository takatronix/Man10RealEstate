package red.man10.realestate.region

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import red.man10.realestate.Plugin
import red.man10.realestate.util.Utility
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class City {

    companion object{

        val cityData = ConcurrentHashMap<String, City>()
        private val gson = Gson()
        fun asyncLoad(){

            Plugin.async.execute {
                cityData.clear()

                val files = File(Plugin.plugin.dataFolder, File.separator).listFiles()?.toMutableList()?:return@execute

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

                        val data = gson.fromJson(jsonStr, City::class.java)

                        Bukkit.getLogger().info("load city : $name")

                        cityData[name] = data

                    }catch (e: IOException){
                        Bukkit.getLogger().info(e.message)
                    }catch (e:java.lang.Exception){
                        Bukkit.getLogger().info("Error")
                    }
                }
            }
        }

        fun where(loc: Location):City?{
            for (city in cityData){
                if (Utility.isWithinRange(loc,city.value.getStart(),city.value.getEnd(),city.value.world,city.value.server)){
                    return city.value
                }
            }
            return null
        }

        fun payTax(){
            val rgList = Region.regionData.values

            Bukkit.getLogger().warning("税金の支払いを行います")

            for (rg in rgList){
                if (rg.taxStatus == "FREE" || rg.ownerUUID == null)continue
                val amount = getTax(rg.id)

                if (rg.taxStatus == "FREE")continue

                if (rg.taxStatus == "WARN"){
                    //ここで支払い失敗したら土地を手放す
                    if (!Plugin.bank.withdraw(rg.ownerUUID!!,amount,
                            "Man10RealEstate Tax","税金の支払い(延滞)")){
                        rg.init()
                        continue
                    }
                    rg.taxStatus = "SUCCESS"
                    rg.asyncSave()
                    continue
                }
                if (rg.taxStatus == "SUCCESS"){
                    //ここでは渓谷のみ
                    if (!Plugin.bank.withdraw(rg.ownerUUID!!,amount,
                            "Man10RealEstate Tax","税金の支払い")){
                        rg.taxStatus = "WARN"
                        rg.asyncSave()
                        continue
                    }
                    rg.taxStatus = "SUCCESS"
                    rg.asyncSave()
                    continue
                }

            }

            Bukkit.getLogger().warning("税金の支払い完了")
        }

        //税額を取得 ペナルティなども考慮済みの額
        fun getTax(rgID:Int):Double{
            val rg = Region.regionData[rgID]?:return 0.0
            if (rg.taxStatus == Region.TaxStatus.FREE)return 0.0
            val city = where(rg.teleport)?:return 0.0

            if (rg.data.tax != 0.0) return rg.data.tax

            val width = rg.startPosition.first.coerceAtLeast(rg.endPosition.first) - rg.startPosition.first.coerceAtMost(rg.endPosition.first) + 1
            val height = rg.startPosition.third.coerceAtLeast(rg.endPosition.third) - rg.startPosition.third.coerceAtMost(rg.endPosition.third) + 1

            return if (rg.taxStatus == Region.TaxStatus.WARN) width * height * city.tax * Plugin.penalty else width * height * city.tax
        }
    }

    var tax = 0.0

    var world = "builder"
    var server = "server"

    var name = "CityName"

    var maxUser = 100

    var ownerScore = 0
    var liveScore = 0

    var defaultPrice = 0.0

    private var startX = 0
    private var startY = 0
    private var startZ = 0
    private var endX = 0
    private var endY = 0
    private var endZ = 0

    fun getStart(): Triple<Int, Int, Int> {
        return Triple(startX,startY,startZ)
    }
    fun getEnd(): Triple<Int, Int, Int> {
        return Triple(endX,endY,endZ)
    }
    fun setStart(triple: Triple<Int,Int,Int>){
        startX = triple.first
        startY = triple.second
        startZ = triple.third
    }
    fun setEnd(triple: Triple<Int,Int,Int>){
        endX = triple.first
        endY = triple.second
        endZ = triple.third
    }

    fun asyncSave(){

        Plugin.async.execute {

            cityData[name] = this

            try {
                val file = File("${Plugin.plugin.dataFolder}/${name}.json")

//                if (file.exists()){
//                    file.delete()
//                }else{
//                    Bukkit.getLogger().info("存在しない都市")
//                    return@execute
//                }

                val jsonStr = gson.toJson(this)
                val writer = FileWriter(file)

                writer.write(jsonStr)
                writer.close()
            }catch (e:IOException){
                Bukkit.getLogger().info(e.message)
                Bukkit.getLogger().info(e.stackTraceToString())
            }

        }
    }

    fun asyncDelete(){
        Plugin.async.execute {
            try {
                val file = File("${Plugin.plugin.dataFolder}/${name}.json")
                file.delete()

            }catch (e:IOException){
                Bukkit.getLogger().info(e.message)
                Bukkit.getLogger().info(e.stackTraceToString())
            }

        }
    }

}