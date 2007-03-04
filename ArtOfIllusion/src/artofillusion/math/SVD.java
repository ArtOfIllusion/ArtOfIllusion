/* Copyright (C) 1999-2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import Jama.*;

/** The SVD class defines methods for solving sets of linear equations by singular value
    decomposition.  It uses classes from the Java Matrix (JAMA) package to factor the 
    matrix.  The complete JAMA package, including documentation and source, can be obtained
    from http://math.nist.gov/javanumerics/jama/ */

public class SVD
{
  /** Solve a set of M linear equations in N unknows.  The parameters are:
  
      a: an array of size [M][N] containing the matrix of coefficients.
      b: an array of length max(M,N) containing the right hand side vector in its first M
         elements.  On exit, the first N elements are overwritten with the solution vector.
      tol: any singular values smaller than tol*(largest singular value) are set to 0.
         If tol is omitted, it defaults to 1.0e-8. */
  
  public static void solve(double a[][], double b[])
  {
    solve(a, b, 1.0e-8);
  }

  public static void solve(double a[][], double b[], double tol)
  {
    int i, j, m, n;
    double d, cutoff, s[], temp[], u[][], v[][];
    Matrix mat;
    SingularValueDecomposition svd;

    m = a.length;
    n = a[0].length;
    mat = new Matrix(a, m, n);
    
    // Factor the matrix.
    
    svd = mat.svd();
    u = svd.getU().getArray();
    v = svd.getV().getArray();
    s = svd.getSingularValues();
    cutoff = s[0]*tol;
    temp = new double [n];

    // Do the back substitution to find the solution vector.
    
    for (i = 0; i < n && s[i] > cutoff; i++)
      {
	d = 0.0;
	for (j = 0; j < m; j++)
	  d += u[j][i]*b[j];
	temp[i] = d/s[i];
      }
    for (i = 0; i < n; i++)
      {
	d = 0.0;
	for (j = 0; j < n; j++)
	  d += v[i][j]*temp[j];
	b[i] = d;
      }
  }
}