package gcewing.architecture.shape;

/** The original plain/corner frame compatibility rules, including rotated windows. */
public final class WindowTopology {
  private WindowTopology() {}

  public static final int[][] DIRECTIONS = {
    {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
  };

  public record Entry(int shapeId, int side, int turn, int disconnected, boolean glazed) {
    public static final Entry EMPTY = new Entry(-1, 1, 0, 0, false);

    public Shape shape() {
      return Shape.byId(shapeId);
    }

    public boolean window() {
      return shapeId >= 30 && shapeId <= 32;
    }

    public Orientation orientation() {
      return new Orientation(side, turn, 0);
    }
  }

  @FunctionalInterface
  public interface Neighborhood {
    Entry at(int x, int y, int z);
  }

  public static int[] sides(Shape shape) {
    return shape == Shape.WindowCorner ? new int[] {0, 3, 1, 4} : new int[] {0, 5, 1, 4};
  }

  public static int frameKind(Shape shape, int localFace) {
    return shape == Shape.WindowCorner
        ? switch (localFace) {
          case 0, 1 -> 2;
          case 3, 4 -> 1;
          default -> 0;
        }
        : switch (localFace) {
          case 0, 1, 4, 5 -> 1;
          default -> 0;
        };
  }

  public static int frameOrientation(int localFace) {
    return localFace <= 1 ? 5 : 1;
  }

  public static boolean matches(Entry a, Entry b, int globalDirection) {
    if (!a.window()
        || !b.window()
        || (a.disconnected & (1 << globalDirection)) != 0
        || (b.disconnected & (1 << (globalDirection ^ 1))) != 0) return false;
    int faceA = a.orientation().localFace(globalDirection),
        faceB = b.orientation().localFace(globalDirection ^ 1);
    int kind = frameKind(a.shape(), faceA);
    if (kind == 0 || kind != frameKind(b.shape(), faceB)) return false;
    int orientA = a.orientation().globalFace(frameOrientation(faceA)),
        orientB = b.orientation().globalFace(frameOrientation(faceB));
    return kind == 1 ? orientA / 2 == orientB / 2 : orientA == orientB;
  }

  public static Entry connected(
      Entry source, int x, int y, int z, int direction, Neighborhood world) {
    int[] v = DIRECTIONS[direction];
    Entry other = world.at(x + v[0], y + v[1], z + v[2]);
    return matches(source, other, direction) ? other : null;
  }

  public static boolean[][] flags(Entry self, Neighborhood world) {
    boolean[][] frame = new boolean[4][4];
    int[] local = sides(self.shape()), global = new int[4];
    for (int i = 0; i < 4; i++) global[i] = self.orientation().globalFace(local[i]);
    for (int i = 0; i < 4; i++) {
      Entry neighbor = connected(self, 0, 0, 0, global[i], world);
      if (neighbor == null) frame[i][1] = true;
      else {
        int j = (i + 3) & 3, k = (i + 1) & 3;
        int[] v = DIRECTIONS[global[i]];
        if (connected(neighbor, v[0], v[1], v[2], global[j], world) == null) frame[j][2] = true;
        if (connected(neighbor, v[0], v[1], v[2], global[k], world) == null) frame[k][0] = true;
        if (neighbor.glazed) frame[i][3] = true;
      }
    }
    return frame;
  }
}
