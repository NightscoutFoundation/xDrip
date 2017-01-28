
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;


import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;

class NewCalibration {
    double bgValue; // Always in mgdl
    long timestamp;
    long offset;
    String uuid;
}


public class SendCalibrations {

    private final String USER_AGENT = "Mozilla/5.0";
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    // key1 is hard coded in xDrip, no need to change it.
    final protected static String key1 = "ebe5c0df162a50ba232d2d721ea8e3e1c5423bb0-12bd-48c3-8932-c93883dfcf1f";
    final protected static String baseUrl = "https://xdrip-plus-updates.appspot.com/xdrip-plus-send-topic/";
    static final byte[] errorbyte = {};

    
    // Send a calibration to xDrip
    public static void main(String[] args) throws Exception {

        SendCalibrations http = new SendCalibrations();

        // Per user key, make sure to change it according to your system!!!
        String key = "FDED3FA8DE0463285661EE1AB95A7E29";
        String toppic = createToppic(key);
        
        NewCalibration newCalibration = readData();
               
        System.out.println("Testing 1 - Send Http GET request");
        String action = "cal2";

        String url = baseUrl+ 
                createToppic(key) +"/" + action + "?payload=" + encryptString(createCalibrationString(newCalibration), key);
        http.sendGet(url);
    }
    
    public static NewCalibration readData() {
        NewCalibration newCalibration = new NewCalibration();
        System.out.println("enter calibration data: time bg \nFor example 27/11/2016 00:30 120");
        String input = System.console().readLine();
        
        String[] splited = input.split("\\s+");
        if (splited.length != 3) {
            System.err.println("Wrong input.");
            System.exit(1);
        }
        java.text.DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date =null;
        try {
            date = df.parse(splited[0] + " " + splited[1]);
        } catch (ParseException e) {
            System.err.println("Error parsing date/time");
            System.exit(2);
        }
        newCalibration.timestamp = date.getTime();
        newCalibration.bgValue = Integer.parseInt(splited[2]);
        System.out.println("Time = " + df.format(new Date(newCalibration.timestamp)) +" bg = "+ newCalibration.bgValue );
        System.out.println("is this correct (y/N)");
        input = System.console().readLine();
        if(!input.equals("y")) {
            System.out.println("Wrong input "+ input);
            System.exit(3);
        }
        newCalibration.uuid = UUID.randomUUID().toString();
        
        System.out.println("Time is " + newCalibration.timestamp);
        return newCalibration;
        
    }
    

    public static String createCalibrationString( NewCalibration newCalibration) {
        Gson gson = new Gson();
        
        NewCalibration nca[] = new NewCalibration[1];
        nca[0] = newCalibration;
        
        System.out.println("json string is" + gson.toJson(nca));
        return gson.toJson(nca);
    }
    
    
    
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public static String getSHA256(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA-256");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            System.err.println( "SHA hash exception: " + e.toString());
            return null;
        }
    }
    
    
    public static String getMD5(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("MD5 hash exception: " + e.toString());
            return null;
        }
    }

    static private String getDriveKeyString(String customkey) {

        String ourFolderResourceKeyHash = getMD5(customkey);
        return ourFolderResourceKeyHash;
    }
    
    public static byte[] encryptBytes(byte[] plainText, String key) {
        
        byte[] keyBytes = getKeyBytes(key1 + getDriveKeyString(key));
        return encryptBytes(plainText, keyBytes);
    }
    
    private static byte[] getKeyBytes(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Password creation exception: " + e.toString());
            return errorbyte;
        }
    }
    
    public static byte[] encryptBytes(byte[] plainText, byte[] keyBytes) {
        byte[] ivBytes = new byte[16];

        if ((keyBytes == null) || (keyBytes.length != 16)) {
            System.err.println("Invalid Keybytes length!");
            return errorbyte;
        }
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(ivBytes);
        byte[] cipherData = encrypt(ivBytes, keyBytes, plainText);
        byte[] destination = new byte[cipherData.length + ivBytes.length];
        System.arraycopy(ivBytes, 0, destination, 0, ivBytes.length);
        System.arraycopy(cipherData, 0, destination, ivBytes.length, cipherData.length);
        return destination;
    }
    
    public static byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
            return cipher.doFinal(textBytes);
        } catch (Exception e) {
            System.err.println("Error during encryption: " + e.toString());
            return errorbyte;
        }
    }
    
    
    public static String encryptString(String plainText, String key) {
        byte[] inbytes = plainText.getBytes(Charset.forName("UTF-8"));
        String encoded = Base64.getEncoder().encodeToString(encryptBytes(inbytes, key));
        encoded = encoded.replaceAll("\\+", "%2b");
        return encoded;
    }
    
    private static String createToppic(String customkey) {
        return getSHA256(customkey).substring(0, 32);
    }

    // HTTP GET request
    private void sendGet(String url) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());
    }

}