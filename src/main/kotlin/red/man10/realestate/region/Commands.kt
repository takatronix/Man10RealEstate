package red.man10.realestate.region

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Constants
import red.man10.realestate.Plugin

class Commands (private val pl :Plugin):CommandExecutor{
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (args.isEmpty()){
            help(sender)
            return true
        }

        val cmd = args[0]

        //範囲指定ワンド取得
        if (cmd == "wand"){
            val wand = ItemStack(Material.STICK)
            val meta = wand.itemMeta
            meta.setDisplayName(Constants.WAND_NAME)
            wand.itemMeta = meta
            sender.inventory.addItem(wand)
            return true
        }




        return false
    }

    fun help(p:Player){

    }


}