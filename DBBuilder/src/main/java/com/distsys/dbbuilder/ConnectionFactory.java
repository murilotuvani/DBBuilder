/*
 *
 *
 */

package com.distsys.dbbuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 21/10/2010 21:29:46
 * @author Murilo
 */
public class ConnectionFactory {
    private static String sgbd = null;
    private static String database = null;
    private static String schema = null;
    private static String host = null;
    private static String port = null;
    private static String user = null;
    private static String password = null;
    private static Connection conn = null;

    static {
        host = System.getProperty("host", "localhost");
        user = System.getProperty("user");
        schema = System.getProperty("schema");
        password = System.getProperty("password");
        database = System.getProperty("database");
        try {
            sgbd = System.getProperty("sgbd", "sqlserver");
            if ("sqlserver".equals(sgbd)) {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                port = "1433";
            } else if ("mysql".equals(sgbd)) {
                Class.forName("com.mysql.jdbc.Driver");
                port = "3306";
            } else if ("oracle".equals(sgbd)) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                port = "1521";
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConnectionFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        port = System.getProperty("port", port);
    }

    public static Connection getConnection() throws SQLException {
        if(conn==null) {
            conn = createConnection();
        }
        return conn;
    }

    private static Connection createConnection() throws SQLException {
        StringBuilder url = new StringBuilder("jdbc:");
        //jtds:sqlserver://

        if ("sqlserver".equals(sgbd)) {
            url.append("jtds:sqlserver://");
        } else if ("mysql".equals(sgbd)) {
            url.append("mysql://");
        } else if ("oracle".equals(sgbd)) {
            url.append("oracle:thin:@//");
        }

        url.append(host);
        if(port!=null) {
            url.append(":");
            url.append(port);
        }
        
        if(!"".equals(database) && database!=null) {
            url.append("/");
            url.append(database);
        }
        if(schema!=null) {
            url.append(";");
            url.append(schema);
        }
        String aurl = url.toString();
//        aurl = "jdbc:oracle:thin:@localhost:1521:XE";
        System.out.println(aurl);
        conn = DriverManager.getConnection(aurl, user, password);
//        if(schema!=null) {
//            conn.setCatalog(schema);
//        }
//        System.out.println("Currente schema : "+conn.createStatement().execute("ALTER SESSION SET CURRENT_SCHEMA=orami"));
        return conn;
    }

    public static void devolver(java.sql.Connection conn) {

    }

    public static void close() throws SQLException  {
        if(conn!=null) {
            conn.close();
        }
    }
}
