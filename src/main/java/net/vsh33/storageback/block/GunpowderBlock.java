package net.vsh33.storageback.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Gunpowder Block: falls like sand, with grey falling dust (1.20.1 has no ColoredFallingBlock). */
public class GunpowderBlock extends FallingBlock {
	public GunpowderBlock(Properties properties) {
		super(properties);
	}

	@Override
	public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
		return 0xFF5A5A5A;
	}
}
