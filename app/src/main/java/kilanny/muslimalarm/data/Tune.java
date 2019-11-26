package kilanny.muslimalarm.data;

import kilanny.muslimalarm.R;

public class Tune {
    public final int rawResId, nameResId, id;
    public boolean selected, playing;

    private Tune(int id, int rawResId, int nameResId) {
        this.id = id;
        this.rawResId = rawResId;
        this.nameResId = nameResId;
    }

    public static Tune findTuneOrDefault(int tune, int defaultTuneIdx) {
        Tune[] tunes = getTunes();
        for (int i = 0; i < tunes.length; ++i) {
            if (tunes[i].rawResId == tune)
                return tunes[i];
        }
        for (int i = 0; i < tunes.length; ++i) {
            if (tunes[i].id == tune)
                return tunes[i];
        }
        return tunes[defaultTuneIdx];
    }

    public static Tune[] getTunes() {
        final int[] sounds = {
                R.raw.air_raid_alarm, R.raw.alarm, R.raw.alarm_1, R.raw.alarm_clock,
                R.raw.alarm_clock_new_s5, R.raw.alarm_loud, R.raw.alarm_ring,
                R.raw.alarm_rooster, R.raw.alarm_vs_turbo, R.raw.alarms, R.raw.animal_cow,
                R.raw.animal_horse, R.raw.animal_sounds_cats, R.raw.car_alaram_2009,
                R.raw.clock_alarm_samsung, R.raw.craw, R.raw.extreme_alarm, R.raw.loud_alarm,
                R.raw.loud_alarm_clock, R.raw.loud_alarm_sound, R.raw.loud_alarm_tone,
                R.raw.loud_continuous_beep, R.raw.loud_snoring, R.raw.monkey_sound,
                R.raw.msg_foundx20, R.raw.old_alarm_clock_best, R.raw.puma_roar,
                R.raw.real_alarm_tone, R.raw.ttwu7, R.raw.warning, R.raw.wolf
        };
        final int[] ids = {
                1, 2, 3,
                4, 5, 6,
                7, 8, 9,
                10, 11, 12, 13,
                14, 15, 16,
                17, 18, 19,
                20, 21, 22,
                23, 24, 25, 26,
                27, 28, 29, 30,
                31
        };
        final int[] names = {
                R.string.nuclear_alert, R.string.hazard_warning, R.string.car_guard,
                R.string.old_peep_alarm, R.string.hazard_traffic, R.string.annoying_surprise,
                R.string.loud_tune, R.string.rooster, R.string.ambulance_vehicle,
                R.string.police, R.string.cow, R.string.horse, R.string.birds,
                R.string.traffic_trouble, R.string.shocks, R.string.crow,
                R.string.extreme_alarm, R.string.warning_siren, R.string.annoying_tune_fixed,
                R.string.upward_annoying_tune, R.string.bubbles, R.string.constant_wave,
                R.string.high_snoring, R.string.monkey, R.string.metal_gear, R.string.old_alarm,
                R.string.tiger, R.string.drops, R.string.phone_alert, R.string.bubbles_fast,
                R.string.wolf
        };
        Tune[] tunes = new Tune[sounds.length];
        for (int i = 0; i < sounds.length; ++i) {
            tunes[i] = new Tune(ids[i], sounds[i], names[i]);
        }
        return tunes;
    }
}
