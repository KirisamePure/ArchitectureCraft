package gcewing.architecture.shape;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Immutable geometry shared by world rendering, inventory rendering and collision generation. */
public record Mesh(List<Triangle> triangles, List<Box> boxes) {
  public record Vertex(
      double x, double y, double z, double u, double v, double nx, double ny, double nz) {
    public Vertex transformed(Orientation o) {
      double[] p = o.point(x, y, z), n = o.vector(nx, ny, nz);
      return new Vertex(p[0], p[1], p[2], u, v, n[0], n[1], n[2]);
    }
  }

  public record Triangle(int material, Vertex a, Vertex b, Vertex c) {}

  public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {}

  private static final Map<String, Mesh> MODELS = new ConcurrentHashMap<>();

  public static Mesh model(String name) {
    return MODELS.computeIfAbsent(name, Mesh::read);
  }

  private static Mesh read(String name) {
    String path = "/assets/architecturecraft/models/" + name + ".objson";
    try (InputStream in = Mesh.class.getResourceAsStream(path)) {
      if (in == null) throw new IOException("Missing mesh " + path);
      JsonObject json =
          JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
              .getAsJsonObject();
      List<Triangle> triangles = new ArrayList<>();
      for (JsonElement element : json.getAsJsonArray("faces")) {
        JsonObject face = element.getAsJsonObject();
        List<Vertex> vertices = new ArrayList<>();
        for (JsonElement ve : face.getAsJsonArray("vertices")) {
          JsonArray v = ve.getAsJsonArray();
          vertices.add(
              new Vertex(
                  v.get(0).getAsDouble() + .5,
                  v.get(1).getAsDouble() + .5,
                  v.get(2).getAsDouble() + .5,
                  v.get(6).getAsDouble(),
                  v.get(7).getAsDouble(),
                  v.get(3).getAsDouble(),
                  v.get(4).getAsDouble(),
                  v.get(5).getAsDouble()));
        }
        for (JsonElement te : face.getAsJsonArray("triangles")) {
          JsonArray t = te.getAsJsonArray();
          triangles.add(
              new Triangle(
                  face.get("texture").getAsInt(),
                  vertices.get(t.get(0).getAsInt()),
                  vertices.get(t.get(1).getAsInt()),
                  vertices.get(t.get(2).getAsInt())));
        }
      }
      List<Box> boxes = new ArrayList<>();
      JsonArray bounds = json.has("boxes") ? json.getAsJsonArray("boxes") : new JsonArray();
      if (bounds.isEmpty() && json.has("bounds")) bounds.add(json.get("bounds"));
      for (JsonElement be : bounds) {
        JsonArray b = be.getAsJsonArray();
        boxes.add(
            new Box(
                b.get(0).getAsDouble() + .5,
                b.get(1).getAsDouble() + .5,
                b.get(2).getAsDouble() + .5,
                b.get(3).getAsDouble() + .5,
                b.get(4).getAsDouble() + .5,
                b.get(5).getAsDouble() + .5));
      }
      return new Mesh(List.copyOf(triangles), List.copyOf(boxes));
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Cannot load " + path, e);
    }
  }

  public Mesh transformed(Orientation o) {
    List<Triangle> ts =
        triangles.stream()
            .map(
                t ->
                    new Triangle(
                        t.material, t.a.transformed(o), t.b.transformed(o), t.c.transformed(o)))
            .toList();
    List<Box> bs = new ArrayList<>();
    for (Box b : boxes) {
      double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
      double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
      for (double x : new double[] {b.x0, b.x1})
        for (double y : new double[] {b.y0, b.y1})
          for (double z : new double[] {b.z0, b.z1}) {
            double[] p = o.point(x, y, z);
            for (int i = 0; i < 3; i++) {
              min[i] = Math.min(min[i], p[i]);
              max[i] = Math.max(max[i], p[i]);
            }
          }
      bs.add(new Box(min[0], min[1], min[2], max[0], max[1], max[2]));
    }
    return new Mesh(ts, List.copyOf(bs));
  }

  public static Mesh join(Collection<Mesh> meshes) {
    List<Triangle> ts = new ArrayList<>();
    List<Box> bs = new ArrayList<>();
    for (Mesh m : meshes) {
      ts.addAll(m.triangles);
      bs.addAll(m.boxes);
    }
    return new Mesh(List.copyOf(ts), List.copyOf(bs));
  }
}
