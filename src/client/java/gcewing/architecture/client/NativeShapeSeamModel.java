package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.Mesh;
import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Hide the native material's side of a seam, including partial contact with a shape. */
final class NativeShapeSeamModel implements BlockStateModel {
  private final BlockStateModel delegate;

  NativeShapeSeamModel(BlockStateModel delegate) {
    this.delegate = delegate;
  }

  static boolean hidesOwnFaces(BlockState state) {
    for (Direction d : Direction.values()) if (state.skipRendering(state, d)) return true;
    return false;
  }

  @Override
  public void collectParts(RandomSource random, List<BlockModelPart> parts) {
    delegate.collectParts(random, parts);
  }

  @Override
  public TextureAtlasSprite particleIcon() {
    return delegate.particleIcon();
  }

  @Override
  public TextureAtlasSprite particleSprite(
      BlockAndTintGetter world, BlockPos pos, BlockState state) {
    return delegate.particleSprite(world, pos, state);
  }

  @Override
  public void emitQuads(
      QuadEmitter emitter,
      BlockAndTintGetter world,
      BlockPos pos,
      BlockState state,
      RandomSource random,
      Predicate<Direction> cullTest) {
    Map<Direction, List<Mesh.Triangle>> coverage = new EnumMap<>(Direction.class);
    for (Direction face : Direction.values()) {
      BlockPos next = pos.relative(face);
      if (!world.getBlockState(next).is(ArchitectureCraft.SHAPE)
          || !(world.getBlockEntity(next) instanceof ShapeBlockEntity be)) continue;
      List<Mesh.Triangle> triangles = new ArrayList<>();
      for (var t : ShapeGeometry.mesh(be.data(), be.getBlockState(), world, next).triangles()) {
        var material =
            t.material() >= 2 ? be.data().secondary().orElse(be.data().base()) : be.data().base();
        if (state.skipRendering(material, face) && ShapeFaceCulling.boundary(t, face.getOpposite()))
          triangles.add(t);
      }
      if (!triangles.isEmpty()) coverage.put(face, triangles);
    }
    if (coverage.isEmpty()) {
      delegate.emitQuads(emitter, world, pos, state, random, cullTest);
      return;
    }
    // Defer replacement emission until after popping our transform. Never emit recursively.
    var replacements = Renderer.get().mutableMesh();
    var output = replacements.emitter();
    emitter.pushTransform(
        quad -> {
          Direction face = quad.lightFace();
          var triangles = coverage.get(face);
          if (triangles == null || !boundary(quad, face)) return true;
          clip(quad, face, triangles, output);
          return false;
        });
    try {
      delegate.emitQuads(emitter, world, pos, state, random, cullTest);
    } finally {
      emitter.popTransform();
    }
    replacements.outputTo(emitter);
  }

  private static boolean boundary(QuadView q, Direction face) {
    int axis = face.getAxis().ordinal(); // Direction.Axis is X, Y, Z.
    double edge = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
    for (int i = 0; i < 4; i++) if (Math.abs(coordinate(q, i, axis) - edge) > 1e-7) return false;
    return true;
  }

  private static double coordinate(QuadView q, int i, int axis) {
    return axis == 0 ? q.x(i) : axis == 1 ? q.y(i) : q.z(i);
  }

  private static double coordinate(Mesh.Vertex v, int axis) {
    return axis == 0 ? v.x() : axis == 1 ? v.y() : v.z();
  }

  /** Barycentric weights retain the source quad's UVs, colors, normals and light values. */
  private record Point(double u, double v, double a, double b, double c) {
    Point mix(Point p, double t) {
      return new Point(
          u + (p.u - u) * t,
          v + (p.v - v) * t,
          a + (p.a - a) * t,
          b + (p.b - b) * t,
          c + (p.c - c) * t);
    }

    double interpolate(double x, double y, double z) {
      return a * x + b * y + c * z;
    }
  }

