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
    private volatile float volume = 0.7f;

    // audio player
    private Clip clip;
    private FloatControl gainControl;
    private final Object clipLock = new Object();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // callbacks
    private final List<Consumer<Track>> onTrackChangedListeners = new ArrayList<>();
    private final List<Consumer<State>> onStateChangedListeners = new ArrayList<>();
    private final List<BiConsumer<Long, Long>> onProgressListeners = new ArrayList<>();
    private final List<Runnable> onQueueEndListeners = new ArrayList<>();

    public PlayerService() {
        // Warm up Java Sound once to reduce first-play latency on cold start.
        executor.submit(this::warmUpAudioSystem);
    }

    // --API--

    // queue management
    public void setQueue(List<Track> tracks, int startIndex) {
        originalQueue = new ArrayList<>(tracks);
        queue = new ArrayList<>(tracks);
        currentIndex = startIndex;
        playCurrentTrack(false, 0L);
    }

    public void setQueuePausedAt(List<Track> tracks, int startIndex, long positionMicroseconds) {
        originalQueue = new ArrayList<>(tracks);
        queue = new ArrayList<>(tracks);
        currentIndex = startIndex;
        playCurrentTrack(true, Math.max(0L, positionMicroseconds));
    }

    // playback controls
    public void play() {
        synchronized (clipLock) {
            if (state == State.PAUSED && clip != null) {
                clip.start();
                setState(State.PLAYING);
                startProgressTimer(); // fix for timing issues when starting from a paused position
            }
        }
    }

    public void pause() {
        synchronized (clipLock) {
            if (state == State.PLAYING && clip != null) {
                clip.stop();
                setState(State.PAUSED);
            }
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
        playCurrentTrack(false, 0L);
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
            playCurrentTrack(false, 0L);
        }
    }

    // seek to a specific position in the track
    public void seekTo(long microseconds) {
        synchronized (clipLock) {
            if (clip != null) {
                clip.setMicrosecondPosition(microseconds);
            }
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        synchronized (clipLock) {
            applyVolumeToClip();
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
        synchronized (clipLock) {
            if (clip != null) {
                clip.stop();
                clip.close();
                clip = null;
                gainControl = null;
            }
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
        synchronized (clipLock) {
            return clip != null ? clip.getMicrosecondPosition() : 0;
        }
    }
    public long getDuration() {
        synchronized (clipLock) {
            return clip != null ? clip.getMicrosecondLength() : 0;
        }
    }
    public float getVolume() {
        return volume;
    }

    // callbacks

    public void setOnTrackChanged(Consumer<Track> trackCallback) {
        if (trackCallback != null) {
            onTrackChangedListeners.add(trackCallback);
        }
    }
    public void setOnStateChanged(Consumer<State> stateCallback) {
        if (stateCallback != null) {
            onStateChangedListeners.add(stateCallback);
        }
    }
    public void setOnProgress(BiConsumer<Long, Long> progressCallback) {
        if (progressCallback != null) {
            onProgressListeners.add(progressCallback);
        }
    }
    public void setOnQueueEnd(Runnable queueEndCallback) {
        if (queueEndCallback != null) {
            onQueueEndListeners.add(queueEndCallback);
        }
    }

    // internal methods

    private void playCurrentTrack(boolean startPaused, long initialPositionMicroseconds) {
        if (queue.isEmpty()) {
            return;
        }
        final Track trackToPlay = queue.get(currentIndex);
        currentTrack = trackToPlay;

        // execute on a separate thread to avoid blocking the UI
        executor.submit(() -> {
            try {
                Clip newClip;
                try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(new File(trackToPlay.getFilePath()))) {
                    AudioFormat baseFormat = rawStream.getFormat();

                    AudioInputStream playbackStream;
                    if (isDirectlyPlayable(baseFormat)) {
                        playbackStream = rawStream;
                    } else {
                        AudioFormat decodedFormat = new AudioFormat(
                                AudioFormat.Encoding.PCM_SIGNED,
                                baseFormat.getSampleRate(),
                                16,
                                baseFormat.getChannels(),
                                baseFormat.getChannels() * 2,
                                baseFormat.getSampleRate(),
                                false
                        );
                        playbackStream = AudioSystem.getAudioInputStream(decodedFormat, rawStream);
                    }

                    try (AudioInputStream in = playbackStream) {
                        newClip = AudioSystem.getClip();
                        newClip.open(in);
                    }
                }

                newClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP && state == State.PLAYING) {
                        // if the track ends, play the next one
                        if (newClip.getMicrosecondPosition() >= newClip.getMicrosecondLength() - 100_000) {
                            if (currentIndex < queue.size() - 1) {
                                currentIndex++;
                                playCurrentTrack(false, 0L);
                            } else {
                                setState(State.STOPPED);
                                for (Runnable listener : new ArrayList<>(onQueueEndListeners)) {
                                    listener.run();
                                }
                            }
                        }
                    }
                });

                synchronized (clipLock) {
                    if (clip != null) {
                        clip.stop();
                        clip.close();
                    }
                    clip = newClip;
                    gainControl = clip.isControlSupported(FloatControl.Type.MASTER_GAIN)
                            ? (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)
                            : null;
                    applyVolumeToClip();
                    if (startPaused) {
                        long clamped = Math.max(0L, Math.min(initialPositionMicroseconds, clip.getMicrosecondLength()));
                        clip.setMicrosecondPosition(clamped);
                    } else {
                        clip.start();
                    }
                }

                if (startPaused) {
                    setState(State.PAUSED);
                } else {
                    setState(State.PLAYING);
                }
                notifyTrackChanged(trackToPlay);
                emitProgressSnapshot();
                if (!startPaused) {
                    startProgressTimer();
                }

            } catch (Exception e) {
                System.err.println("PlayerService error: " + e.getMessage());
            }
        });
    }

    // time stamping for progress updates
    private void startProgressTimer() {
        Thread progressThread = new Thread(() -> {
            while (state == State.PLAYING) {
                long position;
                long duration;
                boolean running;
                synchronized (clipLock) {
                    if (clip == null) {
                        break;
                    }
                    running = clip.isRunning();
                    position = clip.getMicrosecondPosition();
                    duration = clip.getMicrosecondLength();
                }
                if (running) {
                    for (BiConsumer<Long, Long> listener : new ArrayList<>(onProgressListeners)) {
                        listener.accept(position, duration);
                    }
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

    private void applyVolumeToClip() {
        if (clip != null && gainControl != null) {
            float dB = (float) (Math.log10(Math.max(volume, 0.0001f)) * 20);
            dB = Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum()));
            gainControl.setValue(dB);
        }
    }

    private void notifyTrackChanged(Track track) {
        for (Consumer<Track> listener : new ArrayList<>(onTrackChangedListeners)) {
            listener.accept(track);
        }
    }

    private void emitProgressSnapshot() {
        long position;
        long duration;
        synchronized (clipLock) {
            if (clip == null) {
                return;
            }
            position = clip.getMicrosecondPosition();
            duration = clip.getMicrosecondLength();
        }
        for (BiConsumer<Long, Long> listener : new ArrayList<>(onProgressListeners)) {
            listener.accept(position, duration);
        }
    }

    private boolean isDirectlyPlayable(AudioFormat format) {
        return format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && format.getSampleSizeInBits() == 16
                && !format.isBigEndian();
    }

    private void warmUpAudioSystem() {
        try {
            AudioFormat format = new AudioFormat(44_100, 16, 2, true, false);
            byte[] silence = new byte[format.getFrameSize() * 512];
            try (AudioInputStream silentStream = new AudioInputStream(
                    new java.io.ByteArrayInputStream(silence),
                    format,
                    silence.length / format.getFrameSize())) {
                Clip warmupClip = AudioSystem.getClip();
                warmupClip.open(silentStream);
                warmupClip.close();
            }
        } catch (Exception ignored) {
            // ignore any errors during warmup
        }
    }

    // state management
    private void setState(State newState) {
        this.state = newState;
        for (Consumer<State> listener : new ArrayList<>(onStateChangedListeners)) {
            listener.accept(newState);
        }
    }
}