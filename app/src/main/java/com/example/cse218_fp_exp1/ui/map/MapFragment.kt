package com.example.cse218_fp_exp1.ui.map

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.Requirement
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.proximity_sdk.api.*
import com.example.cse218_fp_exp1.databinding.FragmentMapBinding
import kotlin.math.min
import kotlin.random.Random


class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private var handler: ProximityObserver.Handler? = null
    private var observer: ProximityObserver? = null
    // Set this to your app id/token from cloud.estimote website
    private val cloudCredentials = EstimoteCloudCredentials("beacon-test-eric-chs" , "a0545bb6c94e40729210f58edf665bd2")

    // map of beacon IDs to the distances
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
        "90fe98ec293340f451d32480c3fe262a" to (3.0 to 0.0), // E
    )
    private var beaconBounds: Pair<Pair<Double, Double>, Pair<Double, Double>> = (0.0 to 3.0) to (0.0 to 3.0)

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var draw: MyDrawable? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        draw = MyDrawable()
        draw!!.setupSelf(this)

        binding.buttonCenter.setOnClickListener {
            // findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            draw!!.centered = true
            binding.imageFirst.setImageDrawable(draw)
            binding.imageFirst.invalidate()
        }

        binding.buttonAbsolute.setOnClickListener {
            // findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            draw!!.centered = false
            binding.imageFirst.setImageDrawable(draw)
            binding.imageFirst.invalidate()
        }

        binding.buttonNewPoints.setOnClickListener {
            //findNavController().navigate(R.id.action_map_to_dashboard)
            binding.imageFirst.setImageDrawable(draw)
            binding.imageFirst.invalidate()
        }

        binding.imageFirst.setImageDrawable(draw)
        binding.imageFirst.invalidate()

        // Create estimote monitoring
        // use helper factory to ask for required permissions
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        draw = null
        // stop the observation handler
        if (handler != null ) {
            Log.e("estimote", "Stop observing")
            handler!!.stop()
        }
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
                    // TODO update here
                    var currentDistances: MutableMap<String, Double?> = mutableMapOf()
                    for (entry in distances.entries.iterator()) {
                        var (_, ds) = entry.value
                        if (ds.isEmpty()){
                            currentDistances[entry.key] = null
                        } else {
                            currentDistances[entry.key] = ds.min() - 0.5
                        }
                    }
                    draw!!.userPos = calculateCoordinate(currentDistances)
                    binding.imageFirst.setImageDrawable(draw)
                    binding.imageFirst.invalidate()
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

    class MyDrawable : Drawable() {
        var userPos: Pair<Double, Double> = 0.0 to 0.0
        var centered: Boolean = false
        var frag: MapFragment? = null
        var scale: Double? = null
        var center: Pair<Double, Double>? = null

        private val redPaint: Paint = Paint().apply { setARGB(255, 255, 0, 0) }
        private val blackPaint: Paint = Paint().apply { setARGB(255, 0, 0, 0) }
        private val bluePaint: Paint = Paint().apply { setARGB(255, 0, 100, 255) }

        fun setupSelf(f: MapFragment) {
            frag = f
        }

        private fun translatePosition(point: Pair<Double, Double>): Pair<Double, Double> {
            // unpack pair
            val (x, y) = point
            return translatePosition(x, y)
        }

        private fun translatePosition(x: Double, y: Double): Pair<Double, Double> {
            val width: Double = bounds.width().toDouble()
            val height: Double = bounds.height().toDouble()

            if (scale == null || center == null) {
                var (hlower, hupper) = frag!!.beaconBounds.first
                var (vlower, vupper) = frag!!.beaconBounds.second
                center = (hupper + hlower)/2 to (vupper + vlower)/2

                if (width < height) {
                    scale = width * 0.8/(hupper + hlower)
                } else {
                    scale = height * 0.8/(vupper + vlower)
                }
            }

            var (xOut, yOut) = center!!

            if (centered){
                // adjust based on users distance from center
                xOut -=  (userPos.first - width/2)
                yOut -= (userPos.second - height/2)
            } else {
                // absolute
                xOut = width/2 - (center!!.first - x) * scale!!
                yOut = height/2 - (center!!.second - y) * scale!!
            }
            // return
            return xOut to yOut
        }

        override fun draw(canvas: Canvas) {
            //println("starting draw")
            canvas.drawColor(Color.LTGRAY)
            // Get the drawable's bounds
            val width: Int = bounds.width()
            val height: Int = bounds.height()
            val radius: Float = min(width, height).toFloat() / 50

            // Draw a red circle in the center mark user position

            val (x, y) = translatePosition(userPos)
            blackPaint.textSize = 48f
            canvas.drawText("${x.toInt()}, ${y.toInt()}", width.toFloat()/2, height.toFloat()/2, blackPaint)
            canvas.drawText("${userPos.first.toInt()}, ${userPos.second.toInt()}", width.toFloat()/2, height.toFloat()/2 + 100, blackPaint)


            //println("user: $x, $y")

            canvas.drawCircle(x.toFloat(), y.toFloat(), radius, redPaint)

            for (point in frag!!.beacons.values) {
                val (x, y) = translatePosition(point)
                canvas.drawCircle(x.toFloat(), y.toFloat(), radius, bluePaint)
            }
        }

        override fun setAlpha(alpha: Int) {
            // This method is required
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            // This method is required
        }

        override fun getOpacity(): Int =
            // Must be PixelFormat.UNKNOWN, TRANSLUCENT, TRANSPARENT, or OPAQUE
            PixelFormat.OPAQUE
    }
}