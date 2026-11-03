package com.almarel.orion.mapping;

import com.almarel.orion.mapping.visual.BlockVisualProfileRepository;
import com.almarel.orion.mapping.visual.NearestBlockByColorFinder;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.almarel.orion.mapping.block.BlockVariantMapper;

/**
 * Модуль общей (используемой несколькими несвязанными фичами) инфраструктуры:
 * мэппинг вариантов блоков (mapping.core — curve, pattern, brush.copy) и визуальный
 * профиль блоков (mapping.visual — recolor + tag-система, см. BlockTagResolver).
 * Shape-классификация и customid вынесены в RecolorModule и CustomIdModule соответственно.
 */
public class MappingModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(BlockVariantMapper.class);

        bind(BlockVisualProfileRepository.class).in(Scopes.SINGLETON);
        bind(NearestBlockByColorFinder.class).in(Scopes.SINGLETON);
    }
}