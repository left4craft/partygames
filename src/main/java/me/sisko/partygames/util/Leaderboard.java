package me.sisko.partygames.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.entity.Player;

public class Leaderboard {
    private List<PlayerScore> scores;
    private boolean reversed;

    public class PlayerScore {
        private Player player;
        private int score;

        public PlayerScore(final Player player) {
            this.player = player;
            score = 0;
        }

        public void setScore(final int score) {
            this.score = score;
        }

        public void changScore(final int delta) {
            score += delta;
        }

        public final Player getPlayer() {
            return player;
        }

        public final int getScore() {
            return score;
        }
    };

    public Leaderboard(final List<Player> players) {
        scores = new ArrayList<PlayerScore>();
        for(final Player p : players) {
            scores.add(new PlayerScore(p));
        }
        this.reversed = false;
    }

    public Leaderboard(final List<Player> players, boolean reversed) {
        scores = new ArrayList<PlayerScore>();
        for(final Player p : players) {
            scores.add(new PlayerScore(p));
        }
        this.reversed = reversed;
    }

    public Leaderboard(final List<Player> players, boolean reversed, int initialScore) {
        scores = new ArrayList<PlayerScore>();
        for(final Player p : players) {
            scores.add(new PlayerScore(p));
        }
        this.reversed = reversed;

        for(PlayerScore ps : scores) {
            ps.changScore(initialScore);
        }
    }

    public void addPlayer(final Player p) {
        scores.add(new PlayerScore(p));
    }

    public void removePlayer(final Player p) {
        for(int i = 0; i < scores.size(); i++) {
            if(scores.get(i).getPlayer().getUniqueId().equals(p.getUniqueId())) {
                scores.remove(i);
                return;
            }
        }
    }

    public void changeScore(final Player player, final int delta) {
        for(final PlayerScore score : scores) {
            if(score.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                score.changScore(delta);

                // after incrementing, sort the scores
                // order in opposite direction depending on whether scoreboard is reversed
                scores.sort(new Comparator<PlayerScore>(){
                    @Override
                    public int compare(PlayerScore p1, PlayerScore p2) {
                        if(!reversed) return p2.getScore() - p1.getScore();
                        return p1.getScore() - p2.getScore();
                    } 
                });
                return;
            }
        }
    }

    public void setScore(final Player player, final int setScore) {
        for(final PlayerScore score : scores) {
            if(score.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                score.setScore(setScore);

                // after incrementing, sort the scores
                // order in opposite direction depending on whether scoreboard is reversed
                scores.sort(new Comparator<PlayerScore>(){
                    @Override
                    public int compare(PlayerScore p1, PlayerScore p2) {
                        if(!reversed) return p2.getScore() - p1.getScore();
                        return p1.getScore() - p2.getScore();
                    } 
                });
                return;
            }
        }
    }

    public int getScore(final Player player) {
        for(final PlayerScore score : scores) {
            if(score.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                return score.getScore();
            }
        }
        return reversed ? 999 : -999;
    }

    public final List<PlayerScore> getLeaderboard() {
        return scores;
    }

    public final String getPlace(Player p) {
        int place = -2;
        for(int i = 0; i < scores.size(); i++) {
            if(scores.get(i).getPlayer().getUniqueId().equals(p.getUniqueId())) {
                place = i;
                break;
            }
        }
        // otherwise place would be 0-indexed
        place += 1;

        String placeStr = "" + place;

        if(place == 1) {
            placeStr += "st";
        } else if(place == 2) {
            placeStr += "nd";
        } else if (place == 3) {
            placeStr += "rd";
        } else {
            placeStr += "th";
        }

        return placeStr;
    }

    public final boolean contains(Player p) {
        for(final PlayerScore score : scores) {
            if(score.getPlayer().getUniqueId().equals(p.getUniqueId())) return true;
        }
        return false;
    }

    public final List<Player> getWinners() {
        ArrayList<Player> winners = new ArrayList<Player>();
        for(final PlayerScore score : scores) {
            winners.add(score.getPlayer());
        }
        return winners;
    }
}
