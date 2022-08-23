
package chatty.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Wraps around a Map and provides some additional methods intended to make
 * working with Collection or Map values easier.
 * 
 * @author tduva
 */
public class SpecialMap<K, V> implements Map<K, V> {

    private final Map<K, V> map;
    private final Supplier<V> valueCreator;
    private V optional;

    public SpecialMap(Map<K, V> map, Supplier<V> valueCreator) {
        this.map = map;
        this.valueCreator = valueCreator;
    }

    /**
     * Get the value for the given key, or if there is no mapping for the key
     * adds the key with a value created by the {@code valueCreator} defined in
     * the constructor. This is mostly intended for cases where the value is a
     * Map or Collection, for example to add a value:
     * {@code m.getPut(key).add(subValue)}.
     *
     * <p>
     * The default method
     * {@link #putIfAbsent(java.lang.Object, java.lang.Object) putIfAbsent()} is
     * similar, but requires the value to be directly provided, instead of using
     * the {@code valueCreator} which only has to be defined once.
     *
     * @param key
     * @return 
     */
    public V getPut(K key) {
        if (!containsKey(key)) {
            put(key, valueCreator.get());
        }
        return get(key);
    }
    
    /**
     * Get the value for the given key.
     * 
     * <p>
     * If there is no mapping for the key an instance of a value created by
     * {@code valueCreator} defined in the constructor is returned. The instance
     * is shared between all calls to this function where there is no mapping
     * for the key. It is intended to be an empty non-null value, so that for
     * example something like {@code getOptional(key).get(otherKey)} works. It
     * is not recommended to modify the returned value.
     * 
     * @param key
     * @return 
     */
    public V getOptional(K key) {
        if (!containsKey(key)) {
            if (optional == null) {
                optional = valueCreator.get();
            }
            return optional;
        }
        return get(key);
    }
    
    /**
     * The added up size of all value objects of the map. Only supported for Map
     * and Collection values at the moment.
     *
     * <p>
     * For example for {@code Map<String, Set<Emoticon>>} it would count the
     * number of Emoticon objects.
     * 
     * @return 
     */
    public int subSize() {
        int total = 0;
        for (Object value : map.values()) {
            if (value instanceof Map) {
                total += ((Map) value).size();
            }
            else if (value instanceof Collection) {
                total += ((Collection) value).size();
            }
        }
        return total;
    }
    
    public boolean subIsEmpty() {
        return subSize() == 0;
    }
    
    /**
     * Remove a value from all value objects of the map. Only supported for Map
     * and Collection values at the moment.
     * 
     * <p>
     * For example for {@code Map<String, Set<Emoticon>>} it would remove an
     * Emoticon object from all of the Set objects.
     * 
     * @param o 
     */
    public void subRemoveValue(Object o) {
        for (Object value : map.values()) {
            if (value instanceof Map) {
                ((Map) value).values().remove(o);
            }
            else if (value instanceof Collection) {
                ((Collection) value).remove(o);
            }
        }
    }
    
    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

}
