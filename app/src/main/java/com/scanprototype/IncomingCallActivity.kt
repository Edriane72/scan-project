package com.scanprototype

import android.view.View
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.scanprototype.databinding.ActivityIncomingCallBinding

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val number =
            intent.getStringExtra(EXTRA_CALLER_NUMBER).orEmpty()

        val name =
            intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty()

        val verdict =
            intent.getStringExtra(EXTRA_VERDICT)

        binding.textCallerName.text =
            if (name.isBlank()) number else name

        binding.textCallerNumber.text = number

        when (verdict) {

            "WARN" -> {
                showWarnPopup()
            }

            "BLOCK" -> {
                binding.buttonAnswer.visibility = View.GONE

                binding.buttonDecline.contentDescription = "Dismiss"

                showBlockedPopup()
            }
    }

        binding.buttonDecline.setOnClickListener {
            finish()
        }

        binding.buttonAnswer.setOnClickListener {

            val callIntent =
                Intent(this, CallActivity::class.java)

            callIntent.putExtra(
                CallActivity.EXTRA_CALLER_NUMBER,
                number
            )

            callIntent.putExtra(
                CallActivity.EXTRA_CALLER_NAME,
                name
            )

            startActivity(callIntent)

            finish()
        }
    }

    private fun showBlockedPopup() {
        AlertDialog.Builder(this)
            .setTitle("SCAN Protection")
            .setMessage(
                "Potential Malicious Caller.\n\nCall Blocked."
            )
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .show()
    }

    private fun showWarnPopup() {
        AlertDialog.Builder(this)
            .setTitle("SCAN Warning")
            .setMessage(
                "This caller has suspicious behavioral indicators.\n\nProceed with caution."
            )
            .setPositiveButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_VERDICT = "EXTRA_VERDICT"
    }
}