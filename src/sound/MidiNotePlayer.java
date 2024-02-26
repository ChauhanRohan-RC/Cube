package sound;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import processing.sound.Env;
import processing.sound.TriOsc;


/**
 * Midi notes are integers in range [0, 127], which map to specific frequencies
 * */
public class MidiNotePlayer {

    /**
     * Whether multiple notes are allowed to play simultaneously by default
     * */
    public static final boolean DEFAULT_POLY_RHYTHM_ENABLED = true;


    @NotNull
    private final PApplet parent;

    private float attackTime = 0.01f;
    private float sustainTime = 0.5f;
    private float sustainLevel = 0.4f;
    private float releaseTime = 0.25f;

    private float amplitude = 0.5f;
    private float frequency = MusicNotes.DEFAULT_MIDI_BASE_FREQUENCY;

    @Nullable
    private TriOsc lastOscillator;

    /**
     * Whether multiple notes are allowed to play simultaneously
     * */
    private boolean polyRhythmEnabled = DEFAULT_POLY_RHYTHM_ENABLED;

    @NotNull
    private final Env envelop;

    public MidiNotePlayer(@NotNull PApplet parent) {
        this.parent = parent;
        envelop = new Env(parent);
    }

    @NotNull
    private TriOsc configOscillator(@NotNull TriOsc oscillator) {
        oscillator.amp(amplitude);
        oscillator.freq(frequency);
        return oscillator;
    }

    @NotNull
    private TriOsc considerCreateOscillator() {
        TriOsc osc = null;
        if (!polyRhythmEnabled) {
            osc = lastOscillator;
        }

        if (osc == null) {
            osc = new TriOsc(parent);
            if (lastOscillator != null) {
                lastOscillator.stop();
            }
        }

        lastOscillator = osc;
        return configOscillator(osc);
    }

    public float getAmplitude() {
        return amplitude;
    }

    public MidiNotePlayer setAmplitude(float amplitude) {
        this.amplitude = amplitude;
        return this;
    }

    public float getFrequency() {
        return frequency;
    }

    public MidiNotePlayer setFrequency(float frequency) {
        this.frequency = frequency;
        return this;
    }

    public MidiNotePlayer setNote(float midiNote /* [0, 127] */, float baseFrequency) {
        return setFrequency(MusicNotes.midiToFreq(midiNote, baseFrequency));
    }

    public MidiNotePlayer setNote(float midiNote /* [0, 127] */) {
        return setNote(midiNote, MusicNotes.DEFAULT_MIDI_BASE_FREQUENCY);
    }

    public float getAttackTime() {
        return attackTime;
    }

    public MidiNotePlayer setAttackTime(float attackTime) {
        this.attackTime = attackTime;
        return this;
    }

    public float getSustainTime() {
        return sustainTime;
    }

    public MidiNotePlayer setSustainTime(float sustainTime) {
        this.sustainTime = sustainTime;
        return this;
    }

    public float getSustainLevel() {
        return sustainLevel;
    }

    public MidiNotePlayer setSustainLevel(float sustainLevel) {
        this.sustainLevel = sustainLevel;
        return this;
    }

    public float getReleaseTime() {
        return releaseTime;
    }

    public MidiNotePlayer setReleaseTime(float releaseTime) {
        this.releaseTime = releaseTime;
        return this;
    }


    /**
     * @return whether multiple notes are allowed to play simultaneously
     * */
    public boolean isPolyRhythmEnabled() {
        return polyRhythmEnabled;
    }

    /**
     * @param polyRhythmEnabled Whether multiple notes are allowed to play simultaneously
     * */
    public MidiNotePlayer setPolyRhythmEnabled(boolean polyRhythmEnabled) {
        this.polyRhythmEnabled = polyRhythmEnabled;
        return this;
    }

    public MidiNotePlayer togglePolyRhythmEnabled() {
        return setPolyRhythmEnabled(!isPolyRhythmEnabled());
    }

    public MidiNotePlayer play() {
        envelop.play(considerCreateOscillator(), attackTime, sustainTime, sustainLevel, releaseTime);
        return this;
    }

    public MidiNotePlayer play(float midiNote /* [0, 127] */, float baseFrequency) {
        setNote(midiNote,  baseFrequency);
        return play();
    }

    public MidiNotePlayer play(float midiNote /* [0, 127] */) {
        return play(midiNote, MusicNotes.DEFAULT_MIDI_BASE_FREQUENCY);
    }




    public static void main(String[] args) {

    }

}
