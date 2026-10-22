# ArchitectureCraft — Fabric

ArchitectureCraft for **Minecraft 1.21.10**, **Fabric Loader 0.18.6+**, and **Java 21**.
Install [Fabric API](https://modrinth.com/mod/fabric-api) 0.138.4+1.21.10 alongside the mod.
Fabric API is the only required mod dependency. There are no optional integrations or modpack-specific recipes.

## Playing

Craft the circular saw blade and large pulley, then craft the architect's sawbench. Its original illustrated interface contains eight categories and remembers your selection in each category. Insert building material and take the result to cut it: the output slot previews the selected shape without consuming material until you take it. Partial batches are charged once and remain available when you change the selection or close the screen. Shift-click transfers items. Slabs cost twice as many items as full blocks; costs and yields follow the original catalog of 106 entries.

Cladding Sheet is in the Other category and produces one sheet per block. Apply it to a compatible shape to add a secondary surface. Windows accept glass panes, including stained panes. Breaking a shape returns its base shape and secondary material separately; pick-block copies the base shape without duplicating its secondary material.

- **Hammer:** rotate a shape; sneak to cycle its mounting face. Offset shapes alternate their offset before turning.
- **Chisel:** click an edge zone to disconnect/reconnect neighboring shapes, or click the center to remove cladding/glazing. It also harvests glass, glass panes, glowstone and ice.
- **Glowbrush:** make a shape emit light; sneak to remove the glow.

Placement uses the clicked face and hit position, neighboring profiles and stair alignment. A translucent preview shows the planned placement. Roof ridges and valleys adapt to neighbors. Plain and corner windows join compatible frames, including diagonal end caps and glazing boundaries. Original model collision boxes and support masks determine movement, selection and attachment. Source materials supply textures, tint, transparency and mining behavior.

Data packs can extend material acceptance with the block tag `architecturecraft:sawbench_materials` and mark materials emissive with `architecturecraft:emissive_materials`. Both tags are empty by default. Full collision cubes (including transparent mod blocks) and slabs are accepted without a tag or a particular glass superclass; blocks with block entities are excluded unless explicitly allowed.

This branch replaces the Forge 1.7.10 implementation. It does not migrate 1.7.10 worlds or numeric metadata IDs to modern Minecraft saves. Use a new 1.21.10 world. See [PORTING.md](PORTING.md) for the reference and validation scope.

[View the restored sawbench in Minecraft 1.21.10](docs/images/sawbench-1.21.10.png).

## Developing

Use a full **JDK 21** and the checked-in Gradle wrapper:

```sh
./gradlew build
./gradlew runClient
```

`build` runs geometry/reference unit tests and server GameTests and produces the installable jar in `build/libs`. The `-sources.jar` is for development. Server results are in `build/test-results/gametest/results.xml`; JUnit results are in `build/test-results/test`.

The client test loads every shape item, renders a catalog world, clicks the illustrated sawbench, takes output, and compares an actual networked placement with its preview:

```sh
./gradlew runClientGameTest
```

It needs a graphical display (or Xorg/Xvfb with software OpenGL). Screenshots are written under `build/run/clientGameTest/screenshots`. This test is separate from `build`, so ordinary headless builds do not need a display.

Client code lives in `src/client/java`, common/server code in `src/main/java`, and the test-only mod in `src/gametest`. The test mod and its data-pack overrides are excluded from release jars. Original `.objson` models and GUI textures are retained under `assets/architecturecraft`; `shape/RoofMesh.java` retains procedural roof tessellation.

On a cloud machine behind an HTTP proxy, configure Java's HTTP/HTTPS proxy properties from the machine's proxy settings. A separately downloaded JDK may need the machine's trusted CA store via `javax.net.ssl.trustStore`. Keep TLS and artifact verification enabled.
