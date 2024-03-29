package com.ashkiano.stinkyplayers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//TODO opravit chyby v konzoli, když se vejde do netheru nebo endu
//TODO přidat reset, když hráč umře
//TODO přidat configurovatelnost hlášky a proměnnou na její vypnutí
//TODO přidat zprávu co se vypíše hráčům v okolí aby věěli, jaký hráč smrdí
public class StinkyPlayers extends JavaPlugin implements Listener {

    private Map<UUID, Long> lastBathTime = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        Metrics metrics = new Metrics(this, 19169);

        // Print the donation message to the console
        this.getLogger().info("Thank you for using the StinkyPlayers plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://donate.ashkiano.com");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        String message = getConfig().getString("message", "You stink! Take a bath!"); // Default message is "You stink! Take a bath!". Message will be this if not specified otherwise in config.yml
        Player player = event.getPlayer();
        long timeBeforeSmelling = getConfig().getLong("timeBeforeSmelling", 600) * 1000; // Default is 600 seconds (10 minutes). Time is converted to milliseconds
        BlockData blockData = Material.SOUL_SAND.createBlockData();

        if (player.getLocation().getBlock().getType() == Material.WATER) {
            lastBathTime.put(player.getUniqueId(), System.currentTimeMillis());
        } else if (lastBathTime.getOrDefault(player.getUniqueId(), 0L) + timeBeforeSmelling < System.currentTimeMillis()) {
            player.getWorld().spawnParticle(Particle.FALLING_DUST, player.getLocation(), 10, 0.5, 0.5, 0.5, 0, blockData);
            for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                if (nearbyPlayer.getWorld().equals(player.getWorld()) &&
                        nearbyPlayer.getLocation().distance(player.getLocation()) <= 10 &&
                        !nearbyPlayer.getUniqueId().equals(player.getUniqueId())) {
                    nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 1));
                }
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED + message)); // Sends the action bar message
        }
    }

}