package com.example.cse218_fp_exp1.ui.notifications

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

    private var beacons: Map<String, Pair<Double, Double>> = mapOf(
        "687572e4da15128f8cc1096f874d1a37" to (0.0 to 3.0), // L
        "d1610eab3fc6f11a0de3a9924280393d" to (3.0 to 3.0), // I
        "257356716b5cd63031e00e52664b2114" to (0.0 to 0.0), // N
        "90fe98ec293340f451d32480c3fe262a" to (2.0 to 0.0), // E
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
            var currentDistances: MutableMap<String, Double?> = mutableMapOf()
            for (entry in distances.entries.iterator()) {
                var (name, ds) = entry.value
                if (ds.isEmpty()){
                    text += "Beacon: $name Distance: ??? meters\n"
                    currentDistances[entry.key] = null
                } else {
                    text += "Beacon: $name Distance: ${ds.min()} meters\n"
                    currentDistances[entry.key] = ds.min() -1
                }
            }
            val (x, y) = calculateCoordinate(currentDistances)
            text += "Coords: $x, $y"
            textView!!.text = text
        }
        /*
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
         */

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
                for (i in 0 .. 20) {
                    var zone = createZone(index = i, step = 0.5, offset = 0.5)
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
                    var (_, ds) = entry.value
                    if (inRange.contains(entry.key)) {
                        // within range meters of beacon
                        ds.add(range - step)
                    } else {
                        // outside range meters of beacon
                        for (i in ds.filter { it <= range-step}) {
                            ds.remove(i )
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
                var now = System.currentTimeMillis()
                if (now > lastUpdate + 500) {
                    lastUpdate = now
                    refreshButton!!.text = now.toString()
                }
            }
            .build()
        return zone
    }

    private fun calculateCoordinate(radius: Map<String, Double?>): Pair<Double, Double> {
        var beaconIDs = radius.keys.toList()
        // 012, 013, 023, 123
        var x: Double = 0.0
        var y: Double = 0.0

        var m = mapOf<String, Double?>(
            beaconIDs[0] to radius[beaconIDs[0]],
            beaconIDs[1] to radius[beaconIDs[1]],
            beaconIDs[2] to radius[beaconIDs[2]],
        )
        var p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
        }

        m = mapOf<String, Double?>(
            beaconIDs[0] to radius[beaconIDs[0]],
            beaconIDs[1] to radius[beaconIDs[1]],
            beaconIDs[3] to radius[beaconIDs[3]],
        )
        p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
        }

        m = mapOf<String, Double?>(
            beaconIDs[0] to radius[beaconIDs[0]],
            beaconIDs[3] to radius[beaconIDs[3]],
            beaconIDs[2] to radius[beaconIDs[2]],
        )
        p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
        }

        m = mapOf<String, Double?>(
            beaconIDs[3] to radius[beaconIDs[3]],
            beaconIDs[1] to radius[beaconIDs[1]],
            beaconIDs[2] to radius[beaconIDs[2]],
        )
        p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
        }
        return x/4 to y/4
    }

    private fun threePointCalc(beaconRadius: Map<String, Double?>): Pair<Double, Double>? {
        val idIter = beaconRadius.keys.iterator()
        var key = idIter.next()

        val (x1, y1) = beacons[key]?: (-1.0 to -1.0)
        val r1 = beaconRadius[key] ?: return null

        key = idIter.next()
        val (x2, y2) = beacons[key]?: (-1.0 to -1.0)
        val r2 = beaconRadius[key]?: return null


        key = idIter.next()
        val (x3, y3) = beacons[key]?: (-1.0 to -1.0)
        val r3 = beaconRadius[key]?: return null

        val A = 2*x2 - 2*x1
        val B = 2*y2 - 2*y1
        val C = r1*r1 - r2*r2 - x1*x1 + x2*x2 - y1*y1 + y2*y2
        val D = 2*x3 - 2*x2
        val E = 2*y3 - 2*y2
        val F = r2*r2 - r3*r3 - x2*x2 + x3*x2 - y2*y2 + y3*y3
        val x = (C*E - F*B) / (E*A - B*D)
        val y = (C*D - A*F) / (B*D - A*E)
        return x to y

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