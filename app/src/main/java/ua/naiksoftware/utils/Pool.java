package ua.naiksoftware.utils;

import java.util.ArrayList;

/**
 * Пул обьектов. Может использоваться для повышения производительности там, где создается много
 * схожих обьектов в цикле.
 *
 * Created by Naik on 09.07.15.
 */
public class Pool<T extends Pool.Entry> {

    private ArrayList<T> objects;
    private ObjectFactory<T> factory;
    private int size;

    /**
     *
     * @param size максимальный размер пула, когда превышен лимит, обьекты будут просто
     *             созданы с помощью фабрики {@code Pool#ObjectFactory}
     * @param factory фабрика для создания новых элементов пула
     */
    public Pool(int size, ObjectFactory<T> factory) {
        this.size = size;
        this.factory = factory;
        objects = new ArrayList<T>(size);
    }

    /**
     * Получить новый обьект.
     * @return освобожденный обьект из пула, или если пул забит, то будет созден новый обьект
     */
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

    /**
     * Элемент пула обьектов должен реализовать этот интерфейс
     */
    public interface Entry {
        boolean released();
        void release();
    }

    public interface ObjectFactory <T extends Entry> {
        T create();
    }
}
