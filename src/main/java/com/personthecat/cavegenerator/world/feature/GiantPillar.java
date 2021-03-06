package com.personthecat.cavegenerator.world.feature;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.hjson.JsonObject;

import java.util.Optional;
import java.util.Random;

import static net.minecraft.block.BlockStairs.EnumHalf;
import static net.minecraft.block.BlockStairs.EnumShape;
import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

public class GiantPillar extends WorldGenerator {
    /** A convenient reference to Air. */
    private static final IBlockState BLK_AIR = Blocks.AIR.getDefaultState();

    /** Mandatory fields that must be setup by the constructor. */
    private final IBlockState pillarBlock;
    private final int frequency, minHeight, maxHeight, minLength, maxLength;

    /** A null-safe, optional stair block to surround each pillar. */
    private Optional<BlockStairs> stairBlock = Optional.empty();

    /** From Json */
    public GiantPillar(JsonObject pillar) {
        this(
            getGuranteedState(pillar, "GiantPillar"),
            getIntOr(pillar, "frequency", 15),
            getIntOr(pillar, "minHeight", 10),
            getIntOr(pillar, "maxHeight", 50),
            getIntOr(pillar, "minLength", 5),
            getIntOr(pillar, "maxLength", 12),
            getBlock(pillar, "stairBlock").map(IBlockState::getBlock)
        );
    }

    public GiantPillar(
        IBlockState pillarBlock,
        int frequency,
        int minHeight,
        int maxHeight,
        int minLength,
        int maxLength,
        Optional<Block> stairBlock
    ) {
        this.pillarBlock = pillarBlock;
        this.frequency = frequency;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.stairBlock = getStairBlock(stairBlock);
    }

    public GiantPillar(IBlockState pillarBlock, int frequency, int minHeight, int maxHeight, int minLength, int maxLength) {
        this.pillarBlock = pillarBlock;
        this.frequency = frequency;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    private Optional<BlockStairs> getStairBlock(Optional<Block> block) {
        return block.map(b -> {
            // Validate the input stair block.
            if (b instanceof BlockStairs) {
                return (BlockStairs) b;
            } else {
                throw runExF("Error: the input block, %s, is not a valid stair block.", b.toString());
            }
        });
    }

    public IBlockState getPillarBlock() {
        return pillarBlock;
    }

    public int getFrequency() {
        return frequency;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    /** @param pos is the top block in the pillar. */
    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        final int actualMax = pos.getY();
        final int actualMin = getLowestBlock(world, pos);
        // Verify that the position is possible.
        if (actualMin < 0) return false;

        int length = actualMax - actualMin;
        // Ensure that the difference is within the specified bounds.
        if (length < minLength || length > maxLength) return false;

        for (int y = actualMax; y >= actualMin; y--) {
            BlockPos current = new BlockPos(pos.getX(), y, pos.getZ());
            // Start by placing the initial block.
            world.setBlockState(current, pillarBlock, 2);

            // Handle stair blocks, if applicable.
            if (stairBlock.isPresent()) {
                if (y == actualMax) { // We're at the top. Place stairs upward.
                    testPlaceStairs(world, rand, pos, EnumHalf.TOP); // To-do; this block pos needs to be verified.
                } else if (y == actualMin) { // We're at the bottom. Place stairs downward.
                    testPlaceStairs(world, rand, current, EnumHalf.BOTTOM);
                }
            }
        }

        return true;
    }

    /** Determines the lowest block at the given X, Z coordinates. */
    private int getLowestBlock(World world, BlockPos pos) {
        // Keep track of whether the last block was solid instead of recalculating.
        boolean previouslyAir = true;

        for (pos = pos.down(); pos.getY() > minHeight; pos = pos.down()) {
            // Only calculate whether any block is solid one time.
            boolean currentlyAir = !world.getBlockState(pos).isOpaqueCube();

            if (previouslyAir && !currentlyAir) {
                // We're going down. There was air above us, but not at this position.
                // This is the bottom.
                return pos.getY();
            }
            // Reset the previous air marker.
            previouslyAir = currentlyAir;
        }
        // Nothing was found. Just return -1 instead of boxing it in some container.
        return -1;
    }

    /** Tries to randomly place stair blocks around the pillar in all 4 directions. */
    private void testPlaceStairs(World world, Random rand, BlockPos pos, EnumHalf topOrBottom) {
        if (topOrBottom.equals(EnumHalf.TOP)) {
            testPlaceUp(pos.north(), EnumFacing.SOUTH, rand, world, topOrBottom);
            testPlaceUp(pos.south(), EnumFacing.NORTH, rand, world, topOrBottom);
            testPlaceUp(pos.east(), EnumFacing.WEST, rand, world, topOrBottom);
            testPlaceUp(pos.west(), EnumFacing.EAST, rand, world, topOrBottom);
        } else {
            testPlaceDown(pos.north(), EnumFacing.SOUTH, rand, world, topOrBottom);
            testPlaceDown(pos.south(), EnumFacing.NORTH, rand, world, topOrBottom);
            testPlaceDown(pos.east(), EnumFacing.WEST, rand, world, topOrBottom);
            testPlaceDown(pos.west(), EnumFacing.EAST, rand, world, topOrBottom);
        }
    }

    /**
     * Randomly looks +-3 blocks vertically surrounding the given X, Z
     * coordinates. Places stairs given a ~1/3 chance.
     */
    private void testPlaceDown(BlockPos pos, EnumFacing facing, Random rand, World world, EnumHalf topOrBottom) {
        BlockPos previous = pos.down(4);
        // Iterate 3 blocks down, 3 blocks up.
        for (int i = - 3; i <= 3; i++) {
            final BlockPos current = pos.up(i);
            // Verify that we're within the height bounds. Stop randomly.
            if (current.getY() >= minHeight && rand.nextInt(2) == 0) {
                // Find a boundary between solid and air.
                if (world.getBlockState(previous).isOpaqueCube() && world.getBlockState(current).equals(BLK_AIR)) {
                    // Replace air.
                    world.setBlockState(current, getStairRotation(facing, topOrBottom), 16);
                    return;
                }
            }
            previous = current;
        }
    }

    private void testPlaceUp(BlockPos pos, EnumFacing facing, Random rand, World world, EnumHalf topOrBottom) {
        BlockPos previous = pos.up(4);
        for (int i =  3; i >= - 3; i--) {
            final BlockPos current = pos.up(i);
            if (current.getY() >= minHeight && rand.nextInt(2) == 0) {
                // pos.up will add or subtract and is thus still valid.
                if (world.getBlockState(previous).isOpaqueCube() && world.getBlockState(current).equals(BLK_AIR)) {
                    world.setBlockState(current, getStairRotation(facing, topOrBottom), 16);
                    return;
                }
            }
            previous = current;
        }
    }

    /**
     * Determines the correct stair rotation for the given properties.
     * It may make sense to pre-calculate this.
     */
    private IBlockState getStairRotation(EnumFacing facing, EnumHalf topOrBottom) {
        // The null check is handled above.
        return stairBlock.get().getDefaultState()
            .withProperty(BlockStairs.FACING, facing)
            .withProperty(BlockStairs.HALF, topOrBottom)
            .withProperty(BlockStairs.SHAPE, EnumShape.STRAIGHT);
    }
}