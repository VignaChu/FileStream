package fun.vignachu.service;

import javax.sound.midi.*;
import java.io.File;

public class AudioEngine {

    // MIDI 播放引擎封装
    private Sequencer sequencer;
    private Synthesizer synthesizer;
    private Receiver receiver;

    public void init() throws Exception {
        close(); // 防止重复初始化
        sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();

        // 连接
        receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
    }

    public void loadMidi(File file) throws Exception {
        if (sequencer == null || !sequencer.isOpen()) init();
        sequencer.setSequence(MidiSystem.getSequence(file));
    }

    public boolean loadSf2(File file) throws Exception {
        if (synthesizer == null || !synthesizer.isOpen()) return false;
        Soundbank sb = MidiSystem.getSoundbank(file);
        if (!synthesizer.isSoundbankSupported(sb)) return false;
        return synthesizer.loadAllInstruments(sb);
    }

    // 【新增】重置音源为默认
    public void resetSoundbank() throws Exception {
        long currentMicro = 0;
        boolean wasRunning = false;

        // 1. 记录当前播放状态
        if (sequencer != null && sequencer.isOpen()) {
            wasRunning = sequencer.isRunning();
            currentMicro = sequencer.getMicrosecondPosition();
            if (wasRunning) sequencer.stop();
        }

        // 2. 重启合成器 (这将清除所有加载的 SF2)
        if (synthesizer != null && synthesizer.isOpen()) {
            synthesizer.close();
        }
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();

        // 3. 重新连接
        if (sequencer != null && sequencer.isOpen()) {
            receiver = synthesizer.getReceiver();
            sequencer.getTransmitter().setReceiver(receiver);

            // 恢复播放
            sequencer.setMicrosecondPosition(currentMicro);
            if (wasRunning) sequencer.start();
        }
    }

    public void play() {
        if (sequencer != null && sequencer.isOpen() && !sequencer.isRunning()) {
            sequencer.start();
        }
    }

    public void pause() {
        if (sequencer != null && sequencer.isOpen() && sequencer.isRunning()) {
            sequencer.stop();
        }
    }

    public void stop() {
        if (sequencer != null && sequencer.isOpen()) {
            if (sequencer.isRunning()) {
                sequencer.stop();
            }
            sequencer.setMicrosecondPosition(0);
        }
    }

    public void setVolume(int vol) {
        if (synthesizer == null || !synthesizer.isOpen()) return;
        for (MidiChannel ch : synthesizer.getChannels()) {
            if (ch != null) ch.controlChange(7, vol);
        }
    }

    public void setProgress(double percent) {
        if (sequencer != null && sequencer.isOpen()) {
            long len = sequencer.getMicrosecondLength();
            if (len > 0) {
                sequencer.setMicrosecondPosition((long)(percent / 100.0 * len));
            }
        }
    }

    public long getCurrentTimeMicro() {
        return (sequencer != null && sequencer.isOpen()) ? sequencer.getMicrosecondPosition() : 0;
    }

    public long getTotalTimeMicro() {
        return (sequencer != null && sequencer.isOpen()) ? sequencer.getMicrosecondLength() : 1;
    }

    public void close() {
        if (sequencer != null) {
            if (sequencer.isOpen()) {
                sequencer.stop();
                sequencer.close();
            }
            sequencer = null;
        }
        if (synthesizer != null) {
            if (synthesizer.isOpen()) {
                synthesizer.close();
            }
            synthesizer = null;
        }
    }
}