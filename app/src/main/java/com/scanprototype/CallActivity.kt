package com.scanprototype

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.scanprototype.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallBinding
    private val handler = Handler(Looper.getMainLooper())
    private var callState = CallState.DIALING
    private var muteEnabled = false
    private var speakerEnabled = false
    private var connectedSeconds = 0
    private val stateRunnables = mutableListOf<Runnable>()

    private enum class CallState {
        DIALING,
        RINGING,
        CONNECTED,
        ENDED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER).orEmpty()
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty()

        binding.textCallerName.text = if (callerName.isBlank()) {
            callerNumber.ifEmpty { "Unknown Number" }
        } else {
            callerName
        }

        binding.buttonKeypad.setOnClickListener { showKeypadOverlay() }
        binding.buttonMute.setOnClickListener { muteEnabled = !muteEnabled; updateAudioButtons() }
        binding.buttonSpeaker.setOnClickListener { speakerEnabled = !speakerEnabled; updateAudioButtons() }
        binding.buttonEndCall.setOnClickListener { endCall() }
        binding.buttonKeypadClose.setOnClickListener { hideKeypadOverlay() }

        binding.keypadOverlay.setOnClickListener { hideKeypadOverlay() }
        startCallSimulation()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun startCallSimulation() {
        setCallState(CallState.DIALING)
        scheduleStateTransition(2000) {
            setCallState(CallState.RINGING)
        }
        scheduleStateTransition(4500) {
            setCallState(CallState.CONNECTED)
        }
    }

    private fun scheduleStateTransition(delayMs: Long, action: () -> Unit) {
        val runnable = Runnable { action() }
        stateRunnables.add(runnable)
        handler.postDelayed(runnable, delayMs)
    }

    private fun setCallState(state: CallState) {
        callState = state
        when (state) {
            CallState.DIALING -> {
                binding.textCallState.text = "Dialing..."
                binding.textCallTimer.text = "00:00"
            }
            CallState.RINGING -> {
                binding.textCallState.text = "Ringing..."
                binding.textCallTimer.text = "00:00"
            }
            CallState.CONNECTED -> {
                binding.textCallState.text = "Connected"
                binding.textCallTimer.text = formatSeconds(0)
                connectedSeconds = 0
                startCallTimer()
            }
            CallState.ENDED -> {
                binding.textCallState.text = "Call ended"
                binding.textCallTimer.visibility = View.VISIBLE
                stopCallTimer()
            }
        }
        binding.textCallState.alpha = 0f
        binding.textCallState.animate().alpha(1f).setDuration(280).start()
    }

    private fun updateAudioButtons() {
        binding.buttonMute.alpha = if (muteEnabled) 1f else 0.85f
        binding.buttonSpeaker.alpha = if (speakerEnabled) 1f else 0.85f
    }

    private fun startCallTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun stopCallTimer() {
        handler.removeCallbacks(timerRunnable)
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            connectedSeconds += 1
            binding.textCallTimer.text = formatSeconds(connectedSeconds)
            handler.postDelayed(this, 1000)
        }
    }

    private fun formatSeconds(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun showKeypadOverlay() {
        binding.keypadOverlay.visibility = View.VISIBLE
    }

    private fun hideKeypadOverlay() {
        binding.keypadOverlay.visibility = View.GONE
    }

    private fun showMoreOptions() {
        val options = arrayOf("Add to contacts", "Send message", "Call settings")
        AlertDialog.Builder(this)
            .setTitle("More options")
            .setItems(options) { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun endCall() {
        setCallState(CallState.ENDED)
        stopCallTimer()
        handler.postDelayed({ finish() }, 250)
    }

    companion object {
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
    }
}
