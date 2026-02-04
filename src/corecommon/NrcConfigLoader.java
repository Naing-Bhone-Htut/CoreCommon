package corecommon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class NrcConfigLoader {
    
    private static List<String> allowedTypes;

    // Static initializer: Runs once when the class is loaded
    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties prop = new Properties();
        InputStream input = null; // 1. Declare variable outside try block

        try {
            // 2. Open stream inside try block
            input = NrcConfigLoader.class.getClassLoader().getResourceAsStream("nrc_config.properties");
            
            if (input == null) {
                System.out.println("LOG: nrc_config.properties not found. Using default list.");
                allowedTypes = Arrays.asList("C", "AC", "NC", "V", "M", "N", "Z");
                return;
            }

            // Load the properties file
            prop.load(input);

            String types = prop.getProperty("nrc.allowed.types");
            if (types != null && !types.trim().equals("")) { // Java 6 safe empty check
                String[] typeArray = types.split(",");
                // Loop to trim whitespace
                for (int i = 0; i < typeArray.length; i++) {
                    typeArray[i] = typeArray[i].trim().toUpperCase();
                }
                allowedTypes = Arrays.asList(typeArray);
            } else {
                allowedTypes = Arrays.asList("C", "AC", "NC", "V", "M", "N", "Z");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            allowedTypes = Arrays.asList("C", "AC", "NC", "V", "M", "N", "Z");
        } finally {
            // 3. Classic Java 6 "Finally" block to close stream
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // Method to access the list
    public static List<String> getAllowedTypes() {
        return allowedTypes;
    }
    
    // Call this if you manually update the properties file and want to reload without restarting
    public static void reload() {
        loadConfig();
    }
}