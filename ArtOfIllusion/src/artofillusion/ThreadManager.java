/* Copyright (C) 2005-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

/**
 * This class coordinates threads for multi-threaded operations.  The execution model
 * provided by this class is a single "task" (e.g. tracing a ray through a single pixel)
 * which must be executed many times.  The task is parameterized by a single index
 * (e.g. the column containing the pixel).
 * <p>
 * To use this class, pass it an object which implements the Task interface.  It
 * automatically creates an appropriate number of threads based on the number of
 * available processors.  When you call run(), the task is repeatedly executed by
 * the worker threads, with the index running
 * over the desired range.  You may invoke run() any number of times (e.g. once
 * for each row of the image).  Finally, call finish() to clean up the worker threads.
 */

public class ThreadManager
{
  private int numIndices, nextIndex, numWaiting;
  private Thread thread[];
  private Task task;
  private Object controller;
  private boolean controllerWaiting;

  /**
   * Create a new ThreadManager.
   *
   * @param numIndices      the number of values the index should take on (from 0 to numIndices-1)
   * @param task            the task to perform
   */

  public ThreadManager(int numIndices, final Task task)
  {
    this.numIndices = numIndices;
    this.task = task;
    nextIndex = numIndices;
    controller = new Object();
    controllerWaiting = true;
    thread = new Thread [Runtime.getRuntime().availableProcessors()];
    if (thread.length > 1)
    {
      // Create a worker thread for each processor.

      for (int i = 0; i < thread.length; i++)
      {
        thread[i] = new Thread("Worker thread "+(i+1)) {
          public void run()
          {
            // Repeatedly perform the task until we are finished.

            while (true)
            {
              try
              {
                task.execute(nextIndex());
              }
              catch (InterruptedException ex)
              {
                task.cleanup();
                return;
              }
            }
          }
        };
        thread[i].start();
      }
    }
  }

  /**
   * Perform the task the specified number of times.  This method blocks until all
   * occurrences of the task are completed.  If the current thread is interrupted
   * while this method is in progress, all of the worker threads will be interrupted
   * and disposed of.
   */

  public void run()
  {
    if (thread.length == 1)
    {
      // There is only one processor, so just invoke the task directly.

      for (int i = 0; i < numIndices; i++)
        task.execute(i);
      return;
    }

    // Notify all the worker threads, then wait for them to finish.

    synchronized (this)
    {
      controllerWaiting = false;
      nextIndex = 0;
      numWaiting = 0;
      notifyAll();
    }
    synchronized (controller)
    {
      try
      {
        controllerWaiting = true;
        controller.wait();
      }
      catch (InterruptedException ex)
      {
        finish();
      }
    }
  }

  /**
   * Dispose of all the worker threads.  Once this has been called, do not call run() again.
   */

  public void finish()
  {
    if (thread.length > 1)
      for (int i = 0; i < thread.length; i++)
        thread[i].interrupt();
    else
      task.cleanup();
  }

  private synchronized int nextIndex() throws InterruptedException
  {
    while (nextIndex == numIndices)
    {
      // Wait until run() is called again.
      
      numWaiting++;
      if (numWaiting == thread.length)
      {
        while (!controllerWaiting)
          Thread.sleep(1);
        synchronized (controller)
        {
          controller.notify();
        }
      }
      wait();
    }
    return (nextIndex++);
  }

  /**
   * This interface defines a task to be performed by the worker threads.
   */

  public static interface Task
  {
    /**
     * Execute the task for the specified index.
     */

    public void execute(int index);

    /**
     * This is called once from each worker thread when finish() is called.  It gives a chance
     * to do any necessary cleanup.
     */

    public void cleanup();
  }
}
