package com.scanprototype

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.View
import com.scanprototype.scan.HeuristicSettings
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

        binding.switchAdvancedSettings.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutAdvancedSettings.visibility =
            if (isChecked) View.VISIBLE else View.GONE
        }

        updateLogText()

        binding.buttonSimulate.setOnClickListener {
            onSimulateClicked()
        }
    }

    private fun onSimulateClicked() {
        HeuristicSettings.timeWeight =
            binding.editWeightTime.text.toString().toIntOrNull() ?: 2

        HeuristicSettings.velocityWeight =
            binding.editWeightVelocity.text.toString().toIntOrNull() ?: 3

        HeuristicSettings.spoofWeight =
            binding.editWeightSpoof.text.toString().toIntOrNull() ?: 5

        HeuristicSettings.warnThreshold =
            binding.editWarnThreshold.text.toString().toIntOrNull() ?: 4

        HeuristicSettings.blockThreshold =
            binding.editBlockThreshold.text.toString().toIntOrNull() ?: 7

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

        Toast.makeText(
            this,
            "VERDICT = ${result.verdict}",
            Toast.LENGTH_LONG
        ).show()

        binding.textVerdict.text = "${result.verdict} (score=${result.score})"
        binding.textDetails.text = result.details.joinToString(separator = "\n")
        updateLogText()

        if (HudSettings.isHudEnabled(this)) {
            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                putExtra(CallActivity.EXTRA_CALLER_NUMBER, rawNumber)
                putExtra(CallActivity.EXTRA_CALLER_NAME, cnam)
                putExtra(
                    CallActivity.EXTRA_VERDICT,
                    result.verdict.name
                )
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
