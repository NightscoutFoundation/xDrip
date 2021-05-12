package com.eveningoutpost.dexdrip.UtilityModels;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.support.annotation.StringRes;
import android.view.View;

import com.eveningoutpost.dexdrip.BR;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.databinding.DialogTreeSelectorBinding;
import com.eveningoutpost.dexdrip.utils.DexCollectionHelper;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.List;

import me.tatarka.bindingcollectionadapter2.ItemBinding;

import static com.eveningoutpost.dexdrip.ui.helpers.UiHelper.convertDpToPixel;

/**
 * Created by jamorham on 20/12/2017.
 *
 * Guides the user through the process of selecting a source from a hierarchical menu.
 *
 */

import static com.eveningoutpost.dexdrip.xdrip.gs;
public class SourceWizard {
    @SuppressLint("StaticFieldLeak")
    private static final String TAG = "SourceWizard";
    private static volatile SourceWizard sw;
    private AlertDialog dialog;
    private Activity activity;
    // Create the dialog decision tree
    private Tree<Item> root = new Tree<>(new Item(gs(R.string.choose_data_source), gs(R.string.which_system_do_you_use)));

    {
        Tree<Item> g5g6 = root.addChild(new Item("G4, G5 & G6", gs(R.string.which_type_of_device), R.drawable.g5_icon));
        {
            Tree<Item> g4 = g5g6.addChild(new Item("G4", gs(R.string.what_type_of_g4_bridge_device_do_you_use), R.drawable.g4_icon));
            {
                Tree<Item> wixel = g4.addChild(new Item(gs(R.string.bluetooth_wixel), gs(R.string.which_software_is_the_wixel_running), R.drawable.wixel_icon));
                {
                    wixel.addChild(new Item(gs(R.string.xbridge_compatible), DexCollectionType.DexbridgeWixel, R.drawable.wixel_icon));
                    wixel.addChild(new Item(gs(R.string.classic_simple), DexCollectionType.BluetoothWixel, R.drawable.wixel_icon));
                }

                g4.addChild(new Item(gs(R.string.g4_share_receiver), DexCollectionType.DexcomShare, R.drawable.g4_share_icon));
                g4.addChild(new Item(gs(R.string.parakeet_wifi), DexCollectionType.WifiWixel, R.drawable.jamorham_parakeet_marker));
            }
            g5g6.addChild(new Item("G5", DexCollectionType.DexcomG5, R.drawable.g5_icon));
            g5g6.addChild(new Item("G6", DexCollectionType.DexcomG6, R.drawable.g6_icon));
        }

        Tree<Item> libre = root.addChild(new Item(gs(R.string.libre), gs(R.string.what_type_of_libre_bridge_device_do_you_use), R.drawable.libre_icon_image));
        {
            libre.addChild(new Item(gs(R.string.bluetooth_bridge_device_blucon_limitter_bluereader_tomato_etc), DexCollectionType.LimiTTer, R.drawable.bluereader_icon));
            libre.addChild(new Item(gs(R.string.librealarm_app_using_sony_smartwatch), DexCollectionType.LibreAlarm, R.drawable.ic_watch_grey600_48dp));
            libre.addChild(new Item(gs(R.string.libre_patched), DexCollectionType.LibreReceiver, R.drawable.libre_icon_image));

        }
        Tree<Item> other = root.addChild(new Item(gs(R.string.other), gs(R.string.which_type_of_device), R.drawable.wikimedia_question_mark));
        {
            other.addChild(new Item("640G / 670G", DexCollectionType.NSEmulator, R.drawable.mm600_series));
            other.addChild(new Item("Medtrum A6 / S7", DexCollectionType.Medtrum, R.drawable.a6_icon));
            other.addChild(new Item("Nightscout Follower", DexCollectionType.NSFollow, R.drawable.nsfollow_icon));
            other.addChild(new Item("Dex Share Follower", DexCollectionType.SHFollow, R.drawable.nsfollow_icon));
            //
            other.addChild(new Item("EverSense", DexCollectionType.NSEmulator, R.drawable.wikimedia_eversense_icon_pbroks13));
        }
    }


