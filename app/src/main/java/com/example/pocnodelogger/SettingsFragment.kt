package com.example.pocnodelogger

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etCustomPlatform = view.findViewById<EditText>(R.id.etCustomPlatform)
        val btnAddPlatform = view.findViewById<Button>(R.id.btnAddPlatform)
        val tvPlatformList = view.findViewById<TextView>(R.id.tvPlatformList)

        updatePlatformListText(tvPlatformList)

        btnAddPlatform.setOnClickListener {
            val name = etCustomPlatform.text.toString().trim()
            if (name.isNotEmpty()) {
                val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                val currentSet = prefs.getStringSet("custom_platforms", mutableSetOf()) ?: mutableSetOf()
                val newSet = currentSet.toMutableSet()
                newSet.add(name)
                prefs.edit().putStringSet("custom_platforms", newSet).apply()

                etCustomPlatform.text.clear()
                updatePlatformListText(tvPlatformList)
                Toast.makeText(requireContext(), "已新增平台：$name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePlatformListText(textView: TextView) {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val customSet = prefs.getStringSet("custom_platforms", emptySet()) ?: emptySet()
        val allPlatforms = listOf("Foodpanda", "Uber Eats") + customSet.toList() + listOf("其他")
        textView.text = "目前可用平台：\n" + allPlatforms.joinToString("、")
    }
}
