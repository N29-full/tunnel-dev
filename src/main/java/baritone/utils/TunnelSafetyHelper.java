/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.player.LocalPlayer;

public class TunnelSafetyHelper {

    /**
     * Check if mining this block with Tunnel 3 would expose lava in the 3x3 area
     * @param level The world
     * @param pos The position being mined
     * @param tool The tool being used
     * @param player The player
     * @param miningDirection The direction being mined (NORTH, SOUTH, EAST, WEST, UP, DOWN)
     * @return true if it's safe to mine (no lava will be exposed)
     */
    public static boolean isSafeToMineWithTunnel(Level level, BlockPos pos, ItemStack tool, LocalPlayer player, Direction miningDirection) {
        // If tunnel is not active (player sneaking or no tunnel), it's always safe (only 1 block)
        if (!CustomEnchantmentHelper.isTunnelActive(tool, player)) {
            return true;
        }

        // Get the 3x3 area that will be mined
        BlockPos[] affectedBlocks = get3x3Area(pos, miningDirection);

        // Check each block in the 3x3 area and blocks adjacent to them
        for (BlockPos checkPos : affectedBlocks) {
            // Check the block itself
            if (isLavaOrWillExposeLava(level, checkPos)) {
                return false;
            }

            // Check blocks around the 3x3 area (to detect lava that would flow in)
            for (Direction dir : Direction.values()) {
                BlockPos adjacentPos = checkPos.relative(dir);
                if (isLava(level, adjacentPos)) {
                    return false; // Lava adjacent to mining area - dangerous!
                }
            }
        }

        return true; // No lava detected
    }

    /**
     * Get the 3x3 area of blocks that will be mined by Tunnel
     */
    private static BlockPos[] get3x3Area(BlockPos center, Direction miningDirection) {
        BlockPos[] positions = new BlockPos[9];

        // Determine the plane perpendicular to mining direction
        if (miningDirection == Direction.NORTH || miningDirection == Direction.SOUTH) {
            // Mining horizontally along Z axis, 3x3 in XY plane
            int idx = 0;
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    positions[idx++] = center.offset(x, y, 0);
                }
            }
        } else if (miningDirection == Direction.EAST || miningDirection == Direction.WEST) {
            // Mining horizontally along X axis, 3x3 in YZ plane
            int idx = 0;
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    positions[idx++] = center.offset(0, y, z);
                }
            }
        } else if (miningDirection == Direction.UP || miningDirection == Direction.DOWN) {
            // Mining vertically, 3x3 in XZ plane
            int idx = 0;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    positions[idx++] = center.offset(x, 0, z);
                }
            }
        } else {
            // Fallback - just return center position repeated
            for (int i = 0; i < 9; i++) {
                positions[i] = center;
            }
        }

        return positions;
    }

    /**
     * Check if a position contains lava
     */
    private static boolean isLava(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.LAVA;
    }

    /**
     * Check if breaking this block would expose lava
     */
    private static boolean isLavaOrWillExposeLava(Level level, BlockPos pos) {
        // Check if the block itself is lava
        if (isLava(level, pos)) {
            return true;
        }

        // Check if any adjacent block is lava (would flow in when mined)
        for (Direction dir : Direction.values()) {
            if (isLava(level, pos.relative(dir))) {
                return true;
            }
        }

        return false;
    }
}