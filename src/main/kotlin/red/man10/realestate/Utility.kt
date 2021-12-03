package red.man10.realestate

import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent.runCommand
import net.kyori.adventure.text.event.ClickEvent.suggestCommand
import net.kyori.adventure.text.event.HoverEvent.showText
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.Plugin.Companion.prefix
import kotlin.math.abs

object Utility {

    ////////////////////////////////////////////////////////////
    //立体の対角線の頂点から、指定座標が立体の中にあるかどうか判定するメソッド
    ////////////////////////////////////////////////////////////
    fun isWithinRange(loc: Location, start:Triple<Int,Int,Int>, end:Triple<Int,Int,Int>, world:String,server:String):Boolean{

        if (Plugin.serverName!=server)return false
        if (loc.world.name != world)return false

        if (abs((start.first+end.first)-2*loc.blockX) > abs(start.first-end.first) ||
            abs((start.third+end.third)-2*loc.blockZ) > abs(start.third-end.third) ||
            abs((start.second+end.second)-2*loc.blockY) > abs(start.second-end.second))return false

        return true

    }

    //ホバーテキスト、クリックイベント
    fun sendClickMessage(p: Player, text: String, command: String,hoverText : String) {
        val c = text()
        val h =

        p.sendMessage(text("$prefix$text").clickEvent(runCommand("/$command")).hoverEvent(showText(text(hoverText))))
    }

    //サジェストメッセージ
    fun sendSuggest(p: Player, text: String?, command: String,hoverText: String) {
        p.sendMessage(text("${prefix}$text§a§n[ここをクリック！]").clickEvent(suggestCommand("/$command")).hoverEvent(showText(text(hoverText))))
    }

    //prefix付きのメッセージ
    fun sendMessage(player: Player, message: String) {
        player.sendMessage("$prefix$message")
    }

    fun format(double: Double):String{
        return String.format("%,.0f",double)
    }

}