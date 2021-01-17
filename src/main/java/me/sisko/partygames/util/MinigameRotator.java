package me.sisko.partygames.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;


public class MinigameRotator {
    public static final int MIN_PLAYERS = 2;
    public static final int GAMES_PER_ROTATION = 6;

    private static boolean rotating = false;
    private static long startTime = 0;
    private static Leaderboard leaderboard;
    private static List<String> minigames;
    private static int currentMinigame;
    private static BukkitRunnable startGameRunnable = null;

    public static void onJoin(Player p) {
        if (Bukkit.getOnlinePlayers().size() >= MIN_PLAYERS
                && MinigameManager.getGameState().equals(MinigameManager.GameState.NOGAME)) {

            if(startGameRunnable == null) {
                startGameRunnable = new BukkitRunnable(){
                    @Override
                    public void run() {
                        startRotation();
                    }
                };

                startGameRunnable.runTaskLater(Main.getPlugin(), 20 * 30);
                startTime = System.currentTimeMillis() + 30 * 1000;
            }


        } else if (MinigameManager.getGameState().equals(MinigameManager.GameState.NOGAME)) {
            ChatSender.tell(p, "Welcome to party games! The games will begin after " + MIN_PLAYERS + " players join.");
        // this means they are in game
        } else {
            if(!leaderboard.contains(p)) leaderboard.addPlayer(p);
        }
    }

    public static void onLeave(Player p) {
        if(leaderboard != null && leaderboard.contains(p)) leaderboard.removePlayer(p);

        // calculate leave players after they have all left
        // subtract a player to account for the one who left
        if (Bukkit.getOnlinePlayers().size()-1 < MIN_PLAYERS) {
            // minigame state checked by minigame manager

            MinigameManager.forceEndGame();
    
            if(startGameRunnable != null) {
                startGameRunnable.cancel();
            }
            startGameRunnable = null;
            rotating = false;
        }
    }

    public static void startRotation() {

        // make a deep copy
        minigames = new ArrayList<String>();
        minigames.addAll(MinigameManager.getTypes());
        Collections.shuffle(minigames, new Random());
        minigames = minigames.subList(0, GAMES_PER_ROTATION);

        leaderboard = new Leaderboard(Bukkit.getOnlinePlayers().stream().collect(Collectors.toList()));
        
        currentMinigame = 0;
        MinigameManager.playGame(minigames.get(currentMinigame));
        rotating = true;
    }

    public static void forceStartRotation(List<String> games) {

        // make a deep copy
        minigames = new ArrayList<String>();
        minigames.addAll(games);
        leaderboard = new Leaderboard(Bukkit.getOnlinePlayers().stream().collect(Collectors.toList()));
        
        if(startGameRunnable != null) {
            startGameRunnable.cancel();
        }
        
        currentMinigame = 0;
        MinigameManager.playGame(minigames.get(currentMinigame));
        rotating = true;
    }

    public static void updatePoints(final List<Player> winners) {
        final int size = winners.size();
        if(size > 0) leaderboard.changeScore(winners.get(0), 3);
        if(size > 1) leaderboard.changeScore(winners.get(1), 2);
        if(size > 2) leaderboard.changeScore(winners.get(2), 1);
    }
    

    public static void gameComplete() {
        if(!MinigameManager.getGameState().equals(MinigameManager.GameState.NOGAME)) {
            Main.getPlugin().getLogger().warning("gameComplete() called when game is still in progress!");
            return;
        }

        for(Player p : Bukkit.getOnlinePlayers()) {
            if(!leaderboard.contains(p)) leaderboard.addPlayer(p);
        }

        currentMinigame++;
        if(currentMinigame < minigames.size() && Bukkit.getOnlinePlayers().size() >= MIN_PLAYERS) {
            MinigameManager.playGame(minigames.get(currentMinigame));

        // either this is the last minigame
        // or there are not enough players
        } else {

            rotating = false;
            FileConfiguration config = Main.getPlugin().getConfig();

            for(Player p : Bukkit.getOnlinePlayers()) {

                p.teleport(new Location(Main.getWorld(), config.getDouble("spawn.x"), 
                    config.getDouble("spawn.y"), config.getDouble("spawn.z"), (float) config.getDouble("spawn.yaw"),
                    (float) config.getDouble("spawn.pitch")));
            }


            ChatSender.broadcastOVerallWinners(leaderboard);

            // start the rotation for the next game
            // must check for min players, because it is possible for the 
            // game end to be triggered by leaving players

            // this is also where leaderboard calculations should take place
            // because leaderboards are only updated when a rotation finishes with all players
            if (Bukkit.getOnlinePlayers().size() >= MIN_PLAYERS) {
                //if(startGameRunnable != null) startGameRunnable.cancel();
                startTime = System.currentTimeMillis() + 30 * 1000;
                startGameRunnable = new BukkitRunnable(){
                    @Override
                    public void run() {
                        startRotation();
                    }
                };
            
                startGameRunnable.runTaskLater(Main.getPlugin(), 20 * 30);
            } else {
                ChatSender.broadcast("The games were ended early because there are fewer than " + MIN_PLAYERS + " online");
            }
        }
    }

    public static List<String> getScoreboardLines(Player p) {
        List<String> retVal = new ArrayList<String>();

        // if(!MinigameManager.getGameState().equals(GameState.NOGAME)) {
        //     return retVal;
        // }

        final long diff = 1000 + startTime - System.currentTimeMillis();
        final int seconds = (int) Math.floor((diff%60000) / 1000);
        final String seconds_str = seconds < 10 ? "0" + seconds : "" + seconds;

        if(!rotating) {
            if (Bukkit.getOnlinePlayers().size() < MIN_PLAYERS) {
                retVal.add("&bPlayers needed to start: &f" + Bukkit.getOnlinePlayers().size() + "/" + MIN_PLAYERS);
            } else {
                retVal.add("&bGames start: &f" + (int) Math.floor(diff/60000.) + ":" + seconds_str);
            }
        } else if(!MinigameManager.getGameState().equals(GameState.INGAME)) {
            retVal.add("&bGame: &f" + (currentMinigame+1) + "/" + GAMES_PER_ROTATION);

            // pre game or post game
            if(leaderboard.contains(p)) {
                retVal.add("&bYou are in &f" + leaderboard.getPlace(p) + "&b place");
            } else {
                retVal.add("&bYou are spectating");
            }
            retVal.add("&b&nOverall Leaderboard");
    
            for(final PlayerScore score : leaderboard.getLeaderboard()) {
                retVal.add("&a" + score.getPlayer().getDisplayName() + "&r&b: &f" + score.getScore());
            }
    
        }
        // if in game, return an empty list
        return retVal;
    }
}

