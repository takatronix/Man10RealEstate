package red.man10.realestate

import org.bukkit.Location
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import red.man10.realestate.region.Event
import red.man10.realestate.region.Region
import red.man10.realestate.region.User
import java.util.*

object RealEstateAPI {

    /**
     * 権限があるかどうか
     */
    fun hasPermission(p:Player, loc: Location, perm: User.Permission): Boolean {
        return Event.hasPermission(p,loc,perm)
    }

    /**
     * どこかの土地に住んでいるかどうか
     */
    fun isLiving(p:Player):Boolean{

        if (hasRegion(p.uniqueId))return true

        val data = User.userData[p]?:return false

        if (data.isEmpty())return false

        return true

    }

    /**
     * 土地を所有しているかどうか
     */
    fun hasRegion(uuid: UUID):Boolean{
        if (Region.map().values.any { it.ownerUUID == uuid })return true

        return false
    }

    /**
     * 特殊樽を取得
     * @return 通常の樽だった場合、通常樽の中身を返す バレルじゃなかったらnullを返す
     */
    fun getSpecialBarrel(block: Block):Inventory?{

        val state = block.state

        if (state !is Barrel)return null

        return red.man10.realestate.storage.Barrel.getStorage(state)
    }

    fun getSpecialBarrel(barrel:Barrel):Inventory?{
        return red.man10.realestate.storage.Barrel.getStorage(barrel)
    }


    /**
     *
     * 特殊樽ならtrueを返す
     *
     */
    fun isSpecialBarrel(barrel:Barrel):Boolean{
        return red.man10.realestate.storage.Barrel.isSpecialBarrel(barrel)
    }

}