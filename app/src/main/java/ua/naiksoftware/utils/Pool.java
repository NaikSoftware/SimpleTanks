package ua.naiksoftware.utils;

import java.util.ArrayList;

/**
 * Created by Naik on 09.07.15.
 */
public class Pool<T extends Pool.Entry> {

    private ArrayList<T> objects;
    private ObjectFactory<T> factory;
    private int size;

    public Pool(int size, ObjectFactory<T> factory) {
        this.size = size;
        this.factory = factory;
        objects = new ArrayList<T>(size);
    }

    public T obtain() {
        T entry;
        int listSize = objects.size();
        for (int i = 0; i < listSize; i++) {
            entry = objects.get(i);
            if (entry.released()) {
                return entry; /* Типы не совпадают */
            }
        }
        if (listSize < size) {
            entry = factory.create();
            objects.add(entry);/* Типы не совпадают */
            return entry;      /* Типы не совпадают */
        } else {
            return factory.create();
        }
    }

    public void recycle() {
        objects.clear();
    }

    public interface Entry {
        boolean released();
        void release();
    }

    public interface ObjectFactory <T extends Entry> {
        T create();
    }
}
