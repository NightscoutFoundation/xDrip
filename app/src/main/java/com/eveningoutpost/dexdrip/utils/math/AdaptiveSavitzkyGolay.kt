package com.eveningoutpost.dexdrip.utils.math

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.pow

const val DEFAULT_TICKS_PER_MINUTE = 1000.0 * 60
const val LAG_TOLERANCE = 0.5

class AdaptiveSavitzkyGolay @JvmOverloads constructor(
        val lag: Int,
        val polynomialOrder: Int,
        val curvaturePenalty: Double = 0.0,
        val ticksPerUnitTime : Double = DEFAULT_TICKS_PER_MINUTE
) {

    data class RawMeasurement(val time: Long, val glucose: Double)

    private val rawMeasurements = mutableListOf<RawMeasurement>()

    val measurementCount get() = if (curvaturePenalty > 0) maxOf(rawMeasurements.size - 1,0) else rawMeasurements.size

    fun addMeasurement(time: Long, glucose: Double) {
        if (rawMeasurements.isNotEmpty() && time <= rawMeasurements.last().time) {
            TODO("error handling!")
        }
        rawMeasurements.add(RawMeasurement(time,glucose))
    }

    private data class WeightsResult(val w : DoubleArray, val t : DoubleArray, val y : DoubleArray)

    private fun calculateCurvaturePenalizedData(t: DoubleArray, y: DoubleArray) : WeightsResult {
        val dt = (t.asSequence() + sequenceOf(t.last() + 1.0)).zipWithNext { t1, t2 -> t2 - t1}
        val dy = (y.asSequence() + sequenceOf(y.last())).zipWithNext { y1, y2 -> y2 - y1}

        val dydt = dy.zip(dt){dy,dt -> dy/dt}

        val d_diff = dydt.zipWithNext { d1, d2 -> abs(d2 - d1) }.toList().toDoubleArray()
        val d_diff_max = d_diff.max() ?: Double.NaN

        val w = d_diff.map { it -> 1.0 - curvaturePenalty * it / d_diff_max}.toDoubleArray()
        w[w.size - 1] = 1.0

        return WeightsResult(
                w,
                t.drop(1).toDoubleArray(),
                y.drop(1).toDoubleArray()
        )
    }

    fun calculateCoefficients(x: DoubleArray, w: DoubleArray? = null) : DoubleArray {

        var A = arrayOf<DoubleArray>()

        if (w != null) {
            for (i in x.indices) {
                val row = DoubleArray(polynomialOrder + 1)
                for (j in 0..polynomialOrder)
                    row[j] = x[i].pow(j) * w[i]
                A += row
            }
        } else {
            for (i in x.indices) {
                val row = DoubleArray(polynomialOrder + 1)
                for (j in 0..polynomialOrder)
                    row[j] = x[i].pow(j)
                A += row
            }
        }

        val ols = OLSMultipleLinearRegression()
        ols.newSampleData(x,A)
        val coefficients = ols.estimateRegressionParameters()

        w?.forEachIndexed { i, w -> coefficients[i] *= w }

        return coefficients
    }

    private fun findZeroTime() : Long {
        val target = rawMeasurements.last().time - lag * ticksPerUnitTime
        for (m in rawMeasurements.asReversed()) {
            if (abs(m.time - target) < LAG_TOLERANCE * ticksPerUnitTime)
                return m.time
        }
        if (rawMeasurements.first().time > target)
            throw IllegalArgumentException("No raw measurements that happened before requested time lag (" + lag + "min)")
        return target.roundToLong()
    }

    fun estimateValue() : Double {

        if (lag >= measurementCount - 2) {
            throw IllegalStateException("Not enough measurements for specified lag")
        }

        if (polynomialOrder > measurementCount) {
            throw IllegalStateException("Not enough measurements for polynomial order")
        }

        val zeroTime = findZeroTime()
        var t = rawMeasurements.map{ (it.time - zeroTime) / ticksPerUnitTime }.toDoubleArray()
        var y = rawMeasurements.map { it.glucose }.toDoubleArray()

        var w : DoubleArray? = null

        if (curvaturePenalty > 0) {
            val (w,t,y) = calculateCurvaturePenalizedData(t,y)

            // calculate scalar products
            return calculateCoefficients(t,w).asSequence().zip(y.asSequence()) { c,y -> c*y }.sum();
        } else {
            // calculate scalar products
            return calculateCoefficients(t,w).asSequence().zip(y.asSequence()) { c,y -> c*y }.sum();
        }

    }

}