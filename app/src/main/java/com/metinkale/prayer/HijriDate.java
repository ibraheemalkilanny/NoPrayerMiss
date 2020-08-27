package com.metinkale.prayer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.metinkale.prayer.utils.FastTokenizer;

import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.IslamicChronology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import kilanny.muslimalarm.App;
import kilanny.muslimalarm.R;
import kilanny.muslimalarm.data.AppSettings;

public class HijriDate {
    public static final int MUHARRAM = 1;
    public static final int SAFAR = 2;
    public static final int RABIAL_AWWAL = 3;
    public static final int RABIAL_AKHIR = 4;
    public static final int JUMADAAL_AWWAL = 5;
    public static final int JUMADAAL_AKHIR = 6;
    public static final int RAJAB = 7;
    public static final int SHABAN = 8;
    public static final int RAMADAN = 9;
    public static final int SHAWWAL = 10;
    public static final int DHUL_QADA = 11;
    public static final int DHUL_HIJJA = 12;


    public static final int MONTH = 0;
    public static final int ISLAMIC_NEW_YEAR = 1;
    public static final int ASHURA = 2;
    public static final int MAWLID_AL_NABI = 3;
    public static final int THREE_MONTHS = 4;
    public static final int RAGAIB = 5;
    public static final int MIRAJ = 6;
    public static final int BARAAH = 7;
    public static final int RAMADAN_BEGIN = 8;
    public static final int LAYLATALQADR = 9;
    public static final int LAST_RAMADAN = 10;
    public static final int EID_AL_FITR_DAY1 = 11;
    public static final int EID_AL_FITR_DAY2 = 12;
    public static final int EID_AL_FITR_DAY3 = 13;
    public static final int ARAFAT = 14;
    public static final int EID_AL_ADHA_DAY1 = 15;
    public static final int EID_AL_ADHA_DAY2 = 16;
    public static final int EID_AL_ADHA_DAY3 = 17;
    public static final int EID_AL_ADHA_DAY4 = 18;


    private static final TreeMap<Hijri, HijriDate> fromHijri = new TreeMap<>();
    private static final TreeMap<Greg, HijriDate> fromGreg = new TreeMap<>();
    // values will be initialized  in static{}
    private final int MIN_GREG_YEAR;
    private final int MAX_GREG_YEAR;
    private final int MIN_HIJRI_YEAR;
    private final int MAX_HIJRI_YEAR;

    private Hijri hijri;
    private Greg greg;

