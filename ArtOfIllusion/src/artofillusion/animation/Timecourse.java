/* Copyright (C) 2001-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

/** This class represents a quantity which changes as a function of time.  It is defined by
    a series of timepoints, with a value at each one.  There is also a smoothness value for
    each timepoint, which affects how it is interpolated. */

public class Timecourse
{
  private double time[];
  private Smoothness smoothness[];
  private Keyframe value[];
  private boolean subdivideAdaptively;

  public static final int DISCONTINUOUS = 0;
  public static final int LINEAR = 1;
  public static final int INTERPOLATING = 2;
  public static final int APPROXIMATING = 3;

  private static final int FIXED_SUBDIVISION_LEVELS = 3;

  public Timecourse(Keyframe value[], double time[], Smoothness smoothness[])
  {
    this.value = value;
    this.time = time;
    this.smoothness = smoothness;
    subdivideAdaptively = true;
  }

  /** Set the timepoints defining this Timecourse. */

  public void setTimepoints(Keyframe value[], double time[], Smoothness smoothness[])
  {
    this.value = value;
    this.time = time;
    this.smoothness = smoothness;
  }

  /** Add a new timepoint to the Timecourse, and return its index in the list. */

  public int addTimepoint(Keyframe v, double t, Smoothness s)
  {
    // If this has the same time as an existing timepoint, just replace it.

    for (int i = 0; i < time.length; i++)
      if (Math.abs(time[i]-t) < 1e-10)
        {
          value[i] = v;
          time[i] = t;
          smoothness[i] = s;
          return i;
        }

    // Add the new timepoint.

    Keyframe newv[] = new Keyframe [value.length+1];
    double newt[] = new double [time.length+1];
    Smoothness news[] = new Smoothness [smoothness.length+1];
    int i, j;

    for (i = 0; i < time.length && time[i] < t; i++);
    for (j = 0; j < newv.length; j++)
      {
        if (j < i)
          {
            newv[j] = value[j];
            newt[j] = time[j];
            news[j] = smoothness[j];
          }
        else if (j == i)
          {
            newv[j] = v;
            newt[j] = t;
            news[j] = s;
          }
        else
          {
            newv[j] = value[j-1];
            newt[j] = time[j-1];
            news[j] = smoothness[j-1];
          }
      }
    value = newv;
    time = newt;
    smoothness = news;
    return i;
  }

  /** Delete the timepoint at the specified time from the Timecourse. */

  public void removeTimepoint(double t)
  {
    int i;

    for (i = 0; i < time.length && time[i] != t; i++);
    if (i < time.length)
      removeTimepoint(i);
  }

  /** Delete a timepoint from the Timecourse. */

  public void removeTimepoint(int which)
  {
    Keyframe newv[] = new Keyframe [value.length-1];
    double newt[] = new double [time.length-1];
    Smoothness news[] = new Smoothness [smoothness.length-1];

    for (int j = 0; j < newv.length; j++)
      {
        if (j < which)
          {
            newv[j] = value[j];
            newt[j] = time[j];
            news[j] = smoothness[j];
          }
        else
          {
            newv[j] = value[j+1];
            newt[j] = time[j+1];
            news[j] = smoothness[j+1];
          }
      }
    value = newv;
    time = newt;
    smoothness = news;
  }

  /** Delete all timepoints from this timecourse. */

  public void removeAllTimepoints()
  {
    value = new Keyframe [0];
    time = new double [0];
    smoothness = new Smoothness[0];
  }

  /** Move a timepoint to a different time, and return its new index in the list. */

  public int moveTimepoint(int which, double t)
  {
    Keyframe tempv;
    Smoothness temps;
    int newpos;

    for (newpos = 0; newpos < time.length && time[newpos] < t; newpos++);
    tempv = value[which];
    temps = smoothness[which];
    if (newpos > which)
      {
        newpos--;
        for (int i = which; i < newpos; i++)
          {
            value[i] = value[i+1];
            time[i] = time[i+1];
            smoothness[i] = smoothness[i+1];
          }
      }
    else
      for (int i = which; i > newpos; i--)
        {
          value[i] = value[i-1];
          time[i] = time[i-1];
          smoothness[i] = smoothness[i-1];
        }
    value[newpos] = tempv;
    time[newpos] = t;
    smoothness[newpos] = temps;
    return newpos;
  }

  /** Get the time values for this Timecourse. */

  public double [] getTimes()
  {
    return time;
  }

  /** Get the values for this Timecourse. */

