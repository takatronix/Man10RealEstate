package red.man10.realestate

import org.bukkit.entity.Player
import red.man10.realestate.region.User
import red.man10.realestate.region.User.Companion.Permission
import javax.xml.stream.Location

class RealEstateAPI {

    /**
     * 権限があるかどうか
     */
    fun hasPermission(p:Player,loc:Location,perm: Permission) {
        return hasPermission(p,loc,perm)
    }

}