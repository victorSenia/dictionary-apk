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
import android.view.KeyEvent;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
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
    public static final String ACTION_NEXT = "org.leo.dictionary.apk.action.NEXT";
    public static final String ACTION_PREVIOUS = "org.leo.dictionary.apk.action.PREVIOUS";
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
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        PlaybackStateCompat state = new PlaybackStateCompat.Builder().
                setActions(PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).
                setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f).build();
        mediaSession.setActive(true);
        mediaSession.setPlaybackState(state);

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder().
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.app_name)).build();
        mediaSession.setMetadata(metadata);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onSkipToNext() {
                playServiceAdapter.next();
            }

            @Override
            public void onSkipToPrevious() {
                playServiceAdapter.previous();
            }

            @Override
            public void onPause() {
                stopPlayService();
            }

            @Override
            public void onStop() {
                stopPlayService();
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent == null || keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                    return super.onMediaButtonEvent(mediaButtonEvent);
                }
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        onSkipToNext();
                        return true;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        onSkipToPrevious();
                        return true;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        onPause();
                        return true;
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (playServiceAdapter.isPlaying()) {
                            onPause();
                        } else {
                            playServiceAdapter.play();
                        }
                        return true;
                    default:
                        return super.onMediaButtonEvent(mediaButtonEvent);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_SERVICE.equals(action)) {
                stopPlayService();
                return START_NOT_STICKY;
            } else if (ACTION_NEXT.equals(action)) {
                playServiceAdapter.next();
            } else if (ACTION_PREVIOUS.equals(action)) {
                playServiceAdapter.previous();
            }
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

    private void ensurePlaybackStopped() {
        playServiceAdapter.pausePlayService();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        ensurePlaybackStopped();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        ensurePlaybackStopped();
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

        NotificationCompat.Action previousAction = createAction(android.R.drawable.ic_media_previous, ACTION_PREVIOUS);
        NotificationCompat.Action stopAction = createAction(android.R.drawable.ic_media_pause, ACTION_STOP_SERVICE);
        NotificationCompat.Action nextAction = createAction(android.R.drawable.ic_media_next, ACTION_NEXT);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentIntent(pendingIntent)  // <-- opens the app
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(previousAction)
                .addAction(stopAction)
                .addAction(nextAction)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private NotificationCompat.Action createAction(int icon, String action) {
        CharSequence title;
        if (ACTION_PREVIOUS.equals(action)) {
            title = getText(R.string.previous);
        } else if (ACTION_NEXT.equals(action)) {
            title = getText(R.string.next);
        } else {
            title = getText(R.string.pause);
        }
        Intent stopIntent = new Intent(this, PlayServiceService.class);
        stopIntent.setAction(action);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Action(icon, title, stopPendingIntent);
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
