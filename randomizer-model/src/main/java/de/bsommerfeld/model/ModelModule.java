package de.bsommerfeld.model;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import de.bsommerfeld.model.action.config.ActionConfig;
import de.bsommerfeld.model.action.impl.DefaultActionExecutor;
import de.bsommerfeld.model.action.impl.DefaultFocusManager;
import de.bsommerfeld.model.action.repository.DefaultActionRepository;
import de.bsommerfeld.model.action.repository.DefaultActionSequenceRepository;
import de.bsommerfeld.model.action.sequence.DefaultActionSequenceDispatcher;
import de.bsommerfeld.model.action.sequence.DefaultActionSequenceExecutor;
import de.bsommerfeld.model.action.spi.ActionExecutor;
import de.bsommerfeld.model.action.spi.ActionRepository;
import de.bsommerfeld.model.action.spi.ActionSequenceDispatcher;
import de.bsommerfeld.model.action.spi.ActionSequenceExecutor;
import de.bsommerfeld.model.action.spi.ActionSequenceRepository;
import de.bsommerfeld.model.action.spi.FocusManager;
import de.bsommerfeld.model.config.keybind.KeyBindNameTypeMapper;
import de.bsommerfeld.model.config.keybind.KeyBindRepository;
import de.bsommerfeld.model.persistence.GsonProvider;
import de.bsommerfeld.model.persistence.JsonUtil;
import de.bsommerfeld.model.persistence.dao.ActionSequenceDao;
import de.bsommerfeld.model.persistence.de_serializer.ActionJsonDeSerializer;
import de.bsommerfeld.model.persistence.de_serializer.ActionSequenceJsonDeSerializer;

public class ModelModule extends AbstractModule {

    @Override
    protected void configure() {
        // Configuration
        bind(ActionConfig.class).asEagerSingleton();

        // Core application components
        bind(ApplicationContext.class).asEagerSingleton();
        bind(JsonUtil.class).asEagerSingleton();

        // Action components
        bind(ActionRepository.class).to(DefaultActionRepository.class).asEagerSingleton();
        bind(ActionSequenceRepository.class).to(DefaultActionSequenceRepository.class).asEagerSingleton();
        bind(ActionSequenceDispatcher.class).to(DefaultActionSequenceDispatcher.class).asEagerSingleton();
        bind(ActionSequenceExecutor.class).to(DefaultActionSequenceExecutor.class);
        bind(ActionExecutor.class).to(DefaultActionExecutor.class).asEagerSingleton();
        bind(FocusManager.class).to(DefaultFocusManager.class).asEagerSingleton();

        // KeyBind components
        bind(KeyBindRepository.class).asEagerSingleton();
        bind(KeyBindNameTypeMapper.class).asEagerSingleton();

        // Persistence components
        bind(ActionSequenceDao.class).asEagerSingleton();
        bind(ActionJsonDeSerializer.class);
        bind(ActionSequenceJsonDeSerializer.class);

        // Gson provider
        bind(Gson.class).toProvider(GsonProvider.class);
    }
}
