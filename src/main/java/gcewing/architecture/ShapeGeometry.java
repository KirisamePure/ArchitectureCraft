package gcewing.architecture;

import gcewing.architecture.shape.*;
import gcewing.architecture.shape.WindowTopology.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Immutable neighborhood keys keep chunk workers independent of mutable block entities. */
public final class ShapeGeometry {
  private record LocalKey(Shape shape, Entry self, List<Entry> neighbors) {}

  private record Key(LocalKey local, Orientation orientation) {}

  private record CollisionKey(Key key, boolean selection) {}

  private static final Map<LocalKey, Mesh> LOCAL = boundedCache(2048);
  private static final Map<Key, Mesh> MESHES = boundedCache(2048);
  private static final Map<CollisionKey, VoxelShape> COLLISIONS = boundedCache(2048);

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

  public static Entry entry(ShapeData data, BlockState state) {
    return new Entry(
        data.shapeId(),
        state.getValue(ShapeBlock.FACING).get3DDataValue(),
        state.getValue(ShapeBlock.TURN),
        data.disconnected(),
        data.secondary().isPresent());
  }

  private static Key key(ShapeData data, BlockState state, BlockGetter world, BlockPos pos) {
    List<Entry> neighbors = new ArrayList<>(27);
    Shape shape = data.shape();
    for (int x = -1; x <= 1; x++)
      for (int y = -1; y <= 1; y++)
        for (int z = -1; z <= 1; z++) {
          Entry e = Entry.EMPTY;
          if (world != null
              && (shape.window() || shape.roof() && Math.abs(x) + Math.abs(y) + Math.abs(z) == 1)
              && world.getBlockEntity(pos.offset(x, y, z)) instanceof ShapeBlockEntity be)
            e = entry(be.data(), be.getBlockState());
          neighbors.add(e);
        }
    return new Key(
        new LocalKey(shape, entry(data, state), List.copyOf(neighbors)), orientation(state, data));
  }

  private static Neighborhood neighborhood(LocalKey key) {
    return (x, y, z) ->
        x == 0 && y == 0 && z == 0
            ? key.self
            : Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1
                ? key.neighbors.get((x + 1) * 9 + (y + 1) * 3 + z + 1)
                : Entry.EMPTY;
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
                      Neighborhood world = neighborhood(key.local);
                      if (shape.window()) return WindowMesh.create(key.local.self, world);
                      if (shape.roof())
                        return RoofMesh.create(
                            shape,
                            (x, y, z) -> {
                              double[] v = key.orientation.vector(x, y, z);
                              Direction d =
                                  Direction.getApproximateNearest(
                                      (float) v[0], (float) v[1], (float) v[2]);
                              Entry neighbor = world.at(d.getStepX(), d.getStepY(), d.getStepZ());
                              return neighbor.shapeId() < 0
                                      || (key.local.self.disconnected() & (1 << d.get3DDataValue()))
                                          != 0
                                      || (neighbor.disconnected()
                                              & (1 << d.getOpposite().get3DDataValue()))
                                          != 0
                                  ? null
                                  : neighbor.shape();
                            });
                      Mesh model =
                          shape == Shape.DoubleRoofTile
                              ? DoubleRoofMesh.create()
                              : Mesh.model("shape/" + shape.model);
                      return new Mesh(model.triangles(), CollisionMask.boxes(shape));
                    })
                .transformed(key.orientation));
  }

  public static Mesh mesh(ShapeData data, BlockState state, BlockGetter world, BlockPos pos) {
    return mesh(key(data, state, world, pos));
  }

  public static VoxelShape collision(
      ShapeData data, BlockState state, BlockGetter world, BlockPos pos) {
    return collision(data, state, world, pos, true);
  }

  public static VoxelShape collision(
      ShapeData data, BlockState state, BlockGetter world, BlockPos pos, boolean selection) {
    Key key = key(data, state, world, pos);
    return cached(
        COLLISIONS,
        new CollisionKey(key, selection),
        () -> {
          List<Mesh.Box> boxes =
              key.local.shape.window()
                  ? WindowMesh.collision(key.local.self, neighborhood(key.local), selection)
                  : CollisionMask.boxes(key.local.shape);
          boxes = new Mesh(List.of(), boxes).transformed(key.orientation).boxes();
          return voxel(boxes);
        });
  }

  public static VoxelShape voxel(List<Mesh.Box> boxes) {
    VoxelShape result = Shapes.empty();
    for (Mesh.Box b : boxes)
      if (b.x1() > b.x0() && b.y1() > b.y0() && b.z1() > b.z0())
        result = Shapes.or(result, Shapes.box(b.x0(), b.y0(), b.z0(), b.x1(), b.y1(), b.z1()));
    return result.optimize();
  }
}
