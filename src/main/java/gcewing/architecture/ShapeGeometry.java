package gcewing.architecture;

import gcewing.architecture.shape.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class ShapeGeometry {
  private record LocalKey(Shape shape, List<Integer> neighbors, boolean glass) {}

  private record Key(LocalKey local, Orientation orientation) {}

  private static final Map<LocalKey, Mesh> LOCAL = boundedCache(2048);
  private static final Map<Key, Mesh> MESHES = boundedCache(2048);
  private static final Map<Key, VoxelShape> COLLISIONS = boundedCache(1024);

  private ShapeGeometry() {}

  private static <K, V> Map<K, V> boundedCache(int limit) {
    return Collections.synchronizedMap(
        new LinkedHashMap<>(64, .75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
            return size() > limit;
          }
        });
  }

  private static <K, V> V cached(Map<K, V> cache, K key, Supplier<V> factory) {
    V value = cache.get(key);
    if (value == null) {
      value = factory.get();
      cache.put(key, value);
    }
    return value;
  }

  public static Orientation orientation(BlockState state, ShapeData data) {
    return new Orientation(
        state.getValue(ShapeBlock.FACING).get3DDataValue(),
        state.getValue(ShapeBlock.TURN),
        data.offset() / 16.);
  }

  private static Key key(ShapeData data, BlockState state, BlockGetter world, BlockPos pos) {
    Orientation o = orientation(state, data);
    List<Integer> neighbors = new ArrayList<>(6);
    for (Direction local : Direction.values()) {
      int id = -1;
      if (world != null && (data.shape().roof() || data.shape().window())) {
        double[] v = o.vector(local.getStepX(), local.getStepY(), local.getStepZ());
        Direction global =
            Direction.getApproximateNearest((float) v[0], (float) v[1], (float) v[2]);
        if ((data.disconnected() & (1 << global.get3DDataValue())) == 0
            && world.getBlockEntity(pos.relative(global)) instanceof ShapeBlockEntity other
            && (other.data().disconnected() & (1 << global.getOpposite().get3DDataValue())) == 0) {
          if (!data.shape().window()
              || (other.data().shape().window()
                  && other.getBlockState().getValue(ShapeBlock.FACING)
                      == state.getValue(ShapeBlock.FACING)
                  && other
                      .getBlockState()
                      .getValue(ShapeBlock.TURN)
                      .equals(state.getValue(ShapeBlock.TURN)))) id = other.data().shapeId();
        }
      }
      neighbors.add(id);
    }
    return new Key(
        new LocalKey(data.shape(), List.copyOf(neighbors), data.secondary().isPresent()), o);
  }

  private static int neighbor(LocalKey key, int x, int y, int z) {
    Direction d = Direction.getApproximateNearest(x, y, z);
    return key.neighbors.get(d.get3DDataValue());
  }

  private static Mesh mesh(Key key) {
    return cached(
        MESHES,
        key,
        () ->
            cached(
                    LOCAL,
                    key.local,
                    () -> {
                      Shape shape = key.local.shape;
                      if (shape.roof())
                        return RoofMesh.create(
                            shape,
                            (x, y, z) -> {
                              int id = neighbor(key.local, x, y, z);
                              return id < 0 ? null : Shape.byId(id);
                            });
                      if (shape.window())
                        return WindowMesh.create(
                            shape, (x, y, z) -> neighbor(key.local, x, y, z) >= 0, key.local.glass);
                      return Mesh.model("shape/" + shape.model);
                    })
                .transformed(key.orientation));
  }

  public static Mesh mesh(ShapeData data, BlockState state, BlockGetter world, BlockPos pos) {
    return mesh(key(data, state, world, pos));
  }

  public static VoxelShape collision(
      ShapeData data, BlockState state, BlockGetter world, BlockPos pos) {
    Key key = key(data, state, world, pos);
    return cached(
        COLLISIONS,
        key,
        () -> {
          VoxelShape result = Shapes.empty();
          for (Mesh.Box b : mesh(key).boxes())
            if (b.x1() > b.x0() && b.y1() > b.y0() && b.z1() > b.z0())
              result =
                  Shapes.or(result, Shapes.box(b.x0(), b.y0(), b.z0(), b.x1(), b.y1(), b.z1()));
          return result.optimize();
        });
  }
}
