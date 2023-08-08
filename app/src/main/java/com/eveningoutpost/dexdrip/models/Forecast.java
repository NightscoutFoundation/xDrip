package com.eveningoutpost.dexdrip.models;

import android.util.Log;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by jamorham on 08/02/2016.
 */
public class Forecast {

    private static final String TAG = "jamorham forecast";
    // from stackoverflow.com/questions/17592139/trend-lines-regression-curve-fitting-java-library

    public interface TrendLine {
        void setValues(double[] y, double[] x); // y ~ f(x)

        double predict(double x); // get a predicted y for a given x

        double errorVarience();
    }

    public abstract static class OLSTrendLine implements TrendLine {

        RealMatrix coef = null; // will hold prediction coefs once we get values
        Double last_error_rate = null;

        protected abstract double[] xVector(double x); // create vector of values from x

        protected abstract boolean logY(); // set true to predict log of y (note: y must be positive)


        @Override
        public void setValues(double[] y, double[] x) {
            if (x.length != y.length) {
                throw new IllegalArgumentException(String.format("The numbers of y and x values must be equal (%d != %d)", y.length, x.length));
            }
            double[][] xData = new double[x.length][];
            for (int i = 0; i < x.length; i++) {
                // the implementation determines how to produce a vector of predictors from a single x
                xData[i] = xVector(x[i]);
            }
            if (logY()) { // in some models we are predicting ln y, so we replace each y with ln y
                y = Arrays.copyOf(y, y.length); // user might not be finished with the array we were given
                for (int i = 0; i < x.length; i++) {
                    y[i] = Math.log(y[i]);
                }
            }
            final OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
            ols.setNoIntercept(true); // let the implementation include a constant in xVector if desired
            ols.newSampleData(y, xData); // provide the data to the model
            coef = MatrixUtils.createColumnRealMatrix(ols.estimateRegressionParameters()); // get our coefs
            last_error_rate = ols.estimateErrorVariance();
            Log.d(TAG, getClass().getSimpleName() + " Forecast Error rate: errorvar:"
                    + JoH.qs(last_error_rate, 4)
                    + " regssionvar:" + JoH.qs(ols.estimateRegressandVariance(), 4)
                    + "  stderror:" + JoH.qs(ols.estimateRegressionStandardError(), 4));
        }

        @Override
        public double predict(double x) {
            double yhat = coef.preMultiply(xVector(x))[0]; // apply coefs to xVector
            if (logY()) yhat = (Math.exp(yhat)); // if we predicted ln y, we still need to get y
            return yhat;
        }

        public static double[] toPrimitive(Double[] array) {
            if (array == null) {
                return null;
            } else if (array.length == 0) {
                return new double[0];
            }
            final double[] result = new double[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return result;
        }

        public static double[] toPrimitiveFromList(Collection<Double> array) {
            if (array == null) {
                return null;
            }

            return toPrimitive(array.toArray(new Double[array.size()]));
        }

        public double errorVarience() {
            return last_error_rate;
        }

    }

    public static class PolyTrendLine extends OLSTrendLine {
        final int degree;

        public PolyTrendLine(int degree) {
            if (degree < 0)
                throw new IllegalArgumentException("The degree of the polynomial must not be negative");
            this.degree = degree;
        }

        protected double[] xVector(double x) { // {1, x, x*x, x*x*x, ...}
            double[] poly = new double[degree + 1];
            double xi = 1;
            for (int i = 0; i <= degree; i++) {
                poly[i] = xi;
                xi *= x;
            }
            return poly;
        }

        @Override
        protected boolean logY() {
            return false;
        }
    }

    public static class ExpTrendLine extends OLSTrendLine {
        @Override
        protected double[] xVector(double x) {
            return new double[]{1, x};
        }

        @Override
        protected boolean logY() {
            return true;
        }
    }

    public static class PowerTrendLine extends OLSTrendLine {
        @Override
        protected double[] xVector(double x) {
            return new double[]{1, Math.log(x)};
        }

        @Override
        protected boolean logY() {
            return true;
        }
    }

    public static class LogTrendLine extends OLSTrendLine {
        @Override
        protected double[] xVector(double x) {
            return new double[]{1, Math.log(x)};
        }

        @Override
        protected boolean logY() {
            return false;
        }
    }
}