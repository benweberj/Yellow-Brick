package com.benjweber.yellowbrick.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.benjweber.yellowbrick.MapActivity
import com.benjweber.yellowbrick.R
import kotlinx.android.synthetic.main.fragment_filters.*

class FiltersFragment : Fragment() {

    companion object {
        val TAG: String = FiltersFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filters , container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let {
            val adapter =
                ArrayAdapter.createFromResource(
                it,
                R.array.times,
                R.layout.simple_spinner_item
            )

            adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            spinnerTimeFilter.adapter = adapter

            spinnerTimeFilter.onItemSelectedListener = context as MapActivity
        }
    }
}