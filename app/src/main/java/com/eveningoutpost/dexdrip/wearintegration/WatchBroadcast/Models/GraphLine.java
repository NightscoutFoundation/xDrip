package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.PointValue;

public class GraphLine implements Parcelable {
    public static final Creator<GraphLine> CREATOR = new Creator<GraphLine>() {

        @Override
        public GraphLine createFromParcel(Parcel source) {
            return new GraphLine(source);
        }

        @Override
        public GraphLine[] newArray(int size) {
            return new GraphLine[size];
        }
    };
    private List<GraphPoint> values;
    private int color;

    public GraphLine( ) {
        values = new ArrayList<>();
        color = 0;
    }

    public GraphLine(Line line) {
        values = new ArrayList<>();
        line.update(0);
        for (PointValue pointValue : line.getValues()) {
            values.add(new GraphPoint(pointValue.getX(), pointValue.getY()));
        }
        color = line.getColor();
    }

    public GraphLine(Parcel parcel) {
        values = parcel.readArrayList(GraphPoint.class.getClassLoader());
        color = parcel.readInt();
    }

    public List<GraphPoint> getValues() {
        return values;
    }

    public void setValues(List<GraphPoint> values) {
        this.values = values;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeList(values);
        parcel.writeInt(color);
    }
}
