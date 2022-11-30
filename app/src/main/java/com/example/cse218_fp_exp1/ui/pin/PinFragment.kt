package com.example.cse218_fp_exp1.ui.pin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.example.cse218_fp_exp1.databinding.FragmentPinBinding


class PinFragment : Fragment() {

    private var _binding: FragmentPinBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val pinViewModel =
            ViewModelProvider(this)[PinViewModel::class.java]

        _binding = FragmentPinBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.test.text = ".."

        setFragmentResultListener("map pin") { requestKey, bundle ->
            val result = bundle.getDoubleArray("user position")
            // Do something with the result
            binding.test.text = "${result!![0]}, ${result!![1]}"

        }


        return root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}