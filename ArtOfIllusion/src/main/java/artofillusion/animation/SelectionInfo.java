/* This class stores information about a selected keyframe. */

/* Copyright (C) 2001 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

public class SelectionInfo
{
  public Track track;
  public Keyframe key;
  public int keyIndex;
  public boolean selected[];
  
  public SelectionInfo(Track tr, Keyframe k)
  {
    track = tr;
    key = k;
    keyIndex = -1;
    selected = new boolean [track.getValueNames().length];
    Timecourse tc = track.getTimecourse();
    if (tc == null)
      return;
    Keyframe keys[] = tc.getValues();
    int i;
    for (i = 0; keys[i] != key && i < keys.length; i++);
    if (i < keys.length)
      keyIndex = i;
    for (i = 0; i < selected.length; i++)
      selected[i] = true;
  }
}