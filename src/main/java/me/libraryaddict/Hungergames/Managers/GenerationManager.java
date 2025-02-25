package me.libraryaddict.Hungergames.Managers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import me.libraryaddict.Hungergames.Hungergames;
import me.libraryaddict.Hungergames.Configs.LoggerConfig;
import me.libraryaddict.Hungergames.Types.CordPair;
import me.libraryaddict.Hungergames.Types.HungergamesApi;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class GenerationManager {

    private boolean background;
    private BukkitRunnable chunkGeneratorRunnable;
    private ArrayList<CordPair> chunksToGenerate = new ArrayList<CordPair>();
    private LinkedList<Block> dontProcessBlocks = new LinkedList<Block>();
    private List<BlockFace> faces = new ArrayList<BlockFace>();
    private List<BlockFace> jungleFaces = new ArrayList<BlockFace>();
    private LoggerConfig loggerConfig = HungergamesApi.getConfigManager().getLoggerConfig();
    private HashMap<Block, BlockData> queued = new HashMap<Block, BlockData>();
    private BukkitRunnable setBlocksRunnable;

    public GenerationManager() {
        faces.add(BlockFace.UP);
        faces.add(BlockFace.DOWN);
        faces.add(BlockFace.SOUTH);
        faces.add(BlockFace.NORTH);
        faces.add(BlockFace.WEST);
        faces.add(BlockFace.EAST);
        faces.add(BlockFace.SELF);
        jungleFaces.add(BlockFace.UP);
        jungleFaces.add(BlockFace.SELF);
        jungleFaces.add(BlockFace.DOWN);
    }

    public void addToProcessedBlocks(Block block) {
        dontProcessBlocks.add(block);
    }

    public void generateChunks() {
        if (chunkGeneratorRunnable == null) {
            // Get the measurements I need
            int chunks1 = Bukkit.getViewDistance() + 3;
            int chunks2 = 0;
            Hungergames hungergames = HungergamesApi.getHungergames();
            YamlConfiguration mapConfig = YamlConfiguration.loadConfiguration(new File(hungergames.getDataFolder(), "map.yml"));
            // Does the chunk generation run while players are online
            background = true;
            // Load the map configs to find out generation range
            if (mapConfig.getBoolean("GenerateChunks")) {
                chunks2 = (int) Math.ceil(HungergamesApi.getConfigManager().getMainConfig().getBorderSize() / 16)
                        + Bukkit.getViewDistance();
                background = mapConfig.getBoolean("GenerateChunksBackground");
            }
            // Get the max chunk distance
            int chunksDistance = Math.max(chunks1, chunks2);
            Chunk spawn = hungergames.world.getSpawnLocation().getChunk();
            for (int x = -chunksDistance; x <= chunksDistance; x++) {
                for (int z = -chunksDistance; z <= chunksDistance; z++) {
                    CordPair pair = new CordPair(spawn.getX() + x, spawn.getZ() + z);
                    chunksToGenerate.add(pair);
                }
            }
            final double totalChunks = chunksToGenerate.size();
            final World world = hungergames.world;
            final boolean doLogging = mapConfig.getBoolean("GenerateChunks");
            chunkGeneratorRunnable = new BukkitRunnable() {
                private double chunksGenerated;
                private long lastLogged;

                public void run() {
                    if (doLogging && lastLogged + 5000 < System.currentTimeMillis()) {
                        System.out.println(String.format(loggerConfig.getGeneratingChunks(),
                                (int) Math.floor((chunksGenerated / totalChunks) * 100))
                                + "%");
                        lastLogged = System.currentTimeMillis();
                    }
                    long startedGeneration = System.currentTimeMillis();
                    Iterator<CordPair> cordsItel = chunksToGenerate.iterator();
                    while (cordsItel.hasNext() && startedGeneration + (background ? 50 : 5000) > System.currentTimeMillis()) {
                        CordPair pair = cordsItel.next();
                        if (!world.isChunkLoaded(pair.getX(), pair.getZ())) {
                            world.loadChunk(pair.getX(), pair.getZ());
                            world.unloadChunk(pair.getX(), pair.getZ());
                        }
                        cordsItel.remove();
                        chunksGenerated++;
                    }
                    if (!cordsItel.hasNext()) {
                        if (doLogging) {
                            System.out.println(String.format(HungergamesApi.getConfigManager().getLoggerConfig()
                                    .getChunksGenerated(), (int) chunksGenerated));
                        }
                        chunkGeneratorRunnable = null;
                        world.save();
                        cancel();
                    }
                }
            };
            chunkGeneratorRunnable.runTaskTimer(hungergames, 1, 3);
        }
    }

    /**
     * Generate pillars..
     */
    public void generatePillars(Location loc, int radius, BlockData pillarCorner, BlockData pillarInside) {
        int[] cords = new int[] { (int) (-radius / 2.5), (int) (radius / 2.5) };
        int pillarRadius = Math.round(radius / 8);
        for (int px = 0; px <= 1; px++)
            for (int pz = 0; pz <= 1; pz++)
                for (int x = -pillarRadius; x <= pillarRadius; x++) {
                    for (int z = -pillarRadius; z <= pillarRadius; z++) {
                        Block b = loc.getWorld().getBlockAt(x + loc.getBlockX() + cords[px], loc.getBlockY() - 2,
                                z + loc.getBlockZ() + cords[pz]);
                        while (!b.getType().isSolid() || b.isLiquid()) {
                            if (Math.abs(x) == pillarRadius && Math.abs(z) == pillarRadius)
                                setBlockFast(b, pillarCorner);
                            else
                                setBlockFast(b, pillarInside);
                            b = b.getRelative(BlockFace.DOWN);
                        }
                    }
                }
    }

    /**
     * Generates a feast
     * 
     * @param loc
     * @param lowestLevel
     * @param radius
     */
    public void generatePlatform(Location loc, int lowestLevel, int radius, int yHeight, BlockData platformGround) {
        loc.setY(lowestLevel + 1);
        double radiusSquared = radius * radius;
        // Sets to air and generates to stand on
        yHeight = Math.min(loc.getBlockY() + yHeight, loc.getWorld().getMaxHeight());
        for (int radiusX = -radius; radiusX <= radius; radiusX++) {
            for (int radiusZ = -radius; radiusZ <= radius; radiusZ++) {
                if ((radiusX * radiusX) + (radiusZ * radiusZ) <= radiusSquared) {
                    for (int y = yHeight; y >= loc.getBlockY() - 1; y--) {
                        Block b = loc.getWorld().getBlockAt(radiusX + loc.getBlockX(), y, radiusZ + loc.getBlockZ());
                        if (y >= loc.getBlockY()) {// If its less then 0
                            setBlockFast(b, Material.AIR.createBlockData());
                        } else {
                            // Generates to stand on
                            setBlockFast(b, platformGround);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a feast
     * 
     * @param loc
     * @param lowestLevel
     * @param radius
     * @param groundType
     */
    public void generatePlatform(Location loc, int lowestLevel, int radius, BlockData groundType) {
        int yHeight = radius;
        if (yHeight < 4)
            yHeight = 4;
        generatePlatform(loc, lowestLevel, radius, yHeight, groundType);
    }

    public int getHeight(ArrayList<Integer> heights, int radius) {
        List<List<Integer>> commons = new ArrayList<List<Integer>>();
        for (int i = 0; i < heights.size(); i++) {
            List<Integer> numbers = new ArrayList<Integer>();
            numbers.add(heights.get(i));
            for (int a = i; a < heights.size(); a++) {
                if (heights.get(i) - heights.get(a) >= -radius)
                    numbers.add(heights.get(a));
                else
                    break;
            }
            commons.add(numbers);
        }
        int highest = 0;
        List<Integer> found = new ArrayList<Integer>();
        for (List l : commons) {
            if (l.size() > highest) {
                highest = l.size();
                found = l;
            }
        }
        if (found.size() == 0)
            return 0;
        return found.get((int) Math.round(found.size() / 3));
    }

    private Block getHighest(Block b) {
        if (b.getType().isSolid()) {
            return b;
        }
        while (b.getY() > 0 && !b.getRelative(BlockFace.DOWN).getType().isSolid())
            b = b.getRelative(BlockFace.DOWN);
        return b.getRelative(BlockFace.DOWN);
    }

    /**
     * Gets the best Y level to spawn the feast at This also modifies the Location fed to it for use by feast generation
     * 
     * @param loc
     * @param radius
     * @return Best Y Level
     */
    public int getSpawnHeight(Location loc, int radius) {
        ArrayList<Integer> heightLevels = new ArrayList<Integer>();
        for (double degree = 0; degree <= 360; degree += 1) {
            double angle = degree * Math.PI / 180;
            int x = (int) (loc.getX() + .5 + radius * Math.cos(angle));
            int z = (int) (loc.getZ() + radius * Math.sin(angle));
            Block b = getHighest(loc.getWorld().getHighestBlockAt(x, z));
            if (isBlockValid(b)) {
                heightLevels.add(b.getY());
            }
            /*
             * // Do it again but at 2/3 the radius angle = degree * Math.PI /
             * 180; x = (int) (loc.getX() + .5 + ((radius / 3) * 2) *
             * Math.cos(angle)); z = (int) (loc.getZ() + ((radius / 3) * 2) *
             * Math.sin(angle)); b =
             * getHighest(loc.getWorld().getHighestBlockAt(x, z)); if
             * (!b.getChunk().isLoaded()) b.getChunk().load(true); if
             * (isBlockValid(b)) heightLevels.add(b.getY());
             */
        }
        Block b = getHighest(loc.getBlock());
        if (isBlockValid(b)) {
            heightLevels.add(b.getY());
        }
        Collections.sort(heightLevels);
        int y = getHeight(heightLevels, 5);
        if (y == -1)
            y = b.getY();
        loc = new Location(loc.getWorld(), loc.getBlockX(), y + 1, loc.getBlockZ());
        return y;
    }

    private boolean isBlockValid(Block b) {
        // if (b.isLiquid() || b.getRelative(BlockFace.UP).isLiquid())
        // return false;
        return true;
    }

    public boolean isChunkGeneratorRunning() {
        return chunkGeneratorRunnable != null && !background;
    }

    private void removeLeaves(Block b) {
        for (BlockFace face : ((b.getBiome() == Biome.JUNGLE || b.getBiome() == Biome.BAMBOO_JUNGLE || !HungergamesApi
                .getConfigManager().getFeastConfig().isRemoveTrees()) ? jungleFaces : faces)) {
            Block newB = b.getRelative(face);
            // If the blocks are useless decoration
            if (newB.getBlockData() instanceof Leaves || Tag.LOGS.isTagged(newB.getType()) || newB.getType() == Material.VINE) {
                // If they are not queued for deletion and are not marked as dont process
                if (!queued.containsKey(newB) && !dontProcessBlocks.contains(newB)) {
                    // Set it to air
                    setBlockFast(newB, Material.AIR.createBlockData());
                    // If the ground under it is dirt cos it was a tree or something
                    if (newB.getRelative(BlockFace.DOWN).getType() == Material.DIRT) {
                        // Get the block and check if it can turn it to grass
                        newB = newB.getRelative(BlockFace.DOWN);
                        if (!queued.containsKey(newB) && !dontProcessBlocks.contains(newB)) {
                            setBlockFast(newB, Material.GRASS.createBlockData());
                        }
                    }
                }
                // Else remove floating snow.
            } else if (newB.getType() == Material.SNOW && face == BlockFace.UP) {
                if (!queued.containsKey(newB) && !dontProcessBlocks.contains(newB)) {
                    setBlockFast(newB,  Material.AIR.createBlockData());
                }
            }
        }
    }

    public void setBlockFast(Block b, BlockData blockData) {
        try {
            if (!dontProcessBlocks.contains(b) && (b.getBlockData() != blockData)) {
                queued.put(b, blockData);
                if (setBlocksRunnable == null) {
                    setBlocksRunnable = new BukkitRunnable() {
                        public void run() {
                            if (queued.size() == 0) {
                                setBlocksRunnable = null;
                                dontProcessBlocks.clear();
                                cancel();
                            }
                            int i = 0;
                            HashMap<Block, BlockData> toDo = new HashMap<Block, BlockData>();
                            for (Block b : queued.keySet()) {
                                if (i++ >= 200)
                                    break;
                                if (b.getBlockData() == queued.get(b))
                                    i--;
                                toDo.put(b, queued.get(b));
                                if (dontProcessBlocks.contains(b))
                                    continue;
                                b = b.getRelative(BlockFace.UP);
                                while (b != null
                                        && queued.containsKey(b)
                                        && (b.isLiquid() || b.getType() == Material.SAND || b.getType() == Material.ANVIL || b
                                                .getType() == Material.GRAVEL)) {
                                    toDo.put(b, queued.get(b));
                                    b = b.getRelative(BlockFace.UP);
                                }
                            }
                            for (Block b : toDo.keySet()) {
                                queued.remove(b);
                                if (b.getBlockData() == toDo.get(b)
                                        || dontProcessBlocks.contains(b))
                                    continue;
                                // boolean loadChunk = !b.getWorld().isChunkLoaded(b.getChunk().getX(), b.getChunk().getZ());
                                // if (!loadChunk)
                                // b.getWorld().loadChunk(b.getChunk().getX(), b.getChunk().getZ());
                                b.setBlockData(toDo.get(b), true);
                                // if (!loadChunk)
                                // b.getWorld().unloadChunk(b.getChunk().getX(), b.getChunk().getZ());
                                removeLeaves(b);
                            }
                        }
                    };
                    setBlocksRunnable.runTaskTimer(HungergamesApi.getHungergames(), 2, 1);
                }
                // return ((CraftChunk) b.getChunk()).getHandle().a(b.getX() & 15,
                // b.getY(), b.getZ() & 15, typeId, data);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