  public Keyframe [] getValues()
  {
    return value;
  }

  /** Get the smoothness values for this Timecourse. */

  public Smoothness [] getSmoothness()
  {
    return smoothness;
  }

  /**
   * Get whether this timecourse should be evaluated by adaptive subdivision (to minimize the
   * amount of calculation that needs to be done) or always subdivided a fixed number of times.
   */

  public boolean getSubdivideAdaptively()
  {
    return subdivideAdaptively;
  }

  /**
   * Set whether this timecourse should be evaluated by adaptive subdivision (to minimize the
   * amount of calculation that needs to be done) or always subdivided a fixed number of times.
   */

  public void setSubdivideAdaptively(boolean adaptive)
  {
    subdivideAdaptively = adaptive;
  }

  /** Create a duplicate of this Timecourse for a (possibly different) object. */

  public Timecourse duplicate(Object owner)
  {
    double newt[] = new double [time.length];
    Smoothness news[] = new Smoothness [smoothness.length];
    Keyframe newv[] = new Keyframe [value.length];

    for (int i = 0; i < newt.length; i++)
      {
        newt[i] = time[i];
        news[i] = smoothness[i].duplicate();
        newv[i] = value[i].duplicate(owner);
      }
    Timecourse tc = new Timecourse(newv, newt, news);
    tc.subdivideAdaptively = subdivideAdaptively;
    return tc;
  }

  /** Return a subdivided version of this Timecourse. */

  public Timecourse subdivide(int method)
  {
    if (time.length < 2)
      return this;
    double t[] = new double [time.length*2-1];
    Keyframe v[] = new Keyframe [value.length*2-1];
    Smoothness s[] = new Smoothness [smoothness.length*2-1];

    if (method == DISCONTINUOUS)
      {
        for (int i = 0; i < value.length; i++)
          {
            v[i*2] = value[i];
            t[i*2] = time[i];
          }
        for (int i = 0; i < value.length-1; i++)
          {
            v[i*2+1] = value[i].blend(value[i], 1.0, 0.0);
            t[i*2+1] = (time[i]+time[i+1])*0.5;
          }
      }
    else if (method == LINEAR)
      {
        for (int i = 0; i < value.length; i++)
          {
            v[i*2] = value[i];
            t[i*2] = time[i];
          }
        for (int i = 0; i < value.length-1; i++)
          {
            v[i*2+1] = value[i].blend(value[i+1], 0.5, 0.5);
            t[i*2+1] = (time[i]+time[i+1])*0.5;
          }
      }
    else if (method == INTERPOLATING)
      {
        for (int i = 0, j = 0; i < v.length; i++)
          {
            if ((i&1) == 0)
              {
                v[i] = value[j];
                t[i] = time[j];
              }
            else
              {
                TimePoint p1 = getPoint(time, value, smoothness, j-1);
                TimePoint p2 = getPoint(time, value, smoothness, j);
                TimePoint p3 = getPoint(time, value, smoothness, j+1);
                TimePoint p4 = getPoint(time, value, smoothness, j+2);
                TimePoint interp = calcInterpPoint(p1, p2, p3, p4);
                v[i] = interp.value;
                t[i] = interp.time;
                j++;
              }
          }
      }
    else
      {
        for (int i = 0; i < value.length; i++)
          {
            if (i > 0)
            {
              v[i*2-1] = value[i].blend(value[i-1], 0.5, 0.5);
              t[i*2-1] = (time[i]+time[i-1])*0.5;
            }
            TimePoint p1 = getPoint(time, value, smoothness, i-1);
            TimePoint p2 = getPoint(time, value, smoothness, i);
            TimePoint p3 = getPoint(time, value, smoothness, i+1);
            TimePoint approx = calcApproxPoint(p1, p2, p3);
            v[i*2] = approx.value;
            t[i*2] = approx.time;
          }
      }
    for (int i = 0; i < smoothness.length-1; i++)
      {
        s[i*2] = smoothness[i].getSmoother();
        s[i*2+1] = new Smoothness();
      }
    s[(smoothness.length-1)*2] = smoothness[smoothness.length-1].getSmoother();
    Timecourse tc = new Timecourse(v, t, s);
    tc.subdivideAdaptively = subdivideAdaptively;
    return tc;
  }

