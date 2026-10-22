package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.Mesh;
import java.awt.geom.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cull only boundary triangles fully covered by a neighbor whose material hides the shared face.
 */
final class ShapeFaceCulling {
  private record Key(Direction face, BlockState material) {}

  private final BlockAndTintGetter world;
  private final BlockPos pos;
  private final Map<Key, Area> coverage = new HashMap<>();

  ShapeFaceCulling(BlockAndTintGetter world, BlockPos pos) {
    this.world = world;
    this.pos = pos;
  }

  boolean hidden(Mesh.Triangle triangle, Direction face, BlockState material) {
    if (!boundary(triangle, face)) return false;
    Area covered = coverage.computeIfAbsent(new Key(face, material), this::neighbor);
    if (covered.isEmpty()) return false;
    Area remaining = polygon(triangle, face);
    remaining.subtract(covered);
    // Area calculations can leave tiny floating-point slivers at model seams.
    return area(remaining) < 1e-10;
  }

  private Area neighbor(Key key) {
    Direction face = key.face();
    Direction opposite = face.getOpposite();
    BlockPos next = pos.relative(face);
    BlockState state = world.getBlockState(next);
    Area result = new Area();
    if (world.getBlockEntity(next) instanceof ShapeBlockEntity be) {
      Mesh mesh = ShapeGeometry.mesh(be.data(), state, world, next);
      for (var t : mesh.triangles()) {
        BlockState other =
            t.material() >= 2 ? be.data().secondary().orElse(be.data().base()) : be.data().base();
        if (key.material().skipRendering(other, face) && boundary(t, opposite))
          result.add(polygon(t, face));
      }
    } else if (key.material().skipRendering(state, face)) {
      // Use rendered faces, not collision boxes: transparent models may have holes.
      for (var part : MeshRendering.materialModel(state).collectParts(RandomSource.create(42))) {
        var quads = new ArrayList<>(part.getQuads(opposite));
        quads.addAll(part.getQuads(null));
        for (var quad : quads) {
          int[] data = quad.vertices();
          double[][] points = new double[4][3];
          boolean boundary = quad.direction() == opposite;
          for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) points[i][j] = Float.intBitsToFloat(data[i * 8 + j]);
            boundary &= Math.abs(points[i][axis(face)] - edge(opposite)) < 1e-7;
          }
          if (boundary) result.add(polygon(points, face));
        }
      }
    }
    return result;
  }

  private static int axis(Direction face) {
    return switch (face.getAxis()) {
      case X -> 0;
      case Y -> 1;
      case Z -> 2;
    };
  }

  private static double edge(Direction face) {
    return face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
  }

  private static boolean boundary(Mesh.Triangle t, Direction face) {
    int axis = axis(face);
    for (var v : List.of(t.a(), t.b(), t.c())) {
      double[] p = {v.x(), v.y(), v.z()}, n = {v.nx(), v.ny(), v.nz()};
      if (Math.abs(p[axis] - edge(face)) > 1e-7
          || n[axis] * face.getAxisDirection().getStep() < .99999) return false;
    }
    return true;
  }

  private static Area polygon(Mesh.Triangle t, Direction face) {
    return polygon(
        new double[][] {
          {t.a().x(), t.a().y(), t.a().z()},
          {t.b().x(), t.b().y(), t.b().z()},
          {t.c().x(), t.c().y(), t.c().z()}
        },
        face);
  }

  private static Area polygon(double[][] vertices, Direction face) {
    int a = (axis(face) + 1) % 3, b = (axis(face) + 2) % 3;
    Path2D.Double path = new Path2D.Double();
    path.moveTo(vertices[0][a], vertices[0][b]);
    for (int i = 1; i < vertices.length; i++) path.lineTo(vertices[i][a], vertices[i][b]);
    path.closePath();
    return new Area(path);
  }

  private static double area(Area area) {
    double sum = 0, x = 0, y = 0, firstX = 0, firstY = 0;
    double[] p = new double[6];
    for (PathIterator it = area.getPathIterator(null); !it.isDone(); it.next()) {
      switch (it.currentSegment(p)) {
        case PathIterator.SEG_MOVETO -> {
          firstX = x = p[0];
          firstY = y = p[1];
        }
        case PathIterator.SEG_LINETO -> {
          sum += x * p[1] - y * p[0];
          x = p[0];
          y = p[1];
        }
        case PathIterator.SEG_CLOSE -> sum += x * firstY - y * firstX;
        default -> throw new IllegalStateException("Expected polygonal coverage");
      }
    }
    return Math.abs(sum) / 2;
  }
}
