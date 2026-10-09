package com.zerostudio.lite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ToolsFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private var mediator: TabLayoutMediator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        val categories = ToolCategory.values()

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = categories.size
            override fun createFragment(position: Int): Fragment {
                return ToolCategoryFragment.newInstance(categories[position])
            }
        }

        mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = categories[position].title
        }
        mediator?.attach()
    }

    override fun onResume() {
        super.onResume()
        refreshPages()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshPages()
        }
    }

    private fun refreshPages() {
        childFragmentManager.fragments.forEach { f ->
            if (f is ToolCategoryFragment) {
                f.onResume()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediator?.detach()
        mediator = null
    }
}