  private static TimePoint calcInterpPoint(TimePoint p1, TimePoint p2, TimePoint p3, TimePoint p4)
  {
    double w1, w2, w3, w4;

    w1 = -0.0625*p2.smoothness.getRightSmoothness();
    w2 = 0.5-w1;
    w4 = -0.0625*p3.smoothness.getLeftSmoothness();
    w3 = 0.5-w4;
    return new TimePoint(0.5*p2.time + 0.5*p3.time, p1.value.blend(p2.value, p3.value, p4.value, w1, w2, w3, w4), new Smoothness());
  }

  private static TimePoint calcApproxPoint(TimePoint p1, TimePoint p2, TimePoint p3)
  {
    double w1 = 0.125*p2.smoothness.getRightSmoothness();
    double w3 = 0.125*p2.smoothness.getLeftSmoothness();
    double w2 = 1.0-w1-w3;

    return new TimePoint(w1*p1.time + w2*p2.time + w3*p3.time, p1.value.blend(p2.value, p3.value, w1, w2, w3), p2.smoothness.getSmoother());
  }

  private static TimePoint getPoint(double time[], Keyframe value[], Smoothness smoothness[], int index)
  {
    if (index >= 0 && index < time.length)
      return new TimePoint(time[index], value[index], smoothness[index]);
    if (index == -1)
      return new TimePoint(time[0]-(time[1]-time[0]), value[1], smoothness[1]);
    if (index == -2)
    {
      if (time.length > 2)
        return new TimePoint(time[0]-(time[2]-time[0]), value[2], smoothness[2]);
      return new TimePoint(time[0]-2*(time[1]-time[0]), value[0], smoothness[0]);
    }
    int last = time.length-1;
    if (index == time.length)
      return new TimePoint(time[last]+(time[last]-time[last-1]), value[last-1], smoothness[last-1]);
    if (index == time.length+1)
    {
      if (time.length > 2)
        return new TimePoint(time[last]+(time[last]-time[last-2]), value[last-2], smoothness[last-2]);
      return new TimePoint(time[last]-2*(time[last]-time[last-1]), value[last], smoothness[last]);
    }
    return null; // This should never happen.
  }

  /** Evaluate the Timecourse for a particular time, using a particular interpolation method. */

  public Keyframe evaluate(double t, int method)
  {
    if (time.length == 0)
      return null;
    if (time.length == 1)
    return value[0];
    if (t <= time[0])
      t = time[0];
    if (t >= time[time.length-1])
      t = time[time.length-1];
    if (method == DISCONTINUOUS)
      {
        // Return the most recent value.

        int i;
        for (i = 1; i < time.length && t > time[i]; i++);
        return value[i-1];
      }
    if (method == LINEAR)
      {
        // Simply use linear interpolation.

        int i;
        for (i = 1; i < time.length && t > time[i]; i++);
        if (time[i-1] == time[i])
          return value[i];
        double fract = (t-time[i-1])/(time[i]-time[i-1]);
        return value[i-1].blend(value[i], 1.0-fract, fract);
      }
    if (time.length < 7)
    {
      // Subdivide the entire timecourse until it has enough points to use the local subdivision method.

      return subdivide(method).evaluate(t, method);
    }
    Keyframe v1[] = new Keyframe [7], v2[] = new Keyframe [7];
    double t1[] = new double [7], t2[] = new double [7];
    Smoothness s1[] = new Smoothness [7], s2[] = new Smoothness [7];

    // Subdivide the local region of the curve to get a point within one frame of the desired time.

    if (method == INTERPOLATING)
      subdivideLocalInterp(t, value, time, smoothness, v1, t1, s1);
    else
      subdivideLocalApprox(t, value, time, smoothness, v1, t1, s1);
    int numSubdivisions = 1;
    int minSubdivisions = (method == INTERPOLATING ? 1 : 3);
    if (!subdivideAdaptively)
      minSubdivisions = FIXED_SUBDIVISION_LEVELS;
    while (true)
    {
      if (numSubdivisions >= minSubdivisions)
      {
        // Find the nearest point.

        int i;
        for (i = 1; i < t1.length && t > t1[i]; i++);
        if (t1[i-1] == t1[i])
          return v1[i];
        double delta = t-t1[i-1];
        if (delta < 1.0/30.0 || !subdivideAdaptively)
        {
          // Use linear interpolation to get the final value.

          double fract = delta/(t1[i]-t1[i-1]);
          return v1[i-1].blend(v1[i], 1.0-fract, fract);
        }
      }
      if (method == INTERPOLATING)
      {
        subdivideLocalInterp(t, v1, t1, s1, v2, t2, s2);
        subdivideLocalInterp(t, v2, t2, s2, v1, t1, s1);
      }
      else
      {
        subdivideLocalApprox(t, v1, t1, s1, v2, t2, s2);
        subdivideLocalApprox(t, v2, t2, s2, v1, t1, s1);
      }
      numSubdivisions += 2;
    }
  }

