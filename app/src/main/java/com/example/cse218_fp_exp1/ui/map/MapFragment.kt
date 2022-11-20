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
    private var distances: Map<String, Pair<String, MutableSet<Double>>> = mapOf(
        "687572e4da15128f8cc1096f874d1a37" to ("L" to mutableSetOf<Double>()), // L
        "d1610eab3fc6f11a0de3a9924280393d" to ("I" to mutableSetOf<Double>()), // I
        "257356716b5cd63031e00e52664b2114" to ("N" to mutableSetOf<Double>()), // N
        "90fe98ec293340f451d32480c3fe262a" to ("E" to mutableSetOf<Double>()), // E
    )

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
            draw!!.randomizePoints()
            binding.imageFirst.setImageDrawable(draw)
            binding.imageFirst.invalidate()
        }

        draw!!.randomizePoints()
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
                    .withBalancedPowerMode()
                    .build()

                var zones: MutableList<ProximityZone> = mutableListOf()

                // Build the zone you want to observer
                for (i in 1 .. 20) {
                    var zone = createZone(index = i, step = 1.0)
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

    private fun createZone(index: Int, step: Double = 0.5) : ProximityZone{
        var range = index * step
        var zone = ProximityZoneBuilder()
            .forTag("Eric") // change this tag to the tag you added to your estimotes on cloud.estimote

            .inCustomRange(range) // range to be considered in the range of the beacons
            .onEnter { ctx: ProximityZoneContext ->
                Log.e("estimote", ">>>>> ENTERED ${ctx.tag} $range meters, ${ctx.deviceId}")
            }
            .onExit {ctx: ProximityZoneContext ->
                Log.e("estimote", ">>>>> EXITED ${ctx.tag} $range meters, ${ctx.deviceId}")
            }
            .onContextChange { s_ctx: Set<ProximityZoneContext> ->
                //Log.e("estimote", ">>>>> Changed ${s_ctx.size} beacons $range meters,")
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
                        ds.remove(range)
                    }

                    /*
                    if (ds.isEmpty()){

                        Log.e("estimote", "$name distance: ??? meters")
                    } else {
                        Log.e("estimote", "$name distance: ${ds.min()} meters")
                    }
                    */
                }
                println()

            }
            .build()

        return zone
    }

    class MyDrawable : Drawable() {
        private var numPoints: Int = 20
        private var points: Array<Pair<Float, Float>?> = arrayOfNulls(numPoints)

        private var userPos: Pair<Float, Float> = 0f to 0f
        var centered: Boolean = true

        private val redPaint: Paint = Paint().apply { setARGB(255, 255, 0, 0) }
        private val blackPaint: Paint = Paint().apply { setARGB(255, 0, 0, 0) }

        private fun randomizeUserPos() {
            userPos = Random.nextFloat() * bounds.width().toFloat() to Random.nextFloat() * bounds.height().toFloat()
        }
        fun randomizePoints() {
            randomizeUserPos()
            for (i in 0 until numPoints) {
                // Add randomly placed dots
                val x = Random.nextFloat() * bounds.width().toFloat()
                val y = Random.nextFloat() * bounds.height().toFloat()
                points[i] = x to y
            }
        }

        private fun translatePosition(point: Pair<Float, Float>): Pair<Float, Float> {
            // unpack pair
            val (x, y) = point
            return translatePosition(x, y)
        }

        private fun translatePosition(x: Float, y: Float): Pair<Float, Float> {
            val width: Float = bounds.width().toFloat()
            val height: Float = bounds.height().toFloat()

            var xOut = x
            var yOut = y

            if (centered){
                // adjust based on users distance from center
                xOut -=  (userPos.first - width/2)
                yOut -= (userPos.second - height/2)
            }
            // return
            return xOut to yOut
        }

        override fun draw(canvas: Canvas) {
            //println("starting draw")
            canvas.drawColor(Color.GREEN)
            // Get the drawable's bounds
            val width: Int = bounds.width()
            val height: Int = bounds.height()
            val radius: Float = min(width, height).toFloat() / 33f

            // Draw a red circle in the center mark user position

            val (x, y) = translatePosition(userPos)
            //println("user: $x, $y")

            canvas.drawCircle(x, y, radius, redPaint)

            blackPaint.apply { setARGB(255, 0, 0, 0) }
            var i = 1
            for (point in points) {
                // Add randomly placed dots
                val (xf, yf) = translatePosition(point!!)
                //println("point $i: $xf, $yf")
                canvas.drawCircle(xf, yf, radius, blackPaint)
                blackPaint.apply { setARGB(255, 50*i, 50*i, 50*i) }
                i++
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