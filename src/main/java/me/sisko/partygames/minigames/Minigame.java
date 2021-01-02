package me.sisko.partygames.minigames;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.json.JSONObject;

/*
Class representing a Minigame

Some events are declared as abstract because they *must* be implemented by every minigame, but minigames
are free to add additional helper functions and events as needed
*/
public abstract class Minigame implements Listener {

    // must be set properly during setup()
    protected String name;
    protected String description;
    protected String map;

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final String getMap() {
        return map;
    }

    /*
    time before the minigame manager will force end the minigame
    used to prevent an afk player or impossible game from soft-locking
    the server
    override this method to change the default timeout period
    */
    public final long getTimeoutTime() {
        return 20*60*5;
    }

    // verify a json object contains all needed parameters
    public abstract boolean jsonValid(final JSONObject json);

    // set up the minigame, using parameters passed as a json object
    // only needs to be called once
    public abstract void setup(JSONObject json);

    // construct the map, get everything prepared, etc before game starts
    // called once per minigame, must call MinigameManager.initializationComplete()
    // exactly once
    public abstract void initialize();

    // add all players to the minigame but don't let them do anything yet
    public abstract void prestart(final List<Player> players);

    // start the minigame, allowing players to take action
    public abstract void start();

    // called after the game is complete, but before the next game is starting
    // players should no longer be able to take action. Do not teleport
    // players away; this will be done by the next minigame's prestart
    // DO NOT clear the winners list, this will screw up scoringh
    // clear winners list in cleanup() instead
    public abstract void postgame();

    // garbage collection etc
    public abstract void cleanup();

    // called when a game runs out of time, must return a list of current winners
    // postgame() will be called immidiately after this is called
    public abstract List<Player> timeout();

    // called when someone joins during a game
    // this can happen during any stage of the
    // game, so be prepared
    public abstract void addPlayer(Player p);

    // valled when someone leaves a game
    // this can happen during any stage of the
    // game, so be prepared
    public abstract void removePlayer(Player p);

    // return up to 10 lines to be displayed on the scoreboard along with other info
    public abstract List<String> getScoreboardLinesLines(Player p);
}
