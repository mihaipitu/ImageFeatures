/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.util.LinkedList;

/**
 *
 * @author Mihai Pîțu
 */
public class ThreadPool extends ThreadGroup {

    private boolean alive;
    private LinkedList taskQueue;
    private int threadID;
    private static int threadPoolID;
    private ExceptionHandler exceptionHandler;

    public ThreadPool(int numThreads, ExceptionHandler exceptionHandler) {
        super("ThreadPool-" + (threadPoolID++));
        
        this.exceptionHandler = exceptionHandler;
        
        setDaemon(true);

        alive = true;

        taskQueue = new LinkedList();
        for (int i = 0; i < numThreads; i++) {
            new PThread().start();
        }
    }

    public synchronized void addTask(Runnable task) {
        if (!alive) {
            throw new IllegalStateException();
        }
        if (task != null) {
            taskQueue.add(task);
            notify();
        }
    }

    protected synchronized Runnable getTask() throws InterruptedException {
        while (taskQueue.size() == 0) {
            if (!alive) {
                return null;
            }
            wait();
        }
        return (Runnable) taskQueue.removeFirst();
    }


    public synchronized void close() {
        if (alive) {
            alive = false;
            taskQueue.clear();
            interrupt();
        }
    }

    public void join() {
        synchronized (this) {
            alive = false;
            notifyAll();
        }

        // wait for all threads to finish
        Thread[] threads = new Thread[activeCount()];
        int count = enumerate(threads);
        for (int i = 0; i < count; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ex) {
            }
        }
    }


    private class PThread extends Thread {

        public PThread() {
            super(ThreadPool.this, "PooledThread-" + (threadID++));
        }

        @Override
        public void run() {
            while (!isInterrupted()) {

                // get a task to run
                Runnable task = null;
                try {
                    task = getTask();
                } catch (InterruptedException ex) {
                }

                if (task == null) {
                    return;
                    //continue;
                }

                try {
                    task.run();
                } catch (Exception t) {
                   exceptionHandler.handle(t);
                }
            }
        }
    }
}