    public SourceWizard(Activity activity) {
        this.activity = activity;
    }

    public static void start(Activity activity) {
        start(activity, false);
    }

    public synchronized static void start(Activity activity, boolean force) {
        if (sw == null) sw = new SourceWizard(activity);
        if (force) {
            if (sw.showing()) sw.dismiss();
        }
        if (!sw.showing()) sw.getTreeDialog(null);
    }

    private static Tree<Item> findByName(Tree<Item> node, final String name) {

        if (compareName(node, name)) return node;

        final List<Tree<Item>> children = node.getChildren();
        for (Tree<Item> child : children) {
            final Tree<Item> result = findByName(child, name);
            if (result != null) return result;
        }
        return null;
    }

    private static boolean compareName(Tree<Item> node, String name) {
        return node.data.name.equals(name);
    }

    private static String gs(@StringRes final int id) {
        return xdrip.getAppContext().getString(id);
    }

    private boolean showing() {
        return (dialog != null) && dialog.isShowing();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception e) {
            //
        }
    }

    // display the dialog for the selected branch of the tree
    public void getTreeDialog(Tree<Item> branch) {
        if (branch == null) branch = root;

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        // family node
        if (!branch.data.isCollectionType()) {

            final DialogTreeSelectorBinding binding = DialogTreeSelectorBinding.inflate(activity.getLayoutInflater());
            binding.setViewModel(new ViewModel(branch));

            builder.setTitle(branch.data.name);
            builder.setMessage(branch.data.description);

            builder.setView(binding.getRoot());

            // leaf node
        } else {
            final Tree<Item> fbranch = branch;
            final Item item = branch.data;
            builder.setTitle(R.string.are_you_sure);
            builder.setMessage(activity.getString(R.string.set_data_source_to, item.name));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DexCollectionType.setDexCollectionType(item.getCollectionType());
                    dismissDialog();
                    DexCollectionHelper.assistance(activity, item.getCollectionType());
                    sw = null; // work here is done
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getTreeDialog(fbranch.getParent());
                }
            });

        }

        dismissDialog();
        dialog = builder.create();
        try {
            dialog.show();
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception when trying to show source wizard: " + e);
        }
    }

    private synchronized void dismissDialog() {
        if ((dialog != null) && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    // View model item
    public class Item {
        public String name;
        public int resource;
        String description;
        DexCollectionType type;

        Item(String name, String description) {
            this(name, description, 0);
        }

        Item(String name, String description, int resource) {
            this.name = name;
            this.description = description;
            this.resource = resource;
        }

        Item(String name, DexCollectionType type) {
            this(name, type, 0);
        }

        Item(String name, DexCollectionType type, int resource) {
            this.name = name;
            this.description = "^" + type.name();
            this.resource = resource;
            this.type = type;
        }

        public void onClick(View v) {
            getTreeDialog(findByName(root, this.name));
        }

        public boolean isShortName() {
            return this.name.length() < 10;
        }

        public float dpToPx(float size) {
            return convertDpToPixel(size);
        }

        boolean isCollectionType() {
            return this.description.startsWith("^");
        }

        public DexCollectionType getCollectionType() {
            if (isCollectionType()) {
                return type;
            } else {
                return null;
            }
        }
    }

    // View model container
    public class ViewModel {
        public final ObservableList<Item> items = new ObservableArrayList<>();
        public final ItemBinding<Item> itemBinding = ItemBinding.of(BR.item, R.layout.dialog_tree_item);
        private String parent_name = null;

        ViewModel(List<Item> items) {
            this.items.addAll(items);
        }

        ViewModel(Tree<Item> branch) {
            if (branch.getParent() != null) {
                parent_name = branch.getParent().data.name;
            }
            this.items.addAll(branch.getChildElements());
        }

        public boolean showBack() {
            return parent_name != null;
        }

        public void goBack(View v) {
            getTreeDialog(findByName(root, parent_name));
        }

    }

}
