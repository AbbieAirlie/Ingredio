package com.example.ingredio

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ingredio.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("IngredioSettings", Context.MODE_PRIVATE)
        val savedDays = sharedPref.getInt("notification_days_before", 1)

        binding.seekBarNotificationDays.progress = savedDays
        updateDaysText(savedDays)

        binding.seekBarNotificationDays.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateDaysText(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.buttonSaveSettings.setOnClickListener {
            val days = binding.seekBarNotificationDays.progress
            with(sharedPref.edit()) {
                putInt("notification_days_before", days)
                apply()
            }
            Toast.makeText(context, "Settings saved: $days days before", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDaysText(days: Int) {
        binding.textViewDaysValue.text = if (days == 1) "1 day" else "$days days"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}