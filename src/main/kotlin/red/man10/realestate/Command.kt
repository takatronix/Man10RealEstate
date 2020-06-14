package red.man10.realestate

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import red.man10.realestate.Utility.Companion.sendMessage

class Command:CommandExecutor {

    val USER = "mre.user"
    val GUEST = "mre.guest"
    val OP = "mre.op"
    val RENT = "mre.rent"


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label == "mre"){




            sendMessage(sender,"§c§l不明なコマンドです！")

            return false
        }

        if (label == "mreop"){




            sendMessage(sender,"§c§l不明なコマンドです！")

            return false
        }



        return true
    }
}