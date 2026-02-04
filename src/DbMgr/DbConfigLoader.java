/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DbMgr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author nipunshrestha
 */

/**
 * Loads database configuration from db_config.properties
 * Java 1.6 Compatible
 */
public class DbConfigLoader {

    private static String driver;
    private static String notifUrl;
    private static String reportUrl;
    private static String username;
    private static String password;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            // Load file from classpath root
            input = DbConfigLoader.class.getClassLoader().getResourceAsStream("db_config.properties");

            if (input == null) {
                System.out.println("CRITICAL ERROR: db_config.properties not found!");
                return;
            }

            prop.load(input);

            driver = prop.getProperty("db.driver");
            notifUrl = prop.getProperty("db.url.notification");
            reportUrl = prop.getProperty("db.url.report");
            username = prop.getProperty("db.user");
            password = prop.getProperty("db.password");

        } catch (IOException ex) {
            System.out.println("Error loading DB configuration");
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getDriver() { return driver; }
    public static String getNotifUrl() { return notifUrl; }
    public static String getReportUrl() { return reportUrl; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }
}