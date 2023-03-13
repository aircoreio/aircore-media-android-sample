package com.example.aircoremediasample.ui.main

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.aircoremediasample.R
import io.aircore.media.Channel
import io.aircore.media.ChannelParams
import io.aircore.media.Engine
import io.aircore.media.LocalStream
import io.aircore.media.LocalStreamParams
import io.aircore.media.RemoteStream

class MainViewModel(
  private var mActivity: Activity
) : ViewModel() {
  private var mStreamArrayList: ArrayList<RemoteStreamView> = ArrayList()
  private var mAdapter: RemoteStreamAdapter = RemoteStreamAdapter(mActivity, mStreamArrayList)
  private var mChannel: Channel? = null
  private val mRemoteStreams: HashMap<RemoteStream, RemoteStreamView> = HashMap()
  private var mLocalStreamParams: LocalStreamParams? = null
  private var mPak = ""
  private var mUserId = ""
  private var mChannelId = ""
  var mLocalStream: LocalStream? = null
  var mIsPublishing: Boolean = false

  // Create a channel using the Aircore SDK Engine, initialized with the context passed in
  fun createChannel(context: Context): Channel? {
    val engine: Engine = Engine.getInstance(
      context,
      "AircoreMediaSample",
      "Stream is running",
      R.mipmap.ic_launcher
    )
    // Return no new channel if one already exists
    if (mChannel != null) {
      Log.d(TAG, "Existing channel is $mChannel")
      return null
    }
    // Creates channel with default parameters
    mChannel = engine.createChannel(makeChannelParams())
    return if (mChannel != null) {
      Log.d(TAG, "Created channel: $mChannel")
      mChannel
    } else {
      Log.e(
        TAG,
        "Failed to create Channel with PAK $mPak userID $mUserId channelID $mChannelId"
      )
      null
    }
  }

  // Add remoteStream to adapter, which then creates a UI element for it
  fun addRemoteStream(stream: RemoteStream) {
    mActivity.runOnUiThread {
      val wrapper = RemoteStreamView(stream)
      mRemoteStreams[stream] = wrapper
      mStreamArrayList.add(wrapper)
      mAdapter.notifyDataSetChanged()
    }
  }

  // Remove remoteStream from adapter, which then destroys the UI element for it
  fun removeRemoteStream(stream: RemoteStream) {
    mActivity.runOnUiThread {
      val wrapper: RemoteStreamView? = mRemoteStreams.remove(stream)
      if (wrapper != null) {
        mStreamArrayList.remove(wrapper)
        mAdapter.notifyDataSetChanged()
      }
    }
  }

  // Attaches a Channel Delegate to the channel associated with this object
  fun addChannelDelegate(delegate: Channel.Delegate?) {
    mChannel!!.addDelegate(delegate)
  }

  fun getRemoteStreamAdaptor(): RemoteStreamAdapter {
    return mAdapter
  }

    /* Notifies adapter that its data set should be cleared. This is called when the channel reaches
     the terminated state
     */
  fun clearStreamArray() {
    mRemoteStreams.clear()
    mStreamArrayList.clear()
    mAdapter.notifyDataSetChanged()
  }

  // Create a LocalStream using the existing channel
  fun createLocalStream(): LocalStream? {
    if (mChannel == null) {
      Log.e(TAG, "No Channel to create LocalStream")
      return null
    }
    if (mLocalStreamParams == null) {
      // Create the default LocalStream parameters
      mLocalStreamParams = LocalStreamParams.Builder().build()
    }
    return mChannel!!.createLocalStream(mLocalStreamParams)
  }

  // Create channel parameters with Audio publication enabled
  private fun makeChannelParams(): ChannelParams? {
    return ChannelParams.Builder(mPak, mUserId, mChannelId).setAllowPublishAudio(true)
      .build()
  }

  fun getChannel(): Channel? {
    return mChannel
  }

  // Cleans up existing channel and any attached LocalStream
  fun leaveChannel() {
    if (mLocalStream !== null) { mLocalStream!!.stop() }
    mChannel!!.leave()
    mChannel = null
    clearStreamArray()
  }

  fun setPakVars(_pak: String, _userID: String, _channelId: String) {
    mPak = _pak
    mUserId = _userID
    mChannelId = _channelId
  }

  companion object {
    private const val TAG = "Aircore Media Sample App ViewModel"
  }
}
