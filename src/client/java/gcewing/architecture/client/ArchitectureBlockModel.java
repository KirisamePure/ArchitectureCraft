package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.*;
import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class ArchitectureBlockModel implements BlockStateModel {
  private final BlockStateModel fallback;
  private final boolean sawbench;

  public ArchitectureBlockModel(BlockStateModel fallback, boolean sawbench) {
    this.fallback = fallback;
    this.sawbench = sawbench;
  }

  @Override
  public void collectParts(RandomSource random, List<BlockModelPart> parts) {
    fallback.collectParts(random, parts);
  }

  @Override
  public TextureAtlasSprite particleIcon() {
    return fallback.particleIcon();
  }

  @Override
  public TextureAtlasSprite particleSprite(
      BlockAndTintGetter world, BlockPos pos, BlockState state) {
    if (world.getBlockEntity(pos) instanceof ShapeBlockEntity be)
      return MeshRendering.materialModel(be.data().base()).particleIcon();
    return particleIcon();
  }

  @Override
  public void emitQuads(
      QuadEmitter emitter,
      BlockAndTintGetter world,
      BlockPos pos,
      BlockState state,
      RandomSource random,
      Predicate<Direction> cullTest) {
    ShapeData data =
        world.getBlockEntity(pos) instanceof ShapeBlockEntity be ? be.data() : ShapeData.defaults();
    Mesh mesh =
        sawbench
            ? Mesh.model("block/sawbench")
                .transformed(
                    new Orientation(1, state.getValue(SawbenchBlock.FACING).get2DDataValue(), 0))
            : ShapeGeometry.mesh(data, state, world, pos);
    MeshRendering.emit(
        mesh,
        data,
        sawbench,
        !sawbench && state.getValue(ShapeBlock.GLOWING),
        world,
        pos,
        emitter,
        null);
  }
}
