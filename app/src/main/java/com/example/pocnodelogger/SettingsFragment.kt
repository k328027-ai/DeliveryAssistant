package com.example.pocnodelogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val textView = TextView(requireContext()).apply {
            text = "⚙️ 設定頁面\n（第三階段將於此開發基準時薪與預設平台設定）"
            textSize = 18f
            setPadding(48, 96, 48, 48)
        }
        return textView
    }
}
