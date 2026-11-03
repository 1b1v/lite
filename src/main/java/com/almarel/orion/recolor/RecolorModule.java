package com.almarel.orion.recolor;

import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.recolor.core.RecolorMapBuilder;
import com.almarel.orion.recolor.core.RecolorSelectionResolver;
import com.almarel.orion.recolor.core.RegionBlockTypeScanner;
import com.almarel.orion.recolor.core.RecolorExclusionRegistry;
import com.almarel.orion.recolor.palette.QuickPalettePreviewCalculator;
import com.almarel.orion.recolor.presentation.command.RecolorCommand;
import com.almarel.orion.recolor.presentation.command.RecolorPaletteDebugCommand;
import com.almarel.orion.recolor.presentation.gui.RecolorApplyService;
import com.almarel.orion.recolor.presentation.gui.RecolorMenuPresenter;
import com.almarel.orion.recolor.presentation.gui.RecolorMenuView;
import com.almarel.orion.recolor.presentation.gui.RecolorSessionStore;
import com.almarel.orion.recolor.shape.BlockShapeClassifier;
import com.almarel.orion.recolor.shape.MaterialBaseNameExtractor;
import com.almarel.orion.recolor.shape.MaterialFamilyBundler;
import com.almarel.orion.recolor.shape.ShapeAwareStateTransferor;
import com.almarel.orion.recolor.shape.ShapeAwareVariantResolver;
import com.almarel.orion.recolor.shape.ShapeSuffixCandidateGenerator;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.bukkit.event.Listener;

/**
 * Модуль фичи Recolor. Включает в себя и ядро/GUI (recolor.core, recolor.presentation,
 * recolor.palette), и shape-классификацию (recolor.shape) — последняя recolor-эксклюзивна
 * (единственный потребитель во всём плагине), поэтому пакет целиком живёт и биндится
 * здесь, а не в MappingModule. Общий для нескольких фич визуальный профиль блока
 * (mapping.visual, используется также tag-системой) остаётся вне этого модуля.
 */
public class RecolorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RecolorExclusionRegistry.class).in(Scopes.SINGLETON);
        bind(RegionBlockTypeScanner.class).in(Scopes.SINGLETON);
        bind(RecolorMapBuilder.class).in(Scopes.SINGLETON);
        bind(RecolorSessionStore.class).in(Scopes.SINGLETON);
        bind(RecolorMenuPresenter.class).in(Scopes.SINGLETON);
        bind(RecolorMenuView.class).in(Scopes.SINGLETON);
        bind(RecolorApplyService.class).in(Scopes.SINGLETON);
        bind(RecolorSelectionResolver.class).in(Scopes.SINGLETON);
        bind(QuickPalettePreviewCalculator.class).in(Scopes.SINGLETON);
        bind(BlockShapeClassifier.class).in(Scopes.SINGLETON);
        bind(MaterialBaseNameExtractor.class).in(Scopes.SINGLETON);
        bind(ShapeSuffixCandidateGenerator.class).in(Scopes.SINGLETON);
        bind(ShapeAwareVariantResolver.class).in(Scopes.SINGLETON);
        bind(ShapeAwareStateTransferor.class).in(Scopes.SINGLETON);
        bind(MaterialFamilyBundler.class).in(Scopes.SINGLETON);

        Multibinder<AbstractCommand> commandBinder = Multibinder.newSetBinder(binder(), AbstractCommand.class);
        commandBinder.addBinding().to(RecolorCommand.class);
        commandBinder.addBinding().to(RecolorPaletteDebugCommand.class);

        Multibinder<Listener> listenerBinder = Multibinder.newSetBinder(binder(), Listener.class);
        listenerBinder.addBinding().to(RecolorSessionStore.class);
    }
}