package com.example.cse218_fp_exp1.ui.notifications

import android.app.Notification
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.estimote.internal_plugins_api.scanning.ScanHandler
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.Requirement
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.proximity_sdk.api.*
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory
import com.example.cse218_fp_exp1.R
import com.example.cse218_fp_exp1.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private var observer: ProximityObserver? = null
    private var zone: ProximityZone? = null
    private var handler: ProximityObserver.Handler? = null
    private val cloudCredentials = EstimoteCloudCredentials("beacon-test-eric-chs" , "a0545bb6c94e40729210f58edf665bd2")
    private var telemetryFullScanHandler: ScanHandler? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this)[NotificationsViewModel::class.java]

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (handler != null) {
            handler!!.stop()
        }
        if (telemetryFullScanHandler != null) {
            telemetryFullScanHandler!!.stop()
        }

    }
}