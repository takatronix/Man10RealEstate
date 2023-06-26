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

        private val cityData = ConcurrentHashMap<String, City>()
        private val gson = Gson()
        fun asyncLoadAll(){

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

                        val data = gson.fromJson(jsonStr, CityOld.CityData::class.java)

                        if (!data.enable){
                            Bukkit.getLogger().info("unload city : $name")
                            continue
                        }
                        Bukkit.getLogger().info("load city : $name")

                        CityOld.cityData[name] = data

                    }catch (e: IOException){
                        Bukkit.getLogger().info(e.message)
                    }catch (e:java.lang.Exception){
                        Bukkit.getLogger().info("Error")
                    }
                }
            }
        }

        fun where(loc: Location):String?{
            for (city in cityData){
                if (Utility.isWithinRange(loc,city.value.getStart(),city.value.getEnd(),city.value.world,city.value.server)){
                    return city.key
                }
            }
            return null
        }

        fun asyncPayTax(){
            Plugin.async.execute {
                //TODO:リージョンのリストを引いて処理する

            }
        }
    }

    var tax = 0.0

    var world = "builder"
    var server = "server"

    var name = "CityName"

    var maxUser = 100

    var buyScore = 0
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

    fun set(){

        Plugin.async.execute {
            try {
                val file = File("${Plugin.plugin.dataFolder}/${name}.json")

                if (file.exists()){
                    file.delete()
                }else{
                    Bukkit.getLogger().info("存在しない都市")
                    return@execute
                }


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

    fun getTax(rgID:Int):Double{
        val rg = RegionOld.get(rgID)?:return 0.0
        if (rg.isRemitTax)return 0.0

        val width = rg.startPosition.first.coerceAtLeast(rg.endPosition.first) - rg.startPosition.first.coerceAtMost(rg.endPosition.first)
        val height = rg.startPosition.third.coerceAtLeast(rg.endPosition.third) - rg.startPosition.third.coerceAtMost(rg.endPosition.third)

        return width * height * tax
    }

    fun searchCityFromRegion(rgId:Int):String?{
        return where(RegionOld.get(rgId)!!.teleport)
    }




}