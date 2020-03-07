package red.man10.realestate

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.entity.Player

class Utility {

    ////////////////////////////////////////////////////////////////////////////////////////////
    //  マインクラフトチャットに、ホバーテキストや、クリックコマンドを設定する関数
    // [例1] sendHoverText(player,"ここをクリック",null,"/say おはまん");
    // [例2] sendHoverText(player,"カーソルをあわせて","ヘルプメッセージとか",null);
    // [例3] sendHoverText(player,"カーソルをあわせてクリック","ヘルプメッセージとか","/say おはまん");
    fun sendHoverText(p: Player, text: String, hoverText: String, command: String) {
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        var hoverEvent: HoverEvent? = null
        val hover: Array<BaseComponent> = ComponentBuilder(hoverText).create()
        hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)

        //////////////////////////////////////////
        //   クリックイベントを作成する
        var clickEvent: ClickEvent? = null
        clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
        val message: Array<BaseComponent> = ComponentBuilder(text).event(hoverEvent).event(clickEvent).create()
//        p.spigot().sendMessage(message) 何故かエラー吐く
    }


}