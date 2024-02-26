package sound;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import processing.core.PApplet;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


/**
 * A Utility class for MIDI music notes and frequencies.
 * <br><br>
 * MIDI notes have values in range [0, 127], which are mapped to frequencies using {@link #midiToFreq(float)}<br><br>
 * <p>
 * Fundamental Notes -> MIDI Value<br>
 * A0 : 21 <br>
 * B0 : 23 <br>
 * C1 : 24 <br>
 * D1 : 26 <br>
 * E1 : 28 <br>
 * F1 : 29 <br>
 * G1 : 31 <br>
 * </p>
 * Successive overtones have Midi values of 12 greater than the previous note. For ex. A1 : 33, C5 : 72<br><br>
 * <strong>Use {@link #getStandardNotesToMidiMap()} to get standard notes and their overtones mapped to MIDI values</strong><br>
 * <strong>Use {@link #getStandardNoteMidiValue(char, int)}} and {@link #getStandardNoteMidiValue(String)} to get the MIDI value of a note</strong><br>
 * */
public class MusicNotes {

    public static final float DEFAULT_MIDI_BASE_FREQUENCY = 440.0f;     // 440 Hz

    public static float midiToFreq(float midiNote /* [0, 127] */, float baseFreq) {
        return (PApplet.pow(2, ((midiNote - 69) / 12.0f))) * baseFreq;
    }

    public static float midiToFreq(float midiNote /* [0, 127] */) {
        return midiToFreq(midiNote, DEFAULT_MIDI_BASE_FREQUENCY);
    }


    private static final int[] FUNDAMENTAL_NOTES_MIDI = { 21, 23, 24, 26, 28, 29, 31 };
    private static final char[] FUNDAMENTAL_NOTES_SYMBOLS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G' };
    private static final int[] FUNDAMENTAL_NOTES_HARMONICS = { 0, 0, 1, 1, 1, 1, 1 };

    @Nullable
    private static volatile Map<String, Integer> sNotesToMIdi;

    @NotNull
    @Unmodifiable
    private static Map<String, Integer> createStandardNotesToMidiMap() {
        final Map<String, Integer> map = new TreeMap<>();

        for (int i=0; i < FUNDAMENTAL_NOTES_MIDI.length; i++) {
            int note = FUNDAMENTAL_NOTES_MIDI[i];
            int harmonics = FUNDAMENTAL_NOTES_HARMONICS[i];
            final char symbol = FUNDAMENTAL_NOTES_SYMBOLS[i];

            do {
                map.put(noteSymbol(symbol, harmonics), note);
                note += 12;
                harmonics++;
            } while (note <= 127);
        }

        return Collections.unmodifiableMap(map);
    }

    @NotNull
    @Unmodifiable
    public static Map<String, Integer> getStandardNotesToMidiMap() {
        Map<String, Integer> map = sNotesToMIdi;
        if (map == null) {
            synchronized (MusicNotes.class) {
                map = sNotesToMIdi;
                if (map == null) {
                    map = createStandardNotesToMidiMap();
                    sNotesToMIdi = map;
                }
            }
        }

        return map;
    }

    @NotNull
    public static String noteSymbol(char note, int harmonic) {
        return String.valueOf(Character.toUpperCase(note)) + harmonic;
    }

    /**
     * @param note character representing the note: A, B, C up to G
     * @param harmonic 0 for fundamental, 1 for 1st overtone and so on
     * @return MIDI value of the given note, or {@code -1} if not found
     * */
    public static int getStandardNoteMidiValue(char note, int harmonic) {
        if (harmonic < 0)
            throw new IllegalArgumentException("Harmonic must be positive, given " + harmonic);

        return getStandardNotesToMidiMap().getOrDefault(noteSymbol(note, harmonic), -1);
    }

    /**
     * @param note The note with harmonics: A0, B4, C2 up to G
     * @return MIDI value of the given note, or {@code -1} if not found
     * */
    public static int getStandardNoteMidiValue(String note) {
        if (note == null || note.isEmpty())
            throw new IllegalArgumentException("Note must not be empty!!");

        return getStandardNotesToMidiMap().getOrDefault(note.toUpperCase(), -1);
    }

    public static void main(String[] args) {

    }

}
