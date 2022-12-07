package com.example.cse218_fp_exp1.ui.map

import android.content.Context.SENSOR_SERVICE
import android.graphics.*
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.Requirement
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.proximity_sdk.api.*
import com.example.cse218_fp_exp1.MainActivity
import com.example.cse218_fp_exp1.R
import com.example.cse218_fp_exp1.databinding.FragmentMapBinding
import com.example.cse218_fp_exp1.db.EmployeeEntity
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*

class Beacon(ID: String, Name: String, Position: Pair<Double, Double>) {
    var name: String = Name
    var distances: MutableSet<Double> = mutableSetOf(-1.0)
    var id: String = ID
    var position: Pair<Double, Double> = Position
}

var _DEBUG = true

class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private var handler: ProximityObserver.Handler? = null
    private var observer: ProximityObserver? = null
    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var northListener: NorthListener? = null

    // Set this to your app id/token from cloud.estimote website
    private val cloudCredentials = EstimoteCloudCredentials("beacon-test-eric-chs" , "a0545bb6c94e40729210f58edf665bd2")

    // map of beacon IDs to the distances
    private var lastUpdate: Long = 0
    private var lastPositions: Queue<Pair<Double, Double>> = LinkedList()

    // TODO calibrate these per room
    var OFFSET_HEADING: Double = 30 * Math.PI/180
    private var beacons: Map<String, Beacon> = mapOf(
        "687572e4da15128f8cc1096f874d1a37" to Beacon("687572e4da15128f8cc1096f874d1a37", "L", (0.5 to 3.0)),
        "d1610eab3fc6f11a0de3a9924280393d" to Beacon("d1610eab3fc6f11a0de3a9924280393d", "I", (3.0 to 3.0)),
        "257356716b5cd63031e00e52664b2114" to Beacon("257356716b5cd63031e00e52664b2114", "N", (0.5 to 0.5)),
        "90fe98ec293340f451d32480c3fe262a" to Beacon("90fe98ec293340f451d32480c3fe262a", "E", (3.0 to 0.5)),
    )
    private var beaconBounds: Pair<Pair<Double, Double>, Pair<Double, Double>> = (0.0 to 3.5) to (0.0 to 3.5)
    // TODO end
    private var userHeading: Double = 0.0

    private val STEP: Double = 1.0/(3.toDouble())
    private val MAX_QUEUE_SIZE: Int = 16

    private var draw: MyDrawable? = null
    var pins: ArrayList<EmployeeEntity> = ArrayList()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

        northListener = NorthListener(this)
        sensorManager = (requireContext().getSystemService(SENSOR_SERVICE)) as? SensorManager
        if (sensorManager != null) {
            sensorManager!!.registerListener(
                northListener,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }


        val employeeDao = (requireActivity() as MainActivity).db!!.employeeDao()
        draw = MyDrawable()
        draw!!.setupSelf(this)
        lifecycleScope.launch {
            employeeDao.fetchAllEmployee().collect {
                pins = ArrayList(it)
            }
            binding.imageFirst.setImageDrawable(draw)
            binding.imageFirst.invalidate()
        }


        binding.buttonAbsolute.setOnClickListener {
            // findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            draw!!.centered = !draw!!.centered
            binding.buttonAbsolute.text = if (draw!!.centered) "Centered" else "Absolute"
            binding.imageFirst.setImageDrawable(draw)
            binding.imageFirst.invalidate()
        }

        binding.buttonPin.setOnClickListener {
            // findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            // check if user position is loaded
            val bundle = Bundle()
            val arr = DoubleArray(2)
            arr[0] = draw!!.userPos.first
            arr[1] = draw!!.userPos.second
            bundle.putDoubleArray("user position", arr)
            setFragmentResult("map pin", bundle)
            findNavController().navigate(R.id.action_map_to_pin)
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
                        Log.d("estimote", "proximity observer error: $throwable")
                    }
                    .withEstimoteSecureMonitoringDisabled()
                    .withTelemetryReportingDisabled()
                    .withAnalyticsReportingDisabled()
                    .withLowLatencyPowerMode()
                    .build()

                val zones: MutableList<ProximityZone> = mutableListOf()

                // Build the zone you want to observer
                for (i in 0 until (15/STEP).toInt()) {
                    zones.add(createZone(index = i, step = STEP, offset = STEP))
                }

                // start observing, save the handler for later so we can stop it in onDestroy
                handler = observer!!.startObserving(zones)
                Log.d("estimote", "Started Observing")
            },
            { missing: List<Requirement> ->
                Log.e("estimote", "Missing permissions $missing")
            },
            { t: Throwable ->
                //Log.e("estimote", "Error: ${t.message}")
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        draw = null
        // stop the observation handler
        if (handler != null ) {
            Log.d("estimote", "Stop observing")
            handler!!.stop()
        }
        if (sensorManager != null) {
            sensorManager!!.unregisterListener(northListener)
        }
    }

    override fun onPause() {
        super.onPause()
        // to stop the listener and save battery
        if (sensorManager != null) {
            sensorManager!!.unregisterListener(northListener)
        }
    }

    override fun onResume() {
        super.onResume()
        // code for system's orientation sensor registered listeners
        if (sensorManager != null) {
            sensorManager!!.registerListener(
                northListener,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun createZone(index: Int, step: Double = 1.0, offset: Double = 0.0) : ProximityZone{
        val range = index * step + offset
        for (beacon in beacons.values.iterator()) {
            beacon.distances.add(range)
        }

        val zone = ProximityZoneBuilder()
            .forTag("Eric") // change this tag to the tag you added to your estimotes on cloud.estimote
            .inCustomRange(range) // range to be considered in the range of the beacons
            .onEnter { ctx: ProximityZoneContext ->
                //Log.e("estimote", ">>>>> ENTERED ${ctx.tag} $range meters, ${ctx.deviceId}")
                beacons[ctx.deviceId]!!.distances.add(range)
                beacons[ctx.deviceId]!!.distances.add(range + STEP)

            }
            .onExit {ctx: ProximityZoneContext ->
                //Log.e("estimote", ">>>>> EXITED ${ctx.tag} $range meters, ${ctx.deviceId}")
                for (i in beacons[ctx.deviceId]!!.distances.filter { it < range}) {
                    beacons[ctx.deviceId]!!.distances.remove(i)
                }
            }
            .onContextChange { s_ctx: Set<ProximityZoneContext> ->
                try {
                    // Log.e("estimote", ">>>>> Changed ${s_ctx.size} beacons $range meters,")
                    val iter = s_ctx.iterator()
                    var ctx: ProximityZoneContext
                    val inRange: MutableSet<String> = mutableSetOf()
                    while (iter.hasNext()) {
                        ctx = iter.next()
                        //Log.e("estimote", "\t${ctx.tag} ${ctx.deviceId}")
                        inRange.add(ctx.deviceId)
                    }

                    for (beacon in beacons.values.iterator()) {
                        if (inRange.contains(beacon.id)) {
                            // within range meters of beacon
                            beacon.distances.add(range)
                            beacon.distances.add(range + STEP)
                        } else {
                            // outside range meters of beacon
                            for (i in beacon.distances.filter { it < range}) {
                                beacon.distances.remove(i)
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

                    val currentDistances: MutableMap<String, Double?> = mutableMapOf()
                    for (beacon in beacons.values.iterator()) {
                        if (beacon.distances.isEmpty()){
                            currentDistances[beacon.id] = null
                        } else {
                            currentDistances[beacon.id] = beacon.distances.min() - STEP
                        }
                     }
                    var tempPos = calculateCoordinate(currentDistances)
                    tempPos = (
                        min(max(beaconBounds.first.first, tempPos.first), beaconBounds.first.second) to
                        min(max(beaconBounds.second.first, tempPos.second), beaconBounds.second.second)
                    )

                    if (lastPositions.size > MAX_QUEUE_SIZE){
                        lastPositions.remove()
                    }
                    lastPositions.add(tempPos)
                    var avgX = 0.0
                    var avgY = 0.0
                    for (position in lastPositions.iterator()) {
                        avgX += position.first
                        avgY += position.second
                    }
                    draw!!.userPos = avgX/lastPositions.size to avgY/lastPositions.size

                    val now = System.currentTimeMillis()
                        if (now > lastUpdate + 125) {
                        lastUpdate = now
                        //binding.imageFirst.setImageDrawable(draw)
                        binding.imageFirst.invalidate()
                    }
                } catch (e: Exception){
                    Log.e("estimote", e.toString())
                }

            }
            .build()
        return zone
    }

    private fun calculateCoordinate(radius: Map<String, Double?>): Pair<Double, Double> {
        val beaconIDs = radius.keys.toList()
        // 012, 013, 023, 123
        var x = 0.0
        var y = 0.0
        var ratio = 0

        var m = mapOf(
            beaconIDs[0] to radius[beaconIDs[0]],
            beaconIDs[1] to radius[beaconIDs[1]],
            beaconIDs[2] to radius[beaconIDs[2]],
        )
        var p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
            ratio++
        }

        m = mapOf(
            beaconIDs[0] to radius[beaconIDs[0]],
            beaconIDs[1] to radius[beaconIDs[1]],
            beaconIDs[3] to radius[beaconIDs[3]],
        )
        p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
            ratio++
        }

        m = mapOf(
            beaconIDs[0] to radius[beaconIDs[0]],
            beaconIDs[3] to radius[beaconIDs[3]],
            beaconIDs[2] to radius[beaconIDs[2]],
        )
        p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
            ratio++
        }

        m = mapOf(
            beaconIDs[3] to radius[beaconIDs[3]],
            beaconIDs[1] to radius[beaconIDs[1]],
            beaconIDs[2] to radius[beaconIDs[2]],
        )
        p = threePointCalc(m)
        if (p != null) {
            x += p.first
            y += p.second
            ratio++
        }
        return x/ratio to y/ratio
    }

    private fun threePointCalc(beaconRadius: Map<String, Double?>): Pair<Double, Double>? {
        val idIter = beaconRadius.keys.iterator()
        var key = idIter.next()

        val (x1, y1) = beacons[key]!!.position
        val r1 = beaconRadius[key] ?: return null

        key = idIter.next()
        val (x2, y2) = beacons[key]!!.position
        val r2 = beaconRadius[key]?: return null


        key = idIter.next()
        val (x3, y3) = beacons[key]!!.position
        val r3 = beaconRadius[key]?: return null

        val A = 2*x2 - 2*x1
        val B = 2*y2 - 2*y1
        val C = r1*r1 - r2*r2 - x1*x1 + x2*x2 - y1*y1 + y2*y2
        val D = 2*x3 - 2*x2
        val E = 2*y3 - 2*y2
        val F = r2*r2 - r3*r3 - x2*x2 + x3*x2 - y2*y2 + y3*y3
        val x = (C*E - F*B) / (E*A - B*D)
        val y = (C*D - A*F) / (B*D - A*E)
        return (
            min(max(beaconBounds.first.first, x), beaconBounds.first.second) to
            min(max(beaconBounds.second.first, y), beaconBounds.second.second)
        )
    }

    class MyDrawable : Drawable() {
        var userPos: Pair<Double, Double> = -69.0 to -69.0
        var centered: Boolean = false
        private var frag: MapFragment? = null
        private var scale: Double? = null
        private var center: Pair<Double, Double>? = null
        private var heading: Double = 0.0

        private val redPaint: Paint = Paint().apply { setARGB(255, 255, 0, 0) }
        private val blackPaint: Paint = Paint().apply { setARGB(255, 0, 0, 0); textSize=48f }
        private val bluePaint: Paint = Paint().apply { setARGB(255, 0, 120, 255) }
        private val greenPaint: Paint = Paint().apply { setARGB(255, 20, 220, 50) }

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
                val (hlower, hupper) = frag!!.beaconBounds.first
                val (vlower, vupper) = frag!!.beaconBounds.second
                center = (hupper + hlower)/2 to (vupper + vlower)/2

                scale = if (width < height) {
                    width * 0.8/(hupper + hlower)
                } else {
                    height * 0.8/(vupper + vlower)
                }
            }

            var xOut: Double
            var yOut: Double

            if (centered){
                // adjust based on users distance from center
                // use user position as center
                val tempXOut = width/2 - (userPos.first - x) * scale!!
                val tempYOut = height/2 - (userPos.second - y) * scale!!

                /*
                // rotate around users position
                val cHeading = cos(heading)
                val sHeading = sin(heading)
                val xUser = width/2
                val yUser = height/2
                xOut = ((tempXOut-xUser) * cHeading) - ((yUser-tempYOut) * sHeading) + xUser
                xOut = abs((width-xOut))
                yOut = ((yUser-tempYOut) * cHeading) - ((tempXOut-xUser) * sHeading) + yUser
                 */
                xOut = tempXOut
                yOut = tempYOut
            } else {
                // absolute, use center of bounds as center
                xOut = width/2 - (center!!.first - x) * scale!!
                yOut = height/2 - (center!!.second - y) * scale!!
            }
            // return
            return xOut to yOut
        }

        override fun draw(canvas: Canvas) {
            //println("starting draw")
            // cache heading value incase it changes
            heading = frag!!.userHeading
            canvas.drawColor(Color.LTGRAY)
            // Get the drawable's bounds
            val width: Int = bounds.width()
            val height: Int = bounds.height()
            val radius: Float = min(width, height).toFloat() / 50

            // Draw a red circle in the center mark user position
            val (x, y) = translatePosition(userPos)
            val userX = x; val userY = y
            // TODO Draw debug text
            if (_DEBUG) {
                canvas.drawText("${x.toInt()}, ${y.toInt()}", width.toFloat()/2, height.toFloat()/2, blackPaint)
                canvas.drawText("${String.format("%.2f", userPos.first)}, ${String.format("%.2f", userPos.second)}", width.toFloat()/2, height.toFloat()/2 + 60, blackPaint)
            }
            canvas.drawCircle(x.toFloat(), y.toFloat(), radius*1.5f, redPaint)

            // TODO Drawing beacons

            var i = 2
            for (beacon in frag!!.beacons.values.iterator()) {
                val device = beacon.name
                val point = beacon.position
                val (x, y) = translatePosition(point)
                //Log.e("estimote", "$x, $y")
                canvas.drawCircle(x.toFloat(), y.toFloat(), radius/2, bluePaint)
                // TODO drawing debug text
                if (_DEBUG){
                    canvas.drawText("$device: ${beacon.distances.min()}m", width.toFloat()/4, height.toFloat()/2 + 60*i, blackPaint)
                    canvas.drawText(beacon.name, x.toFloat(), y.toFloat(), blackPaint)
                }
                i+=1

            }
            //Log.e("estimote", "${bounds.width()}, ${bounds.height()}")

            for (pin in frag!!.pins) {
                val (x, y) = translatePosition(pin.xCoordinate.toDouble(), pin.yCoordinate.toDouble())
                canvas.drawRect(
                    RectF(x.toFloat(), y.toFloat(), (x + radius*2).toFloat(), (y + radius*2).toFloat()),
                    greenPaint
                )
                canvas.drawText(pin.name, (x-radius).toFloat(), (y+radius).toFloat(), blackPaint)
            }

            // draw direction facing
            // rotate around users position (center)
            val cHeading = cos((heading + frag!!.OFFSET_HEADING) % 360)
            val sHeading = sin((heading+ frag!!.OFFSET_HEADING) % 360)
            val tempXOut = userX
            val tempYOut = userY + 40
            var xOut = ((tempXOut-userX) * cHeading) - ((tempYOut-userY) * sHeading) + userX
            val yOut = ((tempYOut-userY) * cHeading) - ((tempXOut-userX) * sHeading) + userY
            //Log.e("estimote", "\n${frag!!.userHeading}\n${xOut.toFloat()}, ${yOut.toFloat()}")
            blackPaint.strokeWidth = 10f
            canvas.drawLine(userX.toFloat(), userY.toFloat(), xOut.toFloat(), yOut.toFloat(), blackPaint)

        }

        override fun setAlpha(alpha: Int) {
            // This method is required
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            // This method is required
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int =
            // Must be PixelFormat.UNKNOWN, TRANSLUCENT, TRANSPARENT, or OPAQUE
            PixelFormat.OPAQUE
    }

    class NorthListener(fragment: MapFragment): SensorEventListener {
        private val frag: MapFragment = fragment
        override fun onSensorChanged(event: SensorEvent) {
            frag.userHeading = (event.values[0].toDouble() - 180) * Math.PI/180
            //Log.e("estimote", "heading ${event.values[0]}")
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        }

    }
}