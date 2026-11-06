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

import net.raphimc.audiomixer.AudioMixer;
import net.raphimc.audiomixer.processor.dynamics.LimiterProcessor;
import net.raphimc.audiomixer.util.AudioFormat;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.buffer.AudioBufferBuilder;
import net.raphimc.audiomixer.util.functional.FloatConsumer;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.provider.SoundProvider;

public class NoteBlockRenderer extends AudioMixerNoteBlockRenderer {

    public NoteBlockRenderer(final Song song, final AudioFormat audioFormat, final SoundProvider soundProvider) {
        this(song, audioFormat, soundProvider, new Options());
    }

    public NoteBlockRenderer(final Song song, final AudioFormat audioFormat, final SoundProvider soundProvider, final Options options) {
        super(song, new AudioMixer(audioFormat), soundProvider, options);
    }

    public AudioBuffer renderFully(final FloatConsumer progressConsumer) {
        return this.renderFully(progressConsumer, 90F);
    }

    public AudioBuffer renderFully(final FloatConsumer progressConsumer, final float volume) {
        return this.renderFully(progressConsumer, volume, Limiter.WINDOWED);
    }

    public AudioBuffer renderFully(final FloatConsumer progressConsumer, final float volume, final Limiter limiter) {
        final int expectedSampleCount = this.getRootMixer().getFormat().millisToSampleCount((this.getSong().getLengthInSeconds() + 5) * 1000F);
        final AudioBufferBuilder bufferBuilder = new AudioBufferBuilder(this.getRootMixer().getFormat(), expectedSampleCount);
        while (!this.hasReachedEnd()) {
            bufferBuilder.append(this.tick());
            progressConsumer.accept(this.getProgress());
        }
        progressConsumer.accept(100F);
        while (!this.getMixer().isEmpty()) {
            bufferBuilder.append(this.render(1000F));
        }
        final AudioBuffer buffer = bufferBuilder.build().trimTrailingSilence();
        limiter.apply(buffer, volume);
        return buffer;
    }

    public enum Limiter {

        NONE {
            @Override
            protected void apply(final AudioBuffer buffer, final float volume) {
                buffer.multiply(volume / 100F);
            }
        },
        WINDOWED {
            @Override
            protected void apply(final AudioBuffer buffer, final float volume) {
                new LimiterProcessor().process(buffer);
                buffer.multiply(volume / 100F);
                buffer.clipToUnitRange();
            }
        },
        GLOBAL {
            @Override
            protected void apply(final AudioBuffer buffer, final float volume) {
                buffer.limitToUnitRange();
                buffer.multiply(volume / 100F);
                buffer.clipToUnitRange();
            }
        };

        protected abstract void apply(final AudioBuffer buffer, final float volume);

    }

}
