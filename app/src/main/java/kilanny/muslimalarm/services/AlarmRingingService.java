package kilanny.muslimalarm.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.util.Pair;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONException;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import kilanny.muslimalarm.R;
import kilanny.muslimalarm.activities.ShowAlarmActivity;
import kilanny.muslimalarm.data.Alarm;
import kilanny.muslimalarm.data.AlarmDao;
import kilanny.muslimalarm.data.AppDb;
import kilanny.muslimalarm.data.Tune;
import kilanny.muslimalarm.data.Weekday;
import kilanny.muslimalarm.util.AnalyticsTrackers;
import kilanny.muslimalarm.util.Utils;

public class AlarmRingingService extends Service {

    public static final String ARG_IS_PREVIEW = "isPreview";
    public static final String ARG_ALARM = "alarm";
    public static final String ARG_ALARM_TIME = "mAlarmTime";
    private static final String CHANNEL_ID = "kilanny.muslimalarm.services.AlarmRingingService";

    private static final int NOTIFICATION_ID = 1441;
    public static final int MAX_SECONDS_ATTEMPT = 60;

    private final IBinder binder = new LocalBinder();
    private Alarm mAlarm;
    private int mAlarmTime;
    private boolean mIsPreview;
    private boolean mIsVibrating;
    private Timer mDismissTimer;
    private MediaPlayer mediaPlayer;
    private Vibrator mVibrator;
    private int mOldUserSoundVolume = 0;
    private AudioManager mAudioManager;
    private Date startDate;
    private Handler mHandler;
    private int mCountDownProgress;
    private int mCountAttemptDismiss = 0;
    public Function<Integer, Void> onCountDownChanged;

    public AlarmRingingService() {
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId) {
        String channelName = "Muslim Prayer Alarm Ringing Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
        return channelId;
    }

