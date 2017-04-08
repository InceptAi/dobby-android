package com.inceptai.dobby.ai;

/**
 * Created by arunesh on 4/7/17.
 */

public interface RtDataSource<T> {

    /**
     * Listener for a data item update.
     * @param <T> Object representing the data item, such as an Integer.
     */
    interface RtDataListener<T> {

        /**
         * Called when there is an update available.
         *
         * @param dataItem
         */
        void onUpdate(T dataItem);

        /**
         * Called when the real-time data stream has closed.
         */
        void onClose();
    }

    void registerListener(RtDataListener<T> listener);

    void unregisterListener(RtDataListener<T> listener);
}
