package red.man10.realestate

import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.region.Event
import red.man10.realestate.region.User
import java.util.*

class RealEstateAPI {

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

        val data = User.userData[p]?:return false

        if (data.isEmpty())return false

        return true

    }

    /**
     * 土地を所有しているかどうか
     */
    fun hasRegion(uuid: UUID):Boolean{
        for (rg in Plugin.region.map().values){
            if (rg.ownerUUID == uuid)return true
        }
        return false
    }
}