    private Notification initNotification(String alarmLabel, String channelId) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                1,
                getStartActivityIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this,
                    createNotificationChannel(channelId));
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.alarm_notification_statusbar);
        remoteViews.setTextViewText(R.id.alarmTime, Utils.getTimeName(this, mAlarmTime));
        remoteViews.setTextViewText(R.id.alarmLabel, alarmLabel);

        return notificationBuilder.setContentIntent(pendingIntent)
                .setContent(remoteViews)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(pendingIntent, true)
                .build();
    }

    private Intent getStartActivityIntent() {
        Intent intent = new Intent(this, ShowAlarmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra(ShowAlarmActivity.ARG_ALARM, mAlarm);
        intent.putExtra(ShowAlarmActivity.ARG_ALARM_TIME, mAlarmTime);
        intent.putExtra(ShowAlarmActivity.ARG_IS_PREVIEW, mIsPreview);
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //boolean redelivery = (flags & START_FLAG_REDELIVERY) != 0;
        //boolean retry = (flags & START_FLAG_RETRY) != 0;
//        if (isOneInstanceRunning) {
//            // snooze -> click to cancel snooze -> alarmManager fires another service for snoozed
//            startForeground(123, initNotification("", "tmp"));
//            stopSelf();
//            return START_NOT_STICKY;
//        }
        mIsPreview = intent.getBooleanExtra(ARG_IS_PREVIEW, false);
        mAlarm = intent.getParcelableExtra(ARG_ALARM);
        if (mAlarm == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String json = pref.getString("nextAlarmJson", null);
            mAlarmTime = pref.getInt("nextAlarmTime", 0);
            if (json != null) {
                try {
                    mAlarm = Alarm.fromJson(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (mAlarm == null)
                throw new RuntimeException("An alarm must be passed to this service");
        } else {
            mAlarmTime = intent.getIntExtra(ARG_ALARM_TIME, 0);
        }
        mHandler = new Handler();
        startForeground(NOTIFICATION_ID, initNotification(mAlarm.alarmLabel, CHANNEL_ID));

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mOldUserSoundVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int level = (int) Math.round((mAlarm.soundLevel / 100.0) * maxVolume);
        try {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
        mediaPlayer = MediaPlayer.create(this,
                Tune.findTuneOrDefault(mAlarm.alarmTune, 1).rawResId);
        mediaPlayer.setLooping(true);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        startRinging();
        startDate = new Date();
        AnalyticsTrackers.getInstance(this).logAlarmRing(mAlarm);

        //startActivity(getStartActivityIntent());
        return START_REDELIVER_INTENT;
    }

    private void stopRinging() {
        if (mIsVibrating) {
            mVibrator.cancel();
            mIsVibrating = false;
        }
        if (mediaPlayer.isPlaying()) { // stop preview while solving math problems
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }
    }

    private void startRinging() {
        if (!mediaPlayer.isPlaying())
            mediaPlayer.start();
        if (mAlarm.vibrationEnabled && mVibrator != null && !mIsVibrating) {
            Log.v("showAlarm", "Vibrating...");
            //TODO: permission for vibration; not working on redmi 4a
            mVibrator.vibrate(
                    new long[]{5000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000}, 0);
            mIsVibrating = true;
        }
    }

    public void resetCountDown() {
        mCountDownProgress = MAX_SECONDS_ATTEMPT;
    }

    public boolean onAttemptingDismissAlarm() {
        ++mCountAttemptDismiss;
        boolean silent = mCountAttemptDismiss <= 3;
        if (silent)
            stopRinging();
        cancelAttemptingDismissAlarm(false);
        mDismissTimer = new Timer();
        resetCountDown();
        mDismissTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mCountDownProgress > 0)
                    --mCountDownProgress;
                else {
                    mDismissTimer.cancel();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startRinging();
                        }
                    });
                }
                if (onCountDownChanged != null)
                    onCountDownChanged.apply(mCountDownProgress);
            }
        }, 1000, 1000);
        return silent;
    }

    public void cancelAttemptingDismissAlarm(boolean shouldStartRinging) {
        if (mDismissTimer != null) {
            mDismissTimer.cancel();
            mDismissTimer = null;
            if (shouldStartRinging)
                startRinging();
        }
    }

    public void onSnoozeAlarm() {
        if (!mIsPreview) {
            mAlarm.snoozedToTime = System.currentTimeMillis() + mAlarm.snoozeMins * 60000;
            ++mAlarm.snoozedCount;

            Utils.runInBackground(new Function<Pair<Context, Alarm>, Pair<Context, Alarm[]>>() {
                @Override
                public Pair<Context, Alarm[]> apply(Pair<Context, Alarm> input) {
                    AlarmDao alarmDao = AppDb.getInstance(input.first).alarmDao();
                    alarmDao.update(input.second);
                    return new Pair<>(input.first, alarmDao.getAll());
                }
            }, new Function<Pair<Context, Alarm[]>, Void>() {
                @Override
                public Void apply(Pair<Context, Alarm[]> input) {
                    Utils.scheduleAndDeletePrevious(input.first, input.second);
                    return null;
                }
            }, new Pair<>(getApplicationContext(), mAlarm));
        }
    }

    public void onDismissed(final boolean isDone) {
        if (mDismissTimer != null) {
            mDismissTimer.cancel();
            mDismissTimer = null;
        }
        stopRinging();
        try {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOldUserSoundVolume, 0);
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
        mOldUserSoundVolume = 0;
        mediaPlayer.release();
        mediaPlayer = null;
        int mins = (int) (System.currentTimeMillis() - startDate.getTime()) / 60000;
        if (mIsPreview) {
            AnalyticsTrackers.getInstance(this).logAlarmPreviewed(mAlarm);
        } else if (!isDone) {
            AnalyticsTrackers.getInstance(this).logAlarmSnoozed(mAlarm, mins);
        } else {
            AnalyticsTrackers.getInstance(this).logAlarmDismissed(mAlarm, mins);
        }
        Utils.runInBackground(new Function<Pair<Context, Alarm>, Pair<Context, Alarm[]>>() {
            @Override
            public Pair<Context, Alarm[]> apply(Pair<Context, Alarm> input) {
                AlarmDao alarmDao = AppDb.getInstance(input.first).alarmDao();
                input.second.skippedTimeFlag = 0;
                input.second.skippedAlarmTime = null;
                if (isDone) {
                    input.second.snoozedCount = 0;
                    input.second.snoozedToTime = null;

                    if (input.second.weekDayFlags == Weekday.NO_REPEAT) {
                        input.second.oneTimeLeftAlarmsTimeFlags &= ~mAlarmTime;
                        if (input.second.oneTimeLeftAlarmsTimeFlags == 0) { // done!
                            input.second.enabled = false;
                        }
                    }
                }
                alarmDao.update(input.second);
                return new Pair<>(input.first, alarmDao.getAll());
            }
        }, new Function<Pair<Context, Alarm[]>, Void>() {
            @Override
            public Void apply(Pair<Context, Alarm[]> input) {
                Utils.scheduleAndDeletePrevious(input.first, input.second);
                stopSelf();
                return null;
            }
        }, new Pair<>(getApplicationContext(), mAlarm));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        public AlarmRingingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AlarmRingingService.this;
        }
    }
}