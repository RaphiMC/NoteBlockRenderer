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
package net.raphimc.noteblockrenderer.util;

import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Instrument;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

public final class StringUtil {

    private StringUtil() {
    }

    public static String formatDuration(final long durationMillis) {
        return DurationFormatUtils.formatDuration(durationMillis, "[m'm' ][s's' ]S'ms'", false);
    }

    @SuppressWarnings("deprecation")
    public static String getInstrumentDisplayName(final Instrument instrument) {
        return switch (instrument) {
            case MinecraftInstrument minecraftInstrument -> WordUtils.capitalizeFully(minecraftInstrument.name().replace('_', ' '));
            case NbsCustomInstrument customInstrument -> customInstrument.getNameOr("No Name") + " (" + customInstrument.getSoundFilePathOr("No Sound File") + ")";
            default -> throw new IllegalArgumentException("Unsupported instrument type: " + instrument.getClass().getName());
        };
    }

}
