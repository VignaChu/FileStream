package fun.vignachu.model.midi;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MidiParser {
    // 本类将原Midi文件中事件流转换为音符快流
    private final byte[] data;
    private int pos = 0;
    public MidiParser(File file) throws IOException { this.data = Files.readAllBytes(file.toPath()); }

    public MidiData parse() throws Exception {
        MidiData result = new MidiData();
        if (pos+4 > data.length || !readString().equals("MThd")) throw new Exception("Not a MIDI file");
        readInt32(); readInt16(); int trackCount = readInt16(); result.ppq = readInt16();

        int tracksFound = 0;
        while (tracksFound < trackCount && pos < data.length - 8) {
            String chunkType = readString();
            int chunkLen = readInt32();
            if ("MTrk".equals(chunkType)) {
                parseTrack(tracksFound++, chunkLen, result);
            } else {
                pos += chunkLen;
            }
        }
        calcTime(result);
        return result;
    }

    private void parseTrack(int idx, int len, MidiData data) {
        int endPos = pos + len;
        MidiData.MidiTrack track = new MidiData.MidiTrack();
        track.trackNumber = idx;
        long tick = 0; int lastSt = 0;
        Map<Integer, MidiData.MidiNote> active = new HashMap<>();

        while (pos < endPos && pos < this.data.length) {
            tick += readVLQ();
            if (pos >= endPos) break;
            int st = readByte() & 0xFF;
            if (st < 0x80) { st = lastSt; pos--; } else lastSt = st;

            if (st == 0xFF) {
                int type = readByte() & 0xFF; int l = (int)readVLQ();
                if (type == 0x51 && l >= 3) {
                    int mpq = (readByte() & 0xFF)<<16 | (readByte() & 0xFF)<<8 | (readByte() & 0xFF);
                    data.tempoMap.add(new MidiData.TempoEvent(tick, mpq));
                    if(l>3) pos+=(l-3);
                } else pos += l;
            } else if (st == 0xF0 || st == 0xF7) {
                pos += (int)readVLQ();
            } else {
                int cmd = st & 0xF0, ch = st & 0x0F;
                if (pos >= endPos) break;
                int d1 = readByte() & 0xFF, d2 = 0;
                if (cmd != 0xC0 && cmd != 0xD0 && pos < endPos) d2 = readByte() & 0xFF;

                if (cmd == 0x90 && d2 > 0) {
                    MidiData.MidiNote n = new MidiData.MidiNote();
                    n.startTicks = tick; n.pitch = d1; n.velocity = d2; n.channel = ch;
                    active.put(ch*128+d1, n);
                } else if (cmd == 0x80 || (cmd==0x90 && d2==0)) {
                    MidiData.MidiNote n = active.remove(ch*128+d1);
                    if (n != null) { n.endTicks = tick; track.notes.add(n); }
                }
            }
        }
        this.pos = endPos;
        if (!track.notes.isEmpty()) data.tracks.add(track);
    }
    private void calcTime(MidiData data) {
        Collections.sort(data.tempoMap);
        if(data.tempoMap.isEmpty()) data.tempoMap.add(new MidiData.TempoEvent(0, 500000));
        for(MidiData.MidiTrack t : data.tracks) {
            for(MidiData.MidiNote n : t.notes) {
                n.startTimeMs = tickToMs(n.startTicks, data);
                n.durationMs = tickToMs(n.endTicks, data) - n.startTimeMs;
            }
        }
    }
    private double tickToMs(long tick, MidiData data) {
        double ms=0; long pTick=0; int mpq=500000;
        for(MidiData.TempoEvent e : data.tempoMap) {
            if(e.tick >= tick) break;
            ms += (e.tick - pTick) * mpq / (double)(data.ppq*1000);
            pTick = e.tick; mpq = e.mpq;
        }
        ms += (tick - pTick) * mpq / (double)(data.ppq*1000);
        return ms;
    }
    private byte readByte() { return pos < data.length ? data[pos++] : 0; }
    private String readString() { String s = new String(data, pos, 4); pos+=4; return s; }
    private int readInt16() { return ((readByte()&0xFF)<<8)|(readByte()&0xFF); }
    private int readInt32() { return ((readByte()&0xFF)<<24)|((readByte()&0xFF)<<16)|((readByte()&0xFF)<<8)|(readByte()&0xFF); }
    private long readVLQ() { long v=0; int b; do { if(pos>=data.length) break; b=data[pos++]&0xFF; v=(v<<7)|(b&0x7F); } while((b&0x80)!=0); return v; }
}