package com.eveningoutpost.dexdrip.cgm.glupro;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import com.eveningoutpost.dexdrip.BR;
import com.eveningoutpost.dexdrip.MegaStatus;
import com.eveningoutpost.dexdrip.R;

import lombok.Setter;
import lwld.glucose.profile.iface.Device;
import lwld.glucose.profile.iface.State;
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter2.ItemBinding;

/**
 * JamOrHam
 * <p>
 * Glucose Profile shared view model
 */
public class ViewModel {

    private static final String TAG = "GluProViewModel";
    public final ObservableArrayList<Device> scannedDevices = new ObservableArrayList<>();
    public final ObservableBoolean scanning = new ObservableBoolean(false);
    public final ObservableField<State> lastState = new ObservableField<>();

    public final GluProAdapterChain<Device> adapter = new GluProAdapterChain<>();
    public final ItemBinding<Device> itemBinding = ItemBinding.<Device>of(BR.device, R.layout.item_glupro_device).bindExtra(BR.viewModel, this);

    public interface ServiceCallback {
        // when the user selects a device or null for full scan
        void onDeviceSelected(String name, String address);

        // when pin value is supplied
        void onPin(String pin);
    }

    public interface ActivityCallback {
        // close activity
        void finish();
    }

    @Setter
    private volatile ServiceCallback serviceCallback;

    @Setter
    private volatile ActivityCallback activityCallback;


    // selecting a device
    public void select(String name, String address) {
        Log.d(TAG, "selected " + address);
        if (serviceCallback != null) {
            serviceCallback.onDeviceSelected(name, address);
            if (address != null) {
                activityCallback.finish();
                activityCallback = null;
                MegaStatus.startStatus(MegaStatus.GLU_PRO);
            }
        }
    }

    // pin entry field changed
    public void onPinChange(CharSequence pin) {
        // TODO validate as digits etc
        if (pin.length() == 6) {
            Log.d(TAG, "Pin changed to " + pin);
            if (serviceCallback != null) {
                serviceCallback.onPin(pin.toString());
            }
        }
    }

    // for storing recycler view if needed
    public void initRecycler(View v) {
        if (v instanceof RecyclerView) {
            Log.d(TAG, "initRecycler");
        }
    }


    public static class GluProAdapterChain<T> extends BindingRecyclerViewAdapter<T> {

        @Override
        public ViewDataBinding onCreateBinding(LayoutInflater inflater, @LayoutRes int layoutId, ViewGroup viewGroup) {
            //Log.d(TAG, "onCreateBinding");
            try {
                return super.onCreateBinding(inflater, layoutId, viewGroup);
            } catch (Exception e) {
                Log.d(TAG, "onCreateBinding exception: " + e.getMessage());
                return null;
            }
        }

        @Override
        public void onBindBinding(ViewDataBinding binding, int bindingVariable, @LayoutRes int layoutId, int position, T item) {
            //Log.d(TAG, "onBindBinding "+position+" "+item.toString());
            try {
                super.onBindBinding(binding, bindingVariable, layoutId, position, item);
            } catch (Exception e) {
                Log.d(TAG, "onBindBinding exception: " + e.getMessage());
            }
        }
    }

}
