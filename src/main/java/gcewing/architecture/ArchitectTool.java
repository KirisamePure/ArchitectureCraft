package gcewing.architecture;

import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ArchitectTool extends Item {
  public enum Kind {
    CHISEL,
    HAMMER,
    GLOWBRUSH
  }

  private final Kind kind;

  public ArchitectTool(Properties properties, Kind kind) {
    super(properties);
    this.kind = kind;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var level = context.getLevel();
    var pos = context.getClickedPos();
    var player = context.getPlayer();
    if (player == null || !player.mayBuild() || !level.mayInteract(player, pos))
      return InteractionResult.PASS;
    if (!(level.getBlockEntity(pos) instanceof ShapeBlockEntity be)) return InteractionResult.PASS;
    if (level.isClientSide()) return InteractionResult.SUCCESS;
    BlockState state = be.getBlockState();
    ShapeData data = be.data();
    switch (kind) {
      case HAMMER -> {
        if (player.isShiftKeyDown()) state = state.cycle(ShapeBlock.FACING);
        else {
          if (data.offset() != 0) data = data.withOffset(-data.offset());
          if (data.offset() >= 0) state = state.cycle(ShapeBlock.TURN);
        }
        level.setBlock(pos, state, 3);
        be.setData(data);
      }
      case GLOWBRUSH -> {
        level.setBlock(pos, state.setValue(ShapeBlock.GLOWING, !player.isShiftKeyDown()), 3);
        be.setChanged();
      }
      case CHISEL -> {
        if (player.isShiftKeyDown()) {
          Direction face = context.getClickedFace();
          int mask = data.disconnected() ^ (1 << face.get3DDataValue());
          be.setData(data.withConnections(mask));
          if (level.getBlockEntity(pos.relative(face)) instanceof ShapeBlockEntity other) {
            int bit = 1 << face.getOpposite().get3DDataValue();
            other.setData(
                other
                    .data()
                    .withConnections(
                        (mask & (1 << face.get3DDataValue())) != 0
                            ? other.data().disconnected() | bit
                            : other.data().disconnected() & ~bit));
          }
        } else if (data.secondary().isPresent()) {
          ItemStack returned;
          if (data.shape().window()) returned = new ItemStack(data.secondary().get().getBlock());
          else {
            returned = new ItemStack(ArchitectureCraft.CLADDING);
            returned.set(
                ArchitectureCraft.SHAPE_DATA,
                new ShapeData(60, data.secondary().get(), java.util.Optional.empty(), 0, 0));
          }
          if (!player.getAbilities().instabuild) Block.popResource(level, pos, returned);
          be.setData(data.withSecondary(null));
        }
      }
    }
    return InteractionResult.SUCCESS;
  }
}
