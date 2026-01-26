package org.leo.dictionary.apk.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;

import androidx.media.session.MediaButtonReceiver;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.MainActivity;

import javax.inject.Inject;

public class PlayServiceService extends Service {

    private static final String CHANNEL_ID = "PlayServiceChannel";
    @Inject
    PlayServiceAdapter playServiceAdapter;

    private MediaSessionCompat mediaSession;

    public static final String ACTION_STOP_SERVICE = "org.leo.dictionary.apk.action.STOP_SERVICE";
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public PlayServiceService getService() {
            return PlayServiceService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((ApplicationWithDI) getApplication()).appComponent.inject(this);
        setupMediaSession();
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, CHANNEL_ID);
        PlaybackStateCompat state = new PlaybackStateCompat.Builder().
                setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE).
                setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f).build();
        mediaSession.setActive(true);
        mediaSession.setPlaybackState(state);

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder().
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.app_name)).build();
        mediaSession.setMediaButtonReceiver(
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
        );
        mediaSession.setMetadata(metadata);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                stopPlayService();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopPlayService();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        startForeground(1, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        return START_STICKY;
    }

    private void stopPlayService() {
        playServiceAdapter.pausePlayService();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.setActive(false);
        mediaSession.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Notification buildNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setAction(Intent.ACTION_MAIN);
        openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Action stopAction = createStopAction();
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentIntent(pendingIntent)  // <-- opens the app
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(stopAction)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private NotificationCompat.Action createStopAction() {
//        Intent stopIntent = new Intent(this, PlayServiceService.class);
//        stopIntent.setAction(ACTION_STOP_SERVICE);
//        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
//        return new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent);
        return new NotificationCompat.Action(android.R.drawable.ic_media_pause, getText(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE));
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);

        channel.setDescription("Media playback controls");
        channel.setShowBadge(true);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
