package com.eveningoutpost.dexdrip;

/**
 * Created by Emma Black on 1/7/15.
 */
public class BgWatchData implements Comparable<BgWatchData>{
    public double sgv;
    public double high;
    public double low;
    public double timestamp;

    public BgWatchData(double aSgv, double aHigh, double aLow, double aTimestamp) {
        this.sgv = aSgv;
        this.high = aHigh;
        this.low = aLow;
        this.timestamp = aTimestamp;
    }

    @Override
    public boolean equals(Object that){
        if(! (that instanceof BgWatchData)){
            return  false;
        }
        return this.timestamp == ((BgWatchData) that).timestamp;
    }

    @Override
    public int hashCode(){
        return (int) (timestamp%Integer.MAX_VALUE);
    }

    @Override
    public int compareTo(BgWatchData that) {
        // reverse order to get latest first
        if(this.timestamp < that.timestamp) return 1;
        if(this.timestamp > that.timestamp) return  -1;
        return 0;
    }
}
