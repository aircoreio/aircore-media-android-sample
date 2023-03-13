package com.example.aircoremediasample.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.aircoremediasample.R
import com.example.aircoremediasample.databinding.FragmentMainBinding
import com.google.android.material.snackbar.Snackbar
import io.aircore.media.Channel
import io.aircore.media.Channel.JoinState
import io.aircore.media.Engine
import io.aircore.media.LocalStream
import io.aircore.media.RemoteStream
import java.util.*

class MainFragment : Fragment() {

  companion object {
    fun newInstance() = MainFragment()
  }

  private lateinit var viewModel: MainViewModel
  private var recordAudioPermission = 1
  private var _binding: FragmentMainBinding? = null
  private val binding get() = _binding!!

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
        /* initializes the MainViewModel which will hold the active Channel, as well as the
        LocalStream if it is created, and a boolean to track whether the LocalStream is currently
        publishing. The MainViewModel also has an adapter for the ListView which holds UI elements
        for each active RemoteStream of the channel; this is notified by the Channel delegate
        created on joining a Channel.
         */
    viewModel = MainViewModel(requireActivity())
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentMainBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.sdkVersionText.append(getString(R.string.aircore_sdk_version) + Engine.getVersion())
    setupPakUi()
    setupEmptyChannelUi()
  }

  // Buttons are set up to alert then do nothing while a channel is not joined
  private fun setupEmptyChannelUi() {
    binding.leaveChannelBtn.setOnClickListener {
      alert("Need to join a channel first", Snackbar.LENGTH_SHORT)
    }
    binding.publishButton.setOnClickListener {
      alert("Need to join a channel first", Snackbar.LENGTH_SHORT)
    }
    binding.localMuteButton.setOnClickListener {
      alert("Need to join a channel first", Snackbar.LENGTH_SHORT)
    }
  }

  // Sets up UI elements to enable joining a channel
  private fun setupPakUi() {
    val pakInput = binding.pakInputText
    val userInput = binding.userIdInput
    val channelInput = binding.channelIdInput
    // Attempt to join a channel using provided credentials and settings
    binding.joinChannelButton.setOnClickListener {
      val pak = pakInput.text.toString()
      val userId = userInput.text.toString()
      val channelId = channelInput.text.toString()
      if (pak.isEmpty() || userId.isEmpty() || channelId.isEmpty()) {
        alert("All fields need to be set", Snackbar.LENGTH_SHORT)
        return@setOnClickListener
      }
      viewModel.setPakVars(pak, userId, channelId)
      // Requires a context and uses it to create a channel
      val channel: Channel? = context?.let { viewModel.createChannel(it) }
      if (channel == null) {
        alert("Unexpected error creating new channel", Snackbar.LENGTH_SHORT)
        return@setOnClickListener
      }
      // Joins channel and set up the UI for channel functions
      channel.join()
      activity?.runOnUiThread {
        binding.channelId.text = channelId
      }
      setupChannelStatusUi()
      setupLocalstreamUi()
      setupRemoteStreamUI()
    }
  }

  private fun setupChannelStatusUi() {
    // Creates a Channel Delegate to set up callbacks on Channel events
    val joinStateDelegate: Channel.Delegate = object : Channel.Delegate {
      // notifies the viewModel when a new remoteStream is added
      override fun remoteStreamWasAdded(remoteStream: RemoteStream) {
        viewModel.addRemoteStream(remoteStream)
      }

      // notifies the viewModel when a remoteStream is removed
      override fun remoteStreamWasRemoved(
        remoteStream: RemoteStream,
        cause: RemoteStream.TerminationCause
      ) {
        viewModel.removeRemoteStream(remoteStream)
        alert(
          "Remote Stream removed; ID: " + remoteStream.userID +
            "; cause: " + cause,
          Snackbar.LENGTH_SHORT
        )
      }

      // Updates UI when a localStream is added to this Channel
      override fun localStreamWasAdded(stream: LocalStream) {
        viewModel.mIsPublishing = true
        viewModel.mLocalStream = stream
        activity?.runOnUiThread {
          updatePublishButton()
          updateLocalStreamTerminationCauseText(stream.terminationCause.toString())
          updateLocalStreamConnectionStateText(stream.connectionState.toString())
        }
      }

      // Updates the UI when localStream is removed from this Channel
      override fun localStreamWasRemoved(
        localStream: LocalStream,
        cause: LocalStream.TerminationCause
      ) {
        viewModel.mIsPublishing = false
        activity?.runOnUiThread {
          updatePublishButton()
          updateLocalStreamTerminationCauseText(cause.toString())
          updateLocalStreamConnectionStateText(localStream.connectionState.toString())
        }
      }

            /* refresh state labels upon a joinState change. Clears the remoteStreams if the new
            state is terminated
            */
      override fun joinStateDidChange(newJoinState: JoinState, oldJoinState: JoinState) {
        val publishButton = binding.publishButton
        alert(
          "Channel JoinState did change from $oldJoinState to $newJoinState",
          Snackbar.LENGTH_SHORT
        )
        if (newJoinState == JoinState.TERMINATED) {
          viewModel.mIsPublishing = false
          viewModel.clearStreamArray()
          activity?.runOnUiThread {
            publishButton.isEnabled = false
          }
        }
        if (newJoinState == JoinState.JOINED) {
          activity?.runOnUiThread {
            publishButton.isEnabled = true
          }
        }
        refreshStateLabels()
      }

            /* Sample app does not allow for usage of a Session Auth Token, only a Publishable API
            Key which does not come with an expiration time. This delegate function needs to be
            overridden but will do nothing since it will not be called
             */
      override fun sessionAuthTokenNearingExpiry(expiryTime: Date) {}
    }
    // Attaches the Delegate to the Channel in the ViewModel
    viewModel.addChannelDelegate(joinStateDelegate)
    // Sets up button to leave the channel
    binding.leaveChannelBtn.setOnClickListener {
      if (viewModel.getChannel() != null) {
        viewModel.leaveChannel()
        activity?.runOnUiThread {
          binding.channelId.text = ""
        }
      } else {
        alert("could not find channel to leave", Snackbar.LENGTH_SHORT)
      }
    }
    refreshStateLabels()
    binding.publishButton.isEnabled = viewModel.getChannel()?.joinState == JoinState.JOINED
  }

  // Enables buttons for publish and mute of a LocalStream
  private fun setupLocalstreamUi() {
    // Begin publish if not currently publishing; otherwise, stops the current publish
    binding.publishButton.setOnClickListener {
      if (viewModel.mIsPublishing) {
        viewModel.mLocalStream!!.stop()
        return@setOnClickListener
      }
      requestMicPermissionsThenPublish()
    }
    // Toggles the mute state of the existing LocalStream
    binding.localMuteButton.setOnClickListener {
      val muted = viewModel.mLocalStream!!.audioMuted
      viewModel.mLocalStream!!.muteAudio(!muted)
    }
  }

  // Attaches the stream adapter from the viewModel to the list view, enabling creation of
  // remoteStream UI blocks. Adding and removing RemoteStreams is handled by the channel delegate
  private fun setupRemoteStreamUI() {
    val mListView = binding.streamListView
    mListView.adapter = viewModel.getRemoteStreamAdaptor()
  }

  // updates state labels in the UI
  private fun refreshStateLabels() {
    val channel: Channel? = viewModel.getChannel()
    val joinState: Channel.JoinState
    val terminationCause: Channel.TerminationCause
    // Channel is null if the channel was terminated intentionally, so the cause will be STOPPED
    if (channel == null) {
      joinState = Channel.JoinState.TERMINATED
      terminationCause = Channel.TerminationCause.STOPPED
    } else {
      // Otherwise, the Channel state is reported by the existing Channel
      joinState = channel.joinState
      terminationCause = channel.terminationCause
    }
    activity?.runOnUiThread {
      val joinStateText = binding.joinState
      joinStateText.text = joinState.toString()
      val terminationCauseText = binding.terminationCause
      terminationCauseText.text = terminationCause.toString()
      if (joinState == JoinState.TERMINATED || joinState == JoinState.NOT_JOINED) {
        updatePublishButton()
      }
    }
  }

  private fun alert(message: String, length: Int) {
    Snackbar.make(binding.root, message, length).setAction("Action", null).show()
  }

  private fun updatePublishButton() {
    binding.publishButton.text = if (viewModel.mIsPublishing) "Stop" else "Publish"
    binding.localMuteButton.isEnabled = viewModel.mIsPublishing
  }

  private fun updateLocalStreamTerminationCauseText(cause: String) {
    binding.localStreamTerminationCause.text = cause
  }

  private fun updateLocalStreamConnectionStateText(state: String) {
    binding.localStreamConnectionState.text = state
  }

  private fun requestMicPermissionsThenPublish() {
    if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO)
      != PackageManager.PERMISSION_GRANTED
    ) {
      // When permission is not granted by user, show them message why this permission is needed.
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          requireActivity(),
          Manifest.permission.RECORD_AUDIO
        )
      ) {
        alert("Please enable microphone permissions", Snackbar.LENGTH_SHORT)
        ActivityCompat.requestPermissions(
          requireActivity(),
          arrayOf(Manifest.permission.RECORD_AUDIO),
          recordAudioPermission
        )
      } else {
        // Show user dialog to grant permission to record audio
        ActivityCompat.requestPermissions(
          requireActivity(),
          arrayOf(Manifest.permission.RECORD_AUDIO),
          recordAudioPermission
        )
      }
    } else if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO)
      == PackageManager.PERMISSION_GRANTED
    ) {
      // Go ahead with recording audio now
      beginLocalStreamPublish()
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      recordAudioPermission -> {
        if (grantResults.isNotEmpty() &&
          grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
          // permission was granted, start publish
          alert("Permission granted. Try publish again", Snackbar.LENGTH_LONG)
        } else {
          // permission denied. Disable the functionality that depends on this permission.
          alert("Error. Permissions Denied to record audio", Snackbar.LENGTH_LONG)
        }
        return
      }
    }
  }

  private fun beginLocalStreamPublish() {
    // Creates a delegate for the LocalStream to update UI elements to reflect its state
    val delegate: LocalStream.Delegate = object : LocalStream.Delegate {
      // Update the mute button label if the mute state changes
      override fun audioMuteStateDidChange(muted: Boolean) {
        activity?.runOnUiThread {
          binding.localMuteButton.text =
            if (muted) getString(R.string.unmute) else getString(R.string.mute)
        }
      }

      // Enable/Disable the voice activity indicator when state change is reported
      override fun voiceActivityStateDidChange(isActive: Boolean) {
        activity?.runOnUiThread {
          binding.localVadIndicator.visibility = if (isActive) View.VISIBLE else View.INVISIBLE
        }
      }

      // Update the connection state text when a state change is reported
      override fun connectionStateDidChange(
        newState: LocalStream.ConnectionState,
        oldState: LocalStream.ConnectionState
      ) {
        activity?.runOnUiThread {
          updateLocalStreamConnectionStateText(newState.toString())
        }
      }
    }
    val newLocalStream = viewModel.createLocalStream()
    if (newLocalStream != null) {
      viewModel.mLocalStream = newLocalStream
      newLocalStream.addDelegate(delegate)
      newLocalStream.start()
      binding.localMuteButton.text = if (newLocalStream.audioMuted) {
        getString(R.string.unmute)
      } else {
        getString(
          R.string.mute
        )
      }
    } else {
      alert("Error creating localStream", Snackbar.LENGTH_SHORT)
    }
  }
}
