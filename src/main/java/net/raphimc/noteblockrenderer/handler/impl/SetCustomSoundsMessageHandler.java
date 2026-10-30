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
package net.raphimc.noteblockrenderer.handler.impl;

import net.raphimc.noteblockrenderer.NoteBlockRenderer;
import net.raphimc.noteblockrenderer.handler.MessageHandler;
import net.raphimc.noteblockrenderer.message.request.SetCustomSoundsMessage;
import net.raphimc.noteblockrenderer.util.SoundLoader;

public class SetCustomSoundsMessageHandler extends MessageHandler<SetCustomSoundsMessage> {

    @Override
    public void handleMessage(final SetCustomSoundsMessage message) {
        NoteBlockRenderer.instance().setSoundLoader(new SoundLoader(message.getFiles()));
    }

}
