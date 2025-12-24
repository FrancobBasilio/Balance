package com.app.balance.ui // Asegúrate que el package sea el correcto

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.app.balance.R
import com.google.android.material.card.MaterialCardView

class AcercaFragment : Fragment(R.layout.fragment_acerca) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  Buenas Prácticas
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

    // para despegable:
    // @param card La cabecera (tarjeta) que recibe el clic.
    // @param content El texto (TextView) que se mostrará u ocultará.
    //@param arrow La flecha (ImageView) que rotará.

    private fun setupExpandable(card: MaterialCardView, content: TextView, arrow: ImageView) {
        card.setOnClickListener {
            if (content.isVisible) {
                // Ocultar
                content.visibility = View.GONE
                arrow.animate().rotation(0f).setDuration(300).start()
            } else {
                // Mostrar
                content.visibility = View.VISIBLE
                arrow.animate().rotation(180f).setDuration(300).start()
            }
        }
    }
}