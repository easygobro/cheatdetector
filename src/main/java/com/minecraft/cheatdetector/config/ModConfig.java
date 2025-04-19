package com.minecraft.cheatdetector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minecraft.cheatdetector.CheatDetector;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for the CheatDetector mod.
 */
public class ModConfig {
    // Default configuration values
    private boolean debugMode = false;
    private int bypassPermissionLevel = 2;
    
    // Speed hack detection
    private double speedCheckLeniency = 1.3;
    private int maxSpeedViolationsBeforeAction = 5;
    private double maxHorizontalSpeed = 10.0;
    private double maxVerticalSpeed = 5.0;
    
    // Flight hack detection
    private int maxFlightViolationsBeforeAction = 5;
    private double maxAirTime = 3.0;
    private double maxRisingSpeed = 0.8;
    
    // X-ray detection
    private int maxXrayViolationsBeforeAction = 5;
    private double xrayDiamondRatioThreshold = 0.05;
    private Set<String> valuableOres = new HashSet<>(Arrays.asList(
            "minecraft:diamond_ore", 
            "minecraft:deepslate_diamond_ore", 
            "minecraft:ancient_debris"
    ));
    
    // KillAura detection
    private int maxKillAuraViolationsBeforeAction = 5;
    private int maxAttacksPerSecond = 16;
    private int maxTargetsPerTimeWindow = 5;
    private double maxAttackAngle = 120.0;
    
    // Reach hack detection
    private int maxReachViolationsBeforeAction = 5;
    private double maxReachDistance = 3.2;
    
    // NoFall detection
    private int maxNoFallViolationsBeforeAction = 5;
    
    // Reports
    private boolean enableAutoReports = true;
    private boolean saveScreenshots = true;
    private int maxViolationsPerReport = 100;
    
    // File paths
    private static final String CONFIG_DIRECTORY = "config";
    private static final String CONFIG_FILE = "cheatdetector.json";
    
    /**
     * Creates a new configuration instance and loads settings from the config file.
     */
    public ModConfig() {
        createDefaultConfigIfNotExists();
        load();
    }
    
    /**
     * Creates the default configuration file if it doesn't exist.
     */
    private void createDefaultConfigIfNotExists() {
        try {
            Path configDir = Paths.get(CONFIG_DIRECTORY);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            File configFile = new File(CONFIG_DIRECTORY, CONFIG_FILE);
            if (!configFile.exists()) {
                save();
                CheatDetector.LOGGER.info("Created default configuration file");
            }
        } catch (IOException e) {
            CheatDetector.LOGGER.error("Failed to create default configuration file: " + e.getMessage());
        }
    }
    
    /**
     * Loads the configuration from the config file.
     */
    public void load() {
        try (FileReader reader = new FileReader(new File(CONFIG_DIRECTORY, CONFIG_FILE))) {
            Gson gson = new Gson();
            ModConfig loaded = gson.fromJson(reader, ModConfig.class);
            
            // Copy loaded values
            this.debugMode = loaded.debugMode;
            this.bypassPermissionLevel = loaded.bypassPermissionLevel;
            
            // Speed hack
            this.speedCheckLeniency = loaded.speedCheckLeniency;
            this.maxSpeedViolationsBeforeAction = loaded.maxSpeedViolationsBeforeAction;
            this.maxHorizontalSpeed = loaded.maxHorizontalSpeed;
            this.maxVerticalSpeed = loaded.maxVerticalSpeed;
            
            // Flight hack
            this.maxFlightViolationsBeforeAction = loaded.maxFlightViolationsBeforeAction;
            this.maxAirTime = loaded.maxAirTime;
            this.maxRisingSpeed = loaded.maxRisingSpeed;
            
            // X-ray
            this.maxXrayViolationsBeforeAction = loaded.maxXrayViolationsBeforeAction;
            this.xrayDiamondRatioThreshold = loaded.xrayDiamondRatioThreshold;
            this.valuableOres = loaded.valuableOres;
            
            // KillAura
            this.maxKillAuraViolationsBeforeAction = loaded.maxKillAuraViolationsBeforeAction;
            this.maxAttacksPerSecond = loaded.maxAttacksPerSecond;
            this.maxTargetsPerTimeWindow = loaded.maxTargetsPerTimeWindow;
            this.maxAttackAngle = loaded.maxAttackAngle;
            
            // Reach hack
            this.maxReachViolationsBeforeAction = loaded.maxReachViolationsBeforeAction;
            this.maxReachDistance = loaded.maxReachDistance;
            
            // NoFall
            this.maxNoFallViolationsBeforeAction = loaded.maxNoFallViolationsBeforeAction;
            
            // Reports
            this.enableAutoReports = loaded.enableAutoReports;
            this.saveScreenshots = loaded.saveScreenshots;
            this.maxViolationsPerReport = loaded.maxViolationsPerReport;
            
            CheatDetector.LOGGER.info("Configuration loaded successfully");
        } catch (Exception e) {
            CheatDetector.LOGGER.error("Failed to load configuration: " + e.getMessage());
            CheatDetector.LOGGER.error("Using default configuration");
            save(); // Save default config
        }
    }
    
