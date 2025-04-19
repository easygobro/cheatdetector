package com.minecraft.cheatdetector.test;

import com.minecraft.cheatdetector.CheatDetector;
import com.minecraft.cheatdetector.cheat.SpeedHackDetector;
import com.minecraft.cheatdetector.data.PlayerDataManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.TimeUnit;

/**
 * Provides commands to test the SpeedHackDetector functionality
 */
public class TestSpeedHackDetector {
    
    /**
     * Register test commands for the SpeedHackDetector
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Test command for simulating speed hack
            dispatcher.register(
                CommandManager.literal("testspeed")
                    .requires(source -> source.hasPermissionLevel(2)) // Require op permission
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            context.getSource().sendError(Text.literal("This command must be run by a player"));
                            return 0;
                        }
                        
                        context.getSource().sendFeedback(() -> Text.literal("Starting speed hack simulation test..."), false);
                        simulateSpeedHack(player);
                        return 1;
                    })
            );
            
            // Test command for simulating fly hack
            dispatcher.register(
                CommandManager.literal("testfly")
                    .requires(source -> source.hasPermissionLevel(2)) // Require op permission
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            context.getSource().sendError(Text.literal("This command must be run by a player"));
                            return 0;
                        }
                        
                        context.getSource().sendFeedback(() -> Text.literal("Starting flight hack simulation test..."), false);
                        simulateFlightHack(player);
                        return 1;
                    })
            );
        });
    }
    
    /**
     * Simulates a player using a speed hack by artificially manipulating their position data
     * 
     * @param player The player to test with
     */
    private static void simulateSpeedHack(ServerPlayerEntity player) {
        // Get the player's data
        PlayerDataManager.PlayerData playerData = CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
        SpeedHackDetector speedHackDetector = CheatDetector.getInstance().getSpeedHackDetector();
        
        // Get current position
        Vec3d startPos = player.getPos();
        
        // Create a task to simulate high-speed movement
        Thread simulationThread = new Thread(() -> {
            try {
                // Simulate normal movement first (as baseline)
                player.sendMessage(Text.literal("§aSimulating normal movement for 2 seconds..."), false);
                long startTime = System.currentTimeMillis();
                
                // Over 2 seconds, simulate 10 normal movement updates
                for (int i = 0; i < 10; i++) {
                    // Record position with a normal running speed (6-7 blocks/sec)
                    Vec3d newPos = startPos.add(i * 0.6, 0, i * 0.1);
                    long newTime = startTime + (i * 200); // 200ms intervals
                    
                    // Manually update player data to simulate movement
                    playerData.setLastPosition(newPos);
                    playerData.setLastPositionTime(newTime);
                    
                    // Add normal running speed to data (around 6 blocks/sec)
                    playerData.addMovementSpeed(6.0 + (Math.random() * 0.5));
                    
                    // Sleep to simulate time passing
                    TimeUnit.MILLISECONDS.sleep(200);
                }
                
                // Now simulate speed hacking
                player.sendMessage(Text.literal("§cSimulating speed hack for 3 seconds..."), false);
                startTime = System.currentTimeMillis();
                
                // Over 3 seconds, simulate 15 very fast movement updates
                for (int i = 0; i < 15; i++) {
                    // Record position with an impossible speed (30-35 blocks/sec)
                    Vec3d newPos = startPos.add(10 + (i * 2.0), 0, 10 + (i * 0.3));
                    long newTime = startTime + (i * 200); // 200ms intervals
                    
                    // Manually update player data
                    playerData.setLastPosition(newPos);
                    playerData.setLastPositionTime(newTime);
                    
                    // Add impossible speed to data (around 30-35 blocks/sec)
                    playerData.addMovementSpeed(30.0 + (Math.random() * 5.0));
                    
                    // Manually run detection check
                    speedHackDetector.check(player);
                    
                    // Sleep to simulate time passing
                    TimeUnit.MILLISECONDS.sleep(200);
                }
                
                player.sendMessage(Text.literal("§aSpeed hack simulation completed."), false);
                
            } catch (InterruptedException e) {
                player.sendMessage(Text.literal("§cTest interrupted: " + e.getMessage()), false);
            }
        });
        
        // Start the simulation thread
        simulationThread.setDaemon(true);
        simulationThread.start();
    }
    
    /**
     * Simulates a player using a flight hack by artificially manipulating their position data
     * 
     * @param player The player to test with
     */
    private static void simulateFlightHack(ServerPlayerEntity player) {
        // Get the player's data
        PlayerDataManager.PlayerData playerData = CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
        SpeedHackDetector speedHackDetector = CheatDetector.getInstance().getSpeedHackDetector();
        
        // Get current position
        Vec3d startPos = player.getPos();
        
        // Create a task to simulate flying movement
        Thread simulationThread = new Thread(() -> {
            try {
                player.sendMessage(Text.literal("§cSimulating flight hack for 3 seconds..."), false);
                long startTime = System.currentTimeMillis();
                
                // Get player's current positions
                Vec3d currentPos = player.getPos();
                List<Vec3d> positionHistory = playerData.getPositionHistory();
                
                // Clear any existing position history
                positionHistory.clear();
                
                // Over 3 seconds, simulate flying upward
                for (int i = 0; i < 15; i++) {
                    // Record position with upward movement 
                    Vec3d newPos = startPos.add(i * 0.2, i * 0.5, i * 0.2);
                    
                    // Add to position history
                    positionHistory.add(newPos);
                    
                    // Set current position for the next check
                    playerData.setLastPosition(newPos);
                    playerData.setLastPositionTime(startTime + (i * 200));
                    
                    // Force player to be in the air
                    playerData.setGroundState(false, startTime + (i * 200));
                    
                    // Manually run detection check
                    speedHackDetector.checkIrregularMovement(player, playerData);
                    
                    // Sleep to simulate time passing
                    TimeUnit.MILLISECONDS.sleep(200);
                }
                
                player.sendMessage(Text.literal("§aFlight hack simulation completed."), false);
                
            } catch (InterruptedException e) {
                player.sendMessage(Text.literal("§cTest interrupted: " + e.getMessage()), false);
            }
        });
        
        // Start the simulation thread
        simulationThread.setDaemon(true);
        simulationThread.start();
    }
} 