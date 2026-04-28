package dev.ryanhcode.sable.util.iterator;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

@ApiStatus.Internal
public class ListBackedFilterIterator<T> implements Iterator<T> {

    private final Predicate<T> filter;
    private final List<T> backingList;

    private int index;
    private T nextObject;

    public ListBackedFilterIterator(final Predicate<T> filter, final List<T> backingList) {
        this.filter = filter;
        this.backingList = backingList;
    }


    public @Nullable T findNextObject() {
        if (this.nextObject != null) {
            return this.nextObject;
        }

        for (; this.index < this.backingList.size(); this.index++) {
            final T next = this.backingList.get(this.index);

            if (this.filter.test(next)) {
                this.index++;
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
