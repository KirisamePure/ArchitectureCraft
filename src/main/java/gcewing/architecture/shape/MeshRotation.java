package gcewing.architecture.shape;

import java.util.*;

/** Composes the original window frame's Y and Z rotations without floating-point trigonometry. */
public final class MeshRotation {
  private MeshRotation() {}

  public static double[] vector(double x, double y, double z, int rz, int ry) {
    for (int i = 0; i < Math.floorMod(rz, 4); i++) {
      double t = x;
      x = -y;
      y = t;
    }
    for (int i = 0; i < Math.floorMod(ry, 4); i++) {
      double t = x;
      x = z;
      z = -t;
    }
    return new double[] {x, y, z};
  }

  private static Mesh.Vertex vertex(Mesh.Vertex v, int rz, int ry) {
    double[] p = vector(v.x() - .5, v.y() - .5, v.z() - .5, rz, ry),
        n = vector(v.nx(), v.ny(), v.nz(), rz, ry);
    return new Mesh.Vertex(p[0] + .5, p[1] + .5, p[2] + .5, v.u(), v.v(), n[0], n[1], n[2]);
  }

  public static Mesh apply(Mesh mesh, int rz, int ry) {
    var triangles =
        mesh.triangles().stream()
            .map(
                t ->
                    new Mesh.Triangle(
                        t.material(),
                        vertex(t.a(), rz, ry),
                        vertex(t.b(), rz, ry),
                        vertex(t.c(), rz, ry)))
            .toList();
    List<Mesh.Box> boxes = new ArrayList<>();
    for (Mesh.Box b : mesh.boxes()) {
      double[] min = {100, 100, 100}, max = {-100, -100, -100};
      for (double x : new double[] {b.x0(), b.x1()})
        for (double y : new double[] {b.y0(), b.y1()})
          for (double z : new double[] {b.z0(), b.z1()}) {
            double[] p = vector(x - .5, y - .5, z - .5, rz, ry);
            for (int i = 0; i < 3; i++) {
              min[i] = Math.min(min[i], p[i] + .5);
              max[i] = Math.max(max[i], p[i] + .5);
            }
          }
      boxes.add(new Mesh.Box(min[0], min[1], min[2], max[0], max[1], max[2]));
    }
    return new Mesh(triangles, List.copyOf(boxes));
  }
}
