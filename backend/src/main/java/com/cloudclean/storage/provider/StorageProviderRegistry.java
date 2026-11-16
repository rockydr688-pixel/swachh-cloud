package com.cloudclean.storage.provider;

import com.cloudclean.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StorageProviderRegistry {

    private final Map<String, StorageProvider> providerMap = new ConcurrentHashMap<>();

    public StorageProviderRegistry(List<StorageProvider> providers) {
        for (StorageProvider provider : providers) {
            providerMap.put(provider.getProviderType().toLowerCase(), provider);
        }
    }

    public StorageProvider getProvider(String type) {
        if (type == null) {
            throw new BadRequestException("Storage provider type cannot be null");
        }
        StorageProvider provider = providerMap.get(type.toLowerCase());
        if (provider == null) {
            throw new BadRequestException("Unsupported storage provider type: " + type + ". Supported types: " + providerMap.keySet());
        }
        return provider;
    }

    public boolean hasProvider(String type) {
        return type != null && providerMap.containsKey(type.toLowerCase());
    }
}

