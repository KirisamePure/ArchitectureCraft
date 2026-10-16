package gcewing.architecture.shape;

/**
 * Original mounting matrices. side is the modern outward normal; legacy mounting side is side^1.
 */
public record Orientation(int side, int turn, double offset) {
  private static final int[][] VECTORS = {
    {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
  };

  public static Orientation legacy(int side, int turn, double offset) {
    return new Orientation(side ^ 1, turn, offset);
  }

  public int legacySide() {
    return side ^ 1;
  }

  public double[] vector(double x, double y, double z) {
    for (int i = 0; i < Math.floorMod(turn, 4); i++) {
      double t = x;
      x = z;
      z = -t;
    }
    return switch (side) {
      case 0 -> new double[] {x, -y, -z};
      case 2 -> new double[] {-x, -z, -y};
      case 3 -> new double[] {x, -z, y};
      case 4 -> new double[] {-y, -z, x};
      case 5 -> new double[] {y, -z, -x};
      default -> new double[] {x, y, z};
    };
  }

  public double[] inverseVector(double x, double y, double z) {
    double[] a = vector(1, 0, 0), b = vector(0, 1, 0), c = vector(0, 0, 1);
    return new double[] {
      x * a[0] + y * a[1] + z * a[2], x * b[0] + y * b[1] + z * b[2], x * c[0] + y * c[1] + z * c[2]
    };
  }

  public double[] point(double x, double y, double z) {
    double[] p = vector(x - .5 + offset, y - .5, z - .5);
    return new double[] {p[0] + .5, p[1] + .5, p[2] + .5};
  }

  public int globalFace(int local) {
    int[] v = VECTORS[local];
    return nearest(vector(v[0], v[1], v[2]));
  }

  public int localFace(int global) {
    int[] v = VECTORS[global];
    return nearest(inverseVector(v[0], v[1], v[2]));
  }

  private static int nearest(double[] v) {
    for (int i = 0; i < 6; i++)
      if (v[0] * VECTORS[i][0] + v[1] * VECTORS[i][1] + v[2] * VECTORS[i][2] > .99) return i;
    throw new IllegalArgumentException("Not an axis");
  }

  public static int turnToFace(int from, int to) {
    for (int turn = 0; turn < 4; turn++)
      if (new Orientation(1, turn, 0).globalFace(from) == to) return turn;
    return 0;
  }
}
