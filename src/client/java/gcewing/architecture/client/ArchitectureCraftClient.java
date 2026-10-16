package gcewing.architecture.client;

import gcewing.architecture.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ArchitectureCraftClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    MenuScreens.register(ArchitectureCraft.SAWBENCH_MENU, SawbenchScreen::new);
    ModelLoadingPlugin.register(
        context -> {
          context
              .modifyBlockModelAfterBake()
              .register(
                  (model, bake) -> {
                    if (bake.state().is(ArchitectureCraft.SHAPE)
                        || bake.state().is(ArchitectureCraft.SAWBENCH))
                      return new ArchitectureBlockModel(
                          model, bake.state().is(ArchitectureCraft.SAWBENCH));
                    return model;
                  });
          context
              .modifyItemModelAfterBake()
              .register(
                  (model, bake) -> {
                    if (bake.itemId().equals(ArchitectureCraft.id("shape"))
                        || bake.itemId().equals(ArchitectureCraft.id("cladding"))
                        || bake.itemId().equals(ArchitectureCraft.id("sawbench")))
                      return new ArchitectureItemModel(bake.itemId().getPath());
                    return model;
                  });
        });
  }
}
