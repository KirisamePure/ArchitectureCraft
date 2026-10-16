package gcewing.architecture.shape;

import java.util.*;

/** Frames connect to adjacent windows; glass is a separate material and collision layer. */
public final class WindowMesh {
  @FunctionalInterface
  public interface Connected {
    boolean test(int x, int y, int z);
  }

  private WindowMesh() {}

  public static Mesh create(Shape shape, Connected connected, boolean glass) {
    List<Mesh> parts = new ArrayList<>();
    boolean corner = shape == Shape.WindowCorner, mullion = shape == Shape.WindowMullion;
    String centre = corner ? "corner_centre" : mullion ? "mullion_centre" : null;
    if (centre != null) parts.add(Mesh.model("shape/window_" + centre));
    int[][] dirs =
        corner
            ? new int[][] {{0, -1, 0}, {0, 0, 1}, {0, 1, 0}, {-1, 0, 0}}
            : new int[][] {{0, -1, 0}, {1, 0, 0}, {0, 1, 0}, {-1, 0, 0}};
    boolean[] frame = new boolean[4];
    for (int i = 0; i < 4; i++) frame[i] = !connected.test(dirs[i][0], dirs[i][1], dirs[i][2]);
    for (int i = 0; i < 4; i++) {
      String side =
          (i % 2 == 0 && (corner || mullion))
              ? (corner ? "corner_topbot" : "mullion_topbot")
              : "frame_side";
      if (frame[i])
        parts.add(
            rotate(Mesh.model("shape/window_" + side), i, corner && (i == 1 || i == 2) ? -1 : 0));
      else if ((corner || mullion) && i % 2 == 0)
        parts.add(
            Mesh.model("shape/window_" + (corner ? "corner" : "mullion") + "_centre_end" + i));
      if (frame[i] && !frame[(i + 3) % 4])
        parts.add(
            rotate(
                Mesh.model("shape/window_frame_end0"), i, corner && (i == 1 || i == 2) ? -1 : 0));
      if (frame[i] && !frame[(i + 1) % 4]) {
        String end = corner && i % 2 == 0 ? "corner_topbot_end1" : "frame_end1";
        parts.add(
            rotate(Mesh.model("shape/window_" + end), i, corner && (i == 1 || i == 2) ? -1 : 0));
      }
    }
    if (glass) {
      Mesh m = Mesh.model("shape/window_" + (corner ? "corner_glass" : "glass"));
      parts.add(
          new Mesh(
              m.triangles().stream().map(t -> new Mesh.Triangle(2, t.a(), t.b(), t.c())).toList(),
              m.boxes()));
    }
    return Mesh.join(parts);
  }

  private static Mesh rotate(Mesh mesh, int z, int y) {
    List<Mesh.Triangle> ts = new ArrayList<>();
    for (Mesh.Triangle t : mesh.triangles())
      ts.add(
          new Mesh.Triangle(
              t.material(), vertex(t.a(), z, y), vertex(t.b(), z, y), vertex(t.c(), z, y)));
    List<Mesh.Box> boxes = new ArrayList<>();
    for (Mesh.Box b : mesh.boxes()) {
      double[] min = {100, 100, 100}, max = {-100, -100, -100};
      for (double x : new double[] {b.x0(), b.x1()})
        for (double yy : new double[] {b.y0(), b.y1()})
          for (double zz : new double[] {b.z0(), b.z1()}) {
            Mesh.Vertex v = vertex(new Mesh.Vertex(x, yy, zz, 0, 0, 0, 0, 0), z, y);
            double[] p = {v.x(), v.y(), v.z()};
            for (int i = 0; i < 3; i++) {
              min[i] = Math.min(min[i], p[i]);
              max[i] = Math.max(max[i], p[i]);
            }
          }
      boxes.add(new Mesh.Box(min[0], min[1], min[2], max[0], max[1], max[2]));
    }
    return new Mesh(List.copyOf(ts), List.copyOf(boxes));
  }

  private static Mesh.Vertex vertex(Mesh.Vertex v, int z, int y) {
    double x = v.x() - .5, yy = v.y() - .5, zz = v.z() - .5, nx = v.nx(), ny = v.ny(), nz = v.nz();
    for (int i = 0; i < Math.floorMod(z, 4); i++) {
      double t = x;
      x = -yy;
      yy = t;
      t = nx;
      nx = -ny;
      ny = t;
    }
    for (int i = 0; i < Math.floorMod(y, 4); i++) {
      double t = x;
      x = -zz;
      zz = t;
      t = nx;
      nx = -nz;
      nz = t;
    }
    return new Mesh.Vertex(x + .5, yy + .5, zz + .5, v.u(), v.v(), nx, ny, nz);
  }
}
