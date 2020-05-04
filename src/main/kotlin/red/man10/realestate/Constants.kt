package red.man10.realestate

import org.bukkit.entity.Player
import red.man10.realestate.region.ProtectRegionEvent
import red.man10.realestate.region.RegionDatabase
import red.man10.realestate.region.RegionEvent
import red.man10.realestate.region.RegionUserDatabase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class Constants(val p:Plugin) {

    companion object {
        const val WAND_NAME = "範囲指定ワンド"
        //リージョンのデータ
        val regionData = ConcurrentHashMap<Int, RegionDatabase.RegionData>()
        //プレイヤーごとのリージョン情報
        val regionUserData = ConcurrentHashMap<Pair<Player,Int>, RegionUserDatabase.RegionUserData>()
        //worldごとのリージョンid <ワールド名,ワールドないにあるリージョンのidのlist>
        val worldRegion = HashMap<String,MutableList<Int>>()

        val mysqlQueue = LinkedBlockingQueue<String>()
        val isLiked = HashMap<Pair<Player,Int>,Boolean>()

    }
}