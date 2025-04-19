package com.minecraft.cheatdetector.cheat;

import com.minecraft.cheatdetector.CheatDetector;
import com.minecraft.cheatdetector.config.ModConfig;
import com.minecraft.cheatdetector.data.PlayerDataManager;
import com.minecraft.cheatdetector.report.ViolationManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Detects X-ray cheats by analyzing player mining patterns.
 */
public class XrayDetector {
    private final ViolationManager violationManager;
    private final ModConfig config;
    
    /**
     * Create a new X-ray detector.
     * @param violationManager The violation manager
     * @param config The mod configuration
     */
    public XrayDetector(ViolationManager violationManager, ModConfig config) {
        this.violationManager = violationManager;
        this.config = config;
    }
    
    /**
     * Check a player for X-ray cheats.
     * @param player The player to check
     */
    public void check(ServerPlayerEntity player) {
        // Skip players in creative or spectator mode
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        
        PlayerDataManager.PlayerData data = CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
        
        // Check diamond to stone ratio if player has mined enough blocks
        int diamondsMined = data.getDiamondsMined();
        int stoneMined = data.getStoneMined();
        
        // Only check players who have mined a reasonable number of blocks
        if (diamondsMined > 0 && stoneMined > 20) {
            double ratio = (double) diamondsMined / stoneMined;
            double suspiciousRatio = config.getXrayDiamondRatioThreshold();
            
            // If diamond rate is suspiciously high
            if (ratio > suspiciousRatio) {
                // Increase violation level
                data.increaseXrayViolationLevel();
                
                if (data.getXrayViolationLevel() >= config.getMaxXrayViolationsBeforeAction()) {
                    // Log violation with precise information
                    violationManager.logViolation(player, "XRay", 
                            String.format("Diamond/Stone ratio: %.4f (threshold: %.4f), Diamonds: %d, Stone: %d", 
                                    ratio, suspiciousRatio, diamondsMined, stoneMined));
                    
                    // Take action
                    violationManager.handleXrayViolation(player, ratio);
                    
                    // Reset violation level after taking action
                    for (int i = 0; i < 3; i++) {
                        data.decreaseXrayViolationLevel();
                    }
                }
            } else {
                // Decrease violation level if ratio is normal and it's been a while
                long currentTime = System.currentTimeMillis();
                if (currentTime - data.getLastXrayViolationTime() > 10000) { // 10 seconds
                    data.decreaseXrayViolationLevel();
                }
            }
        }
        
        // Additional X-ray detection logic can be added here:
        // 1. Check for targeting specific blocks behind walls
        // 2. Check for unusual mining patterns
        // 3. Check for excessive rare ore finding
        
        // Check for unusual ore discovery patterns
        checkOreDiscoveryPatterns(player, data);
    }
    
    /**
     * Check for unusual ore discovery patterns indicating X-ray.
     * This method analyzes the distribution of valuable ore discoveries
     * to detect if a player is consistently finding valuable ores without
     * mining through typical stone blocks first.
     * 
     * @param player The player to check
     * @param data The player's data
     */
    private void checkOreDiscoveryPatterns(ServerPlayerEntity player, PlayerDataManager.PlayerData data) {
        // This is a more complex detection that would require tracking:
        // 1. The sequence of blocks mined
        // 2. The visibility of ores before they were mined
        // 3. The player's mining direction and patterns
        
        // For now, we're just using the basic diamond-to-stone ratio check
        // A more sophisticated X-ray detection system would track:
        // - Whether valuable blocks were visible before mining (line of sight)
        // - Whether the player is digging directly to valuable ores
        // - Mining efficiency (ratio of valuable blocks to total blocks mined)
        // - Comparison to server averages
        
        // A full implementation would require additional events and data structures
        // that are beyond the scope of this example
    }
} 