package com.example.aircoremediasample.ui.main

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.aircoremediasample.R
import io.aircore.media.RemoteStream

// Adapter for the List View that holds active RemoteStreams, creates a UI element for each one
class RemoteStreamAdapter(
  private val mActivity: Activity,
  streams: ArrayList<RemoteStreamView>
) : ArrayAdapter<RemoteStreamView>(mActivity, 0, streams) {
  // Massive function to create a new view for a given RemoteStream, setting up the UI elements
  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    // Determine if this is converting an existing view or creating a new one
    var newView: View? = convertView
    val streamWrapper: RemoteStreamView? = getItem(position)
    // In normal operation, getItem should never return null. It is a fatal error if it does
    val stream: RemoteStream = streamWrapper!!.stream
    val holder: ViewHolder
    if (convertView == null) {
      newView = LayoutInflater.from(context)
        .inflate(R.layout.remotestream_layout, parent, false)
      holder = ViewHolder()
      holder.userId = newView.findViewById<TextView>(R.id.remoteStreamUserId)
      holder.streamUrl = newView.findViewById<TextView>(R.id.remoteStreamUrl)
      holder.connectionState =
        newView.findViewById<TextView>(R.id.remoteStreamConnectionState)
      holder.muteButton = newView.findViewById<Button>(R.id.remoteStreamMuteButton)
      holder.muteStateButton = newView.findViewById<Button>(R.id.queryMuteStateBtn)
      holder.vadIndicator = newView.findViewById<ImageView>(R.id.vadIndicator)
      newView.tag = holder
    } else {
      holder = newView!!.tag as ViewHolder
    }
    /* Sets a delegate on the new RemoteStreamView, which will alert when its internal RemoteStream
    Delegate functions occur
     */
    streamWrapper.setDelegate(object : RemoteStreamView.Delegate {
      override fun connectionStateDidChange(newState: RemoteStream.ConnectionState?) {
        if (stream.streamUrl !== (holder.streamUrl?.text ?: "")) return
        mActivity.runOnUiThread { holder.connectionState?.text = newState.toString() }
      }

      override fun vadStatusChanged(active: Boolean) {
        if (stream.streamUrl !== (holder.streamUrl?.text ?: "")) return
        mActivity.runOnUiThread { holder.vadIndicator?.visibility = if (active) View.VISIBLE else View.INVISIBLE }
      }
    })
    holder.vadIndicator?.visibility = if (stream.hasVoiceActivity()) View.VISIBLE else View.INVISIBLE
    holder.userId?.text = stream.userID
    holder.streamUrl?.text = stream.streamUrl
    holder.connectionState?.text = stream.connectionState.toString()
    val muted = stream.localAudioMuted
    holder.muteButton?.text = if (muted) {
      mActivity.getString(R.string.unmute)
    } else {
      mActivity.getString(
        R.string.mute
      )
    }
    holder.muteButton?.setOnClickListener(
      View.OnClickListener {
        val muted1 = stream.localAudioMuted
        stream.muteAudio(!muted1)
        mActivity.runOnUiThread {
          holder.muteButton!!.text = if (!muted1) {
            mActivity.getString(
              R.string.unmute
            )
          } else {
            mActivity.getString(R.string.mute)
          }
        }
      }
    )
    holder.muteStateButton?.setOnClickListener(
      View.OnClickListener {
        val remoteMuted = stream.remoteAudioMuted
        val localMuted = stream.localAudioMuted
        mActivity.runOnUiThread {
          Toast.makeText(
            mActivity.applicationContext,
            "Remote mute: $remoteMuted\nLocal mute: $localMuted",
            Toast.LENGTH_LONG
          ).show()
        }
      }
    )
    notifyDataSetChanged()
    return newView!!
  }

  private class ViewHolder {
    var userId: TextView? = null
    var streamUrl: TextView? = null
    var connectionState: TextView? = null
    var muteButton: Button? = null
    var muteStateButton: Button? = null
    var vadIndicator: ImageView? = null
  }
}
