package dr.redditstats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Comunication {

    //Settings
    public String str_loginURL  = "https://ssl.reddit.com/api/login";
    public String str_modLogURL = "http://www.reddit.com/r/DebateReligion/about/log/.json";
    public String str_infoURL   = "http://www.reddit.com/api/info.json";
    public String userAgent     = "DR_Mod_Log_Stats_Retrieval_Agent";
    
    public String user          = "userName";
    public String pwd           = "hunter2";
    
    // Init stuff
    public String cookie        = null;
    public String modhash       = null;
    public String after         = null;
    public int count            = 0;

    // Time constants, in seconds
    public static final long aMinute    = 60;
    public static final long anHour     = aMinute * 60;
    public static final long aDay       = anHour * 24;
    public static final long aWeek      = aDay * 7;
    public static final long aMonth     = aDay * 30;

    // Time variables, in seconds
    public long timeSpan        = aMonth * 1; // IS A SETTING(too high won't help because of limits)
    public long currentTime     = (System.currentTimeMillis() / 1000);
    public long startingTime    = currentTime  - timeSpan;

    // Declarations
    public Map<String, Item> things;
    public Map<String, String> mapThingToAuthor;
    public Map<String, User> users;
    public List<String> thingnames;
    public List<String> buffThingnames;

    // No arguments, uses object variables (wrappers)
    
    public void login() throws IOException {
        URL u = new URL(str_loginURL);
        login(u, user, pwd);
    }

    public String modLog() throws IOException {
        URL u = new URL(str_modLogURL);
        return modLog(u);
    }

    public String getInfo() throws IOException {
        return getInfo(thingnames);
    }

    public String getBuffInfo() throws IOException {
        return getInfo(buffThingnames);
    }

    // To use with data out of the object
    
    public String login(URL url, String user, String pw) throws IOException {
        
        String data = "api_type=json&rem=False&user=" + user + "&passwd=" + pw;
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setRequestProperty("Content-Length", String.valueOf(data.length()));

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(data);
        wr.flush();
        wr.close();
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder sbResponse = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            sbResponse.append(line).append('\n');
        }

        String rCookie = connection.getHeaderField("set-cookie");
        rd.close();

        String response = sbResponse.toString();
        ObjectMapper om = new ObjectMapper();
        JsonNode obj    = om.readTree(response);
        obj             = obj.get("json").get("data");
        JsonNode modh   = obj.get("modhash");
        JsonNode cooki  = obj.get("cookie");
        modhash         = modh.textValue();
        cookie          = cooki.textValue();
        
        System.out.println("Cookie: "+cookie);
        System.out.println("Modhash: "+modhash);
        return rCookie;

    }

    public String modLog(URL url) throws IOException {

        HttpURLConnection connection = null;
        Set<Entry< String, List< String>>> header_fields;

        String newurl = str_modLogURL + "?limit=500";
        if (after != null) {
            newurl = newurl + "&after=" + after;
        }
        url = new URL(newurl);

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.addRequestProperty("Cookie", "reddit_session=" + cookie);
        connection.setRequestProperty("X-Modhash", modhash);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "dr_test testing");
        connection.setDoInput(true);
        connection.setDoOutput(false);

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder sbResponse = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            sbResponse.append(line).append('\n');
        }
        rd.close();

        String response = sbResponse.toString();
        return response;
    }

    public String getInfo(List<String> fullnames) throws IOException {
        if (fullnames.size() < 1) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fullnames.size() - 1; i++) {
            sb.append(fullnames.get(i)).append(',');
        }
        if (fullnames.size() > 1) {
            sb.append(fullnames.get(fullnames.size() - 1));
        }
        String ids = sb.toString();
        Set<Entry< String, List< String>>> header_fields;
        URL url = new URL(str_infoURL + "?id=" + ids);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.addRequestProperty("Cookie", "reddit_session=" + cookie);
        connection.setRequestProperty("X-Modhash", modhash);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "dr_test testing");
        connection.setDoInput(true);
        connection.setDoOutput(false);

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder sbResponse = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            sbResponse.append(line).append('\n');
        }
        rd.close();

        String response = sbResponse.toString();

        return response;
    }

}
