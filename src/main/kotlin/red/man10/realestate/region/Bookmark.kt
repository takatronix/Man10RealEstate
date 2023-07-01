package red.man10.realestate.region

import org.bukkit.entity.Player
import red.man10.realestate.Plugin
import red.man10.realestate.util.MySQLManager
import red.man10.realestate.util.Utility
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Bookmark {

    val bookmarkMap = ConcurrentHashMap<UUID,MutableList<Int>>()

    fun asyncLoadBookmark(p:Player){
        Plugin.async.execute {
            val sql = MySQLManager(Plugin.plugin,"Man10RealEstate")
            val rs = sql.query("SELECT region_id FROM bookmark WHERE uuid='${p.uniqueId}'")?:return@execute

            val list = mutableListOf<Int>()
            while (rs.next()){
                list.add(rs.getInt("region_id"))
            }
            bookmarkMap[p.uniqueId] = list

            rs.close()
            sql.close()
        }
    }

    fun changeBookmark(p:Player,id:Int){
        if (bookmarkMap[p.uniqueId] == null){
            Utility.sendMessage(p,"ブックマークしました！")
            addBookmark(p,id)
        }else{
            Utility.sendMessage(p,"ブックマークを外しました！")
            deleteBookmark(p,id)
        }
    }

    private fun addBookmark(p:Player, id:Int){
        val list = bookmarkMap[p.uniqueId]?: mutableListOf()
        list.add(id)
        bookmarkMap[p.uniqueId] = list
        MySQLManager.mysqlQueue.add("INSERT INTO bookmark (player, uuid, region_id) VALUES ('${p.name}', '${p.uniqueId}', $id)")
    }

    private fun deleteBookmark(p:Player, id:Int){
        val list = bookmarkMap[p.uniqueId]?: mutableListOf()
        list.remove(id)
        bookmarkMap[p.uniqueId] = list
        MySQLManager.mysqlQueue.add("DELETE FROM bookmark WHERE uuid='${p.uniqueId}' and region_id=$id")
    }


}