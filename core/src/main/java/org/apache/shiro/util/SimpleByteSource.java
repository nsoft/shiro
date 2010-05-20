/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shiro.util;

import org.apache.shiro.codec.Base64;
import org.apache.shiro.codec.Hex;

import java.util.Arrays;

/**
 * Very simple {@link ByteSource ByteSource} implementation that maintains an internal {@code byte[]} array and uses the
 * {@link Hex Hex} and {@link Base64 Base64} codec classes to support the
 * {@link #toHex() toHex()} and {@link #toBase64() toBase64()} implementations.
 *
 * @author Les Hazlewood
 * @since 1.0
 */
public class SimpleByteSource implements ByteSource {

    private final byte[] bytes;

    public SimpleByteSource(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public String toHex() {
        return Hex.encodeToString(getBytes());
    }

    public String toBase64() {
        return Base64.encodeToString(getBytes());
    }

    public String toString() {
        return toBase64();
    }

    public int hashCode() {
        return toBase64().hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof SimpleByteSource) {
            SimpleByteSource bs = (SimpleByteSource) o;
            return Arrays.equals(getBytes(), bs.getBytes());
        }
        return false;
    }
}