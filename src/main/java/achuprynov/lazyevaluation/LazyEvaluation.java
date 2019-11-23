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

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Alexander Chuprynov <achuprynov@gmail.com>
 */
public interface LazyEvaluation<E> extends Iterable<E> {

    LazyEvaluation<E> filter(@Nonnull Predicate<E> predicate);

    <T> LazyEvaluation<T> transform(@Nonnull Function<E, T> transformer);

    <T> T aggregate(@Nullable T initValue, @Nonnull Aggregator<E, T> aggregator);

    SortedSet<E> toSet(@Nonnull Comparator<E> comarator);

    List<E> toList();

    @Nullable E findFirst(@Nonnull Predicate<E> predicate);

    public static interface Predicate<E> {
        boolean apply(@Nullable E element);
    }

    public static interface Function<E, T> {
        @Nullable
        T apply(@Nullable E element);
    }

    public static interface Aggregator<E, T> {
        @Nullable
        T apply(@Nullable T aggregator, @Nullable E element);
    }
}
