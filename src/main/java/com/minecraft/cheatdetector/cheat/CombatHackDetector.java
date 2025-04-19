package com.minecraft.cheatdetector.cheat;

import com.minecraft.cheatdetector.CheatDetector;
import com.minecraft.cheatdetector.config.ModConfig;
import com.minecraft.cheatdetector.data.PlayerDataManager;
import com.minecraft.cheatdetector.report.ViolationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects combat-related cheats like kill aura and reach hacks.
 */
public class CombatHackDetector {
    private final ViolationManager violationManager;
    private final ModConfig config;

    /**
     * Create a new combat hack detector.
     * @param violationManager The violation manager
     * @param config The mod configuration
     */
    public CombatHackDetector(ViolationManager violationManager, ModConfig config) {
        this.violationManager = violationManager;
        this.config = config;
    }

    /**
     * Check for kill aura by detecting suspicious attack patterns.
     * Kill aura allows players to automatically attack entities,
     * often with rapid hit rates or unusual angles.
     * 
     * @param player The player to check
     * @param target The entity being attacked
     */
    public void checkKillAura(ServerPlayerEntity player, Entity target) {
        // Skip players in creative mode
        if (player.isCreative()) {
            return;
        }

        PlayerDataManager.PlayerData data = CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
        
        // Record this attack
        data.recordAttack(target.getId());
        
        // Check kill aura patterns
        checkAttackRate(player, data);
        checkMultiAngleAttacks(player, data);
    }

    /**
     * Check if a player's attack rate is suspiciously high.
     * 
     * @param player The player to check
     * @param data The player's data
     */
    private void checkAttackRate(ServerPlayerEntity player, PlayerDataManager.PlayerData data) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastAttack = currentTime - data.getLastAttackTime();
        
        // Ignore first attack or attacks that are reasonably spaced
        if (data.getLastAttackTime() == 0 || timeSinceLastAttack > 200) {
            data.setLastAttackTime(currentTime);
            return;
        }
        
        // Record the time between attacks
        data.addAttackInterval(timeSinceLastAttack);
        data.setLastAttackTime(currentTime);
        
