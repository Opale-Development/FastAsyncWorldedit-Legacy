package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.bukkit.util.image.BukkitImageViewer;
import com.boydti.fawe.command.CFICommands;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.brush.Brush;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

public class BukkitImageListener implements Listener {
    private Location mutable = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

    public BukkitImageListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(AsyncPlayerChatEvent event) {
        Set<Player> recipients = event.getRecipients();
        Iterator<Player> iter = recipients.iterator();
        while (iter.hasNext()) {
            Player player = iter.next();
            if (player.equals(event.getPlayer())) continue;

            FawePlayer<Object> fp = FawePlayer.wrap(player);
            if (!fp.hasMeta()) continue;

            CFICommands.CFISettings settings = fp.getMeta("CFISettings");
            if (settings == null || !settings.hasGenerator()) continue;

            String name = player.getName().toLowerCase();
            if (!event.getMessage().toLowerCase().contains(name)) {
                ArrayDeque<String> buffered = fp.getMeta("CFIBufferedMessages");
                if (buffered == null) fp.setMeta("CFIBufferedMessaged", buffered = new ArrayDeque<String>());
                String full = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
                buffered.add(full);
                iter.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) return;
        handleInteract(event, (Player) event.getRemover(), event.getEntity(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        handleInteract(event, (Player) event.getDamager(), event.getEntity(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) return;

        Player player = event.getPlayer();
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (fp.getMeta("CFISettings") == null) return;
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        } catch (NoSuchFieldError | NoSuchMethodError ignored) {
        }

        List<Block> target = player.getLastTwoTargetBlocks((Set<Material>) null, 100);
        if (target.isEmpty()) return;

        Block targetBlock = target.get(0);
        World world = player.getWorld();
        mutable.setWorld(world);
        mutable.setX(targetBlock.getX() + 0.5);
        mutable.setY(targetBlock.getY() + 0.5);
        mutable.setZ(targetBlock.getZ() + 0.5);
        Collection<Entity> entities = world.getNearbyEntities(mutable, 0.46875, 0, 0.46875);

        if (!entities.isEmpty()) {
            Action action = event.getAction();
            boolean primary = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

            double minDist = Integer.MAX_VALUE;
            ItemFrame minItemFrame = null;

            for (Entity entity : entities) {
                if (entity instanceof ItemFrame) {
                    ItemFrame itemFrame = (ItemFrame) entity;
                    Location loc = itemFrame.getLocation();
                    double dx = loc.getX() - mutable.getX();
                    double dy = loc.getY() - mutable.getY();
                    double dz = loc.getZ() - mutable.getZ();
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist < minDist) {
                        minItemFrame = itemFrame;
                        minDist = dist;
                    }
                }
            }
            if (minItemFrame != null) {
                handleInteract(event, minItemFrame, primary);
                if (event.isCancelled()) return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        handleInteract(event, event.getRightClicked(), true);
    }

    private BukkitImageViewer get(HeightMapMCAGenerator generator) {
        if (generator == null) return null;

        ImageViewer viewer = generator.getImageViewer();
        if (viewer == null || !(viewer instanceof BukkitImageViewer)) return null;

        BukkitImageViewer biv = (BukkitImageViewer) viewer;
        return biv;
    }

    private void handleInteract(PlayerEvent event, Entity entity, boolean primary) {
        handleInteract(event, event.getPlayer(), entity, primary);
    }

    private void handleInteract(Event event, Player player, Entity entity, boolean primary) {
        if (!(entity instanceof ItemFrame)) return;
        ItemFrame itemFrame = (ItemFrame) entity;

        FawePlayer<Object> fp = FawePlayer.wrap(player);
        CFICommands.CFISettings settings = fp.getMeta("CFISettings");
        HeightMapMCAGenerator generator = settings == null ? null : settings.getGenerator();
        BukkitImageViewer viewer = get(generator);
        if (viewer == null) return;

        if (itemFrame.getRotation() != Rotation.NONE) {
            itemFrame.setRotation(Rotation.NONE);
        }

        LocalSession session = fp.getSession();
        BrushTool tool;
        try {
            tool = session.getBrushTool(fp.getPlayer(), false);
        } catch (InvalidToolBindException e) {
            return;
        }

        ItemFrame[][] frames = viewer.getItemFrames();
        if (frames == null || tool == null) {
            viewer.selectFrame(itemFrame);
            player.updateInventory();
            TaskManager.IMP.laterAsync(new Runnable() {
                @Override
                public void run() {
                    viewer.view(generator);
                }
            }, 1);
            return;
        }

        if (tool == null) return;
        BrushSettings context = primary ? tool.getPrimary() : tool.getSecondary();
        Brush brush = context.getBrush();
        if (brush == null) return;
        tool.setContext(context);

        if (event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(true);
        }

        Location target = itemFrame.getLocation();
        Location source = player.getLocation();

        double yawRad = Math.toRadians(source.getYaw() + 90d);
        double pitchRad = Math.toRadians(-source.getPitch());

        double a = Math.cos(pitchRad);
        double xRat = Math.cos(yawRad) * a;
        double zRat = Math.sin(yawRad) * a;

        BlockFace facing = itemFrame.getFacing();
        double thickness = 1 / 32d + 1 / 128d;
        double modX = facing.getModX();
        double modZ = facing.getModZ();
        double dx = source.getX() - target.getX() - modX * thickness;
        double dy = source.getY() + player.getEyeHeight() - target.getY();
        double dz = source.getZ() - target.getZ() - modZ * thickness;

        double offset;
        double localX;
        if (modX != 0) {
            offset = dx / xRat;
            localX = (-modX) * (dz - offset * zRat);
        } else {
            offset = dz / zRat;
            localX = (modZ) * (dx - offset * xRat);
        }
        double localY = dy - offset * Math.sin(pitchRad);
        int localPixelX = (int) ((localX + 0.5) * 128);
        int localPixelY = (int) ((localY + 0.5) * 128);

        UUID uuid = itemFrame.getUniqueId();
        for (int blockX = 0; blockX < frames.length; blockX++) {
            for (int blockY = 0; blockY < frames[0].length; blockY++) {
                if (uuid.equals(frames[blockX][blockY].getUniqueId())) {
                    int pixelX = localPixelX + blockX * 128;
                    int pixelY = (128 * frames[0].length) - (localPixelY + blockY * 128 + 1);

                    int width = generator.getWidth();
                    int length = generator.getLength();
                    int worldX = (int) (pixelX * width / (frames.length * 128d));
                    int worldZ = (int) (pixelY * length / (frames[0].length * 128d));

                    if (worldX < 0 || worldX > width || worldZ < 0 || worldZ > length) return;

                    Vector wPos = new Vector(worldX, 0, worldZ);

                    fp.runAction(new Runnable() {
                        @Override
                        public void run() {
                            viewer.refresh();
                            int topY = generator.getNearestSurfaceTerrainBlock(wPos.getBlockX(), wPos.getBlockZ(), 255, 0, 255);
                            wPos.mutY(topY);

                            EditSession es = new EditSessionBuilder(fp.getWorld()).player(fp).combineStages(false).autoQueue(false).blockBag(null).limitUnlimited().build();
                            ExtentTraverser last = new ExtentTraverser(es.getExtent()).last();
                            if (last.get() instanceof FastWorldEditExtent) last = last.previous();
                            last.setNext(generator);
                            try {
                                brush.build(es, wPos, context.getMaterial(), context.getSize());
                            } catch (MaxChangedBlocksException e) {
                                e.printStackTrace();
                            }
                            es.flushQueue();
                            viewer.view(generator);
                        }
                    }, true, true);


                    return;
                }
            }
        }
    }
}