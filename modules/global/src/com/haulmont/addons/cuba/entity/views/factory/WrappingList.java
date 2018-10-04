package com.haulmont.addons.cuba.entity.views.factory;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * List that wraps its elements - entities into entity views. Implemented for
 * one-to-many relationship implementation.
 *
 * @param <E> entity type.
 * @param <V> entity view type.
 */
public class WrappingList<E extends Entity, V extends BaseEntityView<E>> implements List<V> {

    private static final Logger log = LoggerFactory.getLogger(WrappingList.class);

    private List<E> delegate;

    private Class<V> entityView;

    private transient Map<E, V> entityViewsCache;

    public WrappingList(List<E> delegate, Class<V> entityView) {
        this.delegate = delegate;
        this.entityView = entityView;
        entityViewsCache = new HashMap<>(delegate.size());
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(((V) o).getOrigin());
    }

    @Override
    public Iterator<V> iterator() {
        return new WrappingListIterator(delegate.listIterator());
    }

    @Override
    public Object[] toArray() {
        return delegate.stream().map(e -> wrapElement(e)).toArray();
    }

    @Override
    public <V> V[] toArray(V[] a) {
        Object[] objects = delegate.toArray(new Object[a.length]);
        List collect = Arrays.stream(objects).map(e -> wrapElement((E) e)).collect(Collectors.toList());
        return (V[]) collect.toArray(a);
    }

    @Override
    public boolean add(V v) {
        E origin = v.getOrigin();
        entityViewsCache.put(origin, v);
        return delegate.add(origin);
    }

    @Override
    public boolean remove(Object o) {
        E origin = ((V) o).getOrigin();
        entityViewsCache.remove(origin);
        return delegate.remove(origin);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Collection unwrapped = c.stream().map(e -> ((BaseEntityView) e).getOrigin()).collect(Collectors.toList());
        return delegate.containsAll(unwrapped);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        Collection<E> unwrapped = c.stream().map(v -> {
            entityViewsCache.put(v.getOrigin(), v);
            return v.getOrigin();
        }).collect(Collectors.toList());
        return delegate.addAll(unwrapped);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Collection unwrapped = c.stream().map(e -> {
            BaseEntityView entityView = (BaseEntityView) e;
            Entity origin = entityView.getOrigin();
            entityViewsCache.remove(origin);
            return origin;
        }).collect(Collectors.toList());
        return delegate.removeAll(unwrapped);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Collection unwrapped = c.stream().map(e -> ((BaseEntityView) e).getOrigin()).collect(Collectors.toList());
        Set<E> keysToRemove = new HashSet<>(entityViewsCache.keySet());
        keysToRemove.removeAll(unwrapped);
        keysToRemove.forEach(e -> entityViewsCache.remove(e));
        return delegate.retainAll(unwrapped);
    }

    @Override
    public void clear() {
        entityViewsCache.clear();
        delegate.clear();
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        Collection<E> unwrapped = c.stream().map(v -> {
            entityViewsCache.put(v.getOrigin(), v);
            return v.getOrigin();
        }).collect(Collectors.toList());
        return delegate.addAll(index, unwrapped);
    }

    @Override
    public V get(int index) {
        return wrapElement(delegate.get(index));
    }

    @Override
    public V set(int index, V element) {
        return wrapElement(delegate.set(index, element.getOrigin()));
    }

    @Override
    public void add(int index, V element) {
        E origin = element.getOrigin();
        entityViewsCache.put(origin, element);
        delegate.add(index, origin);
    }

    @Override
    public V remove(int index) {
        E element = delegate.remove(index);
        V v = wrapElement(element);
        entityViewsCache.remove(element);
        return v;
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(((BaseEntityView) o).getOrigin());
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(((BaseEntityView) o).getOrigin());
    }

    @Override
    public ListIterator<V> listIterator() {
        return new WrappingListIterator(delegate.listIterator());
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        return new WrappingListIterator(delegate.listIterator(index));
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
        return new WrappingList<>(delegate.subList(fromIndex, toIndex), entityView);
    }

    private V wrapElement(E element) {
        return entityViewsCache.computeIfAbsent(element, e -> {
            log.trace("Wrapping {} and caching it", e);
            return EntityViewWrapper.wrap(e, entityView);
        });
    }


    class WrappingListIterator implements ListIterator<V> {

        private ListIterator<E> delegate;

        private E lastExtracted = null; //We need it to handle remove() properly.

        public WrappingListIterator(ListIterator<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public V next() {
            E next = delegate.next();
            lastExtracted = next;
            return wrapElement(next);
        }

        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }

        @Override
        public V previous() {
            E previous = delegate.previous();
            lastExtracted = previous;
            return wrapElement(previous);
        }

        @Override
        public int nextIndex() {
            return delegate.nextIndex();
        }

        @Override
        public int previousIndex() {
            return delegate.previousIndex();
        }

        @Override
        public void remove() {
            entityViewsCache.remove(lastExtracted);
            delegate.remove();
        }

        @Override
        public void set(V v) {
            E origin = v.getOrigin();
            entityViewsCache.put(origin, v);
            delegate.set(origin);
        }

        @Override
        public void add(V v) {
            E origin = v.getOrigin();
            entityViewsCache.put(origin, v);
            delegate.add(origin);
        }
    }

}

