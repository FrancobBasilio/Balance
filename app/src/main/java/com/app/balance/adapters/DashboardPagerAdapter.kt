package com.app.balance.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.balance.ui.AhorrosFragment
import com.app.balance.ui.GastosFragment
import com.app.balance.ui.IngresosFragment

class DashboardPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GastosFragment()
            1 -> IngresosFragment()
            2 -> AhorrosFragment()
            else -> GastosFragment()
        }
    }
}