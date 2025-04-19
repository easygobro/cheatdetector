package com.minecraft.cheatdetector.data;

import com.minecraft.cheatdetector.CheatDetector;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player data for cheat detection purposes.
 */
public class PlayerDataManager {
    // Map of player UUID to their tracking data
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    
    /**
     * Get player data for a specific player, creating a new entry if necessary.
     * @param player The player to get data for
     * @return The player's data
     */
    public PlayerData getPlayerData(ServerPlayerEntity player) {
        return playerDataMap.computeIfAbsent(player.getUuid(), uuid -> new PlayerData(player));
    }
    
    /**
     * Check if there's data for a specific player.
     * @param uuid The player's UUID
     * @return true if the player has data, false otherwise
     */
    public boolean hasPlayerData(UUID uuid) {
        return playerDataMap.containsKey(uuid);
    }
    
    /**
     * Remove a player's data.
     * @param uuid The player's UUID
     */
    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }
    
    /**
     * Get all player data entries.
     * @return A map of player UUIDs to their data
     */
    public Map<UUID, PlayerData> getAllPlayerData() {
        return playerDataMap;
    }
    
    /**
     * Save all player data to disk.
     */
    public void saveAllData() {
        // Currently, we're not saving player data across server restarts.
        // This method is only called when the server is shutting down.
    }
    
    /**
     * Class representing player tracking data for cheat detection.
     */
    public static class PlayerData {
        private final UUID uuid;
        private final String playerName;
        
        // Movement tracking
        private Vec3d lastPosition;
        private Vec3d lastVelocity;
        private long lastPositionTime;
        private boolean wasOnGround;
        private long lastGroundTime;
        private long airTime;
        private float lastYaw;
        private float lastPitch;
        private long lastTeleportTime;
        
        // Speed hack tracking
        private int speedViolationLevel;
        private long lastSpeedViolationTime;
        
        // Flight hack tracking
        private int flightViolationLevel;
        private long lastFlightViolationTime;
        private long continuousAirTime;
        
        // X-ray tracking
        private int xrayViolationLevel;
        private long lastXrayViolationTime;
        private Map<String, Integer> minedBlocks = new HashMap<>();
        private int diamondsMined;
        private int stoneMined;
        
        // Combat tracking
        private int killAuraViolationLevel;
        private long lastKillAuraViolationTime;
        private long lastAttackTime;
        private int attackCount;
        private final Map<UUID, Long> attackedEntities = new HashMap<>();
        
        // Reach hack tracking
        private int reachViolationLevel;
        private long lastReachViolationTime;
        
        // NoFall tracking
        private int noFallViolationLevel;
        private long lastNoFallViolationTime;
        private double fallDistance;
        private boolean shouldTakeFallDamage;
        
        /**
         * Store recent movement speeds for analysis
         */
        private final List<Double> recentMovementSpeeds = new ArrayList<>();
        
        /**
         * Store position history for movement analysis
         */
        private final List<Vec3d> positionHistory = new ArrayList<>();
        
        /**
         * Track violations of irregular movement patterns
         */
        private int irregularMovementViolations = 0;
        private long lastIrregularMovementViolationTime = 0;
        
        /**
         * Create player data for a specific player.
         * @param player The player to create data for
         */
        public PlayerData(ServerPlayerEntity player) {
            this.uuid = player.getUuid();
            this.playerName = player.getName().getString();
            this.lastPosition = player.getPos();
            this.lastPositionTime = System.currentTimeMillis();
            this.wasOnGround = player.isOnGround();
            this.lastGroundTime = System.currentTimeMillis();
            this.lastYaw = player.getYaw();
            this.lastPitch = player.getPitch();
        }
        
        // Getters and setters for movement tracking
        
        public Vec3d getLastPosition() {
            return lastPosition;
        }
        
        public void setLastPosition(Vec3d position) {
            this.lastPosition = position;
            this.lastPositionTime = System.currentTimeMillis();
        }
        
        public Vec3d getLastVelocity() {
            return lastVelocity;
        }
        
        public long getLastPositionTime() {
            return lastPositionTime;
        }
        
        public void setLastPositionTime(long time) {
            this.lastPositionTime = time;
        }
        
        public void setGroundState(boolean onGround, long time) {
            if (onGround && !this.wasOnGround) {
                this.lastGroundTime = time;
                this.airTime = 0;
            } else if (!onGround && this.wasOnGround) {
                this.airTime = 0;
            } else if (!onGround) {
                this.airTime = time - this.lastGroundTime;
            }
            this.wasOnGround = onGround;
        }
        
        public boolean wasOnGround() {
            return wasOnGround;
        }
        
        public long getLastGroundTime() {
            return lastGroundTime;
        }
        
        public long getAirTime() {
            return airTime;
        }
        
        public void setRotation(float yaw, float pitch) {
            this.lastYaw = yaw;
            this.lastPitch = pitch;
        }
        
        public float getLastYaw() {
            return lastYaw;
        }
        
        public float getLastPitch() {
            return lastPitch;
        }
        
        public long getLastTeleportTime() {
            return lastTeleportTime;
        }
        
        public void setLastTeleportTime(long lastTeleportTime) {
            this.lastTeleportTime = lastTeleportTime;
        }
        
        // Getters and setters for speed hack tracking
        
        public int getSpeedViolationLevel() {
            return speedViolationLevel;
        }
        
        public void increaseSpeedViolationLevel() {
            this.speedViolationLevel++;
            this.lastSpeedViolationTime = System.currentTimeMillis();
        }
        
        public void decreaseSpeedViolationLevel() {
            if (this.speedViolationLevel > 0) {
                this.speedViolationLevel--;
            }
        }
        
        public long getLastSpeedViolationTime() {
            return lastSpeedViolationTime;
        }
        
        // Getters and setters for flight hack tracking
        
        public int getFlightViolationLevel() {
            return flightViolationLevel;
        }
        
        public void increaseFlightViolationLevel() {
            this.flightViolationLevel++;
            this.lastFlightViolationTime = System.currentTimeMillis();
        }
        
        public void decreaseFlightViolationLevel() {
            if (this.flightViolationLevel > 0) {
                this.flightViolationLevel--;
            }
        }
        
        public long getLastFlightViolationTime() {
            return lastFlightViolationTime;
        }
        
        public long getContinuousAirTime() {
            return continuousAirTime;
        }
        
        public void setContinuousAirTime(long continuousAirTime) {
            this.continuousAirTime = continuousAirTime;
        }
        
        // Getters and setters for X-ray tracking
        
        public int getXrayViolationLevel() {
            return xrayViolationLevel;
        }
        
        public void increaseXrayViolationLevel() {
            this.xrayViolationLevel++;
            this.lastXrayViolationTime = System.currentTimeMillis();
        }
        
        public void decreaseXrayViolationLevel() {
            if (this.xrayViolationLevel > 0) {
                this.xrayViolationLevel--;
            }
        }
        
        public long getLastXrayViolationTime() {
            return lastXrayViolationTime;
        }
        
        public void addMinedBlock(String blockId) {
            minedBlocks.put(blockId, minedBlocks.getOrDefault(blockId, 0) + 1);
            
            if (blockId.contains("diamond_ore")) {
                diamondsMined++;
            } else if (blockId.contains("stone") || blockId.contains("deepslate")) {
                stoneMined++;
            }
        }
        
        public int getMinedBlockCount(String blockId) {
            return minedBlocks.getOrDefault(blockId, 0);
        }
        
        public int getDiamondsMined() {
            return diamondsMined;
        }
        
        public int getStoneMined() {
            return stoneMined;
        }
        
        // Getters and setters for KillAura tracking
        
        public int getKillAuraViolationLevel() {
            return killAuraViolationLevel;
        }
        
        public void increaseKillAuraViolationLevel() {
            this.killAuraViolationLevel++;
            this.lastKillAuraViolationTime = System.currentTimeMillis();
        }
        
        public void decreaseKillAuraViolationLevel() {
            if (this.killAuraViolationLevel > 0) {
                this.killAuraViolationLevel--;
            }
        }
        
        public long getLastKillAuraViolationTime() {
            return lastKillAuraViolationTime;
        }
        
        public void recordAttack(UUID entityId) {
            long currentTime = System.currentTimeMillis();
            attackedEntities.put(entityId, currentTime);
            
            if (currentTime - lastAttackTime < 500) {
                attackCount++;
            } else {
                attackCount = 1;
            }
            
            lastAttackTime = currentTime;
        }
        
        public int getAttackCount() {
            return attackCount;
        }
        
        public long getLastAttackTime() {
            return lastAttackTime;
        }
        
        public int getRecentTargetCount() {
            long currentTime = System.currentTimeMillis();
            // Count entities attacked in the last 2 seconds
            return (int) attackedEntities.values().stream()
                    .filter(time -> currentTime - time < 2000)
                    .count();
        }
        
        // Getters and setters for reach hack tracking
        
        public int getReachViolationLevel() {
            return reachViolationLevel;
        }
        
        public void increaseReachViolationLevel() {
            this.reachViolationLevel++;
            this.lastReachViolationTime = System.currentTimeMillis();
        }
        
        public void decreaseReachViolationLevel() {
            if (this.reachViolationLevel > 0) {
                this.reachViolationLevel--;
            }
        }
        
        public long getLastReachViolationTime() {
            return lastReachViolationTime;
        }
        
        // Getters and setters for NoFall tracking
        
        public int getNoFallViolationLevel() {
            return noFallViolationLevel;
        }
        
        public void increaseNoFallViolationLevel() {
            this.noFallViolationLevel++;
            this.lastNoFallViolationTime = System.currentTimeMillis();
        }
        
        public void decreaseNoFallViolationLevel() {
            if (this.noFallViolationLevel > 0) {
                this.noFallViolationLevel--;
            }
        }
        
        public long getLastNoFallViolationTime() {
            return lastNoFallViolationTime;
        }
        
        public double getFallDistance() {
            return fallDistance;
        }
        
        public void setFallDistance(double fallDistance) {
            this.fallDistance = fallDistance;
        }
        
        public boolean isShouldTakeFallDamage() {
            return shouldTakeFallDamage;
        }
        
        public void setShouldTakeFallDamage(boolean shouldTakeFallDamage) {
            this.shouldTakeFallDamage = shouldTakeFallDamage;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        /**
         * Add a movement speed to the recent speeds list
         * @param speed The speed in blocks per second
         */
        public void addMovementSpeed(double speed) {
            recentMovementSpeeds.add(speed);
            // Trim if too large
            while (recentMovementSpeeds.size() > 30) {
                recentMovementSpeeds.remove(0);
            }
        }
        
        /**
         * Get the list of recent movement speeds
         * @return List of recent speeds in blocks per second
         */
        public List<Double> getRecentMovementSpeeds() {
            return recentMovementSpeeds;
        }
        
        /**
         * Get the position history for this player
         * @return List of recent positions
         */
        public List<Vec3d> getPositionHistory() {
            return positionHistory;
        }
        
        public void increaseIrregularMovementViolations() {
            this.irregularMovementViolations++;
            this.lastIrregularMovementViolationTime = System.currentTimeMillis();
        }
        
        public void decreaseIrregularMovementViolations() {
            if (this.irregularMovementViolations > 0) {
                this.irregularMovementViolations--;
            }
        }
        
        public int getIrregularMovementViolations() {
            return irregularMovementViolations;
        }
        
        public long getLastIrregularMovementViolationTime() {
            return lastIrregularMovementViolationTime;
        }
    }
} 