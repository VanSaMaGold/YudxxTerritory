package yudxx.minecraft.spigot.yudxxTerritory.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotSettings;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.SoundManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.WorldManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.MessageManager;

public class PlotProtectListener implements Listener {

    private final YudxxTerritory plugin;
    private final PlotManager plotManager;
    private final WorldManager worldManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;

    public PlotProtectListener(YudxxTerritory plugin, PlotManager plotManager, WorldManager worldManager,
                               ConfigManager configManager, MessageManager messageManager, SoundManager soundManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.worldManager = worldManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
    }

    private boolean isPlotWorld(Location loc) {
        return loc != null && loc.getWorld() != null && worldManager.isPlotWorld(loc.getWorld().getName());
    }

    private boolean canBuild(Player player, Location location) {
        if (!isPlotWorld(location)) return true;
        if (location.getY() < configManager.getNoBuildY()) return false;
        if (player.hasPermission("yudxxterritory.admin.bypass")) return true;
        PlotData plot = plotManager.getPlotAtLocation(location);
        if (plot == null) return false;
        return plot.canBuild(player.getUniqueId());
    }

    private PlotData getPlotAt(Location loc) {
        if (!isPlotWorld(loc)) return null;
        return plotManager.getPlotAtLocation(loc);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plotManager.isBorderBlock(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("yudxxterritory.admin.bypass")) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-break-border");
            soundManager.playSound(event.getPlayer(), "border-break-deny");
            return;
        }
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-build");
            soundManager.playSound(event.getPlayer(), "denied");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plotManager.isBorderBlock(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("yudxxterritory.admin.bypass")) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-place-border");
            soundManager.playSound(event.getPlayer(), "border-break-deny");
            return;
        }
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-build");
            soundManager.playSound(event.getPlayer(), "denied");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getPlayer() != null) {
            PlotData plot = getPlotAt(event.getBlock().getLocation());
            if (plot != null) {
                if (!plot.getSettings().getBoolean("fire-spread") && !plot.isOwner(event.getPlayer().getUniqueId())) {
                    event.setCancelled(true);
                }
            }
            if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-interact");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-interact");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() == Action.PHYSICAL) {
            Material type = event.getClickedBlock().getType();
            if (type == Material.FARMLAND || type == Material.TURTLE_EGG || type == Material.SNIFFER_EGG) {
                if (!canBuild(event.getPlayer(), event.getClickedBlock().getLocation())) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isInteractableBlock(event.getClickedBlock().getType())) {
                if (!canBuild(event.getPlayer(), event.getClickedBlock().getLocation())) {
                    event.setCancelled(true);
                    messageManager.send(event.getPlayer(), "cant-interact");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof LeashHitch) {
            if (!canBuild(event.getPlayer(), event.getRightClicked().getLocation())) {
                event.setCancelled(true);
                messageManager.send(event.getPlayer(), "cant-interact");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof LeashHitch) {
            if (!canBuild(event.getPlayer(), event.getRightClicked().getLocation())) {
                event.setCancelled(true);
                messageManager.send(event.getPlayer(), "cant-interact");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isPlotWorld(event.getEntity().getLocation())) return;
        Player attacker = getPlayerFromDamager(event.getDamager());
        if (attacker == null) return;
        if (attacker.hasPermission("yudxxterritory.admin.bypass")) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getEntity().getLocation());
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player) {
            if (!plot.getSettings().getBoolean("pvp")) {
                event.setCancelled(true);
                messageManager.send(attacker, "cant-pvp");
                return;
            }
        }
        if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player)) {
            if (!plot.canBuild(attacker.getUniqueId())) {
                event.setCancelled(true);
                messageManager.send(attacker, "cant-attack");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!isPlotWorld(event.getLocation())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getLocation());
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        PlotSettings settings = plot.getSettings();
        if (event.getEntity() instanceof Monster) {
            if (!settings.getBoolean("monster-spawning")) {
                event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof Animals) {
            if (!settings.getBoolean("animal-spawning")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isPlotWorld(event.getLocation())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getLocation());
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        PlotSettings settings = plot.getSettings();
        if (event.getEntity() instanceof Monster) {
            if (!settings.getBoolean("monster-spawning")) {
                event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof Animals) {
            if (!settings.getBoolean("animal-spawning")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null) {
            if (!canBuild(event.getPlayer(), event.getEntity().getLocation())) {
                event.setCancelled(true);
                messageManager.send(event.getPlayer(), "cant-build");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player) {
            if (!canBuild((Player) event.getRemover(), event.getEntity().getLocation())) {
                event.setCancelled(true);
                messageManager.send((Player) event.getRemover(), "cant-build");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (event.getAttacker() instanceof Player) {
            if (!canBuild((Player) event.getAttacker(), event.getVehicle().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (event.getAttacker() instanceof Player) {
            if (!canBuild((Player) event.getAttacker(), event.getVehicle().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isPlotWorld(event.getLocation())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getLocation());
        if (plot != null && !plot.getSettings().getBoolean("explosions")) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isPlotWorld(event.getBlock().getLocation())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getBlock().getLocation());
        if (plot != null && !plot.getSettings().getBoolean("explosions")) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isPlotWorld(event.getBlock().getLocation())) return;
        for (Block block : event.getBlocks()) {
            PlotData fromPlot = plotManager.getPlotAtLocation(block.getLocation());
            PlotData toPlot = plotManager.getPlotAtLocation(block.getRelative(event.getDirection()).getLocation());
            if (fromPlot != toPlot) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!isPlotWorld(event.getBlock().getLocation())) return;
        for (Block block : event.getBlocks()) {
            PlotData fromPlot = plotManager.getPlotAtLocation(block.getLocation());
            PlotData toPlot = plotManager.getPlotAtLocation(block.getRelative(event.getDirection()).getLocation());
            if (fromPlot != toPlot) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!isPlotWorld(event.getSource().getLocation())) return;
        PlotData fromPlot = plotManager.getPlotAtLocation(event.getSource().getLocation());
        PlotData toPlot = plotManager.getPlotAtLocation(event.getBlock().getLocation());
        if (fromPlot != toPlot) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (!isPlotWorld(event.getBlock().getLocation())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getBlock().getLocation());
        if (plot == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (!canBuild(event.getPlayer(), event.getRightClicked().getLocation())) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-interact");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!isPlotWorld(event.getWorld().getSpawnLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!isPlotWorld(event.getPlayer().getLocation())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getPlayer().getLocation());
        if (plot == null) return;
        if (!plot.getSettings().getBoolean("item-discard")) {
            event.setCancelled(true);
            messageManager.send(event.getPlayer(), "cant-discard");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!isPlotWorld(event.getLocation())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getLocation());
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        if (!plot.getSettings().getBoolean("item-drops")) {
            event.setCancelled(true);
        }
    }

    private boolean isInteractableBlock(Material type) {
        String name = type.name();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST
            || type == Material.FURNACE || type == Material.BLAST_FURNACE
            || type == Material.SMOKER || type == Material.BARREL
            || type == Material.HOPPER || type == Material.DROPPER
            || type == Material.DISPENSER || type == Material.BREWING_STAND
            || type == Material.ENCHANTING_TABLE || type == Material.ANVIL
            || type == Material.CHIPPED_ANVIL || type == Material.DAMAGED_ANVIL
            || type == Material.CRAFTING_TABLE || type == Material.LOOM
            || type == Material.STONECUTTER || type == Material.GRINDSTONE
            || type == Material.CARTOGRAPHY_TABLE || type == Material.SMITHING_TABLE
            || type == Material.BEACON || type == Material.BELL
            || type == Material.LECTERN || name.contains("SHULKER_BOX")
            || type == Material.ITEM_FRAME || type == Material.GLOW_ITEM_FRAME
            || type == Material.ARMOR_STAND || type == Material.REPEATER
            || type == Material.COMPARATOR || type == Material.DAYLIGHT_DETECTOR
            || type == Material.NOTE_BLOCK || type == Material.JUKEBOX
            || name.contains("DOOR") || name.contains("TRAPDOOR")
            || name.contains("FENCE_GATE") || name.contains("BUTTON")
            || name.contains("PRESSURE_PLATE") || name.contains("BED");
    }

    private Player getPlayerFromDamager(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }
}