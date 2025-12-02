package fun.vignachu.model.midi;
import java.util.*;
public class MidiData {
    // 存储MIDI文件数据
    public int ppq;
    public List<MidiTrack> tracks = new ArrayList<>();
    public List<TempoEvent> tempoMap = new ArrayList<>();
    public static class MidiNote {
        public long startTicks, endTicks;
        public double startTimeMs, durationMs;
        public int pitch, velocity, channel;
    }
    public static class MidiTrack {
        public int trackNumber;
        public List<MidiNote> notes = new ArrayList<>();
    }
    public static class TempoEvent implements Comparable<TempoEvent> {
        public long tick; public int mpq;
        public TempoEvent(long t, int m) { tick = t; mpq = m; }
        @Override public int compareTo(TempoEvent o) { return Long.compare(this.tick, o.tick); }
    }
}