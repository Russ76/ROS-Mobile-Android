package com.schneewittchen.rosandroid.ui.fragments;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.Editable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.textfield.TextInputLayout;
import com.schneewittchen.rosandroid.R;
import com.schneewittchen.rosandroid.databinding.FragmentMasterBinding;
import com.schneewittchen.rosandroid.model.repositories.rosRepo.connection.ConnectionType;
import com.schneewittchen.rosandroid.utility.Utils;
import com.schneewittchen.rosandroid.viewmodel.MasterViewModel;

import java.util.ArrayList;
import java.util.Objects;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.3.0
 * @created on 10.01.2020
 * @updated on 05.10.2020
 * @modified by Nico Studt
 * @updated on 16.11.2020
 * @modified by Nils Rottmann
 */
public class MasterFragment extends Fragment implements TextView.OnEditorActionListener {

    private static final String TAG = MasterFragment.class.getSimpleName();

    private MasterViewModel mViewModel;
    private FragmentMasterBinding binding;

    private ArrayList<String> ipItemList;
    protected AutoCompleteTextView ipAddressField;
    protected TextInputLayout ipAddressLayout;
    private ArrayAdapter<String> ipArrayAdapter;

    public static MasterFragment newInstance() {
        return new MasterFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMasterBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        updateMasterDetails();
        binding = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(MasterViewModel.class);

        // Define Views --------------------------------------------------------------
        ipAddressField = getView().findViewById(R.id.ipAddessTextView);
        ipAddressLayout = getView().findViewById(R.id.ipAddessLayout);

        ipItemList = new ArrayList<>();
        ipArrayAdapter = new ArrayAdapter<>(this.getContext(),
                R.layout.dropdown_menu_popup_item, ipItemList);
        ipAddressField.setAdapter(ipArrayAdapter);

        String firstDeviceIp = mViewModel.getIPAddress();
        if (firstDeviceIp != null) {
            ipAddressField.setText(firstDeviceIp, false);
        }

        ipAddressField.setOnClickListener(clickedView -> {
            updateIpSpinner();
            ipAddressField.showDropDown();
        });

        ipAddressLayout.setEndIconOnClickListener(v -> {
            ipAddressField.requestFocus();
            ipAddressField.callOnClick();
        });

        ipAddressField.setOnItemClickListener((parent, view, position, id) -> {
            ipAddressField.clearFocus();
        });

        // View model connection -------------------------------------------------------------------

        mViewModel.getMaster().observe(getViewLifecycleOwner(), master -> {
            if (master == null) {
                binding.masterIpEditText.getText().clear();
                binding.masterPortEditText.getText().clear();
                return;
            }

            binding.masterIpEditText.setText(master.ip);
            binding.masterPortEditText.setText(String.valueOf(master.port));
        });

        mViewModel.getCurrentNetworkSSID().observe(getViewLifecycleOwner(),
                networkSSID -> binding.NetworkSSIDText.setText(networkSSID));

        mViewModel.getRosConnection().observe(getViewLifecycleOwner(), this::setRosConnection);

        // User input ------------------------------------------------------------------------------

        binding.connectButton.setOnClickListener(v -> {
                updateMasterDetails();
                mViewModel.setMasterDeviceIp(ipAddressField.getText().toString());
                mViewModel.connectToMaster();
        });
        binding.disconnectButton.setOnClickListener(v -> mViewModel.disconnectFromMaster());
        binding.helpButton.setOnClickListener(v -> showConnectionHelpDialog());
        binding.masterIpEditText.setOnEditorActionListener(this);
        binding.masterPortEditText.setOnEditorActionListener(this);
    }

    private void updateIpSpinner() {
        ipItemList = new ArrayList<>();
        ipItemList = mViewModel.getIPAddressList();
        ipArrayAdapter.clear();
        ipArrayAdapter.addAll(ipItemList);
    }

    private void showConnectionHelpDialog() {
        String[] items = getResources().getStringArray(R.array.connection_checklist);

        new MaterialAlertDialogBuilder(this.requireContext())
                .setTitle(R.string.connection_checklist_title)
                .setItems(items, null)
                .show();
    }

    private void setRosConnection(ConnectionType connectionType) {
        int connectVisibility = View.INVISIBLE;
        int disconnectVisibility = View.INVISIBLE;
        int pendingVisibility = View.INVISIBLE;
        String statustext = getContext().getString(R.string.connected);

        if (connectionType == ConnectionType.DISCONNECTED
                || connectionType == ConnectionType.FAILED) {
            connectVisibility = View.VISIBLE;
            statustext = getContext().getString(R.string.disconnected);

        } else if (connectionType == ConnectionType.CONNECTED) {
            disconnectVisibility = View.VISIBLE;

        } else if (connectionType == ConnectionType.PENDING) {
            pendingVisibility = View.VISIBLE;
            statustext = getContext().getString(R.string.pending);
        }

        if (connectionType == ConnectionType.FAILED) {
            showConnectionHelpDialog();
        }

        binding.statusText.setText(statustext);
        binding.connectedImage.setVisibility(disconnectVisibility);
        binding.disconnectedImage.setVisibility(connectVisibility);
        binding.connectButton.setVisibility(connectVisibility);
        binding.disconnectButton.setVisibility(disconnectVisibility);
        binding.pendingBar.setVisibility(pendingVisibility);
    }

    private void updateMasterDetails() {
        // Update master IP
        Editable masterIp = binding.masterIpEditText.getText();

        if (masterIp != null) {
            mViewModel.setMasterIp(masterIp.toString());
        }

        // Update master port
        Editable masterPort = binding.masterPortEditText.getText();

        if (masterPort != null && masterPort.length() > 0) {
            mViewModel.setMasterPort(masterPort.toString());
        }
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_DONE:
            case EditorInfo.IME_ACTION_NEXT:
            case EditorInfo.IME_ACTION_PREVIOUS:
                updateMasterDetails();

                view.clearFocus();
                Utils.hideSoftKeyboard(view);

                return true;
        }

        return false;
    }
}
