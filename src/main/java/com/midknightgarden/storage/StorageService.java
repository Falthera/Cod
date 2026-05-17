package com.midknightgarden.storage;

import com.midknightgarden.quest.Quest;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface StorageService {
    void initialize();
    void shutdown();
    void saveQuest(Quest quest);
    void getQuestByIdAsync(UUID id, Consumer<Optional<Quest>> callback);
    void markQuestComplete(UUID id);
}
