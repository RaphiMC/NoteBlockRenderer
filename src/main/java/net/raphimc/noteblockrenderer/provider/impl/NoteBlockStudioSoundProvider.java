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

import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

public class NoteBlockStudioSoundProvider extends DirectorySoundProvider {

    public NoteBlockStudioSoundProvider() {
        super(getBasePath());
    }

    private static Path getBasePath() {
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
