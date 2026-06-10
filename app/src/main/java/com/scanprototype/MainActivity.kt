package com.scanprototype

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scanprototype.databinding.ActivityMainBinding
import com.scanprototype.scan.CallEvent
import com.scanprototype.scan.executeSimulationPipeline
import com.scanprototype.scan.StorageLayer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hudEnabled = HudSettings.isHudEnabled(this)
        binding.switchHudToggle.isChecked = hudEnabled
        updateHudStatusLabel(hudEnabled)

        binding.switchHudToggle.setOnCheckedChangeListener { _, isChecked ->
            HudSettings.setHudMode(this, if (isChecked) HudMode.ANDROID_CALL_HUD else HudMode.OFF)
            updateHudStatusLabel(isChecked)
        }

        updateLogText()

        binding.buttonSimulate.setOnClickListener {
            onSimulateClicked()
        }
    }

    private fun onSimulateClicked() {
        val rawNumber = binding.editCallerNumber.text.toString().trim()
        if (rawNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a caller number.", Toast.LENGTH_SHORT).show()
            return
        }

        val rawTimestamp = binding.editTimestamp.text.toString().trim()
        val timestamp = if (rawTimestamp.isEmpty()) {
            System.currentTimeMillis()
        } else {
            rawTimestamp.toLongOrNull() ?: run {
                Toast.makeText(this, "Invalid timestamp value.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val cnam = binding.editCnam.text.toString().trim()
        val event = CallEvent(timestamp = timestamp, callerId = rawNumber, cnam = cnam)
        val result = executeSimulationPipeline(event)

        binding.textVerdict.text = "${result.verdict} (score=${result.score})"
        binding.textDetails.text = result.details.joinToString(separator = "\n")
        updateLogText()

        if (HudSettings.isHudEnabled(this)) {
            val intent = Intent(this, CallActivity::class.java).apply {
                putExtra(CallActivity.EXTRA_CALLER_NUMBER, rawNumber)
                putExtra(CallActivity.EXTRA_CALLER_NAME, cnam)
            }
            startActivity(intent)
        }
    }

    private fun updateHudStatusLabel(enabled: Boolean) {
        binding.textHudStatus.text = if (enabled) "HUD: Enabled" else "HUD: Disabled"
    }

    private fun updateLogText() {
        binding.textLog.text = StorageLayer.formatAuditLog()
    }
}
