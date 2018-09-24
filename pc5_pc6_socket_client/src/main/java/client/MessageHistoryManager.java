package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHistoryManager {
    private Map<String, List<String>> userToHistoryMap;

    MessageHistoryManager(){
        userToHistoryMap = new HashMap<>();
    }

    public void put(String tab, List<String> history){
        userToHistoryMap.put(tab, history);
    }

    public List<String> get(String tab){
        return userToHistoryMap.get(tab);
    }

    public void add(String tab, String feed){
        List<String> currentHistory = userToHistoryMap.get(tab);
        if (currentHistory == null){
            currentHistory = new ArrayList<>();
            currentHistory.add(feed);
            this.put(tab, currentHistory);
        }
        else {
            currentHistory.add(feed);
        }
    }

}
