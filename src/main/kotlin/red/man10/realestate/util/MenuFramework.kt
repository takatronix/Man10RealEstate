package red.man10.realestate.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * マイクラプラグインでメニューを作るためのフレームワーク
 * このクラスを継承させて使用する。
 * 起動時にsetup関数を呼んでPluginインスタンスを渡す
 *
 * (最終更新 2024/11/11) created by Jin Morikawa
 */
open class MenuFramework(val p:Player,private val menuSize: Int, private val title: String){

    lateinit var menu : Inventory
    private var closeAction : OnCloseListener? = null
    private var clickAction : Button.OnClickListener? = null
    private var clickable=true

    companion object{
        private val menuStack = HashMap<UUID,Stack<MenuFramework>>()
        private lateinit var instance : JavaPlugin

        const val CHEST_SIZE = 27
        const val LARGE_CHEST_SIZE= 54

        //      起動時に読み込む
        fun setup(plugin:JavaPlugin){
            instance = plugin
        }

        fun push(p:Player,menu:MenuFramework){
            val stack = menuStack[p.uniqueId]?: Stack()
            if (stack.isNotEmpty() && menu::class == stack.peek()::class){
                return
            }
            stack.push(menu)
            menuStack[p.uniqueId] = stack
        }

        //      スタックの取り出し
        fun pop(p:Player):MenuFramework?{
            val stack = menuStack[p.uniqueId]?:return null
            if (stack.isEmpty())return null
            val menu = stack.pop()
            menuStack[p.uniqueId] = stack
            return menu
        }

        //      スタックの最新を確かめる
        fun peek(p:Player): MenuFramework? {
            val stack = menuStack[p.uniqueId]
            if (stack.isNullOrEmpty()) return null
            return stack.peek()
        }

        fun dispatch(plugin:JavaPlugin,job:()->Unit){
            Bukkit.getScheduler().runTask(plugin, Runnable(job))
        }
    }

    open fun init(){}

    fun clickable(boolean:Boolean){
        clickable=boolean
    }

    fun open(){
//        p.closeInventory()
        Bukkit.getScheduler().runTask(instance,Runnable {
            menu = Bukkit.createInventory(null,menuSize, text(title))
            init()
            push(p,this)
            p.openInventory(menu)
        })
    }

    //slotは0スタート
    fun setButton(button: Button, slot:Int){
        menu.setItem(slot,button.icon())
    }

    //
    fun addButton(button:Button){
        menu.addItem(button.icon())
    }

    //背景として全埋めする
    fun fill(button: Button){
        for (i in 0 until menu.size){
            setButton(button,i)
        }
    }

    fun setCloseAction(action: OnCloseListener){
        closeAction = action
    }

    fun setClickAction(action: Button.OnClickListener){
        clickAction = action
    }

    fun close(e:InventoryCloseEvent){
        closeAction?.closeAction(e)
        if (e.reason == InventoryCloseEvent.Reason.PLAYER){
            pop(p)//ひとつ前のメニューに戻るためにスタックを一個削除
            pop(p)?.open()
        }
        if (e.reason == InventoryCloseEvent.Reason.PLUGIN){
            menuStack.remove(e.player.uniqueId)
        }
    }

    fun interface OnCloseListener{
        fun closeAction(e: InventoryCloseEvent)
    }

    class Button(icon:Material):Cloneable{

        private var buttonItem : ItemStack
        private var actionData : OnClickListener? = null
        private val key = UUID.randomUUID().toString()

        init {
            buttonItem = ItemStack(icon)
            val meta = buttonItem.itemMeta
            meta.persistentDataContainer.set(NamespacedKey.fromString("key")!!
                , PersistentDataType.STRING,key)
            buttonItem.itemMeta = meta
        }

        companion object{

            private val buttonMap = HashMap<String, Button>()

            fun set(button: Button){
                buttonMap[button.key] = button
            }

            fun get(item:ItemStack): Button?{

                if (!item.hasItemMeta())return null

                val meta = item.itemMeta
                val key = meta.persistentDataContainer[NamespacedKey.fromString("key")!!, PersistentDataType.STRING]?:return null

                return buttonMap[key]
            }
        }

        fun setIcon(item:ItemStack): Button {
            buttonItem = item.clone()
            val meta = buttonItem.itemMeta
            meta.persistentDataContainer.set(NamespacedKey.fromString("key")!!
                , PersistentDataType.STRING,key)
            buttonItem.itemMeta = meta
            set(this)
            return this
        }

        fun title(text:String): Button {
            val meta = buttonItem.itemMeta
            meta.displayName(text(text))
            buttonItem.itemMeta = meta
            set(this)
            return this
        }

        fun cmd(int:Int): Button {
            val meta = buttonItem.itemMeta
            meta.setCustomModelData(int)
            buttonItem.itemMeta = meta
            set(this)
            return this
        }

        fun lore(lore:List<String>): Button {
            val loreComponent = mutableListOf<Component>()
            lore.forEach { loreComponent.add(text(it)) }

            val meta = buttonItem.itemMeta
            meta.lore(loreComponent)
            buttonItem.itemMeta = meta
            set(this)
            return this

        }

        fun setClickAction(action: OnClickListener): Button {
            actionData = action
            set(this)
            return this
        }

        fun enchant(boolean: Boolean): Button {

            val meta = buttonItem.itemMeta

            if (!boolean){
                meta.enchants.forEach { meta.removeEnchant(it.key) }
                buttonItem.itemMeta = meta
                set(this)
                return this
            }

            meta.addEnchant(Enchantment.LUCK,1,false)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            buttonItem.itemMeta = meta
            set(this)
            return this
        }


        fun click(e:InventoryClickEvent){
            actionData?.action(e)
        }

        fun icon():ItemStack{
            return buttonItem
        }

        public override fun clone(): Button {
            return super.clone() as Button
        }

        fun interface OnClickListener{
            fun action(e:InventoryClickEvent)
        }
    }

    object MenuListener:Listener{

        @EventHandler(priority = EventPriority.HIGHEST)
        fun clickEvent(e:InventoryClickEvent){

            val p = e.whoClicked

            if (p !is Player)return

            val menu = peek(p) ?:return

            //メニューが違う場合は無視
            if (e.view.title != menu.title)return

            if(!menu.clickable)e.isCancelled=true

            menu.clickAction?.action(e)

            val item = e.currentItem?:return
            val data = Button.get(item) ?:return
            e.isCancelled = true

            data.click(e)
        }

        @EventHandler(priority = EventPriority.LOW)
        fun closeEvent(e:InventoryCloseEvent){

            if (e.player !is Player)return
            val menu = peek(e.player as Player) ?:return
            menu.close(e)
        }

    }
}
