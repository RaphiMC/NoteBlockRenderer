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
package net.raphimc.noteblockrenderer.util;

import net.raphimc.audiomixer.io.mp3.Mp3AudioOutputStream;
import net.raphimc.noteblocklib.model.song.Song;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MetadataUtil {

    private static final String RENDERER = "NoteBlockRenderer";

    private MetadataUtil() {
    }

    public static Mp3AudioOutputStream.Id3Tags getMp3Tags(final Song song) {
        return new Mp3AudioOutputStream.Id3Tags()
            .withTitle(song.getTitle())
            .withArtist(song.getAuthor())
            .withComment(song.getDescription())
            .withCustomField("Renderer", RENDERER);
    }

    public static Map<String, List<String>> getVorbisTags(final Song song) {
        final Map<String, List<String>> tags = new LinkedHashMap<>();
        if (song.getTitle() != null) {
            tags.put("TITLE", List.of(song.getTitle()));
        }
        if (song.getAuthor() != null) {
            tags.put("ARTIST", List.of(song.getAuthor()));
        }
        if (song.getDescription() != null) {
            tags.put("DESCRIPTION", List.of(song.getDescription()));
        }
        tags.put("Renderer", List.of(RENDERER));
        return tags;
    }

}
