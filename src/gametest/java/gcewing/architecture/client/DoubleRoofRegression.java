package gcewing.architecture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gcewing.architecture.*;
import gcewing.architecture.shape.*;
import java.util.*;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class DoubleRoofRegression {
  private record Case(BlockState base, BlockState secondary, int caps) {}

  static void verify(Minecraft mc) {
    var stone = Blocks.STONE_BRICKS.defaultBlockState();
    var glass = Blocks.GLASS.defaultBlockState();
    var cases =
        List.of(
            new Case(glass, stone, 2),
            new Case(stone, glass, 2),
            new Case(glass, glass, 0),
            new Case(stone, Blocks.QUARTZ_BLOCK.defaultBlockState(), 0),
            new Case(glass, Blocks.RED_STAINED_GLASS.defaultBlockState(), 4),
            new Case(glass, null, 0));
    for (var test : cases) {
      var data =
          new ShapeData(
              Shape.DoubleRoofTile.id, test.base, Optional.ofNullable(test.secondary), 0, 0);
      for (Direction side : Direction.values())
        for (int turn = 0; turn < 4; turn++) {
          var state =
              ArchitectureCraft.SHAPE
                  .defaultBlockState()
                  .setValue(ShapeBlock.FACING, side)
                  .setValue(ShapeBlock.TURN, turn);
          var orientation = ShapeGeometry.orientation(state, data);
          List<BakedQuad> quads = new ArrayList<>();
          MeshRendering.emit(
              ShapeGeometry.mesh(data, state, null, null),
              data,
              false,
              false,
              null,
              null,
              Renderer.get().mutableMesh().emitter(),
              quads);
          int count = 0;
          for (var q : quads) {
            double[][] local = new double[3][];
            boolean interfaceFace = true;
            for (int v = 0; v < 4; v++) {
              int[] raw = q.vertices();
              var p =
                  orientation.inverseVector(
                      Float.intBitsToFloat(raw[v * 8]) - .5,
                      Float.intBitsToFloat(raw[v * 8 + 1]) - .5,
                      Float.intBitsToFloat(raw[v * 8 + 2]) - .5);
              if (v < 3) local[v] = p;
              interfaceFace &= Math.abs(p[0] - p[1]) < 1e-6;
            }
            if (!interfaceFace) continue;
            count++;
            if (test.base == stone || test.secondary == stone) {
              if (q.sprite() != MeshRendering.materialModel(stone).particleIcon())
                throw new AssertionError("Glass must expose a stone-textured cut face");
              double ux = local[1][0] - local[0][0],
                  uz = local[1][2] - local[0][2],
                  vx = local[2][0] - local[0][0],
                  vz = local[2][2] - local[0][2];
              double ny = uz * vx - ux * vz;
              if (test.base == stone ? ny <= 0 : ny >= 0)
                throw new AssertionError("Cut face points into the opaque half");
            }
          }
          if (count != test.caps)
            throw new AssertionError(
                "Double roof expected " + test.caps + " interior triangles, got " + count);
        }
      // Validate actual item submit layers too: hidden caps must not shift material assignment.
      var item = new ItemStackRenderState();
      mc.getItemModelResolver()
          .updateForTopItem(item, data.stack(1), ItemDisplayContext.GUI, null, null, 42);
      var submits = new SubmitNodeStorage();
      item.submit(new PoseStack(), submits, 0, 0, 0);
      for (var collection : submits.getSubmitsPerOrder().values())
        for (var submit : collection.getItemSubmits())
          for (var q : submit.quads()) {
            if (q.sprite() == MeshRendering.materialModel(stone).particleIcon()
                && submit.renderType() != ItemBlockRenderTypes.getRenderType(stone))
              throw new AssertionError(
                  "Opaque item geometry incorrectly assigned to the glass layer");
          }
    }
  }
}
