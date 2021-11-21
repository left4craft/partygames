package me.sisko.partygames.util;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * This class will be registered through the register-method in the 
 * plugins onEnable-method.
 */
public class ScorePlaceholder extends PlaceholderExpansion  {

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist(){
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister(){
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * <br>For convienience do we return the author from the plugin.yml
     * 
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor(){
        return "Captain_Sisko";
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest 
     * method to obtain a value if a placeholder starts with our 
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier(){
        return "partygames";
    }

    /**
     * This is the version of the expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion(){
        return "1.0";
    }

    /**
     * This is the method called when a placeholder with our identifier 
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.OfflinePlayer OfflinePlayer}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return Possibly-null String of the requested identifier.
     */
    @Override
    public String onRequest(OfflinePlayer player, String identifier){

        if(player == null){
            return "";
        }

        if(identifier.equals("allwins")){
            return "" + Database.getOverallWins(player);
        }

        if(identifier.equals("allpoints")){
            return "" + Database.getOverallPoints(player);
        }
 
        if(identifier.equals("allgames")){
            return "" + Database.getOverallGames(player);
        }

        // points per game
        if(identifier.equals("allppg")){
            return "" + Database.getOverallAveragePoints(player);
        }

        // winrate
        if(identifier.equals("allwr")){
            return "" + Math.round(Database.getOverallWinRate(player)*100.);
        }

        String[] parts = identifier.split("_");

        // Main.getPlugin().getLogger().info(identifier);
        // Main.getPlugin().getLogger().info(parts[0]);
        // Main.getPlugin().getLogger().info(parts[1]);
        // Main.getPlugin().getLogger().info("" + MinigameManager.isValidType(parts[0]));

        if(parts.length < 2 || !MinigameManager.isValidType(parts[0])) return null;

        if(parts[1].equals("wins")) {
            return "" + Database.getWins(parts[0], player);
        }

        if (parts[1].equals("points")) {
            return "" + Database.getPoints(parts[0], player);
        }

        if (parts[1].equals("games")) {
            return "" + Database.getGames(parts[0], player);
        } 

        if (parts[1].equals("ppg")) {
            return "" + Database.getAveragePoints(parts[0], player);
        }

        if (parts[1].equals("wr")) {
            return "" + Math.round(Database.getWinRate(parts[0], player)*100.);
        } 

        // We return null if an invalid placeholder (f.e. %someplugin_placeholder3%) 
        // was provided
        return null;
    }
}