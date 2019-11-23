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

import achuprynov.lazyevaluation.LazyEvaluation;
import achuprynov.lazyevaluation.LazySequence;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author Alexander Chuprynov <achuprynov@gmail.com>
 */
public class LazyEvaluationTest {

    @Test
    public void testFilter() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        LazyEvaluation<Integer> source = LazySequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Assert.assertEquals("Operation count is correct", 0, counter.get());
        
        LazyEvaluation.Predicate<Integer> predicate = new LazyEvaluation.Predicate<Integer>() {
            @Override
            public boolean apply(Integer element) {
                counter.incrementAndGet();
                return element % 2 == 0;
            }
        };

        LazyEvaluation<Integer> filtered = source.filter(predicate);

        Assert.assertEquals("Operation count is correct", 0, counter.get());
        
        for (Integer element : filtered) {
            counter.incrementAndGet();
            Assert.assertEquals("predicate.apply(element) is correct", true, predicate.apply(element));
        }
        
        Assert.assertEquals("Operation count is correct", 20, counter.get());
    }
    
    @Test
    public void testTransform() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        LazyEvaluation<Integer> source = LazySequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> resultList = Stream.of(0, 1, 1, 2, 2, 3, 3, 4, 4, 5).collect(Collectors.toList());

        Assert.assertEquals("Operation count is correct", 0, counter.get());
        
        LazyEvaluation.Function<Integer, Integer> function = new LazyEvaluation.Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer element) {
                counter.incrementAndGet();
                return element / 2;
            }
        };
        
        LazyEvaluation<Integer> transformed = source.transform(function);

        Assert.assertEquals("Operation count is correct", 0, counter.get());
        
        List<Integer> transformedList = transformed.toList();

        boolean equalLists = transformedList.size() == resultList.size() && resultList.containsAll(transformedList);
        
        Assert.assertEquals("Operation count is correct", true, equalLists);
    }

    @Test
    public void testAggregate() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        LazyEvaluation<Integer> source = LazySequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Assert.assertEquals("Operation count is correct", 0, counter.get());

        LazyEvaluation<Integer> filtered = source.filter(new LazyEvaluation.Predicate<Integer>() {
            @Override
            public boolean apply(Integer element) {
                counter.incrementAndGet();
                return element % 2 == 0;
            }
        });

        Assert.assertEquals("Operation count is correct", 0, counter.get());

        LazyEvaluation<Integer> transformed = filtered.transform(new LazyEvaluation.Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer element) {
                counter.incrementAndGet();
                return element / 2;
            }
        });

        Assert.assertEquals("Operation count is correct", 0, counter.get());

        int result = transformed.aggregate(0, new LazyEvaluation.Aggregator<Integer, Integer>() {
            @Override
            public Integer apply(Integer aggregator, Integer element) {
                counter.incrementAndGet();
                return aggregator + element;
            }
        });

        Assert.assertEquals("Operation count is correct", 20, counter.get());

        Assert.assertEquals("Result is correct", 15, result);
    }

    @Test
    public void testFindFirst() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        LazyEvaluation<Integer> source = LazySequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Assert.assertEquals("Operation count is correct", 0, counter.get());

        LazyEvaluation<Integer> filtered = source.filter(new LazyEvaluation.Predicate<Integer>() {
            @Override
            public boolean apply(Integer element) {
                counter.incrementAndGet();
                return element % 2 == 0;
            }
        });

        Assert.assertEquals("Operation count is correct", 0, counter.get());

        LazyEvaluation<Integer> transformed = filtered.transform(new LazyEvaluation.Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer element) {
                counter.incrementAndGet();
                return element / 2;
            }
        });

        Assert.assertEquals("Operation count is correct", 0, counter.get());

        Integer result = transformed.findFirst(new LazyEvaluation.Predicate<Integer>() {
            @Override
            public boolean apply(Integer element) {
                counter.incrementAndGet();
                return element % 3 == 0;
            }
        });

        Assert.assertEquals("Operation count is correct", 12, counter.get());

        Assert.assertEquals("Result is correct", Integer.valueOf(3), result);
    }

    @Test
    public void testToSet() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        LazyEvaluation<Integer> source = LazySequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        Assert.assertEquals("Operation count is correct", 0, counter.get());
        
        Comparator<Integer> comparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer element1, Integer element2) {
                counter.incrementAndGet();
                if (element1 < element2) {
                    return -1;
                } else if (element1 > element2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        
        SortedSet<Integer> sourceSet = source.toSet(comparator);
        
        Assert.assertEquals("Operation count is correct", 9, counter.get());
        
        counter.set(0);
        
        for (Integer element : sourceSet) {
            int counterValue = counter.incrementAndGet();
            Assert.assertEquals("Element is correct", (Integer)counterValue, element);
        }
    }

    @Test
    public void testToList() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        LazyEvaluation<Integer> source = LazySequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
                
        Assert.assertEquals("Operation count is correct", 0, counter.get());

        List<Integer> sourceList = source.toList();
        
        for (Integer element : sourceList) {
            int counterValue = counter.incrementAndGet();
            Assert.assertEquals("Element is correct", (Integer)counterValue, element);
        }
    }
    
    @Test
    public void testThreadSafe() throws Exception {
        int maxValue = 1000000;
        final int threadCount = 100;
        AtomicInteger threadCountDone = new AtomicInteger(0);

        LazyEvaluation<Integer> source = LazySequence.of(new Iterator<Integer>() {
            private volatile int currentValue = 0;
            
            @Override
            public boolean hasNext() {
                if (currentValue >= maxValue) {
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public Integer next() {
                currentValue++;
                return currentValue;
            }
        });
        
        LazyEvaluation.Predicate<Integer> predicate = new LazyEvaluation.Predicate<Integer>() {
            @Override
            public boolean apply(Integer element) {
                return element == maxValue;
            }
        };

        ArrayList<Thread> threadPool = new ArrayList<Thread>();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                    Integer result = source.findFirst(predicate);
                    Assert.assertEquals("Result is correct", (Integer)maxValue, result);
                    threadCountDone.incrementAndGet();
                }
            });
            threadPool.add(thread);
        }
        
        for (int i = 0; i < threadCount; i++) {
            threadPool.get(i).start();
        }
        
        for (int i = 0; i < threadCount; i++) {
            threadPool.get(i).join();
        }
        
        Assert.assertEquals("Threads done is correct", (Integer)threadCount, (Integer)threadCountDone.get());
    }

}
