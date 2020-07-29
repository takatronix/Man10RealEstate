package red.man10.realestate.fly

import org.bukkit.Bukkit

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import red.man10.realestate.Plugin
import red.man10.realestate.Utility.Companion.sendMessage
import red.man10.realestate.region.Event
import red.man10.realestate.region.User
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Fly {

    val flyData = ConcurrentHashMap<UUID,FlyData?>()

    fun flyOn(p:Player){
        if (!p.allowFlight){
            sendMessage(p,"§e§lflyをオンにしました！")
        }

        p.allowFlight = true
//        p.isFlying = true
    }

    fun flyOff(p:Player){
        if (p.allowFlight){
            sendMessage(p,"§e§lflyをオフにしました！")
        }

        p.isFlying = false
        p.allowFlight = false
    }

    fun finishFlyMode(p:Player){
        flyOff(p)
        sendMessage(p,"§b§lFlyモードの時間が切れました！")

        Bukkit.getScheduler().runTask(Plugin.plugin, Runnable {
            p.removePotionEffect(PotionEffectType.GLOWING)
        })

        flyData.remove(p.uniqueId)
    }

    fun isFlyMode(p:Player): Boolean {
        return flyData[p.uniqueId] !=null
    }

    fun addFlyTime(p:Player,time:Int){

        sendMessage(p,"§b§lflyモードをオンにしました！")

        if (Event.hasPermission(p,p.location,User.Companion.Permission.ALL)){
            flyOn(p)
        }

        val data = if (flyData[p.uniqueId] !=null)flyData[p.uniqueId]!! else FlyData()
        data.time = addDate(data.time,time)

        flyData[p.uniqueId] = data
    }

    //現在地点がfly可能かどうか確認する
    fun checkFly(){

        for (p in Bukkit.getOnlinePlayers()){

            val data = flyData[p.uniqueId]?:continue

            Bukkit.getScheduler().runTask(Plugin.plugin, Runnable {
                p.addPotionEffect(PotionEffect(PotionEffectType.GLOWING,10000,1))
            })

            if (data.time.time<Date().time){
                finishFlyMode(p)
                continue
            }

            if (Event.hasPermission(p,p.location,User.Companion.Permission.ALL)){
                flyOn(p)
                continue
            }

            flyOff(p)

        }

    }

    fun addDate(date: Date, min:Int):Date{

        val calender = Calendar.getInstance()

        calender.time = date
        calender.add(Calendar.MINUTE,min)

        return calender.time
    }

    class FlyData{

        var time = Date()


    }

}