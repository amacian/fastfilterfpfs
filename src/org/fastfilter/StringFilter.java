package org.fastfilter;

public interface StringFilter {

    /**
     * Whether the set may contain the key.
     *
     * @param key the key
     * @return true if the set might contain the key, and false if not
     */
    boolean mayContain(String key);

    /**
     * Get the number of bits in the filter.
     *
     * @return the number of bits
     */
    long getBitCount();

    /**
     * Whether the add operation (after construction) is supported.
     *
     * @return true if yes
     */
    default boolean supportsAdd() {
        return false;
    }

    /**
     * Add a key.
     *
     * @param key the key
     */
    default void add(String key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether the remove operation is supported.
     *
     * @return true if yes
     */
    default boolean supportsRemove() {
        return false;
    }

    /**
     * Remove a key.
     *
     * @param key the key
     */
    default void remove(String key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the number of set bits. This should be 0 for an empty filter.
     *
     * @return the number of bits
     */
    default long cardinality() {
        return -1;
    }

}
