/*
 * This file is part of NoteBlockRenderer - https://github.com/RaphiMC/NoteBlockRenderer
 * Copyright (C) 2026-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblockrenderer.teavm.classlib.java.security;

import java.util.Objects;

public abstract class MessageDigest {

    public static MessageDigest getInstance(final String algorithm) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (algorithm.equals("MD5")) {
            return new Md5MessageDigest();
        } else {
            throw new NoSuchAlgorithmException(algorithm + " MessageDigest not available");
        }
    }

    public void update(final byte[] input, final int offset, final int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        this.engineUpdate(input, offset, len);
    }

    protected abstract void engineUpdate(final byte[] input, final int offset, final int len);

    public abstract byte[] digest();

    public abstract void reset();

    private static class Md5MessageDigest extends MessageDigest {

        private static final int[] SHIFTS = {7, 12, 17, 22, 5, 9, 14, 20, 4, 11, 16, 23, 6, 10, 15, 21};
        private static final int[] CONSTANTS = {
            0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
            0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
            0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
            0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
            0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
            0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
            0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
            0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
            0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
            0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
            0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
            0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
            0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
            0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
            0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
            0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391
        };

        private final byte[] buffer = new byte[64];
        private final int[] words = new int[16];
        private final int[] state = new int[4];
        private int bufferLength;
        private long byteCount;

        private Md5MessageDigest() {
            this.reset();
        }

        @Override
        protected void engineUpdate(final byte[] input, int offset, int len) {
            Objects.checkFromIndexSize(offset, len, input.length);
            this.byteCount += len;
            while (len > 0) {
                final int count = Math.min(len, this.buffer.length - this.bufferLength);
                System.arraycopy(input, offset, this.buffer, this.bufferLength, count);
                this.bufferLength += count;
                offset += count;
                len -= count;
                if (this.bufferLength == this.buffer.length) {
                    this.processBlock();
                    this.bufferLength = 0;
                }
            }
        }

        @Override
        public byte[] digest() {
            final long bitLength = this.byteCount << 3;
            this.buffer[this.bufferLength++] = (byte) 0x80;
            if (this.bufferLength > 56) {
                while (this.bufferLength < 64) {
                    this.buffer[this.bufferLength++] = 0;
                }
                this.processBlock();
                this.bufferLength = 0;
            }
            while (this.bufferLength < 56) {
                this.buffer[this.bufferLength++] = 0;
            }
            for (int i = 0; i < 8; i++) {
                this.buffer[56 + i] = (byte) (bitLength >>> (i * 8));
            }
            this.processBlock();

            final byte[] result = new byte[16];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) (this.state[i / 4] >>> ((i & 3) * 8));
            }
            this.reset();
            return result;
        }

        @Override
        public void reset() {
            this.state[0] = 0x67452301;
            this.state[1] = 0xefcdab89;
            this.state[2] = 0x98badcfe;
            this.state[3] = 0x10325476;
            this.bufferLength = 0;
            this.byteCount = 0;
        }

        private void processBlock() {
            for (int i = 0; i < this.words.length; i++) {
                final int offset = i * 4;
                this.words[i] = (this.buffer[offset] & 0xff)
                    | ((this.buffer[offset + 1] & 0xff) << 8)
                    | ((this.buffer[offset + 2] & 0xff) << 16)
                    | ((this.buffer[offset + 3] & 0xff) << 24);
            }

            int a = this.state[0];
            int b = this.state[1];
            int c = this.state[2];
            int d = this.state[3];
            for (int i = 0; i < 64; i++) {
                final int f;
                final int g;
                if (i < 16) {
                    f = (b & c) | (~b & d);
                    g = i;
                } else if (i < 32) {
                    f = (d & b) | (~d & c);
                    g = (5 * i + 1) & 15;
                } else if (i < 48) {
                    f = b ^ c ^ d;
                    g = (3 * i + 5) & 15;
                } else {
                    f = c ^ (b | ~d);
                    g = (7 * i) & 15;
                }
                final int next = b + Integer.rotateLeft(a + f + CONSTANTS[i] + this.words[g], SHIFTS[(i / 16) * 4 + (i & 3)]);
                a = d;
                d = c;
                c = b;
                b = next;
            }
            this.state[0] += a;
            this.state[1] += b;
            this.state[2] += c;
            this.state[3] += d;
        }

    }

}
