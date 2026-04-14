package com.javify.services;

import com.javify.objects.Track;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlayerService {

    // state of the player
    public enum State { STOPPED, PLAYING, PAUSED }

    private State state = State.STOPPED;
    private Track currentTrack;
    private List<Track> queue = new ArrayList<>();
    private List<Track> originalQueue = new ArrayList<>();
    private int currentIndex = 0;
    private boolean shuffle = false;

    // audio player
    private Clip clip;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // callbacks
    private Consumer<Track> onTrackChanged;
    private Consumer<State> onStateChanged;
    private BiConsumer<Long, Long> onProgress;
    private Runnable onQueueEnd;

    // --API--

    // queue management
    public void setQueue(List<Track> tracks, int startIndex) {
        originalQueue = new ArrayList<>(tracks);
        queue = new ArrayList<>(tracks);
        currentIndex = startIndex;
        playCurrentTrack();
    }

    // playback controls
    public void play() {
        if (state == State.PAUSED && clip != null) {
            clip.start();
            setState(State.PLAYING);
        }
    }

    public void pause() {
        if (state == State.PLAYING && clip != null) {
            clip.stop();
            setState(State.PAUSED);
        }
    }

    public void togglePlayPause() {
        if (state == State.PLAYING) {
            pause();
        } else if (state == State.PAUSED) {
            play();
        }
    }

    public void next() {
        if (queue.isEmpty()) {
            return;
        }
        currentIndex = (currentIndex + 1) % queue.size();
        playCurrentTrack();
    }

    public void previous() {
        if (queue.isEmpty()) {
            return;
        }
        // if the current track is playing, we need to seek to the beginning
        if (clip != null && clip.getMicrosecondPosition() > 3_000_000L) {
            seekTo(0);
        } else {
            currentIndex = (currentIndex - 1 + queue.size()) % queue.size();
            playCurrentTrack();
        }
    }

    // seek to a specific position in the track
    public void seekTo(long microseconds) {
        if (clip != null) {
            clip.setMicrosecondPosition(microseconds);
        }
    }

    public void setVolume(float volume) {
        // volume 0.0 - 1.0
        if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log10(Math.max(volume, 0.0001f)) * 20); // dB = 20 * log10(volume)
            dB = Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum())); // clamp to min/max
            gainControl.setValue(dB);
        }
    }

    // shuffle mode
    public void toggleShuffle() {
        shuffle = !shuffle;
        int currentTrackId = currentTrack != null ? currentTrack.getId() : -1;

        if (shuffle) {
            Collections.shuffle(queue);
            // loop through the queue and move the current track to the front
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getId() == currentTrackId) {
                    Collections.swap(queue, 0, i);
                    currentIndex = 0;
                    break;
                }
            }
        } else {
            // return to the original queue
            queue = new ArrayList<>(originalQueue);
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getId() == currentTrackId) {
                    currentIndex = i;
                    break;
                }
            }
        }
    }

    // stop playback
    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        setState(State.STOPPED);
    }

    // getters

    public State getState() {
        return state;
    }
    public Track getCurrentTrack() {
        return currentTrack;
    }
    public boolean isShuffle() {
        return shuffle;
    }
    public long getPosition() {
        return clip != null ? clip.getMicrosecondPosition() : 0;
    }
    public long getDuration() {
        return clip != null ? clip.getMicrosecondLength() : 0;
    }

    // callbacks

    public void setOnTrackChanged(Consumer<Track> trackCallback) {
        this.onTrackChanged = trackCallback;
    }
    public void setOnStateChanged(Consumer<State> stateCallback) {
        this.onStateChanged = stateCallback;
    }
    public void setOnProgress(BiConsumer<Long, Long> progressCallback) {
        this.onProgress = progressCallback;
    }
    public void setOnQueueEnd(Runnable queueEndCallback) {
        this.onQueueEnd = queueEndCallback;
    }

    // internal methods

    private void playCurrentTrack() {
        if (queue.isEmpty()) {
            return;
        }
        currentTrack = queue.get(currentIndex);

        // execute on a separate thread to avoid blocking the UI
        executor.submit(() -> {
            try {
                if (clip != null) {
                    clip.stop();
                    clip.close();
                }

                File file = new File(currentTrack.getFilePath());
                AudioInputStream rawStream = AudioSystem.getAudioInputStream(file);
                AudioFormat baseFormat = rawStream.getFormat();

                // convert to 16-bit PCM if necessary
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );
                AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, rawStream);

                clip = AudioSystem.getClip();
                clip.open(decodedStream);

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP && state == State.PLAYING) {
                        // if the track ends, play the next one
                        if (clip.getMicrosecondPosition() >= clip.getMicrosecondLength() - 100_000) {
                            if (currentIndex < queue.size() - 1) {
                                currentIndex++;
                                playCurrentTrack();
                            } else {
                                setState(State.STOPPED);
                                if (onQueueEnd != null) onQueueEnd.run();
                            }
                        }
                    }
                });

                clip.start();
                setState(State.PLAYING);
                if (onTrackChanged != null) onTrackChanged.accept(currentTrack);
                startProgressTimer();

            } catch (Exception e) {
                System.err.println("PlayerService error: " + e.getMessage());
            }
        });
    }

    // time stamping for progress updates
    private void startProgressTimer() {
        Thread progressThread = new Thread(() -> {
            while (clip != null && state == State.PLAYING) {
                if (onProgress != null && clip.isRunning()) {
                    onProgress.accept(clip.getMicrosecondPosition(), clip.getMicrosecondLength());
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();
    }

    // state management
    private void setState(State newState) {
        this.state = newState;
        if (onStateChanged != null) onStateChanged.accept(newState);
    }
}