    /**
     * Saves the configuration to the config file.
     */
    public void save() {
        try {
            Path configDir = Paths.get(CONFIG_DIRECTORY);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            File configFile = new File(CONFIG_DIRECTORY, CONFIG_FILE);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(this, writer);
            }
            
            CheatDetector.LOGGER.info("Configuration saved successfully");
        } catch (Exception e) {
            CheatDetector.LOGGER.error("Failed to save configuration: " + e.getMessage());
        }
    }
    
    // Getters and setters
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    public int getBypassPermissionLevel() {
        return bypassPermissionLevel;
    }
    
    public void setBypassPermissionLevel(int bypassPermissionLevel) {
        this.bypassPermissionLevel = bypassPermissionLevel;
    }
    
    public double getSpeedCheckLeniency() {
        return speedCheckLeniency;
    }
    
    public void setSpeedCheckLeniency(double speedCheckLeniency) {
        this.speedCheckLeniency = speedCheckLeniency;
    }
    
    public int getMaxSpeedViolationsBeforeAction() {
        return maxSpeedViolationsBeforeAction;
    }
    
    public void setMaxSpeedViolationsBeforeAction(int maxSpeedViolationsBeforeAction) {
        this.maxSpeedViolationsBeforeAction = maxSpeedViolationsBeforeAction;
    }
    
    public double getMaxHorizontalSpeed() {
        return maxHorizontalSpeed;
    }
    
    public void setMaxHorizontalSpeed(double maxHorizontalSpeed) {
        this.maxHorizontalSpeed = maxHorizontalSpeed;
    }
    
    public double getMaxVerticalSpeed() {
        return maxVerticalSpeed;
    }
    
    public void setMaxVerticalSpeed(double maxVerticalSpeed) {
        this.maxVerticalSpeed = maxVerticalSpeed;
    }
    
    public int getMaxFlightViolationsBeforeAction() {
        return maxFlightViolationsBeforeAction;
    }
    
    public void setMaxFlightViolationsBeforeAction(int maxFlightViolationsBeforeAction) {
        this.maxFlightViolationsBeforeAction = maxFlightViolationsBeforeAction;
    }
    
    public double getMaxAirTime() {
        return maxAirTime;
    }
    
    public void setMaxAirTime(double maxAirTime) {
        this.maxAirTime = maxAirTime;
    }
    
    public double getMaxRisingSpeed() {
        return maxRisingSpeed;
    }
    
    public void setMaxRisingSpeed(double maxRisingSpeed) {
        this.maxRisingSpeed = maxRisingSpeed;
    }
    
    public int getMaxXrayViolationsBeforeAction() {
        return maxXrayViolationsBeforeAction;
    }
    
    public void setMaxXrayViolationsBeforeAction(int maxXrayViolationsBeforeAction) {
        this.maxXrayViolationsBeforeAction = maxXrayViolationsBeforeAction;
    }
    
    public double getXrayDiamondRatioThreshold() {
        return xrayDiamondRatioThreshold;
    }
    
    public void setXrayDiamondRatioThreshold(double xrayDiamondRatioThreshold) {
        this.xrayDiamondRatioThreshold = xrayDiamondRatioThreshold;
    }
    
    public Set<String> getValuableOres() {
        return valuableOres;
    }
    
    public void setValuableOres(Set<String> valuableOres) {
        this.valuableOres = valuableOres;
    }
    
    public int getMaxKillAuraViolationsBeforeAction() {
        return maxKillAuraViolationsBeforeAction;
    }
    
    public void setMaxKillAuraViolationsBeforeAction(int maxKillAuraViolationsBeforeAction) {
        this.maxKillAuraViolationsBeforeAction = maxKillAuraViolationsBeforeAction;
    }
    
    public int getMaxAttacksPerSecond() {
        return maxAttacksPerSecond;
    }
    
    public void setMaxAttacksPerSecond(int maxAttacksPerSecond) {
        this.maxAttacksPerSecond = maxAttacksPerSecond;
    }
    
    public int getMaxTargetsPerTimeWindow() {
        return maxTargetsPerTimeWindow;
    }
    
    public void setMaxTargetsPerTimeWindow(int maxTargetsPerTimeWindow) {
        this.maxTargetsPerTimeWindow = maxTargetsPerTimeWindow;
    }
    
    public double getMaxAttackAngle() {
        return maxAttackAngle;
    }
    
    public void setMaxAttackAngle(double maxAttackAngle) {
        this.maxAttackAngle = maxAttackAngle;
    }
    
    public int getMaxReachViolationsBeforeAction() {
        return maxReachViolationsBeforeAction;
    }
    
    public void setMaxReachViolationsBeforeAction(int maxReachViolationsBeforeAction) {
        this.maxReachViolationsBeforeAction = maxReachViolationsBeforeAction;
    }
    
    public double getMaxReachDistance() {
        return maxReachDistance;
    }
    
    public void setMaxReachDistance(double maxReachDistance) {
        this.maxReachDistance = maxReachDistance;
    }
    
    public int getMaxNoFallViolationsBeforeAction() {
        return maxNoFallViolationsBeforeAction;
    }
    
    public void setMaxNoFallViolationsBeforeAction(int maxNoFallViolationsBeforeAction) {
        this.maxNoFallViolationsBeforeAction = maxNoFallViolationsBeforeAction;
    }
    
    public boolean isEnableAutoReports() {
        return enableAutoReports;
    }
    
    public void setEnableAutoReports(boolean enableAutoReports) {
        this.enableAutoReports = enableAutoReports;
    }
    
    public boolean isSaveScreenshots() {
        return saveScreenshots;
    }
    
    public void setSaveScreenshots(boolean saveScreenshots) {
        this.saveScreenshots = saveScreenshots;
    }
    
    public int getMaxViolationsPerReport() {
        return maxViolationsPerReport;
    }
    
    public void setMaxViolationsPerReport(int maxViolationsPerReport) {
        this.maxViolationsPerReport = maxViolationsPerReport;
    }
    
    /**
     * Get the tolerance factor for speed hack detection.
     * Higher values allow for more leniency in speed detection.
     * @return The speed hack tolerance factor (e.g. 1.2 for 20% tolerance)
     */
    public double getSpeedHackTolerance() {
        return speedCheckLeniency;
    }
    
    /**
     * Get the maximum number of flight violations before taking action.
     * @return The maximum flight violations
     */
    public int getMaxFlyViolationsBeforeAction() {
        return maxFlightViolationsBeforeAction;
    }
} 