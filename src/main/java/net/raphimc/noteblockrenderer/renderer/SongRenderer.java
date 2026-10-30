/*
 * This file is part of NoteBlockRenderer - https://github.com/RaphiMC/NoteBlockRenderer
 * Copyright (C) 2026-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblockrenderer.renderer;

import net.raphimc.audiomixer.AudioMixer;
import net.raphimc.audiomixer.automation.finite.ramp.impl.LinearRampAutomation;
import net.raphimc.audiomixer.processor.dynamics.GainProcessor;
import net.raphimc.audiomixer.processor.dynamics.LimiterProcessor;
import net.raphimc.audiomixer.resampler.Resampler;
import net.raphimc.audiomixer.resampler.impl.BlackmanSincResampler;
import net.raphimc.audiomixer.resampler.impl.CubicResampler;
import net.raphimc.audiomixer.resampler.impl.LinearResampler;
import net.raphimc.audiomixer.resampler.impl.PointResampler;
import net.raphimc.audiomixer.source.audio.impl.BufferedAudioSource;
import net.raphimc.audiomixer.util.AudioFormat;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.buffer.AudioBufferBuilder;
import net.raphimc.audiomixer.util.functional.FloatConsumer;
import net.raphimc.noteblocklib.format.nbs.model.event.NbsSoundStopperEvent;
import net.raphimc.noteblocklib.model.event.Event;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.message.request.RenderMessage;
import net.raphimc.noteblockrenderer.renderer.source.InstrumentNoteAudioSource;
import net.raphimc.noteblockrenderer.renderer.source.NoteAudioSource;
import net.raphimc.noteblockrenderer.renderer.source.PreRenderedNoteAudioSource;
import net.raphimc.noteblockrenderer.util.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SongRenderer {

    private final Song song;
    private final Map<Instrument, AudioBuffer> sounds;
    private final RenderMessage.PlaybackOptions playbackOptions;
    private final RenderMessage.RendererOptions rendererOptions;
    private final AudioMixer mixer;
    private final Supplier<Resampler> resamplerSupplier;
    private final Map<Note, AudioBuffer> preRenderedNotes = new HashMap<>();

    public SongRenderer(final Song song, final Map<Instrument, AudioBuffer> sounds, final RenderMessage.AudioFileOptions audioFileOptions, final RenderMessage.PlaybackOptions playbackOptions, final RenderMessage.RendererOptions rendererOptions) {
        this.song = song;
        this.sounds = sounds;
        this.playbackOptions = playbackOptions;
        this.rendererOptions = rendererOptions;
        this.mixer = new AudioMixer(new AudioFormat(audioFileOptions.getSampleRate(), audioFileOptions.getChannels()));
        this.resamplerSupplier = switch (this.rendererOptions.getResampler()) {
            case "point" -> PointResampler::new;
            case "linear" -> LinearResampler::new;
            case "cubic" -> CubicResampler::new;
            case "sinc" -> BlackmanSincResampler::new;
            default -> throw new UnsupportedOperationException("Unsupported resampler: " + this.rendererOptions.getResampler());
        };
    }

    public void preRenderMostUsedNotes() {
        final Map<Note, Integer> noteGroups = this.song.getNotes().getTicks().stream()
            .map(tick -> this.song.getNotes().get(tick))
            .flatMap(Collection::stream)
            .filter(note -> this.sounds.containsKey(note.getInstrument()))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(_ -> 1))).entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .sorted((Map.Entry.<Note, Integer>comparingByValue().reversed()))
            .limit(1000)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (Note note : noteGroups.keySet()) {
            final BufferedAudioSource source = new InstrumentNoteAudioSource(this.sounds.get(note.getInstrument()), note, this.resamplerSupplier.get());
            final AudioBuffer result = new AudioBuffer(this.mixer.getFormat(), this.mixer.getFormat().millisToFrameCount(source.getRemainingMillisecondLength()));
            source.render(result);
            this.preRenderedNotes.put(note, result);
        }
        final float preRenderedPercentage = ((float) noteGroups.values().stream().mapToInt(Integer::intValue).sum() / this.song.getNotes().getNoteCount() * 100F);
        final float usedMemory = this.preRenderedNotes.values().stream().mapToInt(buffer -> buffer.sampleCount() * Float.BYTES).sum() / (1024F * 1024F);
        Logger.info("Finished pre rendering most used notes, covering " + String.format("%.2f", preRenderedPercentage) + "% of all notes, using " + String.format("%.2f", usedMemory) + " MiB of memory");
    }

    public AudioBuffer render(final FloatConsumer progressConsumer) {
        final int expectedSampleCount = this.mixer.getFormat().millisToSampleCount((this.song.getLengthInSeconds() + 5) * 1000F);
        final AudioBufferBuilder bufferBuilder = new AudioBufferBuilder(this.mixer.getFormat(), expectedSampleCount);
        for (int tick = 0; tick <= this.song.getNotes().getLastTick(); tick++) {
            this.executeTick(tick);
            bufferBuilder.append(this.renderTick(tick));
            progressConsumer.accept((tick / (float) this.song.getNotes().getLastTick()) * 100F);
        }
        while (!this.mixer.isEmpty()) {
            bufferBuilder.append(this.mixer.renderMillis(10F));
        }

        final AudioBuffer buffer = bufferBuilder.build();
        switch (this.rendererOptions.getLimiter()) {
            case "windowed" -> {
                new LimiterProcessor().process(buffer);
                buffer.multiply(this.playbackOptions.getVolume() / 100F);
                buffer.clipToUnitRange();
            }
            case "global" -> {
                buffer.limitToUnitRange();
                buffer.multiply(this.playbackOptions.getVolume() / 100F);
                buffer.clipToUnitRange();
            }
            case "none" -> buffer.multiply(this.playbackOptions.getVolume() / 100F);
            default -> throw new UnsupportedOperationException("Unsupported limiter: " + this.rendererOptions.getLimiter());
        }
        return buffer;
    }

    private void executeTick(final int tick) {
        for (Note note : this.song.getNotes().getOrEmpty(tick)) {
            if (this.preRenderedNotes.containsKey(note)) {
                this.mixer.add(new PreRenderedNoteAudioSource(this.preRenderedNotes.get(note), note));
            } else if (this.sounds.containsKey(note.getInstrument())) {
                this.mixer.add(new InstrumentNoteAudioSource(this.sounds.get(note.getInstrument()), note, this.resamplerSupplier.get()));
            }
        }
        for (Event event : this.song.getEvents().getOrEmpty(tick)) {
            if (event instanceof NbsSoundStopperEvent soundStopperEvent) {
                this.mixer.forEach(source -> {
                    if (source instanceof NoteAudioSource noteAudioSource && soundStopperEvent.shouldStop(noteAudioSource.getNote())) {
                        final GainProcessor gainProcessor = new GainProcessor();
                        source.processors().add(gainProcessor);
                        final LinearRampAutomation automation = new LinearRampAutomation(gainProcessor.gain(), 0F, 100F);
                        automation.finishListeners().add(_ -> this.mixer.preRenderTasks().add(() -> this.mixer.remove(source)));
                        source.automations().add(automation);
                    }
                });
            }
        }
    }

    private AudioBuffer renderTick(final int tick) {
        this.mixer.limitSourceCount(this.rendererOptions.getMaxSounds());
        float millis = 1000F / this.song.getTempoEvents().getEffectiveTempo(tick);
        if (this.playbackOptions.getTimingJitter()) {
            millis += ThreadLocalRandom.current().nextFloat(-1F, 1F);
            if (millis <= 0F) {
                millis = 0.1F;
            }
        }
        return this.mixer.renderMillis(millis);
    }

}
