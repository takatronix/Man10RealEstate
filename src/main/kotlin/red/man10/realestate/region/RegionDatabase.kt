package red.man10.realestate.region

import org.bukkit.entity.Player
import red.man10.realestate.Plugin

class RegionDatabase(private val pl: Plugin) {


    /**
     * リージョンの登録
     *
     * @param owner リージョンのオーナー
     * @param name リージョン名
     * @param status ステータス(Free,Protected,Lock)
     *
     */
    fun registerRegion(owner:Player,name:String,status:String){

        val sql = "INSERT INTO `region` " +
                "(`server`, `world`, `owner_uuid`, `owner_name`, `x`, `y`, `z`, `pitch`, `yaw`, `name`, `status`) " +
                "VALUES ('${owner.server.name}', '${owner.world.name}'," +
                " '${owner.uniqueId}', '${owner.name}','$name', '$status');"
    }

    /**
     * リージョンの座標を登録、変更
     * @param name リージョン名
     * @param start Triple<sx,sy,sz>
     * @param end Triple<ex,ey,ez>
     */
    fun registerRegionCoordinate(name:String,start:Triple<Double,Double,Double>,end:Triple<Double,Double,Double>){
        val sql = "UPDATE `region` SET `sx`='${start.first}', `sy`='${start.second}', `sz`='${start.third}'," +
                " `ex`='${end.first}', `ey`='${end.second}', `ez`='${end.third}' WHERE  `name`='$name';"
    }

    /**
     * テレポート地点の変更
     * @param name リージョン名
     * @param tp List<x,y,z,pitch,yay>
     *
     */
    fun changeRegionTeleport(name:String,tp:List<String>){
        val sql = "UPDATE `region` SET `x`=${tp[0]},`y`=${tp[1]},`z`=${tp[2]}," +
                "`pitch`=${tp[3]},`yay`=${tp[4]};"
    }

    ///////////////////////
    //リージョンの値段を変更
    ///////////////////////
    fun changePrice(name:String,price:Double){
        val sql =  "UPDATE `region` SET `price`='$price' WHERE  `name`=$name;"
    }

    ///////////////////////
    //ステータスの変更
    ///////////////////////
    fun changeRegionStatus(name:String,status: String){
        val sql =  "UPDATE `region` SET `status`='$status' WHERE  `name`=$name;"
    }

    //////////////////////
    //オーナーの変更
    //////////////////////
    fun changeRegionOwner(name:String,owner: Player){
        val sql = "UPDATE `region` SET `owner_uuid`='${owner.uniqueId}', `owner_name='${owner.name}' WHERE `name`=$name;"
    }

    ///////////////////
    //リージョンの削除
    ///////////////////
    fun deleteRegion(name:String){
        val sql = "DELETE FROM `region` WHERE  `name`=$name;"
    }






}