/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package corecommon;

/**
 *
 * @author tskhaing
 */
import DbMgr.DbMgr;
//Write Log

//Added by Nipun
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class CORECOMMON {
    
    public static void main(String[] args) {
    System.out.println(t24CheckCustomerPhone("+959450012554"));
    System.out.println(t24CheckExistingNrc("12/AHLANA(N)017348"));
    System.out.println(t24CheckNrcFormat("13/PALAHTA(C)017343"));
    System.out.println(t24CheckDuplicateNrcWithCustID("12/MABANA(N)133093|213447"));
    System.out.println(t24CheckExistingNrcWithCustId("11/RATHATA(N)079817|213447"));
    }
/*    Commented as updated new NRC Format validation
    public static String t24CheckNrcFormat(String fullNRC) {
        try
        {
            fullNRC = fullNRC.trim();
            if ((fullNRC == null) || (fullNRC.isEmpty())) {                
                return "003|NRC cannot be null";
            }
            String state = null;
            String district = null;
            String naing = null;
            String registerNo = null;

            String[] nrcArr = fullNRC.split("[/()]", 4);
            if (nrcArr.length == 4){
                state = nrcArr[0].toUpperCase();
                district = nrcArr[1].toUpperCase();
                naing = nrcArr[2].toUpperCase();
                registerNo = nrcArr[3].toUpperCase();
                if ((!state.equals("")) && (!district.equals("")) && (!naing.equals("")) && (!registerNo.equals(""))){
                if ((naing.equals("C")) || (naing.equals("AC")) || (naing.equals("NC")) || (naing.equals("V")) || (naing.equals("M")) || (naing.equals("N"))){
                    boolean validStatus =false;
                    if(naing.equals("C") || naing.equals("N")){
                        validStatus = isValid(registerNo);
                    }else{
                        validStatus = true;
                    }
                    if (validStatus == true){
                        String status = DbMgr.checkCustomerNRCFormat(state, district);
                        if (status.split("\\|")[0].equals("000")){
                            //String aaaa = status.split("\\|")[1];
                            if (Integer.parseInt(status.split("\\|")[1]) > 0) {
                            return "000|" + state + "/" + district + "(" + naing + ")" + registerNo;
                            }
                            return "001|NRC [State Number]/[District] format is not invalid!";
                        }
                        return "001|NRC [State Number]/[District] format is not invalid!";
                    }
                    return "001|NRC [RegisterNo] format must be 6 digits!";
                }
                return "001|NRC [NAING/C] format is not invalid!";
                }
                return "002|NRC format is not allowed!";
            }
            return "001|NRC format is not invalid!";
        }
        catch (Exception e)
        {
        e.printStackTrace();

        return "003|" + e.getMessage();
        }
    }

*/

// Newly updated by Nipun
    public static String t24CheckNrcFormat(String fullNRC) {
        try {
            // 1. Basic Null/Empty Check
            if (fullNRC == null || fullNRC.trim().isEmpty()) {
                return "003|NRC cannot be null or empty";
            }
            fullNRC = fullNRC.trim().toUpperCase();
    
            // 2. Regex Pattern for Strict Format Validation
            // Format: [1-2 Digits] / [Text] ( [Type] ) [6 Digits]
            // Group 1: State, Group 2: Township, Group 3: Type, Group 4: Number
            String nrcRegex = "^([0-9]{1,2})/([A-Z]+)\\(([A-Z]+)\\)([0-9]{5,6})$";
            Pattern pattern = Pattern.compile(nrcRegex);
            Matcher matcher = pattern.matcher(fullNRC);
    
            if (!matcher.matches()) {
                // This covers missing brackets, wrong slashes, or non-digit register numbers
                return "001|NRC format is invalid! Expected format: 12/KAMAYA(C)123456";
            }
    
            // 3. Extract Components
            String state = matcher.group(1);
            String district = matcher.group(2); // Township Code
            String naing = matcher.group(3);    // Citizenship Type
            String registerNo = matcher.group(4);
    
            // 4. Validate Citizenship Type (Allowed list)
//            List<String> allowedTypes = Arrays.asList("C", "AC", "NC", "V", "M", "N");
            // Fetch list from the external config loader
            List<String> allowedTypes = NrcConfigLoader.getAllowedTypes();
        
//            if (!allowedTypes.contains(naing)) {
//                return "001|NRC Type [" + naing + "] is invalid!";
//            }

            if (!allowedTypes.contains(naing)) {
                return "001|NRC Type [" + naing + "] is invalid!";
            } else {
                if("N".equals(naing) || "M".equals(naing)){
                    if (registerNo.length() != 5 && registerNo.length() != 6) {
                        return "001|NRC Type [" + naing + "] must have exactly 5 or 6 digits.";
                    }
                }else {
                    if(registerNo.length() != 6){
                        return "001|NRC Type [" + naing + "] must have exactly 6 digits.";
                    }
                }
            }
    
            // 5. Database Validation (State & Township existence)
            String dbStatus = DbMgr.checkCustomerNRCFormat(state, district);
            String[] statusParts = dbStatus.split("\\|");
    
            // Check if DB call was successful (000)
            if ("000".equals(statusParts[0])) {
                // Safely parse the count
                int count = 0;
                try {
                    if (statusParts.length > 1) {
                        count = Integer.parseInt(statusParts[1]);
                    }
                } catch (NumberFormatException e) {
                    // Log error if needed, treat as 0 or handle gracefully
                    return "003|System Error: Invalid DB response format";
                }
    
                if (count > 0) {
                    // Return clean, standardized format
                    return "000|" + state + "/" + district + "(" + naing + ")" + registerNo;
                } else {
                    return "001|NRC State/Township code does not match!";
                }
            }
    
            // Fallback for DB errors
            return "001|NRC State/Township verification failed!";
    
        } catch (Exception e) {
            // ideally use a Logger here: logger.error("NRC Validation Error", e);
            e.printStackTrace(); 
            return "003|" + e.getMessage();
        }
    }
    
    public static boolean isValid(String s) {
        return s.matches( "\\d{5}" ); // must be 4 digits long
    }     
    
    public static String t24CheckExistingNrcWithCustId(String customerNrc){
        try {
        String status = DbMgr.t24CheckExistingNrcWithCustId(customerNrc);
        return status;
      } catch (Exception e) {
        e.printStackTrace();
        return "000|" + e.getMessage();
      } 
    }
    
    public static String t24CheckExistingNrc(String customerNrc) {
    try {
      String status = DbMgr.checkExistingCustomerNRC(customerNrc);
      return status;
    } catch (Exception e) {
      e.printStackTrace();
      return "000|" + e.getMessage();
    } 
  }
    
    public static String t24CheckBlackListCustomer(String nrc) {
        String status="";
        try
        {
            nrc = nrc.trim();
            if ((nrc == null) || (nrc.isEmpty())) {
                return "003|NRC cannot be null";
            }else{
                status= DbMgr.checkBlackListCustomer(nrc.toUpperCase());
            }
             return status;            
        }
        catch (Exception e)
        {
        e.printStackTrace();

        return "003|" + e.getMessage();
        }
    }
    
    public static String t24CheckCustomerPhone(String mobileNo){
        try{
            
            if (mobileNo.substring(0,1).equals("0"))
                mobileNo="95"+ mobileNo.substring(1);
            else if (mobileNo.substring(0,2).equals("95"))
                mobileNo ="95"+  mobileNo.substring(2);
            if (mobileNo.substring(0,1).equals("+"))
                mobileNo ="95"+  mobileNo.substring(3);        
            String customerSMS="";
            customerSMS =mobileNo.substring(0,2);
            if ((!customerSMS.equals("95")) || mobileNo.length() <= 7){
                return "001|"+mobileNo;
            }
        }catch (Exception e){
            e.printStackTrace();
            return "002|" + e.getMessage();
        }
        return "000|"+mobileNo;
    }
    
    public static String CheckCustomerPhone(String mobileNo){
        try{
            
            if (mobileNo.substring(0,1).equals("0"))
                mobileNo = mobileNo.substring(2);
            else if (mobileNo.substring(0,2).equals("95"))
                mobileNo = mobileNo.substring(3);
            if (mobileNo.substring(0,1).equals("+"))
                mobileNo = mobileNo.substring(4);        
            String customerSMS="";
            customerSMS = mobileNo.substring(0,2);
            if ((!customerSMS.equals("95")) || mobileNo.length() <= 7){
                return mobileNo;
            }
        }catch (Exception e){
            e.printStackTrace();
            return "002|" + e.getMessage();
        }
        return mobileNo;
    }

/*    
    public static String t24CheckDuplicateNrcWithCustID(String customerNrc){
        try {
        //String status = DbMgr.t24CheckDuplicateNrcWithCustID(customerNrc);
        String status = DbMgr.t24STOCheckDuplicateNrcWithCustID(customerNrc);
        return status;
      } catch (Exception e) {
        e.printStackTrace();
        return "000|" + e.getMessage();
      } 
    }
*/    
    public static String t24CheckDuplicateNrcWithCustID(String customerNrc) {
        try {
            // 1. Input Validation
            if (customerNrc == null || customerNrc.trim().isEmpty()) {
                return "003|NRC cannot be null or empty";
            }

            // 2. Trim input to ensure DB query is accurate
            customerNrc = customerNrc.trim(); 

            // 3. Call DB Layer
            // usage of t24STOCheckDuplicateNrcWithCustID implies checking against STO table?
            String status = DbMgr.t24STOCheckDuplicateNrcWithCustID(customerNrc);
        
            return status;

        } catch (Exception e) {
            e.printStackTrace();
        
            return "000|System Error: " + e.getMessage(); 
        } 
    }

//    public static String mcbOraUtilCustNrc(String customerId,String nrc) {
//        String currScn = "";
//        try {
//            Class.forName ("oracle.jdbc.OracleDriver");
//            String dbURL = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(HOST=10.1.128.100)(PROTOCOL=TCP)(PORT=1521))(CONNECT_DATA=(SID=MCB)))";
//            String strUserID = "t24";
//            String strPassword = "t24";
//            Connection myConnection=DriverManager.getConnection(dbURL,strUserID,strPassword);
//            Statement sqlStatement = myConnection.createStatement();
//            String readRecordSQL = "SELECT COUNT(*) AS COUNT FROM V_FBNK_CUSTOMER WHERE RECID = '" + customerId + "' AND LEGAL_ID='" + nrc + "'";
//            ResultSet myResultSet = sqlStatement.executeQuery(readRecordSQL);
//            while (myResultSet.next()) {
//                currScn = myResultSet.getString(1);
//            }
//            myResultSet.close();
//            myConnection.close();
//
//        } catch (Exception e) {
//            System.out.println(e);
//            currScn = e.getMessage();
//        }
//        return currScn;
//    }
}
