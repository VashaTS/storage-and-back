package net.vsh33.storageback.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cluster of nine touching pearls: eight in the corners of the block and one in the centre.
 * The outline and collision match the stepped-sphere model; the block is not a full cube, so
 * you can see (and light can pass) between the pearls.
 */
public class EnderPearlBlock extends Block {
	/** Union of the same cuboid slices used by the block model (tools/gen_pearl_model.py). */
	public static final VoxelShape SHAPE = Shapes.or(
			Block.box(2.0, 0.0, 2.56, 6.0, 1.0, 5.44),
			Block.box(2.56, 0.0, 2.0, 5.44, 1.0, 2.56),
			Block.box(2.56, 0.0, 5.44, 5.44, 1.0, 6.0),
			Block.box(1.0, 1.0, 1.84, 7.0, 2.0, 6.16),
			Block.box(1.84, 1.0, 1.0, 6.16, 2.0, 1.84),
			Block.box(1.84, 1.0, 6.16, 6.16, 2.0, 7.0),
			Block.box(0.0, 2.0, 1.12, 8.0, 6.0, 6.88),
			Block.box(1.12, 2.0, 0.0, 6.88, 6.0, 1.12),
			Block.box(1.12, 2.0, 6.88, 6.88, 6.0, 8.0),
			Block.box(1.0, 6.0, 1.84, 7.0, 7.0, 6.16),
			Block.box(1.84, 6.0, 1.0, 6.16, 7.0, 1.84),
			Block.box(1.84, 6.0, 6.16, 6.16, 7.0, 7.0),
			Block.box(2.0, 7.0, 2.56, 6.0, 8.0, 5.44),
			Block.box(2.56, 7.0, 2.0, 5.44, 8.0, 2.56),
			Block.box(2.56, 7.0, 5.44, 5.44, 8.0, 6.0),
			Block.box(10.0, 0.0, 2.56, 14.0, 1.0, 5.44),
			Block.box(10.56, 0.0, 2.0, 13.44, 1.0, 2.56),
			Block.box(10.56, 0.0, 5.44, 13.44, 1.0, 6.0),
			Block.box(9.0, 1.0, 1.84, 15.0, 2.0, 6.16),
			Block.box(9.84, 1.0, 1.0, 14.16, 2.0, 1.84),
			Block.box(9.84, 1.0, 6.16, 14.16, 2.0, 7.0),
			Block.box(8.0, 2.0, 1.12, 16.0, 6.0, 6.88),
			Block.box(9.12, 2.0, 0.0, 14.88, 6.0, 1.12),
			Block.box(9.12, 2.0, 6.88, 14.88, 6.0, 8.0),
			Block.box(9.0, 6.0, 1.84, 15.0, 7.0, 6.16),
			Block.box(9.84, 6.0, 1.0, 14.16, 7.0, 1.84),
			Block.box(9.84, 6.0, 6.16, 14.16, 7.0, 7.0),
			Block.box(10.0, 7.0, 2.56, 14.0, 8.0, 5.44),
			Block.box(10.56, 7.0, 2.0, 13.44, 8.0, 2.56),
			Block.box(10.56, 7.0, 5.44, 13.44, 8.0, 6.0),
			Block.box(2.0, 8.0, 2.56, 6.0, 9.0, 5.44),
			Block.box(2.56, 8.0, 2.0, 5.44, 9.0, 2.56),
			Block.box(2.56, 8.0, 5.44, 5.44, 9.0, 6.0),
			Block.box(1.0, 9.0, 1.84, 7.0, 10.0, 6.16),
			Block.box(1.84, 9.0, 1.0, 6.16, 10.0, 1.84),
			Block.box(1.84, 9.0, 6.16, 6.16, 10.0, 7.0),
			Block.box(0.0, 10.0, 1.12, 8.0, 14.0, 6.88),
			Block.box(1.12, 10.0, 0.0, 6.88, 14.0, 1.12),
			Block.box(1.12, 10.0, 6.88, 6.88, 14.0, 8.0),
			Block.box(1.0, 14.0, 1.84, 7.0, 15.0, 6.16),
			Block.box(1.84, 14.0, 1.0, 6.16, 15.0, 1.84),
			Block.box(1.84, 14.0, 6.16, 6.16, 15.0, 7.0),
			Block.box(2.0, 15.0, 2.56, 6.0, 16.0, 5.44),
			Block.box(2.56, 15.0, 2.0, 5.44, 16.0, 2.56),
			Block.box(2.56, 15.0, 5.44, 5.44, 16.0, 6.0),
			Block.box(10.0, 8.0, 2.56, 14.0, 9.0, 5.44),
			Block.box(10.56, 8.0, 2.0, 13.44, 9.0, 2.56),
			Block.box(10.56, 8.0, 5.44, 13.44, 9.0, 6.0),
			Block.box(9.0, 9.0, 1.84, 15.0, 10.0, 6.16),
			Block.box(9.84, 9.0, 1.0, 14.16, 10.0, 1.84),
			Block.box(9.84, 9.0, 6.16, 14.16, 10.0, 7.0),
			Block.box(8.0, 10.0, 1.12, 16.0, 14.0, 6.88),
			Block.box(9.12, 10.0, 0.0, 14.88, 14.0, 1.12),
			Block.box(9.12, 10.0, 6.88, 14.88, 14.0, 8.0),
			Block.box(9.0, 14.0, 1.84, 15.0, 15.0, 6.16),
			Block.box(9.84, 14.0, 1.0, 14.16, 15.0, 1.84),
			Block.box(9.84, 14.0, 6.16, 14.16, 15.0, 7.0),
			Block.box(10.0, 15.0, 2.56, 14.0, 16.0, 5.44),
			Block.box(10.56, 15.0, 2.0, 13.44, 16.0, 2.56),
			Block.box(10.56, 15.0, 5.44, 13.44, 16.0, 6.0),
			Block.box(2.0, 0.0, 10.56, 6.0, 1.0, 13.44),
			Block.box(2.56, 0.0, 10.0, 5.44, 1.0, 10.56),
			Block.box(2.56, 0.0, 13.44, 5.44, 1.0, 14.0),
			Block.box(1.0, 1.0, 9.84, 7.0, 2.0, 14.16),
			Block.box(1.84, 1.0, 9.0, 6.16, 2.0, 9.84),
			Block.box(1.84, 1.0, 14.16, 6.16, 2.0, 15.0),
			Block.box(0.0, 2.0, 9.12, 8.0, 6.0, 14.88),
			Block.box(1.12, 2.0, 8.0, 6.88, 6.0, 9.12),
			Block.box(1.12, 2.0, 14.88, 6.88, 6.0, 16.0),
			Block.box(1.0, 6.0, 9.84, 7.0, 7.0, 14.16),
			Block.box(1.84, 6.0, 9.0, 6.16, 7.0, 9.84),
			Block.box(1.84, 6.0, 14.16, 6.16, 7.0, 15.0),
			Block.box(2.0, 7.0, 10.56, 6.0, 8.0, 13.44),
			Block.box(2.56, 7.0, 10.0, 5.44, 8.0, 10.56),
			Block.box(2.56, 7.0, 13.44, 5.44, 8.0, 14.0),
			Block.box(10.0, 0.0, 10.56, 14.0, 1.0, 13.44),
			Block.box(10.56, 0.0, 10.0, 13.44, 1.0, 10.56),
			Block.box(10.56, 0.0, 13.44, 13.44, 1.0, 14.0),
			Block.box(9.0, 1.0, 9.84, 15.0, 2.0, 14.16),
			Block.box(9.84, 1.0, 9.0, 14.16, 2.0, 9.84),
			Block.box(9.84, 1.0, 14.16, 14.16, 2.0, 15.0),
			Block.box(8.0, 2.0, 9.12, 16.0, 6.0, 14.88),
			Block.box(9.12, 2.0, 8.0, 14.88, 6.0, 9.12),
			Block.box(9.12, 2.0, 14.88, 14.88, 6.0, 16.0),
			Block.box(9.0, 6.0, 9.84, 15.0, 7.0, 14.16),
			Block.box(9.84, 6.0, 9.0, 14.16, 7.0, 9.84),
			Block.box(9.84, 6.0, 14.16, 14.16, 7.0, 15.0),
			Block.box(10.0, 7.0, 10.56, 14.0, 8.0, 13.44),
			Block.box(10.56, 7.0, 10.0, 13.44, 8.0, 10.56),
			Block.box(10.56, 7.0, 13.44, 13.44, 8.0, 14.0),
			Block.box(2.0, 8.0, 10.56, 6.0, 9.0, 13.44),
			Block.box(2.56, 8.0, 10.0, 5.44, 9.0, 10.56),
			Block.box(2.56, 8.0, 13.44, 5.44, 9.0, 14.0),
			Block.box(1.0, 9.0, 9.84, 7.0, 10.0, 14.16),
			Block.box(1.84, 9.0, 9.0, 6.16, 10.0, 9.84),
			Block.box(1.84, 9.0, 14.16, 6.16, 10.0, 15.0),
			Block.box(0.0, 10.0, 9.12, 8.0, 14.0, 14.88),
			Block.box(1.12, 10.0, 8.0, 6.88, 14.0, 9.12),
			Block.box(1.12, 10.0, 14.88, 6.88, 14.0, 16.0),
			Block.box(1.0, 14.0, 9.84, 7.0, 15.0, 14.16),
			Block.box(1.84, 14.0, 9.0, 6.16, 15.0, 9.84),
			Block.box(1.84, 14.0, 14.16, 6.16, 15.0, 15.0),
			Block.box(2.0, 15.0, 10.56, 6.0, 16.0, 13.44),
			Block.box(2.56, 15.0, 10.0, 5.44, 16.0, 10.56),
			Block.box(2.56, 15.0, 13.44, 5.44, 16.0, 14.0),
			Block.box(10.0, 8.0, 10.56, 14.0, 9.0, 13.44),
			Block.box(10.56, 8.0, 10.0, 13.44, 9.0, 10.56),
			Block.box(10.56, 8.0, 13.44, 13.44, 9.0, 14.0),
			Block.box(9.0, 9.0, 9.84, 15.0, 10.0, 14.16),
			Block.box(9.84, 9.0, 9.0, 14.16, 10.0, 9.84),
			Block.box(9.84, 9.0, 14.16, 14.16, 10.0, 15.0),
			Block.box(8.0, 10.0, 9.12, 16.0, 14.0, 14.88),
			Block.box(9.12, 10.0, 8.0, 14.88, 14.0, 9.12),
			Block.box(9.12, 10.0, 14.88, 14.88, 14.0, 16.0),
			Block.box(9.0, 14.0, 9.84, 15.0, 15.0, 14.16),
			Block.box(9.84, 14.0, 9.0, 14.16, 15.0, 9.84),
			Block.box(9.84, 14.0, 14.16, 14.16, 15.0, 15.0),
			Block.box(10.0, 15.0, 10.56, 14.0, 16.0, 13.44),
			Block.box(10.56, 15.0, 10.0, 13.44, 16.0, 10.56),
			Block.box(10.56, 15.0, 13.44, 13.44, 16.0, 14.0),
			Block.box(5.8612, 5.0718, 6.4601, 10.1388, 6.9, 9.5399),
			Block.box(6.4601, 5.0718, 5.8612, 9.5399, 6.9, 6.4601),
			Block.box(6.4601, 5.0718, 9.5399, 9.5399, 6.9, 10.1388),
			Block.box(5.0718, 6.9, 5.8917, 10.9282, 9.1, 10.1083),
			Block.box(5.8917, 6.9, 5.0718, 10.1083, 9.1, 5.8917),
			Block.box(5.8917, 6.9, 10.1083, 10.1083, 9.1, 10.9282),
			Block.box(5.8612, 9.1, 6.4601, 10.1388, 10.9282, 9.5399),
			Block.box(6.4601, 9.1, 5.8612, 9.5399, 10.9282, 6.4601),
			Block.box(6.4601, 9.1, 9.5399, 9.5399, 10.9282, 10.1388)
	);

	public EnderPearlBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return Shapes.empty();
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}
}
