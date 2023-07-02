/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * IOUtils: A collection of IO-related public static methods.
 */

package org.inksnow.asteroid.internal;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AkIOUtils {

  private static final int DEFAULT_BUFFER_SIZE = 8192;

  private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

  public static byte[] readExactlyNBytes(InputStream is, int length)
      throws IOException {
    if (length < 0) {
      throw new IOException("length cannot be negative: " + length);
    }
    byte[] data = readNBytes(is, length);
    if (data.length < length) {
      throw new EOFException();
    }
    return data;
  }

  public static byte[] readAllBytes(InputStream is) throws IOException {
    return readNBytes(is, Integer.MAX_VALUE);
  }

  public static byte[] readNBytes(InputStream is, int len) throws IOException {
    if (len < 0) {
      throw new IllegalArgumentException("len < 0");
    }

    List<byte[]> bufs = null;
    byte[] result = null;
    int total = 0;
    int remaining = len;
    int n;
    do {
      byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
      int nread = 0;

      // read to EOF which may read more or less than buffer size
      while ((n = is.read(buf, nread,
          Math.min(buf.length - nread, remaining))) > 0) {
        nread += n;
        remaining -= n;
      }

      if (nread > 0) {
        if (MAX_BUFFER_SIZE - total < nread) {
          throw new OutOfMemoryError("Required array size too large");
        }
        total += nread;
        if (result == null) {
          result = buf;
        } else {
          if (bufs == null) {
            bufs = new ArrayList<>();
            bufs.add(result);
          }
          bufs.add(buf);
        }
      }
      // if the last call to read returned -1 or the number of bytes
      // requested have been read then break
    } while (n >= 0 && remaining > 0);

    if (bufs == null) {
      if (result == null) {
        return new byte[0];
      }
      return result.length == total ?
          result : Arrays.copyOf(result, total);
    }

    result = new byte[total];
    int offset = 0;
    remaining = total;
    for (byte[] b : bufs) {
      int count = Math.min(b.length, remaining);
      System.arraycopy(b, 0, result, offset, count);
      offset += count;
      remaining -= count;
    }

    return result;
  }

  public static int readNBytes(InputStream is, byte[] b, int off, int len) throws IOException {
    Objects.requireNonNull(b);
    if (off < 0 || len < 0 || len > b.length - off)
      throw new IndexOutOfBoundsException();
    int n = 0;
    while (n < len) {
      int count = is.read(b, off + n, len - n);
      if (count < 0)
        break;
      n += count;
    }
    return n;
  }

  public static byte[] readFully(InputStream is, int length, boolean readAll)
      throws IOException {
    if (length < 0) {
      throw new IOException("length cannot be negative: " + length);
    }
    if (readAll) {
      return readExactlyNBytes(is, length);
    } else {
      return readNBytes(is, length);
    }
  }
}
