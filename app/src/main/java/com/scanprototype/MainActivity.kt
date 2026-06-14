package com.scanprototype

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import android.view.View
import com.scanprototype.scan.HeuristicSettings
import androidx.appcompat.app.AppCompatActivity
import com.scanprototype.databinding.ActivityMainBinding
import com.scanprototype.scan.CallEvent
import com.scanprototype.scan.executeSimulationPipeline
import com.scanprototype.scan.StorageLayer
import com.scanprototype.scan.DataNormalizer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hudEnabled = HudSettings.isHudEnabled(this)
        binding.switchHudToggle.isChecked = hudEnabled

        binding.switchHudToggle.setOnCheckedChangeListener { _, isChecked ->
            HudSettings.setHudMode(this, if (isChecked) HudMode.ANDROID_CALL_HUD else HudMode.OFF)
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

        val userNumber =
            binding.editUserNumber.text.toString().trim()

        if (userNumber.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter a user SIM number.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rawHour = binding.editTimestamp.text.toString().trim()

        val timestamp = if (rawHour.isEmpty()) {

            System.currentTimeMillis()

        } else {

            val hour = rawHour.toIntOrNull()

        if (hour == null || hour !in 0..23) {

            Toast.makeText(
                this,
                "Please enter an hour between 0 and 23.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

            val calendar = java.util.Calendar.getInstance()

            calendar.set(
                java.util.Calendar.HOUR_OF_DAY,
                hour
            )

            calendar.set(
                java.util.Calendar.MINUTE,
                0
            )

            calendar.set(
                java.util.Calendar.SECOND,
                0
            )

            calendar.set(
                java.util.Calendar.MILLISECOND,
                0
            )

            calendar.timeInMillis
        }

        if (userNumber.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter a user SIM number.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        StorageLayer.deviceNumber = DataNormalizer.normalize(userNumber)
        val normalizedCaller = DataNormalizer.normalize(rawNumber)

        val isWhitelisted =
            StorageLayer.savedContacts.contains(normalizedCaller)

        if (!isWhitelisted &&
            !normalizedCaller.matches(Regex("^639\\d{9}$"))
        ) {
            Toast.makeText(
                this,
                "Invalid phone number format.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }
        val cnam = binding.editCnam.text.toString().trim()
        val event = CallEvent(timestamp = timestamp, callerId = rawNumber, cnam = cnam)
        val result = executeSimulationPipeline(event)

        binding.textVerdict.text =
            "${result.verdict} (score=${result.score})"

        binding.textVerdict.setTextColor(
            when (result.verdict) {
                com.scanprototype.scan.Verdict.ALLOW ->
                    Color.parseColor("#4CAF50")

                com.scanprototype.scan.Verdict.WARN ->
                    Color.parseColor("#FF9800")

                com.scanprototype.scan.Verdict.BLOCK ->
                    Color.parseColor("#F44336")
            }
        )
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

    private fun updateLogText() {
        binding.textLog.text = StorageLayer.formatAuditLog()
    }
}
