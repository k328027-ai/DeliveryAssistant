package com.example.pocnodelogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class StatsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val textView = TextView(requireContext()).apply {
            text = "📊 統計與歷史頁面\n（第二階段將於此開發完整歷史列表與當日總結報表）"
            textSize = 18f
            setPadding(48, 96, 48, 48)
        }
        return textView
    }
}
