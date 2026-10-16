# ArchitectureCraft — Fabric

ArchitectureCraft for **Minecraft 1.21.10**, **Fabric Loader 0.18.6+**, and **Java 21**.
Install [Fabric API](https://modrinth.com/mod/fabric-api) 0.138.4+1.21.10 alongside the mod.
Fabric API is the only required mod dependency. There are no optional integrations or modpack-specific recipes.

## Playing

Craft the circular saw blade and large pulley, then craft the architect's sawbench. The sawbench accepts full building blocks and slabs without block entities. Slabs cost twice as many items as full blocks. Select one of the 106 architectural shapes, then press **Cut**. The displayed material cost and yield match the original shape catalog. Input materials and completed output remain in the sawbench, including when you close its screen. Shift-click transfers items.

The sawbench also cuts a block into 16 cladding sheets. Use cladding on a shape to apply a secondary surface material, or use glass/glass panes on a window to glaze it. Shapes retain their materials when picked or broken.

- **Hammer:** rotate a shape; sneak to change its mounting face. Offset banisters alternate their offset before turning.
- **Chisel:** remove cladding/glazing; sneak to toggle connection to the neighboring shape on the clicked face.
- **Glowbrush:** make a shape emit light; sneak to remove the glow.

Roof ridges and valleys adapt to connected neighbors, and window frames join adjacent aligned windows. Material textures and transparency come from the source block. Shape geometry is shared by terrain meshes, item models, and collision, including the original curved models.

This branch replaces the Forge 1.7.10 implementation. It does **not** migrate 1.7.10 worlds or numeric metadata IDs to modern Minecraft saves. Use a new 1.21.10 world.

## Developing

Use a full **JDK 21** (a JRE is insufficient) and the checked-in Gradle wrapper:

```sh
./gradlew build
./gradlew runClient
```

`build` runs geometry/unit tests and server GameTests and produces `build/libs/architecturecraft-2.0.0-fabric.1.jar`. The `-sources.jar` is for development, not installation. Server GameTest results are in `build/test-results/gametest/results.xml`; JUnit results are in `build/test-results/test`.

The client smoke test loads every shape item, creates and renders a world containing all shapes, and opens the sawbench screen:

```sh
./gradlew runClientGameTest
```

It needs a graphical display (or Xorg/Xvfb with software OpenGL). Screenshots are written under `build/run/clientGameTest/screenshots`. The client smoke test is separate from `build`, so ordinary headless builds do not need a display.

Client code lives in `src/client/java`, common/server code in `src/main/java`, and the test-only mod in `src/gametest`. The test mod is excluded from release jars. Original `.objson` models are retained under `assets/architecturecraft/models`; `shape/RoofMesh.java` retains the procedural roof tessellation.

On a cloud machine behind an HTTP proxy, configure the Java `http.proxyHost`, `http.proxyPort`, `https.proxyHost`, and `https.proxyPort` system properties from its proxy settings. A separately downloaded JDK may need the machine's existing trusted CA store via `javax.net.ssl.trustStore`. Keep TLS and artifact verification enabled.
