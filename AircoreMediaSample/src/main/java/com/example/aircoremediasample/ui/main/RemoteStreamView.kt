package com.example.aircoremediasample.ui.main

import android.util.Log
import io.aircore.media.RemoteStream

/* The Class for a View contained within the RemoteStreamAdapter. This class sets up the RemoteStream
Delegate functions, which alert the Adapter that then runs UI updates
*/
class RemoteStreamView(var stream: RemoteStream) {
  var mDelegate: Delegate? = null

  interface Delegate {
    fun connectionStateDidChange(newState: RemoteStream.ConnectionState?)
    fun vadStatusChanged(active: Boolean)
  }

  fun setDelegate(delegate: Delegate) {
    mDelegate = delegate
  }

  init {
    // Creates and attaches a delegate to the RemoteStream associated with this object
    stream.addDelegate(object : RemoteStream.Delegate {
      // Alert that the connection state changed and trigger the delegates attached to this object
      override fun connectionStateDidChange(
        stream: RemoteStream,
        newState: RemoteStream.ConnectionState,
        oldState: RemoteStream.ConnectionState
      ) {
        Log.d(
          TAG,
          "Stream " + stream.streamUrl +
            " changed connection state to " + newState + " from " + oldState
        )
        mDelegate?.connectionStateDidChange(newState)
      }

      // Debug function to log local audio mute changes on this object's RemoteStream
      override fun localAudioMuteStateDidChange(muted: Boolean) {
        Log.d(
          TAG,
          "Stream " + stream.streamUrl + " has been " +
            (if (muted) "muted" else "unmuted") + " locally"
        )
      }

      // Debug function to log remote audio mute changes on this object's RemoteStream
      override fun remoteAudioMuteStateDidChange(muted: Boolean) {
        Log.d(
          TAG,
          "Stream " + stream.streamUrl + " has been " +
            (if (muted) "muted" else "unmuted") + " remotely"
        )
      }

      // Alert that Voice Activity state changed and trigger the delegates attached to this object
      override fun voiceActivityStateDidChange(isActive: Boolean) {
        if (mDelegate != null) mDelegate!!.vadStatusChanged(isActive)
      }
    })
  }

  companion object {
    private const val TAG = "Aircore Media Sample App RemotestreamView"
  }
}
