package org.infinispan.persistence.spi;

import java.util.function.Predicate;

import org.infinispan.commons.util.IntSet;
import org.infinispan.marshall.core.MarshalledEntry;
import org.reactivestreams.Publisher;

import net.jcip.annotations.ThreadSafe;

/**
 * An interface implementing both {@link AdvancedCacheWriter} and {@link AdvancedCacheLoader} as well as overrides
 * of those methods that can be optimized when a segment is already known for the key or a subset of segments are to
 * be used instead of the entire store.
 * @author wburns
 * @since 9.4
 */
@ThreadSafe
public interface SegmentedAdvancedLoadWriteStore<K, V> extends AdvancedLoadWriteStore<K, V> {
   // CacheLoader methods

   /**
    * Fetches an entry from the storage given a segment to optimize this lookup based on. If a
    * {@link MarshalledEntry} needs to be created here, {@link InitializationContext#getMarshalledEntryFactory()} and
    * {@link InitializationContext#getByteBufferFactory()} should be used.
    * <p>
    * The provided segment may be used for performance purposes, however it it is acceptable to ignore this argument.
    * @implSpec Default implementation just invokes the {@link CacheLoader#load(Object)} method.
    * <pre> {@code
    * MarshalledValue<K, V> value = load(key);
    * }
    * </pre>
    * @param segment the segment that the key maps to
    * @param key the key of the entry to fetch
    * @return the entry, or null if the entry does not exist
    * @throws PersistenceException in case of an error, e.g. communicating with the external storage
    */
   default MarshalledEntry<K, V> load(int segment, Object key) {
      return load(key);
   }

   /**
    * Returns true if the storage contains an entry associated with the given key in the given segment
    * <p>
    * The provided segment may be used for performance purposes, however it it is acceptable to ignore this argument.
    * @implSpec Default implementation just invokes the {@link CacheLoader#contains(Object)} method.
    * <pre> {@code
    * boolean containsKey = contains(key);
    * }
    * </pre>
    * @param segment the segment that the key maps to
    * @param key the key to see if exists
    * @return true if the key is present in this loader with a given segment
    * @throws PersistenceException in case of an error, e.g. communicating with the external storage
    */
   default boolean contains(int segment, Object key) {
      return contains(key);
   }

   // CacheWriter methods

   /**
    * Persists the entry to the storage with the given segment to optimize further lookups based on
    * <p>
    * The provided segment may be used for performance purposes, however it it is acceptable to ignore this argument.
    * @implSpec Default implementation just invokes the {@link CacheWriter#write(MarshalledEntry)} method.
    * <pre> {@code
    * write(entry);
    * }
    * </pre>
    * @param segment the segment to persist this entry to
    * @param entry the entry to write to the store
    * @throws PersistenceException in case of an error, e.g. communicating with the external storage
    * @see MarshalledEntry
    */
   default void write(int segment, MarshalledEntry<? extends K, ? extends V> entry) {
      write(entry);
   }

   /**
    * Removes the entry for the provided key which is in the given segment. This method then returns whether the
    * entry was removed or not.
    * <p>
    * The provided segment may be used for performance purposes, however it it is acceptable to ignore this argument.
    * @implSpec Default implementation just invokes the {@link CacheWriter#delete(Object)} method.
    * <pre> {@code
    * boolean deleted = delete(key);
    * }
    * </pre>
    * @param segment the segment that this key maps to
    * @param key the key of the entry to remove
    * @return true if the entry existed in the persistent store and it was deleted.
    * @throws PersistenceException in case of an error, e.g. communicating with the external storage
    */
   default boolean delete(int segment, Object key) {
      return delete(key);
   }

   // AdvancedCacheLoader methods

   /**
    * Returns the number of elements in the store that map to the given segments that aren't expired.
    * <p>
    * The segments here <b>must</b> be adhered to and the size must not count any entries that don't belong to
    * the provided segments.
    * @param segments the segments which should have their entries counted. Always non null.
    * @return the count of entries in the given segments
    * @throws PersistenceException in case of an error, e.g. communicating with the external storage
    */
   int size(IntSet segments);

   /**
    * Publishes all the keys that map to the given segments from this store. The given publisher can be used by as many
    * {@link org.reactivestreams.Subscriber}s as desired. Keys are not retrieved until a given Subscriber requests
    * them from the {@link org.reactivestreams.Subscription}.
    * <p>
    * Stores will return only non expired keys
    * <p>
    * The segments here <b>must</b> be adhered to and the keys published must not include any that don't belong to
    * the provided segments.
    * @param segments the segments that the keys must map to. Always non null.
    * @param filter a filter
    * @return a publisher that will provide the keys from the store
    */
   Publisher<K> publishKeys(IntSet segments, Predicate<? super K> filter);

   /**
    * Publishes all entries from this store.  The given publisher can be used by as many
    * {@link org.reactivestreams.Subscriber}s as desired. Entries are not retrieved until a given Subscriber requests
    * them from the {@link org.reactivestreams.Subscription}.
    * <p>
    * If <b>fetchMetadata</b> is true this store must guarantee to not return any expired entries.
    * <p>
    * The segments here <b>must</b> be adhered to and the entries published must not include any that don't belong to
    * the provided segments.
    * @param segments the segments that the keys of the entries must map to. Always non null.
    * @param filter a filter on the keys of the entries that if passed will allow the given entry to be returned from the publisher
    * @param fetchValue whether the value should be included in the marshalled entry
    * @param fetchMetadata whether the metadata should be included in the marshalled entry
    * @return a publisher that will provide the entries from the store that map to the given segments
    */
   Publisher<MarshalledEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter, boolean fetchValue,
         boolean fetchMetadata);

   // AdvancedCacheWriter methods

   /**
    * Removes all the data that maps to the given segments from the storage.
    * <p>
    * This method must only remove entries that map to the provided segments.
    * @param segments data mapping to these segments are removed. Always non null.
    * @throws PersistenceException in case of an error, e.g. communicating with the external storage
    */
   void clear(IntSet segments);

   /**
    * Invoked when a node becomes an owner of the given segments. Note this method is only invoked for non shared
    * store implementations.
    * @param segments segments to associate with this store. Always non null.
    * @implSpec This method does nothing by default
    */
   default void addSegments(IntSet segments) { }

   /**
    * Invoked when a node loses ownership of a segment. The provided segments are the ones this node no longer owns.
    * Note this method is only invoked for non shared store implementations.
    * @param segments segments that should no longer be associated with this store. Always non null.
    * @implSpec This method does nothing by default
    */
   default void removeSegments(IntSet segments) { }
}
