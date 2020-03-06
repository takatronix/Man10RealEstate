package red.man10.realestate

import org.bukkit.Bukkit

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level

class MySQLFunc(host: String, db: String, user: String, pass: String, port: String) {
    internal var HOST: String? = null
    internal var DB: String? = null
    internal var USER: String? = null
    internal var PASS: String? = null
    internal var PORT: String? = null
    var con: Connection? = null

    init {
        this.HOST = host
        this.DB = db
        this.USER = user
        this.PASS = pass
        this.PORT = port
    }

    fun open(): Connection? {
        try {
            Class.forName("com.mysql.jdbc.Driver")

            // var s = "jdbc:mysql://" + this.HOST + ":" + this.PORT + "/" + this.DB + "?useSSL=false" + this.USER + this.PASS
            // Bukkit.getLogger().log(Level.INFO,s)

            this.con = DriverManager.getConnection("jdbc:mysql://" + this.HOST + ":" + this.PORT + "/" + this.DB + "?useSSL=false", this.USER, this.PASS)
            return this.con
        } catch (e:Exception) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not connect to MySQL server, error message: ${e.message}")
        }


        return this.con
    }

    fun checkConnection(): Boolean {
        return this.con != null
    }

    fun close(c: Connection?) {
        c?.close()
    }
}