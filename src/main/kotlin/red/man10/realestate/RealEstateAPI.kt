package red.man10.realestate

import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.realestate.region.Event
import red.man10.realestate.region.User
import red.man10.realestate.region.User.Companion.Permission

class RealEstateAPI {

    /**
     * 権限があるかどうか
     */
    fun hasPermission(p:Player, loc: Location, perm: Permission): Boolean {
        return Event.hasPermission(p,loc,perm)
    }

}