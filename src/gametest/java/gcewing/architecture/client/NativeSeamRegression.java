package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.Shape;
import java.util.*;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Exercises the native model's output, not just the ArchitectureCraft side of a shared face. */
final class NativeSeamRegression {
  private record Case(
      Direction face, Shape shape, BlockState material, Direction mounting, double visible) {}

  private static final List<Case> CASES = new ArrayList<>();

  static {
    for (Direction d : Direction.values())
      CASES.add(new Case(d, Shape.SquareSE, Blocks.GLASS.defaultBlockState(), Direction.UP, 0));
    CASES.add(
        new Case(Direction.WEST, Shape.Slab, Blocks.GLASS.defaultBlockState(), Direction.UP, .5));
    CASES.add(
        new Case(Direction.WEST, Shape.Slab, Blocks.GLASS.defaultBlockState(), Direction.DOWN, .5));
    CASES.add(
        new Case(
            Direction.WEST, Shape.RoofTile, Blocks.GLASS.defaultBlockState(), Direction.UP, .5));
    CASES.add(
        new Case(
            Direction.WEST,
            Shape.WindowFrame,
            Blocks.GLASS.defaultBlockState(),
            Direction.UP,
            .8125));
    CASES.add(
        new Case(
            Direction.WEST, Shape.SquareSE, Blocks.STONE.defaultBlockState(), Direction.UP, 1));
    CASES.add(
        new Case(
            Direction.WEST,
            Shape.SquareSE,
            Blocks.RED_STAINED_GLASS.defaultBlockState(),
            Direction.UP,
            1));
    CASES.add(new Case(Direction.WEST, null, Blocks.GLASS.defaultBlockState(), Direction.UP, 1));
  }

  private static BlockPos position(int i) {
    return new BlockPos(24 + (i % 5) * 3, 5 + (i / 5) * 3, 20);
  }

  static void setup(ServerLevel level) {
    for (int i = 0; i < CASES.size(); i++) {
      Case c = CASES.get(i);
      BlockPos p = position(i);
      level.setBlockAndUpdate(p, Blocks.GLASS.defaultBlockState());
      if (c.shape == null) continue;
      BlockPos neighbor = p.relative(c.face);
      level.setBlockAndUpdate(
          neighbor,
          ArchitectureCraft.SHAPE.defaultBlockState().setValue(ShapeBlock.FACING, c.mounting));
      ((ShapeBlockEntity) level.getBlockEntity(neighbor))
          .setData(new ShapeData(c.shape.id, c.material, Optional.empty(), 0, 0));
    }
  }

  static void verify(Minecraft mc) {
    for (int i = 0; i < CASES.size(); i++) {
      Case c = CASES.get(i);
      BlockPos p = position(i);
      var state = mc.level.getBlockState(p);
      var model = mc.getBlockRenderer().getBlockModel(state);
      if (!(model instanceof NativeShapeSeamModel))
        throw new AssertionError("Native glass model was not wrapped");
      var mesh = Renderer.get().mutableMesh();
      model.emitQuads(mesh.emitter(), mc.level, p, state, RandomSource.create(42), face -> false);
      double[] area = {0};
      mesh.forEach(
          q -> {
            if (q.lightFace() != c.face) return;
            area[0] += area(q, 0, 1, 2) + area(q, 0, 2, 3);
            if (q.cullFace() != c.face || q.emissive())
              throw new AssertionError("Clipping changed native render flags");
            for (int v = 0; v < 4; v++) {
              var sprite = model.particleIcon();
              if (q.u(v) < sprite.getU0() - 1e-6
                  || q.u(v) > sprite.getU1() + 1e-6
                  || q.v(v) < sprite.getV0() - 1e-6
                  || q.v(v) > sprite.getV1() + 1e-6)
                throw new AssertionError("Clipping stretched UVs outside the material sprite");
            }
          });
      if (Math.abs(area[0] - c.visible) > 1e-6)
        throw new AssertionError(
            "Native shared face case "
                + i
                + " expected visible area "
                + c.visible
                + ", got "
                + area[0]);
      // Other faces of the native block must remain complete.
      for (Direction d : Direction.values())
        if (d != c.face) {
          double[] exposed = {0};
          mesh.forEach(
              q -> {
                if (q.lightFace() == d) exposed[0] += area(q, 0, 1, 2) + area(q, 0, 2, 3);
              });
          if (Math.abs(exposed[0] - 1) > 1e-6)
            throw new AssertionError("Unrelated native face removed: " + d);
        }
    }
  }

  private static double area(QuadView q, int a, int b, int c) {
    double ux = q.x(b) - q.x(a), uy = q.y(b) - q.y(a), uz = q.z(b) - q.z(a);
    double vx = q.x(c) - q.x(a), vy = q.y(c) - q.y(a), vz = q.z(c) - q.z(a);
    double x = uy * vz - uz * vy, y = uz * vx - ux * vz, z = ux * vy - uy * vx;
    return Math.sqrt(x * x + y * y + z * z) / 2;
  }
}
