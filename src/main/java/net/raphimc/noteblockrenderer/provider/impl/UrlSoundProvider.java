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
import java.net.URI;
import java.net.URLConnection;

public class UrlSoundProvider implements SoundProvider {

    private static final String DEFAULT_BASE_URL = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/refs/heads/26.3/assets/minecraft/sounds/";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final URI baseUri;

    public UrlSoundProvider() {
        this(URI.create(DEFAULT_BASE_URL));
    }

    public UrlSoundProvider(final URI baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public InputStream getSoundInputStream(final Instrument instrument) throws IOException {
        final String soundPath = getInstrumentSoundPath(instrument);
        if (soundPath.startsWith("/") || soundPath.contains("..") || soundPath.contains(":") || soundPath.contains("%")) {
            throw new IOException("Invalid sound path: " + soundPath);
        }
        final URLConnection connection = this.baseUri.resolve(soundPath).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        return connection.getInputStream();
    }

    private static String getInstrumentSoundPath(final Instrument instrument) throws IOException {
        if (instrument instanceof MinecraftInstrument minecraftInstrument) {
            return minecraftInstrument.mcSoundPath() + ".ogg";
        } else if (instrument instanceof NbsCustomInstrument customInstrument) {
            final String normalizedPath = customInstrument.getSoundFilePathOr("").replace('\\', '/');
            if (normalizedPath.startsWith("minecraft/")) {
                return normalizedPath.substring(10);
            }
        }
        throw new IOException("Unsupported instrument type: " + instrument.getClass().getName());
    }

}
