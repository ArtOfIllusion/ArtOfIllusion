/* Copyright (C) 2007 by Peter Eastman
   Changes copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.util;

import static org.junit.Assert.*;
import org.junit.Test;


import java.util.concurrent.atomic.*;



public class TestThreadManager
{
  @Test
  public void testRun()
  {
    final AtomicBoolean flags[] = new AtomicBoolean[1000];
    for (int i = 0; i < flags.length; i++)
      flags[i] = new AtomicBoolean();
    final AtomicBoolean error = new AtomicBoolean();
    ThreadManager tm = new ThreadManager(flags.length, new artofillusion.util.ThreadManager.Task()
    {
      @Override
      public void execute(int index)
      {
        if (flags[index].get())
          error.set(true);
        flags[index].set(true);
      }
      @Override
      public void cleanup()
      {
      }
    });
    for (int repeat = 0; repeat < 50; repeat++)
    {
      for (int i = 0; i < flags.length; i++)
        flags[i].set(false);
      error.set(false);
      tm.run();
      assertFalse(error.get());
      for (int i = 0; i < flags.length; i++)
        assertTrue(flags[i].get());
    }
  }

  @Test
  public void testCancel()
  {
    final AtomicBoolean canceled = new AtomicBoolean();
    final AtomicInteger errorCount = new AtomicInteger();
    final ThreadManager tm = new ThreadManager();
    tm.setNumIndices(1000);
    tm.setTask(new ThreadManager.Task()
    {
      @Override
      public void execute(int index)
      {
        if (canceled.get())
          errorCount.incrementAndGet();
        if (index == 500)
        {
          tm.cancel();
          canceled.set(true);
        }
      }
      @Override
      public void cleanup()
      {
      }
    });
    for (int repeat = 0; repeat < 50; repeat++)
    {
      canceled.set(false);
      errorCount.set(0);
      tm.run();
      assertTrue(errorCount.get() < Runtime.getRuntime().availableProcessors());
    }
  }
}
