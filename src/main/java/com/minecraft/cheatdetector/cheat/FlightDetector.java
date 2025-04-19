package com.minecraft.cheatdetector.cheat;

import com.minecraft.cheatdetector.CheatDetector;
import com.minecraft.cheatdetector.config.ModConfig;
import com.minecraft.cheatdetector.data.PlayerDataManager;
import com.minecraft.cheatdetector.report.ViolationManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Detects flight-related cheats.
 */
public class FlightDetector {
    private final ViolationManager violationManager;
    private final ModConfig config;
    
    /**
     * Create a new flight detector.
     * @param violationManager The violation manager
     * @param config The mod configuration
     */
    public FlightDetector(ViolationManager violationManager, ModConfig config) {
        this.violationManager = violationManager;
        this.config = config;
    }
    
    /**
     * Check a player for flight hacks.
     * @param player The player to check
     */
    public void check(ServerPlayerEntity player) {
        // Skip players in creative or spectator mode
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        
        // Skip if player is allowed to fly
        if (player.getAbilities().allowFlying) {
            return;
        }
        
        // Skip if player is riding an entity
        if (player.hasVehicle()) {
            return;
        }
        
        // Skip if player is using an elytra
        if (player.isFallFlying()) {
            return;
        }
        
        // Skip if player has levitation effect
        if (player.hasStatusEffect(StatusEffects.LEVITATION)) {
            return;
        }
        
        // Skip if player has slow falling effect
        if (player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            return;
        }
        
        PlayerDataManager.PlayerData data = CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
        Vec3d currentPos = player.getPos();
        long currentTime = System.currentTimeMillis();
        
        // Skip if this is the first position update or if player recently teleported
        if (data.getLastPosition() == null || 
                currentTime - data.getLastTeleportTime() < 2000) {
            data.setLastPosition(currentPos, currentTime);
            data.setGroundState(player.isOnGround(), currentTime);
            return;
        }
        
        // Update ground state
        data.setGroundState(player.isOnGround(), currentTime);
        
        // Calculate time delta in seconds
        double timeDelta = (currentTime - data.getLastPositionTime()) / 1000.0;
        
        // Skip if time delta is too small or too large
        if (timeDelta < 0.05 || timeDelta > 1.0) {
            data.setLastPosition(currentPos, currentTime);
            return;
        }
        
        // Check for prolonged air time without falling
        if (!player.isOnGround()) {
            long airTime = data.getAirTime();
            
            // Check for rising while in air for too long
            if (airTime > config.getMaxAirTime() * 1000) { // Convert to milliseconds
                Vec3d lastPos = data.getLastPosition();
                
                // Check if player is staying level or rising while in air
                if (currentPos.y >= lastPos.y) {
                    // Calculate vertical velocity
                    double verticalVelocity = (currentPos.y - lastPos.y) / timeDelta;
                    
                    // Player is moving up while in air for too long - potential flight
                    if (verticalVelocity >= 0) {
                        // Increase violation level
                        data.increaseFlightViolationLevel();
                        
                        if (data.getFlightViolationLevel() >= config.getMaxFlightViolationsBeforeAction()) {
                            // Log violation
                            violationManager.logViolation(player, "FlightHack", 
                                    String.format("Airtime: %.1fs, Vertical velocity: %.2f blocks/s", 
                                            airTime / 1000.0, verticalVelocity));
                            
                            // Take action
                            violationManager.handleFlyHackViolation(player, verticalVelocity, lastPos);
                            
                            // Reset violation level after taking action
                            for (int i = 0; i < 3; i++) {
                                data.decreaseFlightViolationLevel();
                            }
                        }
                    }
                } else {
                    // Player is falling, which is expected
                    // Check if they're falling too slowly though
                    double verticalVelocity = (currentPos.y - lastPos.y) / timeDelta;
                    
                    // In Minecraft, gravity is about -0.08 blocks per tick, or roughly -1.6 blocks/s
                    // If falling is much slower than that, it could be a slow-fall hack
                    if (verticalVelocity > -0.5 && airTime > 1500) { // More than 1.5 seconds in air
                        // Increase violation level
                        data.increaseFlightViolationLevel();
                        
                        if (data.getFlightViolationLevel() >= config.getMaxFlightViolationsBeforeAction()) {
                            // Log violation
                            violationManager.logViolation(player, "SlowFallHack", 
                                    String.format("Airtime: %.1fs, Fall velocity: %.2f blocks/s", 
                                            airTime / 1000.0, verticalVelocity));
                            
                            // Take action
                            violationManager.handleFlyHackViolation(player, verticalVelocity, lastPos);
                            
                            // Reset violation level after taking action
                            for (int i = 0; i < 3; i++) {
                                data.decreaseFlightViolationLevel();
                            }
                        }
                    }
                }
            }
        } else {
            // Player is on ground, decrease violation level over time
            if (currentTime - data.getLastFlightViolationTime() > 10000) { // 10 seconds
                data.decreaseFlightViolationLevel();
            }
        }
        
        // Update last position
        data.setLastPosition(currentPos, currentTime);
    }
} 