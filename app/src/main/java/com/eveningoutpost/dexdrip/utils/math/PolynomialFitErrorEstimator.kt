package com.eveningoutpost.dexdrip.utils.math

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import java.lang.RuntimeException
import kotlin.math.pow

/**
 * General error estimator measuring a least-squares fit to a polynomial based on two separate
 * data sets.
 *
 * This error estimator takes a set of filtered measurements and fits a polynomial of [order] to
 * those data points. It then estimates the error variance of a second set of raw measurements
 * w.r.t. the fitted polynomial, optionally debiasing the raw data using the method given by [bias].
 * It returns the error variance as defined by dot(r,r) / (size(r) - order - 1), where r is the vector
 * of (possibly debiased) residuals of the raw data set when compared to the predicted values of the
 * fitted polynomial.
 *
 * This error estimator is derived from the one used for noise estimation in the Dexcom codepath and
 * uses the same [OLSMultipleLinearRegression] class. The original implementation is not well suited
 * to the Libre2 sensor, which produces a raw reading every minute, and the estimated noise for that
 * sensor should take the characteristics of the raw data into account. At the same time, the polynomial
 * approximation curve should be based on the filtered data (produced once every five minutes), as that
 * is the primary data presented to the user and possibly downstream apps like Nightscout or AAPS.
 *
 * The filtered data will typically exhibit some time lag when compared to the raw data, which artificially
 * inflates for rising / falling BG curves. In order to eliminate this artifical error, the algorithm
 * can optionally debias the residuals of the raw data by subtracting either the mean or the median
 * residual.
 */
class PolynomialFitErrorEstimator @JvmOverloads constructor(
        val order : Int,
        val bias : Bias = Bias.MEDIAN,
        val ticksPerUnitTime : Double = DEFAULT_TICKS_PER_MINUTE
) {

    init {
        if (order < 1) {
            throw IllegalArgumentException("polynomial order must be >= 1")
        }
    }

    enum class Bias {
        NONE, MEAN, MEDIAN
    }

    data class Measurement(val time: Long, val glucose: Double)

    private val rawMeasurements = mutableListOf<Measurement>()
    private val filteredMeasurements = mutableListOf<Measurement>()

    fun addRawMeasurement(time: Long, glucose: Double) {
        if (rawMeasurements.isNotEmpty() && time <= rawMeasurements.last().time) {
            TODO("error handling!")
        }
        rawMeasurements.add(Measurement(time,glucose))
    }

    fun addFilteredMeasurement(time: Long, glucose: Double) {
        if (filteredMeasurements.isNotEmpty() && time <= filteredMeasurements.last().time) {
            TODO("error handling!")
        }
        filteredMeasurements.add(Measurement(time,glucose))
    }

    private fun median(a : DoubleArray) : Double {
        val a_s = a.sortedArray()
        val midpoint = a_s.size / 2
        if (a_s.size % 2 == 0) {
            return 0.5 * (a_s[midpoint - 1] + a_s[midpoint])
        } else {
            return a_s[midpoint]
        }
    }

    private fun mean(a : DoubleArray) : Double {
        return a.sum() / a.size;
    }

    fun estimateError() : Double {

        val zeroTime = filteredMeasurements.first().time

        val t_f = filteredMeasurements.map{ (it.time - zeroTime) / ticksPerUnitTime }.toDoubleArray()
        val y_f = filteredMeasurements.map{ it.glucose }.toDoubleArray()

        var A = arrayOf<DoubleArray>()

        for (i in t_f.indices) {
            val row = DoubleArray(order)
            for (j in 1..order)
                row[j-1] = t_f[i].pow(j)
            A += row
        }

        // we have to skip the first column in the matrix and set isNoIntercept = false. That makes
        // the OLSMultipleLinearRegression class add the first intercept column (which is all 1s).
        // There is a bug in the validation code inside OLSMultipleLinearRegression that trips otherwise
        // if we have the minimum viable number of data points (order + 1).
        val ols = OLSMultipleLinearRegression()
        ols.isNoIntercept = false

        try {
            ols.newSampleData(y_f, A)

            val params = ols.estimateRegressionParameters()

            val predictions = DoubleArray(rawMeasurements.size)
            val t_r = rawMeasurements.map{ (it.time - zeroTime) / ticksPerUnitTime }.toDoubleArray()
            val y_r = rawMeasurements.map{ it.glucose }.toDoubleArray()

            for (i in predictions.indices) {
                predictions[i] = 0.0
                for (j in 0..order)
                    predictions[i] += params[j] * t_r[i].pow(j)
            }

            val r = y_r.zip(predictions) { y, p -> y - p }.toDoubleArray()
            val b = when(bias) {
                Bias.MEAN -> mean(r)
                Bias.MEDIAN -> median(r)
                else -> 0.0
            }

            for (i in r.indices)
                r[i] -= b

            // formula copied from OLS.estimateErrorVariance()
            return r.asSequence().map{ it * it }.sum() / (r.size - order - 1)

        } catch (e : RuntimeException) {
            return Double.NaN;
        }

    }
}
