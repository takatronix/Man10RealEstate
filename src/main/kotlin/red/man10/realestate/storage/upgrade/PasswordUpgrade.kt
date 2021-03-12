package red.man10.realestate.storage.upgrade

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.storage.RemoteController
import red.man10.realestate.storage.upgrade.PasswordUpgrade.Type.PUT_PASS
import red.man10.realestate.storage.upgrade.PasswordUpgrade.Type.SET_PASS

class PasswordUpgrade : Upgrade() ,Listener{

    override val upgradeName = "password"

    private val slots = arrayOf(40,12,13,14,21,22,23,30,31,32)
    private val cmd = 567
    private val numbers = HashMap<Int,Pair<Int,ItemStack>>()

    private val numericPadMap = HashMap<Player, Data>()

    class Data{
        lateinit var controller : ItemStack
        lateinit var type : Type
        var numbers = ""
        var title = ""
        var maxDigit = 0
    }

    enum class Type{
        SET_PASS,
        PUT_PASS
    }

    init {

        for (i in 0..9){
            val item = ItemStack(Material.IRON_NUGGET)
            val meta = item.itemMeta
            meta.setDisplayName(i.toString())
            meta.setCustomModelData((cmd - i))
            item.itemMeta = meta
            numbers[i] = Pair(slots[i],item)
        }

    }

    fun getUpgrade(): ItemStack {
        return super.getUpgrade("§ePasswordApp", mutableListOf("紛失時に悪用されないように","パスワードを設定することができます"))
    }

    fun openNumericPad(p:Player){

        val data = numericPadMap[p]?: Data()

        if (data.numbers.length>=data.maxDigit) return

        val inv = Bukkit.createInventory(null,54,data.title)

        for (i in 0..9){
            inv.setItem(numbers[i]!!.first,numbers[i]!!.second)
        }

        p.openInventory(inv)

    }

    fun openPasswordSetting(p:Player, controller:ItemStack){

        val data = numericPadMap[p]?: Data()
        data.controller = controller
        data.maxDigit = 4
        data.type = SET_PASS
        data.title = "パスワードを設定する"
        numericPadMap[p] = data

        openNumericPad(p)

    }

    fun openCheckingPassword(p:Player,controller: ItemStack){

        val data = numericPadMap[p]?: Data()
        data.controller = controller
        data.maxDigit = 4
        data.type = PUT_PASS
        data.title = "パスワードを入力してください"
        numericPadMap[p] = data

        openNumericPad(p)

    }

    fun hasPassword(controller: ItemStack):Boolean{

        if (getPassword(controller) == "0000")return false

        return true
    }

    fun setPassword(p:Player,controller:ItemStack,pass:String){

        val meta = controller.itemMeta!!
        meta.persistentDataContainer.set(NamespacedKey(plugin,"password"), PersistentDataType.STRING,pass)
        controller.itemMeta = meta

        sendMessage(p,"パスワードを[$pass]に設定しました")
        if (!hasPassword(controller)){
            sendMessage(p,"[0000]なのでパスワードは無効になっています")
        }
    }

    fun getPassword(controller:ItemStack):String{

        val meta = controller.itemMeta!!

        return meta.persistentDataContainer[NamespacedKey(plugin,"password"), PersistentDataType.STRING]?:"0000"
    }



    private fun getClickedNumber(slot:Int):Int{
        for (i in 0..9){
            if (slot == slots[i])return i
        }
        return -1
    }

    @EventHandler
    fun clickNumericPad(e:InventoryClickEvent){

        val p = e.whoClicked as Player

        if (!numericPadMap.containsKey(p))return

        e.isCancelled = true

        if (e.action == InventoryAction.HOTBAR_SWAP)return

        if (!slots.contains(e.slot))return

        val num = getClickedNumber(e.slot)

        val data = numericPadMap[p]?:return

        data.numbers +=num

        sendMessage(p,data.numbers)

        if (data.numbers.length >= data.maxDigit){

            when(data.type){
                SET_PASS ->{
                    setPassword(p,data.controller,data.numbers)
                    numericPadMap.remove(p)
                    p.closeInventory()
                    return
                }

                PUT_PASS->{

                    if (data.numbers != getPassword(data.controller)){
                        p.closeInventory()
                        sendMessage(p,"パスワードが違います！")
                        numericPadMap.remove(p)
                        return
                    }

                    RemoteController.openInventory(data.controller,p,0,RemoteController.getStringLocationList(data.controller))
                    numericPadMap.remove(p)
                    return

                }
            }

        }

        numericPadMap[p] = data

        openNumericPad(p)
    }
}