  /** The following two methods are called by the evaluate() method.  Given a value of
      time (t) and a set of timepoints (v1, t1, s1), they return a new set of
      timepoints (v2, t2, s2) found by subdividing the initial timepoints around t. */

  private void subdivideLocalInterp(double t, Keyframe v1[], double t1[], Smoothness s1[],
      Keyframe v2[], double t2[], Smoothness s2[])
  {
    int i;
    for (i = 1; i < t1.length && t > t1[i]; i++);

    // Repeatedly subdivide this local region of the curve to find its value at the
    // specified time.

    TimePoint p1 = getPoint(t1, v1, s1, i-3);
    TimePoint p2 = getPoint(t1, v1, s1, i-2);
    TimePoint p3 = getPoint(t1, v1, s1, i-1);
    TimePoint p4 = getPoint(t1, v1, s1, i);
    TimePoint p5 = getPoint(t1, v1, s1, i+1);
    TimePoint p6 = getPoint(t1, v1, s1, i+2);
    v2[0] = p2.value;
    t2[0] = p2.time;
    s2[0] = p2.smoothness.getSmoother();
    TimePoint interp = calcInterpPoint(p1, p2, p3, p4);
    v2[1] = interp.value;
    t2[1] = interp.time;
    s2[1] = interp.smoothness;
    v2[2] = p3.value;
    t2[2] = p3.time;
    s2[2] = p3.smoothness.getSmoother();
    interp = calcInterpPoint(p2, p3, p4, p5);
    v2[3] = interp.value;
    t2[3] = interp.time;
    s2[3] = interp.smoothness;
    v2[4] = p4.value;
    t2[4] = p4.time;
    s2[4] = p4.smoothness.getSmoother();
    interp = calcInterpPoint(p3, p4, p5, p6);
    v2[5] = interp.value;
    t2[5] = interp.time;
    s2[5] = interp.smoothness;
    v2[6] = p5.value;
    t2[6] = p5.time;
    s2[6] = p5.smoothness.getSmoother();
  }

  private void subdivideLocalApprox(double t, Keyframe v1[], double t1[], Smoothness s1[],
      Keyframe v2[], double t2[], Smoothness s2[])
  {
    int i;
    for (i = 1; i < t1.length && t > t1[i]; i++);

    // Repeatedly subdivide this local region of the curve to find its value at the
    // specified time.

    TimePoint p1 = getPoint(t1, v1, s1, i-3);
    TimePoint p2 = getPoint(t1, v1, s1, i-2);
    TimePoint p3 = getPoint(t1, v1, s1, i-1);
    TimePoint p4 = getPoint(t1, v1, s1, i);
    TimePoint p5 = getPoint(t1, v1, s1, i+1);
    TimePoint p6 = getPoint(t1, v1, s1, i+2);
    TimePoint approx = calcApproxPoint(p1, p2, p3);
    v2[0] = approx.value;
    t2[0] = approx.time;
    s2[0] = approx.smoothness;
    v2[1] = p2.value.blend(p3.value, 0.5, 0.5);
    t2[1] = 0.5*(p2.time+p3.time);
    s2[1] = new Smoothness();
    approx = calcApproxPoint(p2, p3, p4);
    v2[2] = approx.value;
    t2[2] = approx.time;
    s2[2] = approx.smoothness;
    v2[3] = p3.value.blend(p4.value, 0.5, 0.5);
    t2[3] = 0.5*(p3.time+p4.time);
    s2[3] = new Smoothness();
    approx = calcApproxPoint(p3, p4, p5);
    v2[4] = approx.value;
    t2[4] = approx.time;
    s2[4] = approx.smoothness;
    v2[5] = p4.value.blend(p5.value, 0.5, 0.5);
    t2[5] = 0.5*(p4.time+p5.time);
    s2[5] = new Smoothness();
    approx = calcApproxPoint(p4, p5, p6);
    v2[6] = approx.value;
    t2[6] = approx.time;
    s2[6] = approx.smoothness;
  }

  private static class TimePoint
  {
    public double time;
    public Keyframe value;
    public Smoothness smoothness;

    public TimePoint(double time, Keyframe value, Smoothness smoothness)
    {
      this.time = time;
      this.value = value;
      this.smoothness = smoothness;
    }
  }
}