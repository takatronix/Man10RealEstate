package red.man10.realestate

import org.bukkit.Location
import org.bukkit.entity.Player
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
}