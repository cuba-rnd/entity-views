package com.haulmont.cuba.core.views.factory;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.views.BaseEntityView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;


public class WrappingList<E extends Entity, V extends BaseEntityView<E>> implements List<V> {

    private List<E> delegate;

    private Class<V> entityView;

    public WrappingList(List<E> delegate, Class<V> entityView) {
        this.delegate = delegate;
        this.entityView = entityView;
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
        return delegate.contains(((V)o).getOrigin());
    }

    @Override
    public Iterator<V> iterator() {
        return new WrappingListIterator(delegate.listIterator());
    }

    @Override
    public Object[] toArray() {
        return delegate.stream().map(e -> EntityViewWrapper.wrap(e, entityView)).toArray();
    }

    @Override
    public <V> V[] toArray(V[] a) {
        Object[] objects = delegate.toArray(new Object[a.length]);
        List collect = Arrays.stream(objects).map(e -> EntityViewWrapper.wrap((E) e, entityView)).collect(Collectors.toList());
        return (V[])collect.toArray(a);
    }

    @Override
    public boolean add(V v) {
        return delegate.add(v.getOrigin());
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(((V)o).getOrigin());
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Collection unwrapped = c.stream().map(e -> ((BaseEntityView) e).getOrigin()).collect(Collectors.toList());
        return delegate.containsAll(unwrapped);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        Collection<E> unwrapped = c.stream().map(BaseEntityView::getOrigin).collect(Collectors.toList());
        return delegate.addAll(unwrapped);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Collection unwrapped = c.stream().map(e -> ((BaseEntityView) e).getOrigin()).collect(Collectors.toList());
        return delegate.removeAll(unwrapped);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Collection unwrapped = c.stream().map(e -> ((BaseEntityView) e).getOrigin()).collect(Collectors.toList());
        return delegate.retainAll(unwrapped);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        Collection<E> unwrapped = c.stream().map(BaseEntityView::getOrigin).collect(Collectors.toList());
        return delegate.addAll(index, unwrapped);
    }

    @Override
    public V get(int index) {
        return EntityViewWrapper.wrap(delegate.get(index), entityView);
    }

    @Override
    public V set(int index, V element) {
        return EntityViewWrapper.wrap(delegate.set(index, element.getOrigin()), entityView);
    }

    @Override
    public void add(int index, V element) {
        delegate.set(index, element.getOrigin());
    }

    @Override
    public V remove(int index) {
        return EntityViewWrapper.wrap(delegate.remove(index), entityView);
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(((BaseEntityView)o).getOrigin());
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(((BaseEntityView)o).getOrigin());
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

    class WrappingListIterator implements ListIterator<V> {

        private ListIterator<E> delegate;

        public WrappingListIterator(ListIterator<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public V next() {
            return EntityViewWrapper.wrap(delegate.next(), entityView);
        }

        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }

        @Override
        public V previous() {
            return EntityViewWrapper.wrap(delegate.previous(), entityView);
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
            delegate.remove();
        }

        @Override
        public void set(V v) {
            delegate.set(v.getOrigin());
        }

        @Override
        public void add(V v) {
            delegate.add(v.getOrigin());
        }
    }

}

