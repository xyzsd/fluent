/*
 *
 *  Copyright (C) 2025, xyzsd (Zach Del)
 *  Licensed under either of:
 *
 *    Apache License, Version 2.0
 *       (see LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0)
 *    MIT license
 *       (see LICENSE-MIT) or http://opensource.org/licenses/MIT)
 *
 *  at your option.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *
 */
package fluent.bundle;

import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;


/// A simple, concurrent, threadsafe simple LRU function cache.
///
/// This cache *should not* be shared between bundles, as the Locale is not part of the cache key.
///
@NullMarked
public final class LRUFunctionCache implements FluentFunctionCache {

    private static final int DEFAULT_SIZE = 32;


    private final LRUCache<CacheKey, FluentFunction> lruCache;


    private LRUFunctionCache(final int size) {
        lruCache = new LRUCache<>( size );
    }

    ///  Create a FluentFunctionCache with the default size.
    public static FluentFunctionCache of() {
        return new LRUFunctionCache( DEFAULT_SIZE );
    }

    ///  Create a FluentFunctionCache with the specified size, which must be a positive integer.
    ///
    /// @throws IllegalArgumentException if size is <= 0
    public static FluentFunctionCache of(final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException( "size must > 0" );
        }

        return new LRUFunctionCache( size );
    }




    ///  {@inheritDoc}
    @SuppressWarnings("unchecked")
    public <T extends FluentFunction> T getFunction(final FluentFunctionFactory<T> factory, final Locale locale, final Options options) {
        requireNonNull( factory );
        requireNonNull( locale );
        requireNonNull( options );

        if (factory.canCache()) {
            final CacheKey key = new CacheKey( factory.name(), options );
            return (T) lruCache.computeIfAbsent( key, __ -> factory.create( locale, options ) );
        }

        return factory.create( locale, options );
    }

    /// we need to create a key based on the function name AND the options.
    /// we DO NOT use Locale, so this cache SHOULD NOT be shared between bundles
    private static final class CacheKey {
        private final String name;
        private final Options options;
        private final int hash;

        CacheKey(String name, Options options) {
            this.name = name;
            this.options = options;
            this.hash = (name.hashCode() << 3) ^ options.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o instanceof CacheKey cacheKey) {
                return name.equals( cacheKey.name ) && options.equals( cacheKey.options );
            }

            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    ///  A very simple concurrent LRU cache
    private static class LRUCache<K, V> {
        private final int size;
        // todo: re-evaluate ... if we are explicitly controlling the locks, we may not need ConcurrentXXX classes
        private final ConcurrentHashMap<K, V> cache;
        private final ConcurrentLinkedDeque<K> ordering;
        private final ReentrantLock lock;

        public LRUCache(final int size) {
            this.size = size;
            this.cache = new ConcurrentHashMap<>( size );
            this.ordering = new ConcurrentLinkedDeque<>();
            this.lock = new ReentrantLock();
        }

        public @Nullable V get(final K key) {
            lock.lock();
            try {
                final V value = cache.get( key );

                if (value != null) {
                    ordering.remove( key );
                    ordering.addFirst( key );
                }

                return value;
            } finally {
                lock.unlock();
            }
        }

        public void put(final K key, final V value) {
            lock.lock();
            try {
                if (cache.containsKey( key )) {
                    cache.put( key, value );
                    ordering.remove( key );
                    ordering.addFirst( key );
                } else {
                    if (cache.size() >= size) {
                        final K leastRecent = ordering.pollLast();
                        if (leastRecent != null) {
                            cache.remove( leastRecent );
                        }
                    }

                    cache.put( key, value );
                    ordering.addFirst( key );
                }
            } finally {
                lock.unlock();
            }
        }

        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            lock.lock();
            try {
                V value = get( key );
                if (value == null) {
                    value = mappingFunction.apply( key );
                    put( key, value );
                }
                return value;
            } finally {
                lock.unlock();
            }
        }
    }
}
