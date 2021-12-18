package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;

public class KOTHMinigame extends Minigame {

    private int winningScore;

    private List<Location> spawns;
    private Set<Player> onHill;
    private Location spectatorSpawn;

    private Leaderboard hillTime;
    private BukkitRunnable incrementHillTime;

    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawns", "spectator_spawn", "win_time" };
        for (final String key : keys) {
            if (!json.has(key))
                return false;
        }
        return true;
    }

    @Override
    public void setup(final JSONObject json) {

        name = json.getString("name");
        description = json.getString("description");
        map = json.getString("map");

        Main.getPlugin().getLogger().info("Setting up a KOTH map " + map);

        spawns = new ArrayList<Location>();
        for(final Object spawn : json.getJSONArray("spawns")) {
            JSONObject spawnJson = (JSONObject) spawn;

            spawns.add(new Location(Main.getWorld(), spawnJson.getDouble("x"),
                spawnJson.getDouble("y"), spawnJson.getDouble("z"),
                spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch")));
        }

        if(spawns.size() < 8) {
            Main.getPlugin().getLogger().warning("Fewer than 8 spawns in KOTH  map!");
        }

        winningScore = json.getInt("win_time")*20;

        JSONObject specJson = json.getJSONObject("spectator_spawn");
        spectatorSpawn = new Location(Main.getWorld(), specJson.getDouble("x"),
            specJson.getDouble("y"), specJson.getDouble("z"),
            specJson.getFloat("yaw"), specJson.getFloat("pitch"));

        onHill = new HashSet<Player>();

    }

    @Override
    public void initialize() {
        //winners = new ArrayList<Player>();

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {
        // leaderboard counting how many kills each player has
        hillTime = new Leaderboard(players); 


        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            p.teleport(spectatorSpawn);
        }

        incrementHillTime = new BukkitRunnable() {
            @Override
            public void run() {
                boolean completeFlag = false;
                for(Player p : MinigameManager.getIngamePlayers()) {
                    if (onHill.contains(p)) {
                        hillTime.changeScore(p, 1);

                        int score = hillTime.getScore(p);
                        if(score % 10 == 0) {
                            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, SoundCategory.BLOCKS, 1f, 1f);
                        }
                        if(hillTime.getScore(p) >= winningScore) {
                            completeFlag = true;
                        }
                    }
                }
                if(completeFlag) {
                    MinigameManager.gameComplete(hillTime.getWinners());
                    incrementHillTime.cancel();
                }
            }
        };

        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        for(int i = 0; i < MinigameManager.getIngamePlayers().size(); i++) {
            final Player p = MinigameManager.getIngamePlayers().get(i);
            p.teleport(spawns.get(i % spawns.size()));
        }
        incrementHillTime.runTaskTimer(Main.getPlugin(), 20l, 1l);
    }

    @Override
    public void postgame() {
        incrementHillTime.cancel();
    }

    @Override
    public void cleanup() {
        incrementHillTime.cancel();
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setFlying(false);
            p.setCollidable(true);
            p.setAllowFlight(false);
            p.setInvisible(false);
            p.getInventory().clear();
            p.setHealth(20);
        }
        //winners.clear();
    }

    @Override
    public final List<Player> timeout() {
        //winners.addAll(MinigameManager.getIngamePlayers());
        //Collections.reverse(winners);
        return hillTime.getWinners();
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(spectatorSpawn);
        p.setInvisible(true);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setCollidable(false);
        p.getInventory().clear();
        p.setHealth(20);
        p.setFireTicks(0);
    }

    @Override
    public void removePlayer(Player p) {
        p.setFlying(false);
        p.setCollidable(true);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.getInventory().clear();
        p.setHealth(20);

        if(MinigameManager.getNumberInGame() <= 1 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            MinigameManager.gameComplete(hillTime.getWinners());
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            final Player damager = (Player) e.getDamager();
            final Player damagee = (Player) e.getEntity();

            if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damager) && MinigameManager.isInGame(damagee)) {
                e.setCancelled(false);
                e.setDamage(0);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(e.getTo().clone().add(0, -0.5, 0).getBlock().getType().equals(Material.RED_WOOL)) {
            onHill.add(e.getPlayer());
        } else if(onHill.contains(e.getPlayer())) {
            onHill.remove(e.getPlayer());
        }
    }

    @Override
    public final List<String> getScoreboardLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(MinigameManager.isInGame(p)) {
            if(onHill.contains(p)) {
                retVal.add("&bYou are &aon the hill");
            } else {
                retVal.add("&bYou are &coff the hill");
            }
            
        } else {
            retVal.add("&bYou are &cspectating");
        }
        retVal.add("&b&nKOTH Leaderboard");
        for(PlayerScore score : hillTime.getLeaderboard()) {
            retVal.add("&a" + score.getPlayer().getDisplayName() + "&r&b: &f" + String.format("%,.1f", score.getScore()/20d) );
        }

        return retVal;
    }

}