package net.vsh33.storageback.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Feather Block: landing on it deals only 20% of the normal fall damage, like a hay bale. */
public class SoftLandingBlock extends Block {
	public SoftLandingBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
		entity.causeFallDamage(fallDistance, 0.2F, level.damageSources().fall());
	}
}
