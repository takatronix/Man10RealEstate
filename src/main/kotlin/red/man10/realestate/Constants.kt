package red.man10.realestate

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.region.RegionDatabase
import red.man10.realestate.region.RegionUserDatabase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue


class Constants(val p:Plugin) {

    companion object {
        const val WAND_NAME = "範囲指定ワンド"

        var prefix = "[§5Man10RealEstate§f]"

        //リージョンのデータ
        val regionData = ConcurrentHashMap<Int, RegionDatabase.RegionData>()
        //プレイヤーごとのリージョン情報
        val regionUserData = ConcurrentHashMap<Player, HashMap<Int,RegionUserDatabase.RegionUserData>>()
        //worldごとのリージョンid <ワールド名,ワールドないにあるリージョンのidのlist>
        val worldRegion = HashMap<String,MutableList<Int>>()

        val mysqlQueue = LinkedBlockingQueue<String>()

        val isLike = HashMap<Player,MutableList<Int>>()

        val ownerData = HashMap<Player,MutableList<Int>>()//オーナーメニューを開くための辞書


        //  マインクラフトチャットに、ホバーテキストや、クリックコマンドを設定する関数
        fun sendHoverText(p: Player, text: String, hoverText: String, command: String) {
            //////////////////////////////////////////
            //      ホバーテキストとイベントを作成する
            val hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(hoverText).create())

            //////////////////////////////////////////
            //   クリックイベントを作成する
            val clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$command")
            val message = ComponentBuilder(text).event(hoverEvent).event(clickEvent).create()
            p.spigot().sendMessage(*message)
        }

        //サジェストメッセージ
        fun sendSuggest(p: Player, text: String?, command: String?) {

            //////////////////////////////////////////
            //   クリックイベントを作成する
            var clickEvent: ClickEvent? = null
            if (command != null) {
                clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)
            }

            val message = ComponentBuilder("$text§a§l[ここをクリックで自動入力！]").event(clickEvent).create()
            p.spigot().sendMessage(*message)
        }

        //prefix付きのブロードキャストメッセージ
        fun broadcast(message: String) {
            Bukkit.broadcastMessage("$prefix $message")
        }

        //prefix付きのメッセージ
        fun sendMessage(player: Player, message: String) {
            player.sendMessage("$prefix $message")
        }


    }
}