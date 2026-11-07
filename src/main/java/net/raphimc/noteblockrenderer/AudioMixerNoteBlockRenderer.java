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
import net.raphimc.audiomixer.automation.finite.ramp.impl.LinearRampAutomation;
import net.raphimc.audiomixer.mixer.Mixer;
import net.raphimc.audiomixer.mixer.MultithreadedMixer;
import net.raphimc.audiomixer.processor.dynamics.GainProcessor;
import net.raphimc.audiomixer.resampler.Resampler;
import net.raphimc.audiomixer.resampler.impl.LinearResampler;
import net.raphimc.audiomixer.source.audio.AudioSource;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.functional.FloatConsumer;
import net.raphimc.noteblocklib.format.nbs.model.event.NbsSoundStopperEvent;
import net.raphimc.noteblocklib.model.event.Event;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.player.SongRenderer;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblockrenderer.provider.SoundProvider;
import net.raphimc.noteblockrenderer.source.MonoInstrumentNoteAudioSource;
import net.raphimc.noteblockrenderer.source.NoteAudioSource;
import net.raphimc.noteblockrenderer.source.PreRenderedNoteAudioSource;
import net.raphimc.noteblockrenderer.source.StereoInstrumentNoteAudioSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AudioMixerNoteBlockRenderer extends SongRenderer<AudioBuffer> {

    private final AudioMixer rootMixer;
    private final Mixer mixer;
    private final Options options;
    private final Random timingJitterRandom;
    private final Map<Instrument, AudioBuffer> sounds = new HashMap<>();
    private final Map<Note, AudioBuffer> preRenderedNotes = new HashMap<>();
    private float timingJitterDriftMillis;

    protected AudioMixerNoteBlockRenderer(final Song song, final AudioMixer rootMixer, final SoundProvider soundProvider, final Options options) {
        super(song);
        this.rootMixer = rootMixer;
        this.options = options;
        this.timingJitterRandom = new Random(song.getFileNameOr("").hashCode());
        if (this.options.multiThreaded()) {
            this.mixer = new MultithreadedMixer();
            this.rootMixer.add(this.mixer);
        } else {
            this.mixer = this.rootMixer;
        }
        for (Instrument instrument : SongUtil.getUsedInstruments(song)) {
            try {
                this.sounds.put(instrument, soundProvider.getSoundBuffer(instrument));
            } catch (final Throwable e) {
                this.options.soundLoadExceptionHandler().accept(instrument, e);
            }
        }
    }

    public void preRenderMostUsedNotes(final FloatConsumer progressConsumer) {
        this.preRenderMostUsedNotes(progressConsumer, 512L * 1024L * 1024L);
    }

    public void preRenderMostUsedNotes(final FloatConsumer progressConsumer, final long maxMemorySize) {
        this.preRenderedNotes.clear();
        if (maxMemorySize <= 0L) {
            progressConsumer.accept(100F);
            return;
        }
        final List<Note> notesByFrequency = this.getSong().getNotes().getTicks().stream()
            .map(tick -> this.getSong().getNotes().get(tick))
            .flatMap(Collection::stream)
            .filter(note -> this.options.noteFilter().test(note))
            .filter(note -> this.sounds.containsKey(note.getInstrument()))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(ignored -> 1))).entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .sorted((Map.Entry.<Note, Integer>comparingByValue().reversed()))
            .map(Map.Entry::getKey)
            .toList();

        long usedMemory = 0L;
        int processedNotes = 0;
        for (Note note : notesByFrequency) {
            final AudioSource source = this.createNoteAudioSource(note);
            final int frameCount = this.rootMixer.getFormat().millisToFrameCount(source.getRemainingMillisecondLength());
            final long requiredMemory = (long) frameCount * this.rootMixer.getFormat().channelCount() * Float.BYTES;
            if (requiredMemory <= maxMemorySize - usedMemory) {
                final AudioBuffer result = new AudioBuffer(this.rootMixer.getFormat(), frameCount);
                source.render(result);
                this.preRenderedNotes.put(note, result);
                usedMemory += requiredMemory;
            }
            final float noteProgress = (float) ++processedNotes / notesByFrequency.size();
            final float memoryProgress = (float) usedMemory / maxMemorySize;
            progressConsumer.accept(Math.max(noteProgress, memoryProgress) * 100F);
        }
        progressConsumer.accept(100F);
    }

    public AudioMixer getRootMixer() {
        return this.rootMixer;
    }

    public Mixer getMixer() {
        return this.mixer;
    }

    @Override
    protected void playNote(final Note note) {
        if (this.options.noteFilter().test(note)) {
            final AudioSource source = this.createNoteAudioSource(note);
            if (source != null) {
                this.mixer.add(source);
            }
        }
    }

    @Override
    protected void handleEvent(final Event event) {
        if (event instanceof NbsSoundStopperEvent soundStopperEvent) {
            this.mixer.forEach(source -> {
                if (source instanceof NoteAudioSource noteAudioSource && soundStopperEvent.shouldStop(noteAudioSource.getNote())) {
                    final GainProcessor gainProcessor = new GainProcessor();
                    source.processors().add(gainProcessor);
                    final LinearRampAutomation automation = new LinearRampAutomation(gainProcessor.gain(), 0F, 100F);
                    automation.finishListeners().add(ignored -> this.rootMixer.preRenderTasks().add(() -> this.mixer.remove(source)));
                    source.automations().add(automation);
                }
            });
        }
    }

    @Override
    protected AudioBuffer render(float millis) {
        this.mixer.limitSourceCount(this.options.maxSimultaneousSoundCount());
        if (this.options.timingJitter()) {
            final float steering = this.timingJitterDriftMillis / 10F;
            float jitter;
            do {
                jitter = this.timingJitterRandom.nextFloat(-Math.min(1F + steering, 1F), Math.min(1F - steering, 1F));
            } while (millis + jitter <= 0F);
            this.timingJitterDriftMillis += jitter;
            millis += jitter;
        }
        return this.rootMixer.renderMillis(millis);
    }

    private AudioSource createNoteAudioSource(final Note note) {
        if (this.preRenderedNotes.containsKey(note)) {
            return new PreRenderedNoteAudioSource(this.preRenderedNotes.get(note), note);
        } else if (this.sounds.containsKey(note.getInstrument())) {
            return switch (this.rootMixer.getFormat().channelCount()) {
                case 1 -> new MonoInstrumentNoteAudioSource(this.sounds.get(note.getInstrument()), note, this.options.resamplerSupplier().get());
                case 2 -> new StereoInstrumentNoteAudioSource(this.sounds.get(note.getInstrument()), note, this.options.resamplerSupplier().get());
                default -> throw new IllegalStateException("AudioMixer format must be mono or stereo: " + this.rootMixer.getFormat());
            };
        } else {
            return null;
        }
    }

    public record Options(BiConsumer<Instrument, Throwable> soundLoadExceptionHandler, Supplier<Resampler> resamplerSupplier, boolean multiThreaded, boolean timingJitter, int maxSimultaneousSoundCount, Predicate<Note> noteFilter) {

        private static final BiConsumer<Instrument, Throwable> DEFAULT_SOUND_LOAD_EXCEPTION_HANDLER = (instrument, exception) -> {
            System.err.println("Failed to load sound for instrument '" + instrument + "': " + exception.getMessage());
        };
        private static final Supplier<Resampler> DEFAULT_RESAMPLER_SUPPLIER = LinearResampler::new;
        private static final Predicate<Note> DEFAULT_NOTE_FILTER = note -> true;

        public Options() {
            this(DEFAULT_SOUND_LOAD_EXCEPTION_HANDLER, DEFAULT_RESAMPLER_SUPPLIER, false, true, Integer.MAX_VALUE, DEFAULT_NOTE_FILTER);
        }

        public Options withSoundLoadExceptionHandler(final BiConsumer<Instrument, Throwable> soundLoadExceptionHandler) {
            return new Options(soundLoadExceptionHandler, this.resamplerSupplier, this.multiThreaded, this.timingJitter, this.maxSimultaneousSoundCount, this.noteFilter);
        }

        public Options withResamplerSupplier(final Supplier<Resampler> resamplerSupplier) {
            return new Options(this.soundLoadExceptionHandler, resamplerSupplier, this.multiThreaded, this.timingJitter, this.maxSimultaneousSoundCount, this.noteFilter);
        }

        public Options withMultiThreaded(final boolean multiThreaded) {
            return new Options(this.soundLoadExceptionHandler, this.resamplerSupplier, multiThreaded, this.timingJitter, this.maxSimultaneousSoundCount, this.noteFilter);
        }

        public Options withTimingJitter(final boolean timingJitter) {
            return new Options(this.soundLoadExceptionHandler, this.resamplerSupplier, this.multiThreaded, timingJitter, this.maxSimultaneousSoundCount, this.noteFilter);
        }

        public Options withMaxSimultaneousSoundCount(final int maxSimultaneousSoundCount) {
            return new Options(this.soundLoadExceptionHandler, this.resamplerSupplier, this.multiThreaded, this.timingJitter, maxSimultaneousSoundCount, this.noteFilter);
        }

        public Options withNoteFilter(final Predicate<Note> noteFilter) {
            return new Options(this.soundLoadExceptionHandler, this.resamplerSupplier, this.multiThreaded, this.timingJitter, this.maxSimultaneousSoundCount, noteFilter);
        }

    }

}
