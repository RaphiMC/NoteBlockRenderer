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

import net.raphimc.audiomixer.util.JavaAudioFormatUtil;
import net.raphimc.audiomixer.util.PcmSampleEncoding;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.NoteBlockPlayer;
import net.raphimc.noteblockrenderer.provider.SoundProvider;
import net.raphimc.noteblockrenderer.provider.impl.NoteBlockStudioSoundProvider;
import net.raphimc.noteblockrenderer.provider.impl.UrlSoundProvider;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;

public final class RealtimeExample {

    private RealtimeExample() {
    }

    public static void main(final String[] args) throws Throwable {
        final AudioFormat format = JavaAudioFormatUtil.getJavaAudioFormat(48000, 2, PcmSampleEncoding.S16_LE);
        final File input = new File("input.nbs");

        // Read the song
        final Song song = NoteBlockLib.readSong(input);
        // Create a sound provider
        final SoundProvider soundProvider = new NoteBlockStudioSoundProvider().then(new UrlSoundProvider());
        // Create the player (Additional options can be set in the constructor)
        final NoteBlockPlayer player = new NoteBlockPlayer(AudioSystem.getSourceDataLine(format), song, soundProvider);
        // Start the player (it will be paused by default, so you need to unpause it)
        player.setPaused(false);

        final JSlider volumeSlider = new JSlider(0, 100, 100);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setMajorTickSpacing(10);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.addChangeListener(event -> player.getRootMixer().gain().set(volumeSlider.getValue() / 100F));
        final JSlider positionSlider = new JSlider(0, player.getSong().getNotes().getLastTick(), 0);
        positionSlider.addChangeListener(event -> player.setTick(positionSlider.getValue()));
        final JButton playPauseButton = new JButton("Pause");
        playPauseButton.addActionListener(event -> {
            if (player.isPaused()) {
                player.setPaused(false);
                playPauseButton.setText("Pause");
            } else {
                player.setPaused(true);
                playPauseButton.setText("Play");
            }
        });
        final JPanel listPanel = new JPanel(new GridLayout(10, 0));
        final Timer timer = new Timer(10, event -> {
            positionSlider.setValue(player.getTick());
            listPanel.removeAll();
            listPanel.add(new JLabel("Position (tick): %,d / %,d".formatted(player.getTick(), player.getSong().getNotes().getLastTick())));
            listPanel.add(new JLabel("Position (ms): %,d / %,d".formatted(player.getMillisecondPosition(), player.getSong().getLengthInSeconds() * 1000)));
            listPanel.add(new JLabel("Progress (%%): %,.02f / 100".formatted(player.getProgress())));
            listPanel.add(new JLabel("CPU load (%%): %,.0f / 100".formatted(player.getSourceDataLineWriter().getProcessingLoad())));
            listPanel.add(new JLabel("Simultaneous sounds: %,d".formatted(player.getMixer().size())));
            listPanel.revalidate();
        });
        timer.start();
        for (ActionListener listener : timer.getActionListeners()) {
            listener.actionPerformed(null);
        }

        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.add(new JLabel("Volume"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
        panel.add(volumeSlider, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 5), 0, 0));
        panel.add(new JLabel("Position"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
        panel.add(positionSlider, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 5), 0, 0));
        panel.add(playPauseButton, new GridBagConstraints(0, 2, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        panel.add(listPanel, new GridBagConstraints(0, 3, 2, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        panel.add(Box.createVerticalGlue(), new GridBagConstraints(0, 100, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));

        final JFrame frame = new JFrame("RealtimeExample");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(frame.getWidth() * 2, frame.getHeight());
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
