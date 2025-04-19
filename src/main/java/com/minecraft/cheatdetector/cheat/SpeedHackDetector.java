package com.minecraft.cheatdetector.cheat;

import com.minecraft.cheatdetector.CheatDetector;
import com.minecraft.cheatdetector.config.ModConfig;
import com.minecraft.cheatdetector.data.PlayerDataManager;
import com.minecraft.cheatdetector.report.ViolationManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Detects speed hacks by monitoring player movement patterns.
 */
public class SpeedHackDetector {
    private final ViolationManager violationManager;
    private final ModConfig config;
    
    /**
     * Create a new speed hack detector.
     * @param violationManager The violation manager
     * @param config The mod configuration
     */
    public SpeedHackDetector(ViolationManager violationManager, ModConfig config) {
        this.violationManager = violationManager;
        this.config = config;
    }
    
    /**
     * Check if a player is moving too fast or using other movement cheats.
     * 
     * @param player The player to check
     */
    public void check(ServerPlayerEntity player) {
        // Skip players in creative/spectator mode
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        
        // Skip if player is riding an entity
        if (player.hasVehicle()) {
            return;
        }
        
        PlayerDataManager.PlayerData data = CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
        
        // Record current position
        Vec3d currentPos = player.getPos();
        long currentTime = System.currentTimeMillis();
        
        // Skip if this is the first position record or if too much time has passed
        if (data.getLastPositionTime() == 0 || currentTime - data.getLastPositionTime() > 1000) {
            data.setLastPosition(currentPos);
            data.setLastPositionTime(currentTime);
            return;
        }
        
        // Calculate speed in blocks per second
        Vec3d lastPos = data.getLastPosition();
        double timeDelta = (currentTime - data.getLastPositionTime()) / 1000.0; // convert to seconds
        
        // Horizontal speed calculation (ignoring Y axis)
        Vec3d horizontalMovement = new Vec3d(
                currentPos.x - lastPos.x,
                0,
                currentPos.z - lastPos.z
        );
        
        double horizontalSpeed = horizontalMovement.length() / timeDelta;
        
        // Update position data
        data.setLastPosition(currentPos);
        data.setLastPositionTime(currentTime);
        
        // Record speed for pattern analysis
        data.addMovementSpeed(horizontalSpeed);
        
        // Calculate expected maximum speed based on game mechanics
        double maxSpeed = calculateMaxAllowedSpeed(player);
        
        // Check for speed violations
        if (horizontalSpeed > maxSpeed) {
            // Not an immediate violation - check for sustained speed
            checkSustainedSpeedViolation(player, data, horizontalSpeed, maxSpeed);
        } else {
            // Gradually decrease violation level for normal movement
            if (currentTime - data.getLastSpeedViolationTime() > 10000) { // 10 seconds
                data.decreaseSpeedViolationLevel();
            }
        }
        
        // Check for irregular movement patterns (teleportation/flying)
        checkIrregularMovement(player, data);
    }
    
    /**
     * Check if a player is consistently moving faster than allowed.
     * 
     * @param player The player to check
     * @param data The player's data
     * @param currentSpeed The current speed
     * @param maxAllowedSpeed The maximum allowed speed
     */
    private void checkSustainedSpeedViolation(ServerPlayerEntity player, PlayerDataManager.PlayerData data, 
                                             double currentSpeed, double maxAllowedSpeed) {
        List<Double> speeds = data.getRecentMovementSpeeds();
        
        // Only check for violations if we have enough data
        if (speeds.size() >= 5) {
            // Calculate average speed over recent movements
            double avgSpeed = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            
            // Calculate how much the speed exceeds the allowed limit (as a percentage)
            double overSpeedPercentage = ((avgSpeed / maxAllowedSpeed) - 1.0) * 100;
            
            // If average speed is consistently above the limit by a significant margin
            if (avgSpeed > maxAllowedSpeed && overSpeedPercentage > 10) {
                data.increaseSpeedViolationLevel();
                data.setLastSpeedViolationTime(System.currentTimeMillis());
                
                if (data.getSpeedViolationLevel() >= config.getMaxSpeedViolationsBeforeAction()) {
                    violationManager.logViolation(player, "SpeedHack", 
                            String.format("Moving at %.2f blocks/s (%.2f%% over limit)", 
                                    avgSpeed, overSpeedPercentage));
                    
                    // Handle the violation
                    violationManager.handleSpeedViolation(player, avgSpeed);
                    
                    // Reset violation level after taking action
                    for (int i = 0; i < 3; i++) {
                        data.decreaseSpeedViolationLevel();
                    }
                }
            }
            
            // Trim the list to prevent it from growing too large
            while (speeds.size() > 20) {
                speeds.remove(0);
            }
        }
    }
    
    /**
     * Check for irregular movement patterns that might indicate teleportation or flying.
     * 
     * @param player The player to check
     * @param data The player's data
     */
    private void checkIrregularMovement(ServerPlayerEntity player, PlayerDataManager.PlayerData data) {
        Vec3d currentPos = player.getPos();
        List<Vec3d> positionHistory = data.getPositionHistory();
        
        // Add current position to history
        positionHistory.add(currentPos);
        
        // Trim history to keep only recent positions
        while (positionHistory.size() > 10) {
            positionHistory.remove(0);
        }
        
        // Need at least 3 positions to check for irregular movement
        if (positionHistory.size() < 3) {
            return;
        }
        
        // Check for vertical movement inconsistencies (flying)
        if (!player.abilities.allowFlying && !player.isOnGround() && !player.isTouchingWater()) {
            Vec3d prevPos = positionHistory.get(positionHistory.size() - 2);
            
            // If player is moving upward while in the air
            if (currentPos.y > prevPos.y && player.getVelocity().y > 0 && !player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                data.increaseIrregularMovementViolations();
                
                if (data.getIrregularMovementViolations() >= config.getMaxFlyViolationsBeforeAction()) {
                    violationManager.logViolation(player, "FlyHack", 
                            String.format("Irregular vertical movement detected (y-vel: %.2f)", player.getVelocity().y));
                    
                    // Handle the violation
                    violationManager.handleFlyViolation(player);
                    
                    // Reset violation level after taking action
                    for (int i = 0; i < 3; i++) {
                        data.decreaseIrregularMovementViolations();
                    }
                }
            }
        } else {
            // Gradually decrease violations when on ground
            if (player.isOnGround() && data.getLastIrregularMovementViolationTime() > 10000) {
                data.decreaseIrregularMovementViolations();
            }
        }
    }
    
    /**
     * Calculate the maximum allowed speed for a player based on their current status effects.
     * 
     * @param player The player
     * @return The maximum allowed speed in blocks per second
     */
    private double calculateMaxAllowedSpeed(ServerPlayerEntity player) {
        // Base walking speed in Minecraft is about 4.3 blocks per second
        double baseSpeed = 4.3;
        
        // Add speed boost from effects
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
            baseSpeed *= (1.0 + 0.2 * amplifier);
        }
        
        // Reduce speed for slowness effect
        if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1;
            baseSpeed *= (1.0 - 0.15 * amplifier);
        }
        
        // Add speed for sprinting
        if (player.isSprinting()) {
            baseSpeed *= 1.3;
        }
        
        // Add a tolerance factor to account for server lag and other factors
        double tolerance = config.getSpeedHackTolerance(); // e.g. 1.2 for 20% tolerance
        
        return baseSpeed * tolerance;
    }
} 