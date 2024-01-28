package red.man10.realestate.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

object Logger {

    fun logger(uuid:UUID,log:String,id: Int = -1){
        val p = Bukkit.getOfflinePlayer(uuid)
        MySQLManager.mysqlQueue.add("INSERT INTO log (date, player, uuid, region_id, log) " +
                "VALUES (DEFAULT, '${p.name}', '${uuid}', $id, '${log}')")
    }

    fun logger(p:Player,log:String,id: Int = -1){
        MySQLManager.mysqlQueue.add("INSERT INTO log (date, player, uuid, region_id, log) " +
                "VALUES (DEFAULT, '${p.name}', '${p.uniqueId}', $id, '${log}')")
    }

    fun logger(log: String ,id:Int = -1){

        MySQLManager.mysqlQueue.add("INSERT INTO log (date, player, uuid, region_id, log) " +
                "VALUES (DEFAULT, 'Server', 'Server', $id, '${log}')")
    }

}