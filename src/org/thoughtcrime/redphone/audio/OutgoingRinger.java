/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.redphone.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;

import java.io.IOException;

/**
 * Handles loading and playing the sequence of sounds we use to indicate call initialization.
 *
 * @author Stuart O. Anderson
 */
public class OutgoingRinger implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

  private static final String TAG = OutgoingRinger.class.getSimpleName();

  private MediaPlayer    mediaPlayer;
  private int            currentSoundID;
  private Context        context;
  private SettableFuture future;

  public OutgoingRinger(Context context) {
    this.context = context;
  }

  public ListenableFuture playSonar() {
    return start(R.raw.redphone_sonarping, true);
  }

  public ListenableFuture playHandshake() {
    return start(R.raw.redphone_handshake, true);
  }

  public ListenableFuture playRing() {
    return start(R.raw.redphone_outring, true);
  }

  public ListenableFuture playComplete() {
    return start(R.raw.redphone_completed, false);
  }

  public ListenableFuture playFailure() {
    return start(R.raw.redphone_failure, false);
  }

  public ListenableFuture playBusy() {
    return start(R.raw.redphone_busy, true);
  }

  private ListenableFuture start(int soundID, boolean loop) {
    if (soundID == currentSoundID) return future;

    if (mediaPlayer != null) shutdown();

    currentSoundID = soundID;
    future = new SettableFuture();

    mediaPlayer = new MediaPlayer();
    mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
    mediaPlayer.setOnCompletionListener(this);
    mediaPlayer.setOnPreparedListener(this);
    mediaPlayer.setLooping(loop);

    String packageName = context.getPackageName();
    Uri dataUri = Uri.parse("android.resource://" + packageName + "/" + currentSoundID);

    try {
      mediaPlayer.setDataSource(context, dataUri);
      mediaPlayer.prepareAsync();
    } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
      Log.w(TAG, e);
      // TODO Auto-generated catch block
      future.set(false);
    }

    return future;
  }

  public void stop() {
    if( mediaPlayer == null ) return;
    try {
      mediaPlayer.stop();
    } catch( IllegalStateException e ) {
    }
    shutdown();
  }

  public void onCompletion(MediaPlayer mp) {
    if (mp.isLooping()) return;

    shutdown();
  }

  private void shutdown() {
    mediaPlayer.release();
    mediaPlayer = null;
    currentSoundID = -1;
    future.set(true);
  }

  public void onPrepared(MediaPlayer mp) {
    AudioManager am = ServiceUtil.getAudioManager(context);

    if (am.isBluetoothScoAvailableOffCall()) {
      Log.d(TAG, "bluetooth sco is available");
      try {
        am.startBluetoothSco();
      } catch (NullPointerException e) {
        // Lollipop bug (https://stackoverflow.com/questions/26642218/audiomanager-startbluetoothsco-crashes-on-android-lollipop)
      }
    }

    mp.start();
  }
}
