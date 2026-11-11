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
package net.raphimc.noteblockrenderer.provider.impl;

import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblockrenderer.provider.SoundProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MapSoundProvider implements SoundProvider {

    private final Map<Instrument, byte[]> sounds;

    public MapSoundProvider(final Map<Instrument, byte[]> sounds) {
        this.sounds = new HashMap<>(sounds);
    }

    @Override
    public InputStream getSoundInputStream(final Instrument instrument) throws IOException {
        if (this.sounds.containsKey(instrument)) {
            return new ByteArrayInputStream(this.sounds.get(instrument));
        } else {
            throw new IOException("Missing sound sample");
        }
    }

}
