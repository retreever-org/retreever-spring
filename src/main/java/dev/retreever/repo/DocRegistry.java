/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.repo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic ordered registry for storing items by key.
 * Used by various resolver components to accumulate
 * intermediate or final documentation models.
 *
 * @param <T> type of item stored in the registry
 */
public class DocRegistry<T> {

    private final Map<String, T> items = new LinkedHashMap<>();

    public boolean contains(String key) {
        return items.containsKey(key);
    }

    public void add(String key, T item) {
        items.putIfAbsent(key, item);
    }

    public T get(String key) {
        return items.get(key);
    }

    public Map<String, T> getAll() {
        return items;
    }

    public int size() {
        return items.size();
    }
}
