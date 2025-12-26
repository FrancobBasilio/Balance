package com.app.balance.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.app.balance.R

class AcercaFragment : Fragment(R.layout.fragment_acerca) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Buenas Prácticas
        setupExpandable(
            view.findViewById(R.id.mcv_practicas),
            view.findViewById(R.id.tv_practicas),
            view.findViewById(R.id.iv_practicas)
        )

        // Preguntas Frecuentes
        setupExpandable(
            view.findViewById(R.id.mcv_pf_1),
            view.findViewById(R.id.tv_pf_1),
            view.findViewById(R.id.iv_pf_1)
        )

        setupExpandable(
            view.findViewById(R.id.mcv_pf_2),
            view.findViewById(R.id.tv_pf_2),
            view.findViewById(R.id.iv_pf_2)
        )

        setupExpandable(
            view.findViewById(R.id.mcv_pf_3),
            view.findViewById(R.id.tv_pf_3),
            view.findViewById(R.id.iv_pf_3)
        )
    }

    private fun setupExpandable(header: LinearLayout, content: TextView, arrow: ImageView) {
        header.setOnClickListener {
            if (content.isVisible) {
                content.visibility = View.GONE
                arrow.animate().rotation(0f).setDuration(200).start()
            } else {
                content.visibility = View.VISIBLE
                arrow.animate().rotation(180f).setDuration(200).start()
            }
        }
    }
}
