package me.sisko.partygames.minigames;

import java.util.List;

import org.bukkit.entity.Player;
import org.json.JSONObject;

public class MazeMinigame extends Minigame {

    private Location spawn;

    private List<Player> winners;

    @Override
    public boolean jsonValid(JSONObject json) {
        // TODO Auto-generated method stub
        final String[] keys = {"name", "description", "map", "spawn", "end_x", "end_z"};
        for(final String key : keys) {
            if(!json.has(key)) return false;
        }
        return true;
    }

    @Override
    public void setup(JSONObject json) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initialize() {
        // TODO Auto-generated method stub

    }

    @Override
    public void prestart(List<Player> players) {
        // TODO Auto-generated method stub
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void postgame() {
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Player> timeout() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addPlayer(Player p) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePlayer(Player p) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<String> getScoreboardLines(Player p) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
