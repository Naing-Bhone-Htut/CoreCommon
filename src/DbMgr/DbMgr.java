package DbMgr;

import DbMgr.DbConfigLoader;
import java.sql.*;

public class DbMgr {

    /**
     * Get Connection for Notification DB
     * Reads credentials from db_config.properties via DbConfigLoader
     */
    public static Connection getConnection() {
        try {
            Class.forName(DbConfigLoader.getDriver()).newInstance();
            return DriverManager.getConnection(
                    DbConfigLoader.getNotifUrl(), 
                    DbConfigLoader.getUsername(), 
                    DbConfigLoader.getPassword());
        } catch (Exception e) {
            System.out.println("Error connecting to Notification DB: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get Connection for Reporting DB
     */
    public static Connection getReportingConnection() {
        try {
            Class.forName(DbConfigLoader.getDriver()).newInstance();
            return DriverManager.getConnection(
                    DbConfigLoader.getReportUrl(), 
                    DbConfigLoader.getUsername(), 
                    DbConfigLoader.getPassword());
        } catch (Exception e) {
            System.out.println("Error connecting to Report DB: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Centralized Resource Closing (Safe Handling)
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { /* Ignore */ }
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) { /* Ignore */ }
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) { /* Ignore */ }
        }
    }

    // ==========================================================================
    // BUSINESS METHODS
    // ==========================================================================

    public static String checkCustomerNRCFormat(String state, String township) {
        String strValue = "000|0"; 
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            if (conn == null) return "001|DB Connection Failed";

            // Use PreparedStatement to prevent SQL Injection
            String sql = "SELECT COUNT(*) FROM NRCTRANSLATION WHERE STATE = ? AND TOWNSHIP = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, state);
            pstmt.setString(2, township);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                strValue = "000|" + rs.getString(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "001|" + ex.getMessage().toLowerCase();
        } finally {
            close(conn, pstmt, rs);
        }
        return strValue;
    }

    public static String checkExistingCustomerNRC(String customerNrc) {
        String strValue = "001";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getReportingConnection();
            if (conn == null) return "002|DB Connection Failed";

            String sql = "SELECT TOP 1 CUSTOMER_NO,SHORT_NAME,LEGAL_ID,LEGAL_DOC_NAME " +
                         "FROM T24_FBNK_CUSTOMER WHERE LEGAL_ID = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, customerNrc);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                strValue = "000|" + rs.getString(1) + '*' + rs.getString(2) + '*' + rs.getString(3) + '*' + rs.getString(4);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "002|" + ex.getMessage().toLowerCase();
        } finally {
            close(conn, pstmt, rs);
        }
        return strValue;
    }

    public static String t24CheckExistingNrcWithCustId(String customerNrc) {
        String strValue = "001";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            if (customerNrc == null || customerNrc.indexOf('|') == -1) {
                return "002|Invalid input format";
            }
            String[] parts = customerNrc.split("\\|");
            String nrcId = parts[0];
            String custNo = parts[1];

            conn = getReportingConnection();
            if (conn == null) return "002|DB Connection Failed";
            
            String sql = "SELECT TOP 1 C.CUSTOMER_NO, C2.SHORT_NAME, C.LEGAL_ID, C.LEGAL_DOC_NAME " +
                         "FROM MCB_CUS_DOC_INFO C, MCB_CUSTOMER C2 " +
                         "WHERE C.CUSTOMER_NO = C2.CUSTOMER_NO " +
                         "AND C.LEGAL_DOC_NAME = 'NATIONAL.ID' " +
                         "AND LEFT(CUSTOMER_STATUS,2) <> '51' " +
                         "AND C.LEGAL_ID = ? AND C.CUSTOMER_NO <> ?";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nrcId);
            pstmt.setString(2, custNo);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                strValue = "000|" + rs.getString(1) + '*' + rs.getString(2) + '*' + rs.getString(3) + '*' + rs.getString(4);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return "002|" + ex.getMessage().toLowerCase();
        } finally {
            close(conn, pstmt, rs);
        }
        return strValue;
    }

    public static String checkBlackListCustomer(String customerNrc) {
        String strValue = "001";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            if (conn == null) return "002|DB Connection Failed";

            if (customerNrc != null && !customerNrc.trim().equals("")) {
                String sql = "SELECT TOP 1 [ID],[Branch],[Name],[FullName],[DOB],[LegalIdType],[LegalIdNumber],[EntryDate],[Reason], " +
                      "CASE [Active] WHEN 1 THEN 'ACTIVE' ELSE 'INAU' END 'Active',[Inputter],[Authorizer],[Creationtimestamp] " +
                      "FROM [EB.BLACKLIST.CUSTOMER] WHERE LEGALIDNUMBER = ? AND ACTIVE='1' ORDER BY Creationtimestamp DESC";
                
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, customerNrc);
            } else {
                return ""; 
            }

            rs = pstmt.executeQuery();

            if (rs.next()) {
                strValue = "000|" + rs.getString(1) + '*' + rs.getString(2) + '*' + rs.getString(3) + '*' + rs.getString(4) + 
                           '*' + rs.getString(5) + '*' + rs.getString(6) + '*' + rs.getString(7) + '*' + rs.getString(8) + 
                           '*' + rs.getString(9) + '*' + rs.getString(10) + '*' + rs.getString(11) + '*' + rs.getString(12) + 
                           '*' + rs.getString(13) + '^';
            }

        } catch (Exception ex) {
            return "002|" + ex.getMessage().toLowerCase();
        } finally {
            close(conn, pstmt, rs);
        }
        return strValue;
    }

    public static String t24CheckDuplicateNrcWithCustID(String customerNrc) {
        String strValue = "001";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            if (customerNrc == null || customerNrc.indexOf('|') == -1) {
                return "002|Invalid input format";
            }
            String[] parts = customerNrc.split("\\|");
            
            conn = getReportingConnection();
            if (conn == null) return "002|DB Connection Failed";

            String sql = "SELECT C.CUSTOMER_NO FROM MCB_CUS_DOC_INFO C, MCB_CUSTOMER C2 " +
                         "WHERE C.CUSTOMER_NO = C2.CUSTOMER_NO " +
                         "AND C.LEGAL_DOC_NAME = 'NATIONAL.ID' " +
                         "AND LEFT(CUSTOMER_STATUS,2) <> '51' " +
                         "AND C.LEGAL_ID = ? AND C.CUSTOMER_NO <> ?";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, parts[0]);
            pstmt.setString(2, parts[1]);

            rs = pstmt.executeQuery();

            int c = 0;
            StringBuilder sb = new StringBuilder();
            
            while (rs.next()) {
                if (c == 0) {
                    sb.append("000|");
                    c++;
                }
                sb.append(rs.getString(1)).append('*');
            }
            
            if (c > 0) {
                strValue = sb.toString();
            }

        } catch (Exception ex) {
            return "002|" + ex.getMessage().toLowerCase();
        } finally {
            close(conn, pstmt, rs);
        }
        return strValue;
    }

    public static String t24STOCheckDuplicateNrcWithCustID(String customerNrc) {
        String strValue = "001";
        Connection conn = null;
        CallableStatement cs = null;
        ResultSet rs = null;

        try {
            if (customerNrc == null || customerNrc.indexOf('|') == -1) {
                return "002|Invalid input format";
            }
            String[] parts = customerNrc.split("\\|");

            conn = getReportingConnection();
            if (conn == null) return "002|DB Connection Failed";

            String sql = "{call dbo.sp_getCust_with_NRC(?,?)}";
            
            cs = conn.prepareCall(sql);
            cs.setString(1, parts[0]);
            cs.setString(2, parts[1]);
            
            rs = cs.executeQuery();

            int c = 0;
            StringBuilder sb = new StringBuilder();

            while (rs.next()) {
                if (c == 0) {
                    sb.append("000|");
                    c++;
                }
                sb.append(rs.getString(1)).append('*');
            }

             if (c > 0) {
                strValue = sb.toString();
            }

        } catch (Exception ex) {
            return "002|" + ex.getMessage().toLowerCase();
        } finally {
            close(conn, cs, rs);
        }
        return strValue;
    }
}