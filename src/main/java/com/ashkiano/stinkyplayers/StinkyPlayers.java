package com.ashkiano.stinkyplayers;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import com.jeff_media.updatechecker.UserAgentBuilder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//TODO add bypass permission so these players will never smell
public class StinkyPlayers extends JavaPlugin implements Listener {

    private final Map<UUID, Long> lastBathTime = new HashMap<>();
    private final File lastBathFile = new File(this.getDataFolder(), "lastBath.yml");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if(!lastBathFile.exists()){
            saveResource("lastBath.yml", false);
        }

        getServer().getPluginManager().registerEvents(this, this);

        new Metrics(this, 19169);

        new UpdateChecker(this, UpdateCheckSource.SPIGOT, "111426")
                .setNotifyRequesters(false)
                .setNotifyOpsOnJoin(false)
                .setUserAgent(UserAgentBuilder.getDefaultUserAgent())
                .checkEveryXHours(12)
                .onSuccess((commandSenders, latestVersion) -> {
                    String messagePrefix = "&8[&6Stinky Players&8] ";
                    String currentVersion = getDescription().getVersion();

                    if (currentVersion.equalsIgnoreCase(latestVersion)) {
                        String updateMessage = color(messagePrefix + "&aYou are using the latest version of StinkyPlayers!");

                        Bukkit.getConsoleSender().sendMessage(updateMessage);
                        Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).forEach(player -> player.sendMessage(updateMessage));
                        return;
                    }

                    List<String> updateMessages = List.of(
                            color(messagePrefix + "&cYour version of StinkyPlayers is outdated!"),
                            color(String.format(messagePrefix + "&cYou are using %s, latest is %s!", currentVersion, latestVersion)),
                            color(messagePrefix + "&cDownload latest here:"),
                            color("&6https://www.spigotmc.org/resources/stinkyplayers-1-16-1-21.111426/")
                    );

                    Bukkit.getConsoleSender().sendMessage(updateMessages.toArray(new String[]{}));
                    Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).forEach(player -> player.sendMessage(updateMessages.toArray(new String[]{})));
                })
                .onFail((commandSenders, e) -> {
                }).checkNow();

        // Print the donation message to the console
        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getConsoleSender().sendMessage(
                ChatColor.GOLD + "Thank you for using the StinkyPlayers plugin!",
                ChatColor.GOLD + "If you enjoy using this plugin!",
                ChatColor.GOLD + "Please consider making a donation to support the development!",
                ChatColor.GOLD + "You can donate at: " + ChatColor.GREEN + "https://donate.ashkiano.com"
        ), 20);
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    //this only check the bypass permission, but if needed this can be changed after
    private boolean playerHasPermission(Player player){
        return player.hasPermission("stinky.bypass");
    }

    private void setLastBathTime(Player player) {
        YamlConfiguration lastBathConfig= new YamlConfiguration();
        YamlConfiguration.loadConfiguration(lastBathFile);
        lastBathConfig.set(String.valueOf(player.getUniqueId()), System.currentTimeMillis());
        try{
            lastBathConfig.save(lastBathFile);
        }catch(IOException e){
            Bukkit.getConsoleSender().sendMessage(String.valueOf(e));
        }
    }
    private long getLastBathTime(Player player) {
        YamlConfiguration lastBathConfig = YamlConfiguration.loadConfiguration(lastBathFile);
        return lastBathConfig.getLong(String.valueOf(player.getUniqueId()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(getLastBathTime(event.getPlayer())==0L){
            setLastBathTime(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(!playerHasPermission(player)){


        long timeBeforeSmelling = getConfig().getLong("Settings.Time-Before-Smelling", 600) * 1000; // Default is 600 seconds (10 minutes). Time is converted to milliseconds

        // CONDITIONS TO RESET BATHTIME
        //Is on Water
        if (player.getLocation().getBlock().getType() == Material.WATER) {
            lastBathTime.put(player.getUniqueId(), System.currentTimeMillis());
            setLastBathTime(player);
            return;
        }

        //Is on rain
        if(player.getWorld().isThundering() || player.getWorld().hasStorm()){
            int highestBlockLocation = player.getWorld().getHighestBlockYAt(player.getLocation());
            if(highestBlockLocation<= player.getLocation().getY()){
                lastBathTime.put(player.getUniqueId(), System.currentTimeMillis());
                setLastBathTime(player);
                return;
            }
        }

        if ((getLastBathTime(player) + timeBeforeSmelling) > System.currentTimeMillis()) {
            return;
        }

        BlockData blockData = Material.SOUL_SAND.createBlockData();

        player.getWorld().spawnParticle(Particle.FALLING_DUST, player.getLocation(), 10, 0.5, 0.5, 0.5, 0, blockData);

        player.getNearbyEntities(10, 10, 10).stream().filter(entity -> entity instanceof Player).forEach(entity -> {
            Player nearbyPlayer = (Player) entity;
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 1));

            if (!getConfig().getBoolean("Settings.Stink-Message-Other.Enabled", true)) {
                return;
            }

            nearbyPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(
                            color(getConfig().getString("Settings.Stink-Message-Other.Message",
                                    "%player% near you hasn't taken a bath, it's making you nauseous!").replace("%player%", player.getName()))
                    )); // Sends the action bar message
        });

        if (!getConfig().getBoolean("Settings.Stink-Message.Enabled", true)) {
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(
                        color(getConfig().getString("Settings.Stink-Message.Message", "You stink! Take a bath!"))
                )); // Sends the action bar message
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!playerHasPermission(event.getEntity())){
            lastBathTime.put(event.getEntity().getUniqueId(), System.currentTimeMillis());
            setLastBathTime(event.getEntity());
        }
    }
}