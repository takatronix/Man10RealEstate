package red.man10.realestate

import com.google.gson.Gson
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.Plugin.Companion.plugin

object Utility {

    val gson = Gson()

    ////////////////////////////////////////////////////////////
    //立体の対角線の頂点から、指定座標が立体の中にあるかどうか判定するメソッド
    ////////////////////////////////////////////////////////////
    fun isWithinRange(loc: Location, start:Triple<Double,Double,Double>, end:Triple<Double,Double,Double>, world:String,server:String):Boolean{

        if (Plugin.serverName!=server)return false
        if (loc.world.name != world)return false

        val x = loc.blockX
        val y = loc.blockY
        val z = loc.blockZ

        if (x < start.first.coerceAtMost(end.first) || x > start.first.coerceAtLeast(end.first))return false
        if (y < start.second.coerceAtMost(end.second) || y > start.second.coerceAtLeast(end.second))return false
        if (z < start.third.coerceAtMost(end.third) || z > start.third.coerceAtLeast(end.third))return false

        return true

    }

    //ホバーテキスト、クリックイベント
    fun sendHoverText(p: Player, text: String, hoverText: String, command: String) {
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        val hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(hoverText).create())

        //////////////////////////////////////////
        //   クリックイベントを作成する
        val clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$command")
        val message = ComponentBuilder(Plugin.prefix +text).event(hoverEvent).event(clickEvent).create()
        p.spigot().sendMessage(*message)
    }

    //サジェストメッセージ
    fun sendSuggest(p: Player, text: String?, command: String?) {

        //////////////////////////////////////////
        //   クリックイベントを作成する
        var clickEvent: ClickEvent? = null
        if (command != null) {
            clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/$command")
        }

        val message = ComponentBuilder("${Plugin.prefix}$text§a§l[ここをクリックで自動入力！]").event(clickEvent).create()
        p.spigot().sendMessage(*message)
    }

    //prefix付きのメッセージ
    fun sendMessage(player: Player, message: String) {
        player.sendMessage("${Plugin.prefix} $message")
    }

    //Jsonデータからロケーション値に変換
    fun jsonToLocation(string: String): Location {

        val jsonObj = gson.fromJson(string, LocationProperty::class.java)

        return Location(Bukkit.getWorld(jsonObj.world), jsonObj.x, jsonObj.y, jsonObj.z,jsonObj.yaw,jsonObj.pitch)
    }

    //現在のロケーションをJsonデータに変換
    fun locationToJson(location: Location):String{

        val property = LocationProperty()

        property.world = location.world.name
        property.x = location.x
        property.y = location.y
        property.z = location.z
        property.pitch = location.pitch
        property.yaw = location.yaw

        return gson.toJson(property)
    }

    class LocationProperty{

        var world : String = ""

        var x  = 0.0
        var y  = 0.0
        var z  = 0.0

        var pitch = 0.0F
        var yaw = 0.0F

    }
}