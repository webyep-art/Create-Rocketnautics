package dev.ryanhcode.sable.util.iterator;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

@ApiStatus.Internal
public class IteratorBackedFilterIterator<T> implements Iterator<T> {

    private final Predicate<T> filter;
    private final Iterator<T> backingIterator;

    private T nextObject;

    public IteratorBackedFilterIterator(final Predicate<T> filter, final Iterator<T> backingIterator) {
        this.filter = filter;
        this.backingIterator = backingIterator;
    }

    public @Nullable T findNextObject() {
        if (this.nextObject != null) {
            return this.nextObject;
        }

        while (this.backingIterator.hasNext()) {
            final T next = this.backingIterator.next();
            if (this.filter.test(next)) {
                return this.nextObject = next;
            }
        }

        return null;
    }

    @Override
    public boolean hasNext() {
        return this.findNextObject() != null;
    }

    @Override
    public T next() {
        if (this.findNextObject() == null) {
            throw new NoSuchElementException();
        }

        final T result = this.nextObject;
        this.nextObject = null;
        return result;
    }
}
