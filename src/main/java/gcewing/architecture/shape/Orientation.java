package gcewing.architecture.shape;

/** The 24 cube orientations; side uses Minecraft's down/up/north/south/west/east order. */
public record Orientation(int side, int turn, double offset) {
  public double[] vector(double x, double y, double z) {
    for (int i = 0; i < Math.floorMod(turn, 4); i++) {
      double t = x;
      x = -z;
      z = t;
    }
    return switch (side) {
      case 0 -> new double[] {x, -y, -z};
      case 2 -> new double[] {x, z, -y};
      case 3 -> new double[] {x, -z, y};
      case 4 -> new double[] {-y, x, z};
      case 5 -> new double[] {y, -x, z};
      default -> new double[] {x, y, z};
    };
  }

  public double[] point(double x, double y, double z) {
    double[] p = vector(x - .5 + offset, y - .5, z - .5);
    return new double[] {p[0] + .5, p[1] + .5, p[2] + .5};
  }
}
