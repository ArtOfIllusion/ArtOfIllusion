/* Copyright (C) 2001-2003 by Peter Eastman

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
    int i, j;
    
    if (method == DISCONTINUOUS)
      {
        for (i = 0; i < value.length; i++)
          {
            v[i*2] = value[i];
            t[i*2] = time[i];
          }
        for (i = 0; i < value.length-1; i++)
          {
            v[i*2+1] = value[i].blend(value[i], 1.0, 0.0);
            t[i*2+1] = (time[i]+time[i+1])*0.5;
          }
      }
    else if (method == LINEAR || time.length < 3)
      {
        for (i = 0; i < value.length; i++)
          {
            v[i*2] = value[i];
            t[i*2] = time[i];
          }
        for (i = 0; i < value.length-1; i++)
          {
            v[i*2+1] = value[i].blend(value[i+1], 0.5, 0.5);
            t[i*2+1] = (time[i]+time[i+1])*0.5;
          }
      }
    else if (method == INTERPOLATING)
      {
        v[0] = value[0];
        t[0] = time[0];
        v[1] = calcInterpPoint(value, smoothness, 0, 0, 1, 2);
        t[1] = calcInterpTime(time, smoothness, 0, 0, 1, 2);
        for (i = 2, j = 1; i < v.length-2; i++)
          {
            if ((i&1) == 0)
              {
                v[i] = value[j];
                t[i] = time[j];
              }
            else
              {
                v[i] = calcInterpPoint(value, smoothness, j-1, j, j+1, j+2);
                t[i] = calcInterpTime(time, smoothness, j-1, j, j+1, j+2);
                j++;
              }
          }
        v[i] = calcInterpPoint(value, smoothness, j-1, j, j+1, j+1);
        t[i] = calcInterpTime(time, smoothness, j-1, j, j+1, j+1);
        v[i+1] = value[j+1];
        t[i+1] = time[j+1];
      }
    else
      {
        v[0] = value[0];
        t[0] = time[0];
        for (i = 1; i < value.length-1; i++)
          {
            v[i*2-1] = value[i].blend(value[i-1], 0.5, 0.5);
            t[i*2-1] = (time[i]+time[i-1])*0.5;
            v[i*2] = calcApproxPoint(value, smoothness, i-1, i, i+1);
            t[i*2] = calcApproxTime(time, smoothness, i-1, i, i+1);
          }
        v[i*2-1] = value[i].blend(value[i-1], 0.5, 0.5);
        t[i*2-1] = (time[i]+time[i-1])*0.5;
        v[i*2] = value[i];
        t[i*2] = time[i];
      }
    for (i = 0; i < smoothness.length-1; i++)
      {
        s[i*2] = smoothness[i].getSmoother();
        s[i*2+1] = new Smoothness();
      }
    s[i*2] = smoothness[i].getSmoother();
    Timecourse tc = new Timecourse(v, t, s);
    tc.subdivideAdaptively = subdivideAdaptively;
    return tc;
  }

  private static Keyframe calcInterpPoint(Keyframe value[], Smoothness smoothness[], int i, int j, int k, int m)
  {
    double w1, w2, w3, w4;
    
    w1 = -0.0625*smoothness[j].getRightSmoothness();
    w2 = 0.5-w1;
    w4 = -0.0625*smoothness[k].getLeftSmoothness();
    w3 = 0.5-w4;
    return value[i].blend(value[j], value[k], value[m], w1, w2, w3, w4);
  }

  private static double calcInterpTime(double time[], Smoothness smoothness[], int i, int j, int k, int m)
  {
    double w1, w2, w3, w4;
    
    w1 = -0.0625*smoothness[j].getRightSmoothness();
    w2 = 0.5-w1;
    w4 = -0.0625*smoothness[k].getLeftSmoothness();
    w3 = 0.5-w4;
    return (w1*time[i] + w2*time[j] + w3*time[k] + w4*time[m]);
  }

  private static Keyframe calcApproxPoint(Keyframe value[], Smoothness smoothness[], int i, int j, int k)
  {
    double w1 = 0.125*smoothness[j].getRightSmoothness();
    double w3 = 0.125*smoothness[j].getLeftSmoothness();
    double w2 = 1.0-w1-w3;
    
    return value[i].blend(value[j], value[k], w1, w2, w3);
  }

  private static double calcApproxTime(double time[], Smoothness smoothness[], int i, int j, int k)
  {
    double w1 = 0.125*smoothness[j].getRightSmoothness();
    double w3 = 0.125*smoothness[j].getLeftSmoothness();
    double w2 = 1.0-w1-w3;
    
    return (w1*time[i] + w2*time[j] + w3*time[k]);
  }
  
  /** Evaluate the Timecourse for a particular time, using a particular interpolation method. */
  
  public Keyframe evaluate(double t, int method)
  {
    if (time.length == 0)
      return null;
    if (t <= time[0])
      return value[0];
    if (t >= time[time.length-1])
      return value[time.length-1];
    if (method == DISCONTINUOUS)
      {
        // Return the most recent value.
        
        int i;
        for (i = 1; i < time.length && t > time[i]; i++);
        return value[i-1];
      }
    if (method == LINEAR || time.length == 2)
      {
        // Simply use linear interpolation.
        
        int i;
        for (i = 1; i < time.length && t > time[i]; i++);
        if (time[i-1] == time[i])
          return value[i];
        double fract = (t-time[i-1])/(time[i]-time[i-1]);
        return value[i-1].blend(value[i], 1.0-fract, fract);
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
    int i, ind1, ind2, ind3, ind4, ind5, ind6, last = t1.length-1;
    for (i = 1; i < t1.length && t > t1[i]; i++);
    
    // Repeatedly subdivide this local region of the curve to find its value at the
    // specified time.
    
    if (i == 1)
      ind1 = ind2 = ind3 = 0;
    else if (i == 2)
      {
        ind1 = ind2 = 0;
        ind3 = 1;
      }
    else
      {
        ind1 = i-3;
        ind2 = i-2;
        ind3 = i-1;
      }
    if (i == last)
      ind4 = ind5 = ind6 = last;
    else if (i == last-1)
      {
        ind4 = last-1;
        ind5 = ind6 = last;
      }
    else
      {
        ind4 = i;
        ind5 = i+1;
        ind6 = i+2;
      }
    v2[0] = v1[ind2];
    t2[0] = t1[ind2];
    s2[0] = s1[ind2].getSmoother();
    v2[1] = calcInterpPoint(v1, s1, ind1, ind2, ind3, ind4);
    t2[1] = calcInterpTime(t1, s1, ind1, ind2, ind3, ind4);
    s2[1] = new Smoothness();
    v2[2] = v1[ind3];
    t2[2] = t1[ind3];
    s2[2] = s1[ind3].getSmoother();
    v2[3] = calcInterpPoint(v1, s1, ind2, ind3, ind4, ind5);
    t2[3] = calcInterpTime(t1, s1, ind2, ind3, ind4, ind5);
    s2[3] = new Smoothness();
    v2[4] = v1[ind4];
    t2[4] = t1[ind4];
    s2[4] = s1[ind5].getSmoother();
    v2[5] = calcInterpPoint(v1, s1, ind3, ind4, ind5, ind6);
    t2[5] = calcInterpTime(t1, s1, ind3, ind4, ind5, ind6);
    s2[5] = new Smoothness();
    v2[6] = v1[ind5];
    t2[6] = t1[ind5];
    s2[6] = s1[ind5].getSmoother();
  }

  private void subdivideLocalApprox(double t, Keyframe v1[], double t1[], Smoothness s1[], 
      Keyframe v2[], double t2[], Smoothness s2[])
  {
    int i, ind1, ind2, ind3, ind4, ind5, ind6, last = t1.length-1;
    for (i = 1; i < t1.length && t > t1[i]; i++);
    
    // Repeatedly subdivide this local region of the curve to find its value at the
    // specified time.
    
    if (i == 1)
      ind1 = ind2 = ind3 = 0;
    else if (i == 2)
      {
        ind1 = ind2 = 0;
        ind3 = 1;
      }
    else
      {
        ind1 = i-3;
        ind2 = i-2;
        ind3 = i-1;
      }
    if (i == last)
      ind4 = ind5 = ind6 = last;
    else if (i == last-1)
      {
        ind4 = last-1;
        ind5 = ind6 = last;
      }
    else
      {
        ind4 = i;
        ind5 = i+1;
        ind6 = i+2;
      }
    v2[0] = calcApproxPoint(v1, s1, ind1, ind2, ind3);
    t2[0] = calcApproxTime(t1, s1, ind1, ind2, ind3);
    s2[0] = s1[ind2].getSmoother();
    v2[1] = v1[ind2].blend(v1[ind3], 0.5, 0.5);
    t2[1] = 0.5*(t1[ind2]+t1[ind3]);
    s2[1] = new Smoothness();
    v2[2] = calcApproxPoint(v1, s1, ind2, ind3, ind4);
    t2[2] = calcApproxTime(t1, s1, ind2, ind3, ind4);
    s2[2] = s1[ind3].getSmoother();
    v2[3] = v1[ind3].blend(v1[ind4], 0.5, 0.5);
    t2[3] = 0.5*(t1[ind3]+t1[ind4]);
    s2[3] = new Smoothness();
    v2[4] = calcApproxPoint(v1, s1, ind3, ind4, ind5);
    t2[4] = calcApproxTime(t1, s1, ind3, ind4, ind5);
    s2[4] = s1[ind4].getSmoother();
    v2[5] = v1[ind4].blend(v1[ind5], 0.5, 0.5);
    t2[5] = 0.5*(t1[ind4]+t1[ind5]);
    s2[5] = new Smoothness();
    v2[6] = calcApproxPoint(v1, s1, ind4, ind5, ind6);
    t2[6] = calcApproxTime(t1, s1, ind4, ind5, ind6);
    s2[6] = s1[ind5].getSmoother();
  }
}