package red.man10.realestate.storage

import org.bukkit.*
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Plugin.Companion.prefix
import red.man10.realestate.region.Event
import red.man10.realestate.region.User
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class Barrel {

    companion object{
        val title = "§e§l特殊樽"
    }

    fun setStorageItem(inv:Inventory,block:Block){

        val list = mutableListOf<ItemStack>()

        for (i in 0..53){
            val item = inv.getItem(i)
            if (item == null){
                list.add(ItemStack(Material.AIR))
                continue
            }
            list.add(item)
        }

        val state = block.state
        if (state !is Barrel)return

        state.persistentDataContainer.set(NamespacedKey(plugin,"storage"), PersistentDataType.STRING,itemStackArrayToBase64(list.toTypedArray()))

        state.update()
    }

    fun openStorage(barrel:Barrel, p:Player){

        val inv = Bukkit.createInventory(null,54, title)

        val storage = barrel.persistentDataContainer[NamespacedKey(plugin,"storage"), PersistentDataType.STRING]

        if (storage == null){
            p.openInventory(inv)
            return
        }

        val items = itemStackArrayFromBase64(storage)

        for (item in items){
            inv.addItem(item)
        }

        p.openInventory(inv)

    }

    fun dropStorage(barrel: Barrel){
        val storage = barrel.persistentDataContainer[NamespacedKey(plugin,"storage"), PersistentDataType.STRING]?:return

        val items = itemStackArrayFromBase64(storage)

        items.forEach { if (it.type != Material.AIR) {barrel.world.dropItem(barrel.location, it) }}
    }

    fun hasPermission(p:Player,barrel: Barrel):Boolean{

        if (Event.hasPermission(p,barrel.location,User.Companion.Permission.ALL))return true

        val owners = barrel.persistentDataContainer[NamespacedKey(plugin,"owners"), PersistentDataType.STRING]?.split(";")?:return false

        if (owners.contains(p.uniqueId.toString()))return true

        return false

    }




    ////////////////////////////////////////
    //base64 stack
    /////////////////////////////////////////
    @Throws(IllegalStateException::class)
    fun itemStackArrayToBase64(items: Array<ItemStack>): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)

            // Write the size of the inventory
            dataOutput.writeInt(items.size)

            // Save every element in the list
            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }

            // Serialize that array
            dataOutput.close()
            return Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

    @Throws(IOException::class)
    fun itemStackArrayFromBase64(data: String): MutableList<ItemStack> {
        try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())

            // Read the serialized inventory
            for (i in items.indices) {
                items[i] = dataInput.readObject() as ItemStack
            }

            dataInput.close()
            return unwrapItemStackMutableList(items.toMutableList())
        } catch (e: ClassNotFoundException) {
            throw IOException("Unable to decode class type.", e)
        }

    }

    fun unwrapItemStackMutableList(list: MutableList<ItemStack?>): MutableList<ItemStack>{
        val unwrappedList = mutableListOf<ItemStack>()
        for (item in list) {
            if (item != null) {
                unwrappedList.add(item)
            }
        }
        return unwrappedList
    }

}