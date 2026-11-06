/*
 * This file is part of NoteBlockRenderer - https://github.com/RaphiMC/NoteBlockRenderer
 * Copyright (C) 2024-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblockrenderer.provider;

import net.raphimc.audiomixer.io.AudioIo;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.noteblocklib.model.note.Instrument;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface SoundProvider {

    InputStream getSoundInputStream(final Instrument instrument) throws IOException;

    default AudioBuffer getSoundBuffer(final Instrument instrument) throws IOException {
        try (InputStream inputStream = this.getSoundInputStream(instrument)) {
            return AudioIo.read(inputStream);
        }
    }

    default SoundProvider then(final SoundProvider next) {
        return instrument -> {
            try {
                return this.getSoundInputStream(instrument);
            } catch (final IOException outer) {
                try {
                    return next.getSoundInputStream(instrument);
                } catch (final IOException inner) {
                    inner.addSuppressed(outer);
                    throw inner;
                }
            }
        };
    }

}