  private static void clip(
      QuadView source, Direction face, List<Mesh.Triangle> coverage, QuadEmitter output) {
    int axis = face.getAxis().ordinal(), u = (axis + 1) % 3, v = (axis + 2) % 3;
    for (int[] indices : new int[][] {{0, 1, 2}, {0, 2, 3}}) {
      List<Point> original = new ArrayList<>();
      for (int i = 0; i < 3; i++)
        original.add(
            new Point(
                coordinate(source, indices[i], u),
                coordinate(source, indices[i], v),
                i == 0 ? 1 : 0,
                i == 1 ? 1 : 0,
                i == 2 ? 1 : 0));
      if (area(original) < 1e-12) continue;
      List<List<Point>> pieces = new ArrayList<>();
      pieces.add(original);
      for (var triangle : coverage) {
        double[][] cutter = new double[3][2];
        var vertices = List.of(triangle.a(), triangle.b(), triangle.c());
        for (int i = 0; i < 3; i++) {
          cutter[i][0] = coordinate(vertices.get(i), u);
          cutter[i][1] = coordinate(vertices.get(i), v);
        }
        double winding = cross(cutter[0], cutter[1], cutter[2][0], cutter[2][1]);
        if (Math.abs(winding) < 1e-12) continue;
        List<List<Point>> next = new ArrayList<>();
        for (var piece : pieces) {
          List<Point> inside = piece;
          for (int edge = 0; edge < 3 && !inside.isEmpty(); edge++) {
            double[] start = cutter[edge], end = cutter[(edge + 1) % 3];
            var outside = halfPlane(inside, start, end, -Math.signum(winding));
            if (area(outside) > 1e-12) next.add(outside);
            inside = halfPlane(inside, start, end, Math.signum(winding));
          }
        }
        pieces = next;
        if (pieces.isEmpty()) break;
      }
      for (var polygon : pieces)
        for (int i = 1; i + 1 < polygon.size(); i++) {
          Point[] points = {polygon.get(0), polygon.get(i), polygon.get(i + 1), polygon.get(i + 1)};
          output.copyFrom(source);
          for (int j = 0; j < 4; j++) {
            Point p = points[j];
            output.pos(
                j,
                (float)
                    p.interpolate(source.x(indices[0]), source.x(indices[1]), source.x(indices[2])),
                (float)
                    p.interpolate(source.y(indices[0]), source.y(indices[1]), source.y(indices[2])),
                (float)
                    p.interpolate(
                        source.z(indices[0]), source.z(indices[1]), source.z(indices[2])));
            output.uv(
                j,
                (float)
                    p.interpolate(source.u(indices[0]), source.u(indices[1]), source.u(indices[2])),
                (float)
                    p.interpolate(
                        source.v(indices[0]), source.v(indices[1]), source.v(indices[2])));
            int color = 0;
            for (int shift = 0; shift < 32; shift += 8)
              color |=
                  ((int)
                          Math.round(
                              p.interpolate(
                                  (source.color(indices[0]) >>> shift) & 255,
                                  (source.color(indices[1]) >>> shift) & 255,
                                  (source.color(indices[2]) >>> shift) & 255)))
                      << shift;
            output.color(j, color);
            int light = 0;
            for (int shift = 0; shift <= 16; shift += 16)
              light |=
                  ((int)
                          Math.round(
                              p.interpolate(
                                  (source.lightmap(indices[0]) >>> shift) & 65535,
                                  (source.lightmap(indices[1]) >>> shift) & 65535,
                                  (source.lightmap(indices[2]) >>> shift) & 65535)))
                      << shift;
            output.lightmap(j, light);
            if (source.hasNormal(indices[0])
                && source.hasNormal(indices[1])
                && source.hasNormal(indices[2]))
              output.normal(
                  j,
                  (float)
                      p.interpolate(
                          source.normalX(indices[0]),
                          source.normalX(indices[1]),
                          source.normalX(indices[2])),
                  (float)
                      p.interpolate(
                          source.normalY(indices[0]),
                          source.normalY(indices[1]),
                          source.normalY(indices[2])),
                  (float)
                      p.interpolate(
                          source.normalZ(indices[0]),
                          source.normalZ(indices[1]),
                          source.normalZ(indices[2])));
          }
          output.emit();
        }
    }
  }

  private static double cross(double[] a, double[] b, double x, double y) {
    return (b[0] - a[0]) * (y - a[1]) - (b[1] - a[1]) * (x - a[0]);
  }

  private static List<Point> halfPlane(List<Point> polygon, double[] a, double[] b, double sign) {
    List<Point> result = new ArrayList<>();
    Point prev = polygon.getLast();
    double prevD = cross(a, b, prev.u, prev.v) * sign;
    for (Point current : polygon) {
      double d = cross(a, b, current.u, current.v) * sign;
      if ((prevD >= 0) != (d >= 0)) result.add(prev.mix(current, prevD / (prevD - d)));
      if (d >= 0) result.add(current);
      prev = current;
      prevD = d;
    }
    return result;
  }

  private static double area(List<Point> points) {
    double sum = 0;
    for (int i = 0; i < points.size(); i++) {
      var a = points.get(i);
      var b = points.get((i + 1) % points.size());
      sum += a.u * b.v - a.v * b.u;
    }
    return Math.abs(sum) / 2;
  }
}
