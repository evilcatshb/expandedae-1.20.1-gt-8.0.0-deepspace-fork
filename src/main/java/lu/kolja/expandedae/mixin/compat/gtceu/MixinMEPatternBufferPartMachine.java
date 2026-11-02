package lu.kolja.expandedae.mixin.compat.gtceu;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.helpers.patternprovider.PatternContainer;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.MouseData;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;
import com.mojang.blaze3d.platform.InputConstants;
import lu.kolja.expandedae.api.misc.PatternHelper;
import lu.kolja.expandedae.api.patternprovider.IHighlightable;
import lu.kolja.expandedae.definition.ExpLang;
import lu.kolja.expandedae.xmod.gtceu.ExpGtceu;
import lu.kolja.mixinloadconditions.LoadCondition;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@LoadCondition(loadIf = "gtceu")
@Mixin(value = MEPatternBufferPartMachine.class, remap = false)
public abstract class MixinMEPatternBufferPartMachine extends MEBusPartMachine implements ICraftingProvider, PatternContainer, IDataStickInteractable, IHighlightable {

    public MixinMEPatternBufferPartMachine(BlockEntityCreationInfo info, IO io, NotifiableItemStackHandler inventory) {
        super(info, io, inventory);
    }

    @Shadow @Final private InternalInventory internalPatternInventory;

    @Inject(
            method = "getPanelBuilder",
            at = @At("RETURN")
    )
    private void attachConfigurators(PosGuiData data, PanelSyncManager syncManager, UISettings settings, CallbackInfoReturnable<MachineUIPanelBuilder> cir) {
        syncManager.registerServerSyncedAction("eae_modify_patterns", packet -> {
            int mult = packet.readVarInt();
            for (int i = 0; i < this.internalPatternInventory.size(); i++) {
                var currentStack = this.internalPatternInventory.getStackInSlot(i);
                if (currentStack.isEmpty()) continue;
                var newStack = PatternHelper.modifyPatterns(
                        currentStack,
                        mult,
                        this.getLevel());
                this.internalPatternInventory.setItemDirect(i, newStack);
            }
        });

        MachineUIPanelBuilder builder = cir.getReturnValue();
        Consumer<Flow> prev = builder.leftConfigurators();
        builder.leftConfigurators(f -> {
            prev.accept(f);
            f.child(new ButtonWidget<>()
                    .size(18)
                    .overlay(ExpGtceu.MULTIPLY_OVERLAY)
                    .onMousePressed((context, button) -> {
                        MouseData mouseData = MouseData.create(button);
                        int mult = (mouseData.shift() ? 2 : 1)
                                * (mouseData.ctrl() ? 8 : 1)
                                * (button == InputConstants.MOUSE_BUTTON_RIGHT ? -1 : 1);
                        syncManager.callSyncedAction("eae_modify_patterns", buf -> buf.writeVarInt(mult));
                        return true;
                    })
                    .tooltip(new RichTooltip()
                            .addLine(ExpLang.GUI_TOOLTIPS_MODIFY_PATTERNS_GT.text())
                            .addLine(ExpLang.GUI_TOOLTIPS_MODIFY_PATTERNS_HINT_GT.text())
                    )
            );
        });
    }

    @Override
    public BlockEntity eae$getBlockPos() {
        return this;
    }
}
