package com.benjweber.yellowbrick.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.benjweber.yellowbrick.MapActivity
import com.benjweber.yellowbrick.R
import com.benjweber.yellowbrick.YBApp
import com.google.android.gms.dynamic.SupportFragmentWrapper
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.fragment_filters.*

class FiltersFragment : Fragment() {
    val TAG: String = FiltersFragment::class.java.simpleName

    companion object {
        val TAG: String = FiltersFragment::class.java.simpleName
        const val OUT_TIMES_SELECTION = "OUT_TIMES_SELECTION"
        const val OUT_TYPES_SELECTION = "OUT_TYPES_SELECTION"
    }

    private var timesSelection = 0
    private var typesSelection = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            with (savedInstanceState) {
                timesSelection = getInt(OUT_TIMES_SELECTION)
                typesSelection = getInt(OUT_TYPES_SELECTION)
            }
        } else {
            val timesSelectionArgument = arguments?.getInt(OUT_TIMES_SELECTION)
            val typesSelectionArgument = arguments?.getInt(OUT_TYPES_SELECTION)

            timesSelectionArgument?.let {
                timesSelection = it
            }

            typesSelectionArgument?.let {
                typesSelection = it
            }

        }
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
        btnToFiltered.setOnClickListener { fragmentManager?.popBackStack() }
        context?.let {

            //Set up time range spinner

            val timesAdapter = ArrayAdapter.createFromResource (
                it,
                R.array.times,
                R.layout.simple_spinner_item
            )

            timesAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            spinnerTimeFilter.adapter = timesAdapter

            spinnerTimeFilter.onItemSelectedListener = context as MapActivity
            spinnerTimeFilter.setSelection(timesSelection)

            // Set up crime type spinner

            val typesAdapter = ArrayAdapter.createFromResource (
                it,
                R.array.crimeTypes,
                R.layout.simple_spinner_item
            )

            typesAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            spinnerTypeFilter.adapter = typesAdapter

            spinnerTypeFilter.onItemSelectedListener = context as MapActivity
            spinnerTypeFilter.setSelection(typesSelection)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(OUT_TIMES_SELECTION, spinnerTimeFilter.selectedItemPosition)
        outState.putInt(OUT_TYPES_SELECTION, spinnerTypeFilter.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }
}