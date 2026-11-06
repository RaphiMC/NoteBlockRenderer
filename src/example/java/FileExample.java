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

import net.raphimc.audiomixer.io.mp3.Mp3AudioOutputStream;
import net.raphimc.audiomixer.util.AudioFormat;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.io.seekable.SeekableBufferedOutputStream;
import net.raphimc.audiomixer.util.io.seekable.SeekableFileOutputStream;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.NoteBlockRenderer;
import net.raphimc.noteblockrenderer.provider.SoundProvider;
import net.raphimc.noteblockrenderer.provider.impl.NoteBlockStudioSoundProvider;
import net.raphimc.noteblockrenderer.provider.impl.UrlSoundProvider;
import net.raphimc.noteblockrenderer.util.MetadataUtil;

import java.io.File;

public final class FileExample {

    private FileExample() {
    }

    public static void main(final String[] args) throws Throwable {
        final File input = new File("input.nbs");
        final File output = new File("output.mp3");

        // Read the song
        final Song song = NoteBlockLib.readSong(input);
        // Create a sound provider
        final SoundProvider soundProvider = new NoteBlockStudioSoundProvider().then(new UrlSoundProvider());
        // Create the renderer (Additional options can be set in the constructor)
        final NoteBlockRenderer renderer = new NoteBlockRenderer(song, new AudioFormat(48000, 2), soundProvider);
        // Render the song to an audio buffer
        final AudioBuffer outputAudioBuffer = renderer.renderFully(progress -> {
            System.out.printf("Rendering progress: %.02f %%%n", progress);
        });
        System.out.println("Rendering completed! Output audio buffer length: " + (int) Math.ceil(outputAudioBuffer.millisecondLength() / 1000F) + " seconds");
        // Write the audio buffer to a file (Other formats can be used as well, such as WAV or FLAC)
        final Mp3AudioOutputStream mp3AudioOutputStream = new Mp3AudioOutputStream(new SeekableBufferedOutputStream(new SeekableFileOutputStream(output)), outputAudioBuffer.format(), MetadataUtil.getMp3Tags(song));
        mp3AudioOutputStream.write(outputAudioBuffer);
        mp3AudioOutputStream.close();
    }

}
