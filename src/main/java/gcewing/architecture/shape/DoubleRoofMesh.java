package gcewing.architecture.shape;

import java.util.*;

/** Two complementary roof prisms separated by the local plane y=x. */
public final class DoubleRoofMesh {
  private DoubleRoofMesh() {}

  public static Mesh create() {
    Mesh shell = Mesh.model("shape/double_roof_tile");
    List<Mesh.Triangle> triangles = new ArrayList<>(shell.triangles());
    // Original shell uses slots 0/2. Projected slots 1/3 mark the two internal
    // caps, which the renderer can suppress when the adjacent half hides them.
    for (int material : new int[] {1, 3}) {
      double sign = material == 1 ? 1 : -1;
      double n = sign / Math.sqrt(2);
      Mesh.Vertex a = vertex(0, 0, n),
          b = vertex(0, 1, n),
          c = vertex(1, 1, n),
          d = vertex(1, 0, n);
      triangles.add(new Mesh.Triangle(material, a, material == 1 ? b : d, c));
      triangles.add(new Mesh.Triangle(material, a, c, material == 1 ? d : b));
    }
    return new Mesh(List.copyOf(triangles), shell.boxes());
  }

  private static Mesh.Vertex vertex(double x, double z, double n) {
    return new Mesh.Vertex(x, x, z, z, 1 - x, -n, n, 0);
  }

  public static boolean isInterface(Mesh.Triangle triangle) {
    return triangle.material() == 1 || triangle.material() == 3;
  }
}
