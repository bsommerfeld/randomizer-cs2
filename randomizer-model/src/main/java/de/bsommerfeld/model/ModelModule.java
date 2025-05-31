package de.bsommerfeld.model;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import de.bsommerfeld.model.action.repository.ActionRepository;
import de.bsommerfeld.model.action.repository.ActionSequenceRepository;
import de.bsommerfeld.model.action.sequence.ActionSequenceDispatcher;
import de.bsommerfeld.model.action.sequence.ActionSequenceExecutorRunnable;
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
        bind(ApplicationContext.class).asEagerSingleton();
        bind(ActionRepository.class).asEagerSingleton();
        bind(ActionSequenceRepository.class).asEagerSingleton();
        bind(KeyBindRepository.class).asEagerSingleton();
        bind(KeyBindNameTypeMapper.class).asEagerSingleton();
        bind(ActionSequenceDispatcher.class).asEagerSingleton();
        bind(JsonUtil.class).asEagerSingleton();
        bind(ActionSequenceDao.class).asEagerSingleton();

        bind(ActionSequenceExecutorRunnable.class);
        bind(ActionJsonDeSerializer.class);
        bind(ActionSequenceJsonDeSerializer.class);

        bind(Gson.class).toProvider(GsonProvider.class);
    }
}
