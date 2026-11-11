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

import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblockrenderer.provider.SoundProvider;

import java.io.IOException;
import java.io.InputStream;

public class ResourceSoundProvider implements SoundProvider {

    private final String basePath;

    public ResourceSoundProvider() {
        this("");
    }

    public ResourceSoundProvider(final String basePath) {
        this.basePath = basePath;
    }

    @Override
    public InputStream getSoundInputStream(final Instrument instrument) throws IOException {
        final String soundPath = getInstrumentSoundPath(instrument);
        if (soundPath.startsWith("/") || soundPath.contains("..")) {
            throw new IOException("Invalid sound path: " + soundPath);
        }
        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(this.basePath + soundPath);
        if (inputStream == null) {
            throw new IOException("Resource not found: " + this.basePath + soundPath);
        }
        return inputStream;
    }

    private static String getInstrumentSoundPath(final Instrument instrument) throws IOException {
        if (instrument instanceof MinecraftInstrument minecraftInstrument) {
            return "minecraft/" + minecraftInstrument.mcSoundPath() + ".ogg";
        } else if (instrument instanceof NbsCustomInstrument customInstrument) {
            return customInstrument.getSoundFilePathOr("").replace('\\', '/');
        }
        throw new IOException("Unsupported instrument type: " + instrument.getClass().getName());
    }

}
