package com.minecraft.cheatdetector.report;

import com.minecraft.cheatdetector.CheatDetector;
import com.minecraft.cheatdetector.config.ModConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Manages the recording and handling of cheat violations.
 */
public class ViolationManager {
    private final ModConfig config;
    private final Map<UUID, List<Violation>> violationMap = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Create a new violation manager.
     * @param config The mod configuration
     */
    public ViolationManager(ModConfig config) {
        this.config = config;
        
        // Create reports directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get("reports"));
        } catch (IOException e) {
            CheatDetector.LOGGER.error("Failed to create reports directory: " + e.getMessage());
        }
    }
    
    /**
     * Log a violation by a player.
     * @param player The player who violated
     * @param type The type of violation
     * @param details Details about the violation
     */
    public void logViolation(ServerPlayerEntity player, String type, String details) {
        UUID playerUuid = player.getUuid();
        String playerName = player.getName().getString();
        String timestamp = dateFormat.format(new Date());
        
        // Create a new violation
        Violation violation = new Violation(type, details, timestamp);
        
        // Add to the violation map
        List<Violation> playerViolations = violationMap.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        playerViolations.add(violation);
        
        // Trim the list if it's too long
        if (playerViolations.size() > config.getMaxViolationsPerReport()) {
            playerViolations.remove(0);
        }
        
        // Log to console
        CheatDetector.LOGGER.info("[{}] {} violated {}: {}", timestamp, playerName, type, details);
        
        // Log to player report file
        saveViolationToFile(playerUuid, playerName, violation);
        
        // Notify admins if they're online
        notifyAdmins(player, type, details);
    }
    
    /**
     * Save a violation to the player's report file.
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param violation The violation to save
     */
    private void saveViolationToFile(UUID playerUuid, String playerName, Violation violation) {
        File reportFile = new File("reports", playerUuid.toString() + ".txt");
        boolean isNewFile = !reportFile.exists();
        
        try (FileWriter writer = new FileWriter(reportFile, true)) {
            // Write player info at the top of a new file
            if (isNewFile) {
                writer.write("Player: " + playerName + "\n");
                writer.write("UUID: " + playerUuid + "\n");
                writer.write("Report created: " + dateFormat.format(new Date()) + "\n");
                writer.write("=========================================\n\n");
            }
            
            // Write the violation
            writer.write("[" + violation.timestamp() + "] " + violation.type() + ": " + violation.details() + "\n");
        } catch (IOException e) {
            CheatDetector.LOGGER.error("Failed to save violation to file: " + e.getMessage());
        }
    }
    
    /**
     * Notify all online admins about a violation.
     * @param player The player who violated
     * @param type The type of violation
     * @param details Details about the violation
     */
    private void notifyAdmins(ServerPlayerEntity player, String type, String details) {
        String message = player.getName().getString() + " violated " + type + ": " + details;
        
        // Send message to all players with permission level 2 or higher (ops by default)
        player.getServer().getPlayerManager().getPlayerList().forEach(admin -> {
            if (admin.hasPermissionLevel(2)) {
                admin.sendMessage(Text.literal("[CheatDetector] " + message).formatted(Formatting.RED), false);
            }
        });
    }
    
    /**
     * Handle a speed hack violation.
     * @param player The player who violated
     * @param speed The detected speed
     */
    public void handleSpeedViolation(ServerPlayerEntity player, double speed) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            player.sendMessage(Text.literal("[CheatDetector] Speed hack detected: " + speed + " blocks/sec").formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Handle a flight hack violation.
     * @param player The player who violated
     */
    public void handleFlyViolation(ServerPlayerEntity player) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            player.sendMessage(Text.literal("[CheatDetector] Flight hack detected").formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Handle a speed hack violation.
     * @param player The player who violated
     * @param speed The detected speed
     * @param lastValidPosition The last valid position
     */
    public void handleSpeedHackViolation(ServerPlayerEntity player, double speed, Vec3d lastValidPosition) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            player.sendMessage(Text.literal("[CheatDetector] Speed hack detected: " + speed + " blocks/sec").formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Handle a flight hack violation.
     * @param player The player who violated
     * @param verticalSpeed The detected vertical speed
     * @param lastValidPosition The last valid position
     */
    public void handleFlyHackViolation(ServerPlayerEntity player, double verticalSpeed, Vec3d lastValidPosition) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            player.sendMessage(Text.literal("[CheatDetector] Flight hack detected: " + verticalSpeed + " blocks/sec vertical").formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Handle an X-ray violation.
     * @param player The player who violated
     * @param ratio The suspicious diamond to stone ratio
     */
    public void handleXrayViolation(ServerPlayerEntity player, double ratio) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            String message = ratio < 0 
                    ? "[CheatDetector] X-ray hack detected: Direct mining to hidden ores" 
                    : "[CheatDetector] X-ray hack detected: Diamond/Stone ratio " + ratio;
            player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Handle a kill aura violation.
     * @param player The player who violated
     * @param value The value related to the violation (attack count, etc.)
     */
    public void handleKillAuraViolation(ServerPlayerEntity player, int value) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            String message = value < 0 
                    ? "[CheatDetector] KillAura hack detected: Suspicious attack patterns" 
                    : "[CheatDetector] KillAura hack detected: " + value + " attacks";
            player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Handle a reach hack violation.
     * @param player The player who violated
     * @param distance The attack distance
     */
    public void handleReachHackViolation(ServerPlayerEntity player, double distance) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            player.sendMessage(Text.literal("[CheatDetector] Reach hack detected: " + distance + " blocks").formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Handle a NoFall violation.
     * @param player The player who violated
     * @param fallDistance The fall distance
     */
    public void handleNoFallViolation(ServerPlayerEntity player, double fallDistance) {
        // For detection purposes, we don't take any action on the player
        // except logging the violation
        
        // If debug mode is enabled, notify the player
        if (config.isDebugMode()) {
            player.sendMessage(Text.literal("[CheatDetector] NoFall hack detected: " + fallDistance + " blocks").formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Get all violations by a player.
     * @param playerUuid The player's UUID
     * @return A list of all recorded violations by the player
     */
    public List<Violation> getPlayerViolations(UUID playerUuid) {
        return violationMap.getOrDefault(playerUuid, Collections.emptyList());
    }
    
    /**
     * Record representing a single violation.
     */
    public record Violation(String type, String details, String timestamp) {
    }
} 