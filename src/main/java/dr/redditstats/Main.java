package dr.redditstats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        try {
            // Settings
            int max_retrievals  = 3500; // Getting a code 500 if this number is higher, reddit's fault
            int subsPerReq      = 64;   // Can be used to diminish amount of requests/speed up, hasn't been optimized
            
            // Init stuff (careful there)
            int limitcounter    = 0;
            int total           = 0;
            Comunication co     = new Comunication();
            long lastTime       = co.currentTime;
            co.mapThingToAuthor = new HashMap<>();
            co.users            = new HashMap<>();
            co.thingnames       = new ArrayList<>();
            co.things           = new HashMap<>();
            String last         = null;
            String json_modlog  = null;
            ObjectMapper om     = null;
            JsonNode root       = null;
            JsonNode thingArr   = null;
            Iterator it         = null;
            
            System.out.println("Starting time:"+lastTime + "; Current time :"+System.currentTimeMillis()+";");

            co.login();
            // Play nice with the API guidelines, limit request frequency to at most 1 per 2 seconds.
            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

            while (lastTime > co.startingTime && limitcounter < max_retrievals) {
                System.out.println("Retrieving mod log...");
                json_modlog = co.modLog();
                
                om          = new ObjectMapper();
                root        = om.readTree(json_modlog);
                thingArr    = root.get("data").get("children");
                it          = thingArr.elements();

                while (it.hasNext()) {
                    limitcounter++;
                    JsonNode thing  = (JsonNode) it.next();
                    thing           = thing.get("data");
                    long created    = (long) thing.get("created_utc").asDouble();
                    Item item       = new Item();
                    item.fullname   = thing.get("target_fullname").asText();
                    item.approved   = thing.get("action").asText().equals("approvecomment") || thing.get("action").asText().equals("approvelink");
                    item.removed    = thing.get("action").asText().equals("removecomment") || thing.get("action").asText().equals("removelink");
                    item.created    = created;

                    if (created > co.startingTime) {
                        
                        if ((item.approved || item.removed) ) {
                            
                            if (!co.thingnames.contains(item.fullname)) {
                                co.thingnames.add(item.fullname);
                            }
                            Item itemToAdd = co.things.get(item.fullname);
                            
                            if (itemToAdd == null) itemToAdd = item;
                            else {
                                if (item.created > itemToAdd.created) {
                                    itemToAdd = item;
                                }
                            }
                            co.things.put(itemToAdd.fullname, itemToAdd);
                        }
                    }

                    last = item.fullname;
                    lastTime = created;

                }
                System.out.println("Retrieved so far "+limitcounter+" log entries.");
                co.count = limitcounter;
                co.after = root.get("data").get("after").asText();

                // Play nice with the API guidelines, limit request frequency to at most 1 per 2 seconds.
                try {
                    Thread.sleep(1000 * 2);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            int pos = 0;
            while (pos < co.thingnames.size()) {
                System.out.println("Retrieving submissions...");
                // Buff and positioning the count pointer and fetch results
                co.buffThingnames = new ArrayList<>();
                int stop = (pos + subsPerReq < co.thingnames.size()) ? 
                                                pos + subsPerReq : co.thingnames.size();
                for (int i = pos; i < stop; i++) { co.buffThingnames.add(co.thingnames.get(i)); }
                pos = stop;
                
                // Actually do stuff with the results
                String result   = co.getBuffInfo();
                om              = new ObjectMapper();
                root            = om.readTree(result);
                thingArr        = root.get("data").get("children");
                it              = thingArr.elements();
                while (it.hasNext()) {
                    JsonNode thing = (JsonNode) it.next();
                    thing = thing.get("data");
                    String author = thing.get("author").asText();
                    String fullname = thing.get("name").asText();
                    co.mapThingToAuthor.put(fullname, author);
                }
                System.out.println("Retrieved "+(pos)+" out of "+co.thingnames.size()+" submissions.");
                // Play nice with the API guidelines, limit request frequency to at most 1 per 2 seconds.
                try {
                    Thread.sleep(2 * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            // Add users and stats to them
            for (String k : co.mapThingToAuthor.keySet()) {
                String author = co.mapThingToAuthor.get(k);
                User u = co.users.get(author);
                // Create if user does not exist
                if (u == null) {
                    u = new User();
                    u.username = author;
                    co.users.put(author, u);
                }
                u.reported += 1;
                u.things.add(k);
                if (co.things.get(k).removed) {
                    u.removed += 1;
                } else if (co.things.get(k).approved) {
                    u.approved += 1;
                }
            }
            
            // Print the whole thing to the stdout in the markdown table format
            System.out.println("User | Reported | Approved | Removed");
            System.out.println("--- | :-: | :-: | :-:");
            for (String k : co.users.keySet()) {
                User u = co.users.get(k);
                String toPrint = u.username + " | " + u.reported + " | " + u.approved + " | " + u.removed;
                total += u.reported;
                System.out.println(toPrint); // You can filter out which users to print here
            }
            // Totals
            System.out.println("Accepted " + co.thingnames.size() + "/" + total + " out of " + limitcounter + " entries.");

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
