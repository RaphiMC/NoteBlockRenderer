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
package net.raphimc.noteblockrenderer;

import net.raphimc.audiomixer.LimitingAudioMixer;
import net.raphimc.audiomixer.util.JavaAudioFormatUtil;
import net.raphimc.audiomixer.util.SourceDataLineWriter;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.provider.SoundProvider;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class NoteBlockPlayer extends AudioMixerNoteBlockRenderer implements AutoCloseable {

    private static final int DEFAULT_BUFFER_MILLIS = 50;

    private final SourceDataLineWriter sourceDataLineWriter;
    private boolean paused = true;

    public NoteBlockPlayer(final SourceDataLine sourceDataLine, final Song song, final SoundProvider soundProvider) throws LineUnavailableException {
        this(sourceDataLine, song, soundProvider, new Options());
    }

    public NoteBlockPlayer(final SourceDataLine sourceDataLine, final Song song, final SoundProvider soundProvider, final Options options) throws LineUnavailableException {
        this(sourceDataLine, song, soundProvider, options, DEFAULT_BUFFER_MILLIS);
    }

    public NoteBlockPlayer(final SourceDataLine sourceDataLine, final Song song, final SoundProvider soundProvider, final Options options, final int bufferMillis) throws LineUnavailableException {
        super(song, new LimitingAudioMixer(JavaAudioFormatUtil.getAudioFormat(sourceDataLine.getFormat())), soundProvider, options);
        this.sourceDataLineWriter = new SourceDataLineWriter(sourceDataLine, bufferMillis, this::tick);
    }

    @Override
    public AudioBuffer tick() {
        if (!this.paused) {
            return super.tick();
        } else {
            return new AudioBuffer(this.getRootMixer().getFormat(), 0);
        }
    }

    /**
     * @return Whether the player is paused.
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * Pauses or resumes the player.
     *
     * @param paused Whether the player should be paused.
     */
    public void setPaused(final boolean paused) {
        this.paused = paused;
    }

    @Override
    public void close() {
        this.sourceDataLineWriter.close();
    }

    @Override
    public LimitingAudioMixer getRootMixer() {
        return (LimitingAudioMixer) super.getRootMixer();
    }

    public SourceDataLineWriter getSourceDataLineWriter() {
        return this.sourceDataLineWriter;
    }

}