        // Only check if we have enough data
        if (data.getAttackIntervals().size() >= 10) {
            // Calculate average attack interval
            double avgInterval = data.getAttackIntervals().stream().mapToLong(Long::longValue).average().orElse(0);
            
            // If attacks are unusually rapid (below threshold and consistent)
            if (avgInterval < config.getKillAuraMinAttackInterval()) {
                // Calculate standard deviation to determine consistency
                double stdDev = calculateStandardDeviation(data.getAttackIntervals(), avgInterval);
                
                // Consistent rapid attacks are suspicious
                if (stdDev < 50) { // Low variation in timing suggests automated attacks
                    data.increaseKillAuraViolationLevel();
                    
                    if (data.getKillAuraViolationLevel() >= config.getMaxKillAuraViolationsBeforeAction()) {
                        violationManager.logViolation(player, "KillAura", 
                                String.format("Rapid attacks (avg: %.2fms, stdDev: %.2f)", avgInterval, stdDev));
                        
                        // Handle the violation
                        violationManager.handleKillAuraViolation(player, avgInterval);
                        
                        // Reset violation level after taking action
                        for (int i = 0; i < 3; i++) {
                            data.decreaseKillAuraViolationLevel();
                        }
                    }
                }
            } else {
                // Gradually decrease violation level if attack patterns are normal
                if (currentTime - data.getLastKillAuraViolationTime() > 10000) { // 10 seconds
                    data.decreaseKillAuraViolationLevel();
                }
            }
            
            // Trim the list to prevent it from growing too large
            while (data.getAttackIntervals().size() > 20) {
                data.getAttackIntervals().remove(0);
            }
        }
    }
    
    /**
     * Check if a player is attacking multiple entities in different directions too quickly.
     * This can indicate kill aura or other automated combat cheats.
     * 
     * @param player The player to check
     * @param data The player's data
     */
    private void checkMultiAngleAttacks(ServerPlayerEntity player, PlayerDataManager.PlayerData data) {
        List<Integer> recentTargets = data.getRecentAttackTargets();
        
        // Only check if we have enough recent attacks
        if (recentTargets.size() < 3) {
            return;
        }
        
        // Get entities around the player
        List<Entity> nearbyEntities = new ArrayList<>();
        Box box = player.getBoundingBox().expand(5.0);
        player.getWorld().getOtherEntities(player, box, entity -> entity instanceof LivingEntity).forEach(nearbyEntities::add);
        
        // Check if player has attacked multiple entities in different directions
        int differentAngleAttacks = 0;
        Vec3d lastDirection = null;
        
        for (Entity entity : nearbyEntities) {
            if (recentTargets.contains(entity.getId())) {
                Vec3d direction = entity.getPos().subtract(player.getPos()).normalize();
                
                if (lastDirection != null) {
                    // Calculate angle between attack directions
                    double angle = Math.toDegrees(Math.acos(lastDirection.dotProduct(direction)));
                    
                    // If angle is significant, count it as a different angle attack
                    if (angle > 45) {
                        differentAngleAttacks++;
                    }
                }
                
                lastDirection = direction;
            }
        }
        
        // If player attacked in multiple different directions within a short time
        if (differentAngleAttacks >= 2) {
            data.increaseKillAuraViolationLevel();
            
            if (data.getKillAuraViolationLevel() >= config.getMaxKillAuraViolationsBeforeAction()) {
                violationManager.logViolation(player, "KillAura", 
                        String.format("Multiple angle attacks (%d different angles)", differentAngleAttacks));
                
                // Handle the violation
                violationManager.handleKillAuraViolation(player, differentAngleAttacks);
                
                // Reset violation level after taking action
                for (int i = 0; i < 3; i++) {
                    data.decreaseKillAuraViolationLevel();
                }
            }
        }
    }
    
    /**
     * Check if a player's attack exceeds the maximum allowed reach distance.
     * 
     * @param player The player to check
     * @param target The entity being attacked
     */
    public void checkReach(ServerPlayerEntity player, Entity target) {
        // Skip players in creative mode
        if (player.isCreative()) {
            return;
        }
        
        // Calculate actual distance to target
        double distance = player.squaredDistanceTo(target);
        double reach = Math.sqrt(distance);
        
        // Get player data
        PlayerDataManager.PlayerData data = CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
        
        // Maximum allowed reach (vanilla is typically 3.0 blocks in survival, add some tolerance)
        double maxReach = target instanceof PlayerEntity ? config.getMaxPvpReach() : config.getMaxPveReach();
        
        // Account for latency
        int ping = player.networkHandler.getLatency();
        double pingCompensation = ping / 1000.0 * 0.5; // 0.5 blocks per second of ping
        maxReach += pingCompensation;
        
        // Check if reach exceeds limit
        if (reach > maxReach) {
            // Increment violation level
            data.increaseReachViolationLevel();
            
            if (data.getReachViolationLevel() >= config.getMaxReachViolationsBeforeAction()) {
                violationManager.logViolation(player, "ReachHack", 
                        String.format("Reached %.2f blocks (max allowed: %.2f, ping: %dms)", reach, maxReach, ping));
                
                // Handle the violation
                violationManager.handleReachViolation(player, reach);
                
                // Reset violation level after taking action
                for (int i = 0; i < 3; i++) {
                    data.decreaseReachViolationLevel();
                }
            }
        } else {
            // Gradually decrease violation level for normal reaches
            long currentTime = System.currentTimeMillis();
            if (currentTime - data.getLastReachViolationTime() > 10000) { // 10 seconds
                data.decreaseReachViolationLevel();
            }
        }
    }
    
    /**
     * Calculate the standard deviation of a list of long values.
     * 
     * @param values The values
     * @param mean The mean of the values
     * @return The standard deviation
     */
    private double calculateStandardDeviation(List<Long> values, double mean) {
        double sumSquaredDiffs = 0;
        for (long value : values) {
            double diff = value - mean;
            sumSquaredDiffs += diff * diff;
        }
        return Math.sqrt(sumSquaredDiffs / values.size());
    }
} 