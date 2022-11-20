package com.example.cse218_fp_exp1.ui.notifications

import android.app.Activity
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.estimote.internal_plugins_api.scanning.ScanHandler
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.Requirement
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.proximity_sdk.api.*
import com.example.cse218_fp_exp1.databinding.FragmentNotificationsBinding


class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private var observer: ProximityObserver? = null
    private var zone: ProximityZone? = null
    private var handler: ProximityObserver.Handler? = null
    private val cloudCredentials = EstimoteCloudCredentials("beacon-test-eric-chs" , "a0545bb6c94e40729210f58edf665bd2")
    private var telemetryFullScanHandler: ScanHandler? = null
    private var textView: TextView? = null
    private var refreshButton: Button? = null
    private var lastUpdate: Long = 0

    private var distances: Map<String, Pair<String, MutableSet<Double>>> = mapOf(
        "687572e4da15128f8cc1096f874d1a37" to ("L" to mutableSetOf<Double>(-1.0)), // L
        "d1610eab3fc6f11a0de3a9924280393d" to ("I" to mutableSetOf<Double>(-1.0)), // I
        "257356716b5cd63031e00e52664b2114" to ("N" to mutableSetOf<Double>(-1.0)), // N
        "90fe98ec293340f451d32480c3fe262a" to ("E" to mutableSetOf<Double>(-1.0)), // E
    )
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

        textView = binding.textNotifications
        refreshButton = binding.refreshLocations
        refreshButton!!.setOnClickListener {
            var text = ""
            for (entry in distances.entries.iterator()) {
                var (name, ds) = entry.value
                if (ds.isEmpty()){
                    text += "Beacon: $name Distance: ??? meters\n"
                } else {
                    text += "Beacon: $name Distance: ${ds.min()} meters\n"
                }

            }
            textView!!.text = text
        }
        /*
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
         */
        observer = ProximityObserverBuilder(requireContext(), cloudCredentials)
            .onError { throwable ->
                Log.e("estimote", "proximity observer error: $throwable")
            }
            .withBalancedPowerMode()
            .build()

        RequirementsWizardFactory.createEstimoteRequirementsWizard().fulfillRequirements(
            requireActivity(),
            // function that runs when we have required permissions
            {
                // create observer from our credentials
                observer = ProximityObserverBuilder(requireContext(), cloudCredentials)
                    .onError { throwable ->
                        Log.e("estimote", "proximity observer error: $throwable")
                    }
                    .withEstimoteSecureMonitoringDisabled()
                    .withTelemetryReportingDisabled()
                    .withAnalyticsReportingDisabled()
                    .withLowLatencyPowerMode()
                    .build()

                var zones: MutableList<ProximityZone> = mutableListOf()

                // Build the zone you want to observer
                for (i in 0 .. 15) {
                    var zone = createZone(index = i, step = 1.0, offset = 1.0)
                    zones.add(zone)
                }

                // start observing, save the handler for later so we can stop it in onDestroy
                handler = observer!!.startObserving(zones)
                Log.e("estimote", "Started Observing")
            },
            { missing: List<Requirement> ->
                Log.e("estimote", "Missing permissions $missing")
            },
            { t: Throwable ->
                Log.e("estimote", "Error: ${t.message}")
            }
        )
        Log.e("estimote", "Started Observing")
        return root
    }
    private fun createZone(index: Int, step: Double = 1.0, offset: Double = 0.0) : ProximityZone{
        var range = index * step + offset
        for (entry in distances.entries.iterator()) {
            var (_, ds) = entry.value
            ds.add(range)
        }
        var zone = ProximityZoneBuilder()
            .forTag("Eric") // change this tag to the tag you added to your estimotes on cloud.estimote

            .inCustomRange(range) // range to be considered in the range of the beacons
            .onEnter { ctx: ProximityZoneContext ->
                //Log.e("estimote", ">>>>> ENTERED ${ctx.tag} $range meters, ${ctx.deviceId}")
            }
            .onExit {ctx: ProximityZoneContext ->
                //Log.e("estimote", ">>>>> EXITED ${ctx.tag} $range meters, ${ctx.deviceId}")
            }
            .onContextChange { s_ctx: Set<ProximityZoneContext> ->
                var now = System.currentTimeMillis()
                if (now > lastUpdate + 100) {
                    lastUpdate = now
                    // Log.e("estimote", ">>>>> Changed ${s_ctx.size} beacons $range meters,")
                    var iter = s_ctx.iterator()
                    var ctx: ProximityZoneContext
                    var inRange: MutableSet<String> = mutableSetOf<String>()
                    while (iter.hasNext()) {
                        ctx = iter.next()
                        //Log.e("estimote", "\t${ctx.tag} ${ctx.deviceId}")
                        inRange.add(ctx.deviceId)
                    }

                    for (entry in distances.entries.iterator()) {
                        var (name, ds) = entry.value
                        if (inRange.contains(entry.key)) {
                            // within range meters of beacon
                            ds.add(range)
                        } else {
                            // outside range meters of beacon
                            for (i in ds.filter { it <= range }) {
                                ds.remove(i)
                            }
                        }
                        /*
                        if (ds.isEmpty()){
                            Log.e("estimote", "$name distance: ??? meters")
                        } else {
                            Log.e("estimote", "$name distance: ${ds.min()} meters")
                        }
                        */
                    }
                    //println()
                }
            }
            .build()
        return zone
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