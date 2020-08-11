package com.eveningoutpost.dexdrip.utils.math

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.QRDecomposition
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

const val DEFAULT_TICKS_PER_MINUTE = 1000.0 * 60
const val LAG_TOLERANCE = 0.5

class AdaptiveSavitzkyGolay @JvmOverloads constructor(
        val lag: Int,
        val polynomialOrder: Int,
        val curvaturePenalty: Double = 0.0,
        val weightedAverageFraction: Double = 0.0,
        val weightedAverageHorizon: Int = 15,
        val ticksPerUnitTime: Double = DEFAULT_TICKS_PER_MINUTE
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

    private fun calculateCoefficients(x: DoubleArray, w: DoubleArray? = null) : DoubleArray {

        var A = Array2DRowRealMatrix(x.size,polynomialOrder + 1)

        if (w != null) {
            for (i in x.indices) {
                for (j in 0..polynomialOrder)
                    A.setEntry(i,j,x[i].pow(j) * w[i])
            }
        } else {
            for (i in x.indices) {
                for (j in 0..polynomialOrder)
                    A.setEntry(i,j,x[i].pow(j))
            }
        }

        val AT = A.transpose()
        val ATA = AT.multiply(A)

        val QR = QRDecomposition(ATA)
        val ATA_inv = QR.solver.inverse

        val coefficients = AT.preMultiply(ATA_inv.getRow(0))

        w?.forEachIndexed { i, w -> coefficients[i] *= w }

        return coefficients
    }

    fun calculateWeightedAverage() : Double {
        val horizon = rawMeasurements.last().time - weightedAverageHorizon * ticksPerUnitTime
        val slope = 1.0 / horizon
        var sum = 0.0
        var weights = 0.0
        for (v in rawMeasurements) {
            val weight = max(0.0,slope * (v.time - horizon))
            sum += weight * v.glucose
            weights += weight
        }
        return sum / weights
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
        var y = rawMeasurements.map{ it.glucose }.toDoubleArray()

        var w : DoubleArray? = null

        val asgValue = if (curvaturePenalty > 0) {
            val (w,t,y) = calculateCurvaturePenalizedData(t,y)

            // calculate scalar product
            calculateCoefficients(t,w).asSequence().zip(y.asSequence()) { c,y -> c*y }.sum()
        } else {
            // calculate scalar product
            calculateCoefficients(t,w).asSequence().zip(y.asSequence()) { c,y -> c*y }.sum()
        }

        return if (weightedAverageFraction > 0.0) {
            val weightedAverageValue = calculateWeightedAverage()
            weightedAverageFraction * weightedAverageValue + (1.0 - weightedAverageFraction) * asgValue
        } else {
            asgValue
        }

    }

}