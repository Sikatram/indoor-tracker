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
    private var zone: ProximityZone? = null
    // Set this to your app id/token from cloud.estimote website
    private val cloudCredentials = EstimoteCloudCredentials("beacon-test-eric-chs" , "a0545bb6c94e40729210f58edf665bd2")

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
            onRequirementsFulfilled= fun () {
                // create observer from our credentials
                observer = ProximityObserverBuilder(requireContext(), cloudCredentials)
                    .onError { throwable ->
                        Log.e("estimote", "proximity observer error: $throwable")
                    }
                    .withBalancedPowerMode()
                    .build()

                // Build the zone you want to observer
                zone = ProximityZoneBuilder()
                    .forTag("Eric") // change this tag to the tag you added to your estimotes on cloud.estimote
                    .inCustomRange(3.0) // range to be considered in the range of the beacons?
                    .onEnter (
                        // function that's run when you enter within range of a tag, not when you enter in range of a beacon
                        fun (ctx: ProximityZoneContext){
                            Log.e("estimote", ">>>>> ENTERED ${ctx.tag} ${ctx.deviceId}")
                        }
                    )
                    .onExit (
                        // function that's run when you exit  range of a tag, not when you exit range of a beacon
                        fun (ctx: ProximityZoneContext) {
                            Log.e("estimote", ">>>>> EXITED ${ctx.tag} ${ctx.deviceId}")
                        }
                    )
                    .onContextChange (
                        // function when # of beacons you are within range of changes
                        // will probably be our main function we will use
                        fun (ctxs: Set<ProximityZoneContext>) {
                            Log.e("estimote", ">>>>> Changed ${ctxs.size} beacons")
                            var iter = ctxs.iterator()
                            var ctx: ProximityZoneContext
                            while (iter.hasNext()) {
                                ctx = iter.next()
                                Log.e("estimote", "\t${ctx.tag} ${ctx.deviceId}")
                            }
                        }
                    )
                    .build()

                // start observing, save the handler for later so we can stop it in onDestroy
                handler = observer!!.startObserving(zone!!)
                Log.e("estimote", "Started Observing")
            },
            onRequirementsMissing = fun (missing: List<Requirement>) {Log.e("estimote", "Missing permissions $missing")},
            onError = fun (t: Throwable) {Log.e("estimote", "Error: ${t.message}")}
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