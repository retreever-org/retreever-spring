/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.repo;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
