package me.sisko.partygames.minigames;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

/*
Class representing a Minigame

Some events are declared as abstract because they *must* be implemented by every minigame, but minigames
are free to add additional helper functions and events as needed
*/
public abstract class Minigame implements Listener {

    public abstract String getName();
    public abstract String getDescription();
    public abstract String getMap();

    // verify a json object contains all needed parameters
    public abstract boolean jsonValid(JSONObject json);

    // set up the minigame, using parameters passed as a json object
    // only needs to be called once
    public abstract void setup(JSONObject json);

    // construct the map, get everything prepared, etc before 
    public abstract void initialize();

    // add all players to the minigame and start it
    public abstract void start(final List<Player> players);

    // garbage collection etc
    public abstract void cleanup();

    @EventHandler
    public abstract void onJoin(PlayerJoinEvent e);

    @EventHandler
    public abstract void onLeave(PlayerQuitEvent e);
}
