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
import java.util.Locale;
import java.util.UUID;

public class NoteBlockStudioSoundProvider implements SoundProvider {

    private final Path basePath;

    public NoteBlockStudioSoundProvider() {
        this(getSystemBasePath());
    }

    public NoteBlockStudioSoundProvider(final String basePath) {
        this(Path.of(basePath));
    }

    public NoteBlockStudioSoundProvider(final File basePath) {
        this(basePath.toPath());
    }

    public NoteBlockStudioSoundProvider(final Path basePath) {
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

    private static Path getSystemBasePath() {
        final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.startsWith("windows")) {
            return Path.of(System.getProperty("user.home"), "Minecraft Note Block Studio", "Data", "Sounds");
        } else if (osName.startsWith("linux")) {
            return Path.of(System.getProperty("user.home"), ".config", "Minecraft_Note_Block_Studio", "data", "sounds");
        } else if (osName.startsWith("mac")) { // untested
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "Minecraft Note Block Studio", "data", "sounds");
        } else {
            return Path.of(UUID.randomUUID().toString()); // invalid path to ensure it fails
        }
    }

}
