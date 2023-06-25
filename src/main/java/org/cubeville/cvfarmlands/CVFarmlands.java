package org.cubeville.cvfarmlands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class CVFarmlands extends JavaPlugin implements Listener {
    UUID farmlandId;

    List<MaterialFilterRegion> breakPermRegions;

    private static WorldGuardPlugin worldGuard = (WorldGuardPlugin)Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");

    Set<Material> ignoredMaterials;

    private class MaterialFilterRegion extends CuboidRegion {
        Set<Material> includedMaterial;

        public MaterialFilterRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
            super(xMin, yMin, zMin, xMax, yMax, zMax);
            this.includedMaterial = new HashSet<>();
        }

        public void addMaterial(Material material) {
            this.includedMaterial.add(material);
        }

        public boolean isIncluded(Material material) {
            return this.includedMaterial.contains(material);
        }
    }

    private class CuboidRegion {
        int xMin;

        int yMin;

        int zMin;

        int xMax;

        int yMax;

        int zMax;

        public CuboidRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.zMin = zMin;
            this.xMax = xMax;
            this.yMax = yMax;
            this.zMax = zMax;
        }

        public boolean blockIsWithin(int x, int y, int z) {
            return (x >= this.xMin && x <= this.xMax && z >= this.zMin && z <= this.zMax && y >= this.yMin && y <= this.yMax);
        }
    }

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, (Plugin)this);
        this.farmlandId = UUID.fromString(getConfig().getString("farmlands_id"));
        World farmlands = getServer().getWorld(this.farmlandId);
        ConfigurationSection regionList = getConfig().getConfigurationSection("breakpermission");
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(farmlands));
        this.breakPermRegions = new ArrayList<>();
        if (regionList != null)
            for (String region : regionList.getKeys(false)) {
                ProtectedRegion rg = manager.getRegion(region);
                if (rg == null) {
                    System.out.println("Can't find region " + region + ", ignoring.");
                    continue;
                }
                BlockVector3 wgmin = rg.getMinimumPoint();
                Vector min = new Vector(wgmin.getX(), wgmin.getY(), wgmin.getZ());
                BlockVector3 wgmax = rg.getMaximumPoint();
                Vector max = new Vector(wgmax.getX(), wgmax.getY(), wgmax.getZ());
                MaterialFilterRegion bpr = new MaterialFilterRegion(min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
                List<String> materialList = regionList.getStringList(region);
                for (String material : materialList)
                    bpr.addMaterial(Material.valueOf(material));
                this.breakPermRegions.add(bpr);
            }
        this.ignoredMaterials = new HashSet<>();
        this.ignoredMaterials.add(Material.BLACK_SHULKER_BOX);
        this.ignoredMaterials.add(Material.BLUE_SHULKER_BOX);
        this.ignoredMaterials.add(Material.BROWN_SHULKER_BOX);
        this.ignoredMaterials.add(Material.CYAN_SHULKER_BOX);
        this.ignoredMaterials.add(Material.GRAY_SHULKER_BOX);
        this.ignoredMaterials.add(Material.GREEN_SHULKER_BOX);
        this.ignoredMaterials.add(Material.LIGHT_BLUE_SHULKER_BOX);
        this.ignoredMaterials.add(Material.LIGHT_GRAY_SHULKER_BOX);
        this.ignoredMaterials.add(Material.LIME_SHULKER_BOX);
        this.ignoredMaterials.add(Material.MAGENTA_SHULKER_BOX);
        this.ignoredMaterials.add(Material.ORANGE_SHULKER_BOX);
        this.ignoredMaterials.add(Material.PINK_SHULKER_BOX);
        this.ignoredMaterials.add(Material.PURPLE_SHULKER_BOX);
        this.ignoredMaterials.add(Material.RED_SHULKER_BOX);
        this.ignoredMaterials.add(Material.YELLOW_SHULKER_BOX);
        this.ignoredMaterials.add(Material.WHITE_SHULKER_BOX);
        this.ignoredMaterials.add(Material.SHULKER_BOX);
        this.ignoredMaterials.add(Material.SCAFFOLDING);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (!loc.getWorld().getUID().equals(this.farmlandId))
            return;
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL)
            return;
        if (this.ignoredMaterials.contains(block.getType()))
            return;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        for (int i = 0; i < this.breakPermRegions.size(); i++) {
            if (((MaterialFilterRegion)this.breakPermRegions.get(i)).blockIsWithin(x, y, z)) {
                if (!((MaterialFilterRegion)this.breakPermRegions.get(i)).isIncluded(block.getType())) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§c§lHey! §7Sorry, but you can't place that block here.");
                }
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (!loc.getWorld().getUID().equals(this.farmlandId))
            return;
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL)
            return;
        if ((event.getBlockReplacedState().getType() == Material.AIR || event.getBlockReplacedState().getType() == Material.CAVE_AIR) &&
                this.ignoredMaterials.contains(event.getBlock().getBlockData().getMaterial())) {
            if (loc.getBlockY() == 0)
                return;
            Block underneath = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
            if (underneath.getType() != Material.DIRT_PATH && underneath.getType() != Material.FARMLAND)
                return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("§c§lHey! §7Sorry, but you can't place that block here.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!event.getPlayer().getLocation().getWorld().getUID().equals(this.farmlandId))
            return;
        event.setCancelled(true);
    }
}
