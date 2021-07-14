package red.man10.realestate

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent.runCommand
import net.kyori.adventure.text.event.ClickEvent.suggestCommand
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.Plugin.Companion.prefix

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
    fun sendClickMessage(p: Player, text: String, command: String) {

        p.sendMessage(Component.text("$prefix$text").clickEvent(runCommand(command)))

    }

    //サジェストメッセージ
    fun sendSuggest(p: Player, text: String?, command: String) {
        p.sendMessage(Component.text("${prefix}text").clickEvent(suggestCommand(command)))
    }

    //prefix付きのメッセージ
    fun sendMessage(player: Player, message: String) {
        player.sendMessage("$prefix$message")
    }

    fun format(double: Double):String{
        return String.format("%,.0f",double)
    }

}