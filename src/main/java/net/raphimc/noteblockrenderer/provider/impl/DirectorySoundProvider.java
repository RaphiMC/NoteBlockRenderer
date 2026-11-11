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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectorySoundProvider implements SoundProvider {

    private final Path basePath;

    public DirectorySoundProvider(final String basePath) {
        this(Path.of(basePath));
    }

    public DirectorySoundProvider(final File basePath) {
        this(basePath.toPath());
    }

    public DirectorySoundProvider(final Path basePath) {
        this.basePath = basePath.toAbsolutePath().normalize();
    }

    @Override
    public InputStream getSoundInputStream(final Instrument instrument) throws IOException {
        final Path soundFile = this.basePath.resolve(getInstrumentSoundPath(instrument)).normalize();
        if (!soundFile.startsWith(this.basePath)) {
            throw new IOException("Sound path is outside of base path: " + soundFile);
        }
        if (!Files.isRegularFile(soundFile)) {
            throw new IOException("Failed to find sound file at path: " + soundFile);
        }
        return Files.newInputStream(soundFile);
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
