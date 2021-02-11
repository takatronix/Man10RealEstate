package red.man10.realestate

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.Plugin.Companion.plugin

object Utility {

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

}