    public HijriDate(Context context, Hijri hijri, Greg greg) {
        this.hijri = hijri;
        this.greg = greg;
        int maxGregYear = 2019;
        int minGregYear = 2019;
        int maxHijriYear = 1440;
        int minHijriYear = 1440;
        BufferedReader is = null;
        try {
            is = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.hijri)));
            String line;
            while (true) {
                line = is.readLine();
                if (line == null) {
                    break;
                }
                if (line.contains("HD"))
                    continue;//first line
                FastTokenizer ft = new FastTokenizer(line, "\t");

                int d = ft.nextInt();
                int m = ft.nextInt();
                int y = ft.nextInt();
                if (m == 12) maxHijriYear = Math.max(maxHijriYear, y);
                if (m == 1) minHijriYear = Math.min(minHijriYear, y);
                Hijri hijri1 = new Hijri(y, m, d);
                d = ft.nextInt();
                m = ft.nextInt();
                y = ft.nextInt();

                maxGregYear = Math.max(maxGregYear, y);
                minGregYear = Math.min(minGregYear, y);
                Greg greg1 = new Greg(y, m, d);

                if (hijri1.day == 1) {
                    fromHijri.put(hijri1, this);
                    fromGreg.put(greg1, this);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { is.close(); }
            catch (Exception ignored) { }
        }
        MIN_GREG_YEAR = minGregYear;
        MAX_GREG_YEAR = maxGregYear;
        MIN_HIJRI_YEAR = minHijriYear;
        MAX_HIJRI_YEAR = maxHijriYear;
    }

    public int getDay() {
        return hijri.day;
    }

    public int getMonth() {
        return hijri.month;
    }

    public int getYear() {
        return hijri.year;
    }


    private static HijriDate create(Context context, Hijri hijri, Greg greg) {
        HijriDate bundle = new HijriDate(context, hijri, greg);
        if (hijri.day == 1) {
            fromHijri.put(hijri, bundle);
            fromGreg.put(greg, bundle);
        }
        return bundle;
    }


    public static HijriDate fromGreg(Context context, int y, int m, int d) {
        return fromGreg(context, new LocalDate(y, m, d));
    }

    public static HijriDate fromHijri(Context context, int y, int m, int d) {
        return fromHijri(context, new LocalDate(y, m, d, IslamicChronology.getInstanceUTC()));
    }


    public static HijriDate fromHijri(Context context, LocalDate ld) {
        if (!(ld.getChronology() instanceof IslamicChronology)) {
            throw new RuntimeException("fromHijri can only be used with a IslamicChronology");
        }

        Hijri hijri = new Hijri(ld.getYear(), ld.getMonthOfYear(), ld.getDayOfMonth());

        HijriDate date = fromHijri.get(hijri);
        if (date != null) {
            return date;
        }

        Map.Entry<Hijri, HijriDate> floor = fromHijri.floorEntry(hijri);
        HijriDate last;
        AppSettings settings = AppSettings.getInstance(context);
        int hfix = settings.getInt("HijriFix", 0);
        if (floor == null || (last = floor.getValue()) == null || fromHijri.ceilingKey(hijri) == null) {
            LocalDate gregorian = ld.toDateTimeAtStartOfDay().withChronology(ISOChronology.getInstanceUTC()).toLocalDate();
            if (hfix != 0) {
                gregorian = gregorian.plusDays(hfix);
            }
            Greg greg = new Greg(gregorian.getYear(), gregorian.getMonthOfYear(), gregorian.getDayOfMonth());
            return create(context, hijri, greg);
        } else {
            LocalDate gregLd = last.getLocalDate().plusDays(hijri.day - 1);
            if (hfix != 0) {
                gregLd = gregLd.plusDays(hfix);
            }
            Greg greg = new Greg(gregLd.getYear(), gregLd.getMonthOfYear(), gregLd.getDayOfMonth());
            return create(context, hijri, greg);
        }
    }


    public static HijriDate fromGreg(Context context, LocalDate ld) {
        if (!(ld.getChronology() instanceof GregorianChronology || ld.getChronology() instanceof ISOChronology)) {
            throw new RuntimeException("fromGreg can only be used with a GregorianChronology");
        }

        AppSettings settings = AppSettings.getInstance(context);
        int hfix = settings.getInt("HijriFix", 0);
        if (hfix != 0) {
            ld = ld.plusDays(hfix);
        }


        Greg greg = new Greg(ld.getYear(), ld.getMonthOfYear(), ld.getDayOfMonth());

        HijriDate date = fromGreg.get(greg);
        if (date != null) {
            return date;
        }

        Map.Entry<Greg, HijriDate> floor = fromGreg.floorEntry(greg);
        HijriDate last;
        if (floor == null || (last = floor.getValue()) == null || fromGreg.ceilingKey(greg) == null) {
            LocalDate islamic = ld.toDateTimeAtStartOfDay().withChronology(IslamicChronology.getInstanceUTC()).toLocalDate();
            Hijri hijri = new Hijri(islamic.getYear(), islamic.getMonthOfYear(), islamic.getDayOfMonth());
            return create(context, hijri, greg);
        } else {
            int y = last.hijri.year;
            int m = last.hijri.month;
            int d = last.hijri.day;
            LocalDate lastLD = last.getLocalDate();
            int diff = Days.daysBetween(lastLD, ld).getDays();

            d += diff;

            Hijri hijri = new Hijri(y, m, d);
            return create(context, hijri, greg);
        }
    }

    public LocalDate getLocalDate() {
        return new LocalDate(greg.year, greg.month, greg.day);
    }

    public HijriDate plusDays(Context context, int days) {
        return HijriDate.fromGreg(context, getLocalDate().plusDays(days));
    }


    public HijriDate plusYears(Context context, int years) {
        return HijriDate.fromGreg(context, getLocalDate().plusYears(years));
    }


    public HijriDate plusMonths(Context context, int month) {
        return HijriDate.fromGreg(context, getLocalDate().plusMonths(month));
    }

    @NonNull
    public static List<Pair<HijriDate, Integer>> getHolydaysForGregYear(Context context, int year) {
        int hijriYear = fromGreg(context, year, 1, 1).getYear();
        List<Pair<HijriDate, Integer>> holydays = new ArrayList<>();
        for (Pair<HijriDate, Integer> entry : getHolydaysForHijriYear(context, hijriYear)) {
            if (entry.first.greg.year == year) {
                holydays.add(entry);
            }
        }

        for (Pair<HijriDate, Integer> entry : getHolydaysForHijriYear(context, hijriYear + 1)) {
            if (entry.first.greg.year == year) {
                holydays.add(entry);
            }
        }

        return holydays;
    }

    @NonNull
    public static List<Pair<HijriDate, Integer>> getHolydaysForHijriYear(Context context, int year) {
        List<Pair<HijriDate, Integer>> dates = new ArrayList<>(12 + 18);
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, MUHARRAM, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, MUHARRAM, 1), ISLAMIC_NEW_YEAR));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, MUHARRAM, 10), ASHURA));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SAFAR, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RABIAL_AWWAL, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RABIAL_AWWAL, 11), MAWLID_AL_NABI));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RAJAB, 1), THREE_MONTHS));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RABIAL_AKHIR, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, JUMADAAL_AWWAL, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, JUMADAAL_AKHIR, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RAJAB, 1), MONTH));
        HijriDate ragaib = HijriDate.fromGreg(context, HijriDate.fromHijri(context, year, RAJAB, 1)
                .getLocalDate().withDayOfWeek(DateTimeConstants.FRIDAY));
        if (ragaib.getMonth() < RAJAB)
            ragaib = ragaib.plusDays(context, 7);
        dates.add(new Pair<>(ragaib.plusDays(context, -1), RAGAIB));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RAJAB, 26), MIRAJ));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SHABAN, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SHABAN, 14), BARAAH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RAMADAN, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RAMADAN, 1), RAMADAN_BEGIN));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, RAMADAN, 26), LAYLATALQADR));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SHAWWAL, 1).plusDays(context, -1), LAST_RAMADAN));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SHAWWAL, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SHAWWAL, 1), EID_AL_FITR_DAY1));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SHAWWAL, 2), EID_AL_FITR_DAY2));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, SHAWWAL, 3), EID_AL_FITR_DAY3));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, DHUL_QADA, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, DHUL_HIJJA, 9), ARAFAT));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, DHUL_HIJJA, 1), MONTH));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, DHUL_HIJJA, 10), EID_AL_ADHA_DAY1));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, DHUL_HIJJA, 11), EID_AL_ADHA_DAY2));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, DHUL_HIJJA, 12), EID_AL_ADHA_DAY3));
        dates.add(new Pair<>(HijriDate.fromHijri(context, year, DHUL_HIJJA, 13), EID_AL_ADHA_DAY4));
        return dates;
    }


    public static int isHolyday(Context context) {
        return fromGreg(context, LocalDate.now()).getHolyday(context);
    }

    public int getHolyday(Context context) {
        HijriDate tmp;
        if (hijri.day == 1 && hijri.month == MUHARRAM) {
            return ISLAMIC_NEW_YEAR;
        } else if (hijri.day == 10 && hijri.month == MUHARRAM) {
            return ASHURA;
        } else if (hijri.day == 11 && hijri.month == RABIAL_AWWAL) {
            return MAWLID_AL_NABI;
        } else if (hijri.day == 1 && hijri.month == RAJAB) {
            return THREE_MONTHS;
        } else if ((tmp = fromGreg(context, getLocalDate().plusDays(1))).getLocalDate().getWeekyear() == DateTimeConstants.FRIDAY
                && tmp.hijri.day <= 7 && tmp.hijri.month == RAJAB) {//we need this, because it might be also the last night of the previous night


            return RAGAIB;
        } else if (hijri.day == 26 && hijri.month == RAJAB) {
            return MIRAJ;
        } else if (hijri.day == 14 && hijri.month == SHABAN) {
            return BARAAH;
        } else if (hijri.day == 1 && hijri.month == RAMADAN) {
            return RAMADAN_BEGIN;
        } else if (hijri.day == 26 && hijri.month == RAMADAN) {
            return LAYLATALQADR;
        } else if ((tmp = fromGreg(context, getLocalDate().plusDays(1))).getMonth() == SHAWWAL && tmp.getDay() == 1) {
            return LAST_RAMADAN;
        } else if (hijri.day == 1 && hijri.month == SHAWWAL) {
            return EID_AL_FITR_DAY1;
        } else if (hijri.day == 2 && hijri.month == SHAWWAL) {
            return EID_AL_FITR_DAY2;
        } else if (hijri.day == 3 && hijri.month == SHAWWAL) {
            return EID_AL_FITR_DAY3;
        } else if (hijri.day == 9 && hijri.month == DHUL_HIJJA) {
            return ARAFAT;
        } else if (hijri.day == 10 && hijri.month == DHUL_HIJJA) {
            return EID_AL_ADHA_DAY1;
        } else if (hijri.day == 11 && hijri.month == DHUL_HIJJA) {
            return EID_AL_ADHA_DAY2;
        } else if (hijri.day == 12 && hijri.month == DHUL_HIJJA) {
            return EID_AL_ADHA_DAY3;
        } else if (hijri.day == 13 && hijri.month == DHUL_HIJJA) {
            return EID_AL_ADHA_DAY4;
        }

        return 0;
    }

    public static HijriDate now(Context context) {
        return fromGreg(context, LocalDate.now());
    }


    private static class Hijri extends DateIntern<Hijri> {
        Hijri(int year, int month, int day) {
            super(year, month, day);
        }
    }


    private static class Greg extends DateIntern<Hijri> {
        Greg(int year, int month, int day) {
            super(year, month, day);
        }
    }

    private abstract static class DateIntern<K extends DateIntern> implements Comparable<K> {
        final int year;
        final int month;
        final int day;

        public DateIntern(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        @Override
        public int compareTo(@NonNull K o) {
            int x = hashCode();
            int y = o.hashCode();
            return Integer.valueOf(x).compareTo(Integer.valueOf(y));
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DateIntern))
                return false;
            if (!getClass().equals(o.getClass()))
                return false;
            final DateIntern other = (DateIntern) o;

            return other.hashCode() == hashCode();
        }

        public int hashCode() {
            return year * 10000 + month * 100 + day;
        }

    }


    @Override
    public int hashCode() {
        return hijri.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof HijriDate))
            return false;
        return hijri.hashCode() == ((HijriDate) obj).hijri.hashCode();
    }

    public int getMinGregYear() {
        return MIN_GREG_YEAR;
    }

    public int getMaxGregYear() {
        return MAX_GREG_YEAR;
    }

    public int getMinHijriYear() {
        return MIN_HIJRI_YEAR;
    }

    public int getMaxHijriYear() {
        return MAX_HIJRI_YEAR;
    }

}

