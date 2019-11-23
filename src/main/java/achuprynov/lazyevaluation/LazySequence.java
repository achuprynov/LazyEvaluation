/*
 * Copyright (C) 2017 Alexander Chuprynov <achuprynov@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package achuprynov.lazyevaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Alexander Chuprynov <achuprynov@gmail.com>
 */
public class LazySequence<E> implements LazyEvaluation<E> {

    private volatile E element = null;
    private volatile LazySequence<E> nextSequence = null;
    private final Supplier<E> elementSupplier;
    private final Supplier<LazySequence<E>> nextSupplier;
    private static volatile LazySequence<?> emptySequence;

    public LazySequence() {
        this.elementSupplier = null;
        this.nextSupplier = null;
    }

    public LazySequence(Supplier<E> elementSupplier, Supplier<LazySequence<E>> nextSupplier) {
        this.elementSupplier = Objects.requireNonNull(elementSupplier);
        this.nextSupplier = Objects.requireNonNull(nextSupplier);
    }

    public static <E> LazySequence<E> of(E... elements) {
        return of(Arrays.asList(elements).iterator());
    }
    
    public static <E> LazySequence<E> of(Iterator<E> iterator) {
        if (iterator.hasNext()) {
            return new LazySequence<>(
                () -> iterator.next(), 
                () -> of(iterator));
        } else {
            return (LazySequence<E>)empty();
        }
    }

    /**
     * An iterator over a lazy sequence. NOT THREAD-SAFE!
     */
    @Override
    public Iterator<E> iterator() {
        LazySequence<E> thisLazySequence = this;

        return new Iterator<E>() {
            private volatile LazySequence<E> lazySequence = thisLazySequence;

            @Override
            public boolean hasNext() {
                return lazySequence.element() != null;
            }

            @Override
            public E next() {
                final E element = lazySequence.element();
                lazySequence = lazySequence.next();
                return element;
            }
        };
    }

    private E element() {
        if (element == null && elementSupplier != null) {
            synchronized(this) {
                if (element == null) {
                    element = elementSupplier.get();
                }
            }
        }
        return element;
    }

    private LazySequence<E> next() {
        if (nextSequence == null) {
            synchronized(this) {
                if (nextSequence == null) {
                    nextSequence = nextSupplier.get();
                }
            }
        }
        return nextSequence;
    }
    
    private static synchronized <E> LazySequence<E> empty() {
        if (emptySequence == null) {
            emptySequence = new LazySequence<E>();
        }
        return (LazySequence<E>)emptySequence;
    }
    
    @Override
    public LazySequence<E> filter(@Nonnull LazyEvaluation.Predicate<E> predicate) {
        LazySequence<E> thisLazySequence = this;
        
        Iterator<LazySequence<E>> iterator = new Iterator<LazySequence<E>>() {
            private volatile LazySequence<E> lazySequence = thisLazySequence;

            @Override
            public boolean hasNext() {
                return lazySequence != null && lazySequence.element() != null;
            }

            @Override
            public LazySequence<E> next() {
                LazySequence<E> currentSequence = lazySequence;
                lazySequence = lazySequence.next();
                return currentSequence;
            }
        };

        return new LazySequence<E>(
            () -> {
                while (iterator.hasNext()) {
                    LazySequence<E> sequence = iterator.next();
                    if (predicate.apply(sequence.element())) {
                        return sequence.element();
                    }
                }
                return null;
            },
            () -> {
                if (iterator.hasNext()) {
                    return iterator.next().filter(predicate);
                } else {
                    return empty();
                }
            });
    }

    @Override
    public <T> LazySequence<T> transform(@Nonnull Function<E, T> transformer) {
        return new LazySequence<T>(
            () -> {
                if (element() != null) {
                    return transformer.apply(element());
                } else {
                    return null;
                }
            }, 
            () -> { 
                if (next() != null) {
                    return next().transform(transformer);
                } else {
                    return empty();
                }
            });
    }
    
    @Override
    public <T> T aggregate(@Nullable T initValue, @Nonnull Aggregator<E, T> aggregator) {
        for (E element : this) {
            initValue = aggregator.apply(initValue, element);
        }
        return initValue;
    }

    @Override
    public SortedSet<E> toSet(@Nonnull Comparator<E> comarator) {
          Stream<E> targetStream = StreamSupport.stream(this.spliterator(), false);
          return targetStream.sorted(comarator).collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
    }

    @Override
    public List<E> toList() {
       Stream<E> targetStream = StreamSupport.stream(this.spliterator(), false);
       return targetStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public @Nullable E findFirst(@Nonnull LazyEvaluation.Predicate<E> predicate) {
        for (E element : this) {
            if (predicate.apply(element)) {
                return element;
            }
        }
        return null;
    }
}
